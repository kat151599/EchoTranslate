from __future__ import annotations

import os
import logging
import threading
import time
from dataclasses import dataclass, field
from typing import Any

import numpy as np

logger = logging.getLogger(__name__)

# PaddleOCR/PaddleX CPU fallback: disable oneDNN path that is problematic on
# some Windows + Paddle 3.x installations.
os.environ.setdefault("FLAGS_use_mkldnn", "0")


@dataclass
class OcrBlock:
    source: str
    confidence: float
    box: list[int]
    language: str
    raw_ids: list[int] = field(default_factory=list)


def merge_ocr_blocks(blocks: list[OcrBlock]) -> list[OcrBlock]:
    """Build reading order from immutable OCR detections, then wrap rows."""
    if not blocks:
        return blocks

    @dataclass(frozen=True)
    class RawBlock:
        raw_id: int
        text: str
        confidence: float
        box: tuple[int, int, int, int]
        language: str
        multiline: bool

    @dataclass(frozen=True)
    class VisualRow:
        raw_ids: tuple[int, ...]
        text: str
        confidence: float
        box: tuple[int, int, int, int]
        language: str
        is_speaker: bool = False

    # This is the merge boundary: do not mutate or reuse incoming OCR objects.
    # All row membership below refers exclusively to these original geometries.
    raw_input = tuple(
        (index, block.source, float(block.confidence), tuple(int(v) for v in block.box), block.language)
        for index, block in enumerate(blocks)
    )
    initial_heights = sorted(max(1, box[3] - box[1]) for _, _, _, box, _ in raw_input)
    # Lower median keeps one tall multiline detection from becoming the
    # baseline when the screenshot has only a few OCR fragments.
    initial_median = initial_heights[(len(initial_heights) - 1) // 2]
    ordinary_heights = sorted(
        max(1, box[3] - box[1])
        for _, _, _, box, _ in raw_input
        if box[3] - box[1] <= 1.6 * initial_median
    )
    typical_line_height = ordinary_heights[(len(ordinary_heights) - 1) // 2] if ordinary_heights else initial_median
    raw_blocks = tuple(
        RawBlock(raw_id, text, confidence, box, language, box[3] - box[1] > 1.6 * typical_line_height)
        for raw_id, text, confidence, box, language in raw_input
    )
    raw_by_id = {block.raw_id: block for block in raw_blocks}
    capture_left = min(block.box[0] for block in raw_blocks)
    capture_right = max(block.box[2] for block in raw_blocks)
    capture_width = max(1, capture_right - capture_left)

    for block in raw_blocks:
        logger.info(
            "OCR RAW id=%s text=%r box=%s height=%s",
            block.raw_id, block.text, list(block.box), block.box[3] - block.box[1],
        )
        if block.multiline:
            logger.info("OCR RAW MULTILINE id=%s text=%r box=%s", block.raw_id, block.text, list(block.box))

    def contains_cjk(text: str) -> bool:
        return any("\u4e00" <= char <= "\u9fff" or "\u3040" <= char <= "\u30ff" for char in text)

    def is_speaker_name(upper: VisualRow, lower: VisualRow) -> bool:
        ux1, uy1, ux2, uy2 = upper.box
        lx1, ly1, lx2, ly2 = lower.box
        upper_width = max(1, ux2 - ux1)
        lower_width = max(1, lx2 - lx1)
        gap = ly1 - uy2
        return (
            len(upper.text.strip()) <= 20
            and upper_width / lower_width < 0.45
            and -0.20 * typical_line_height <= gap <= 0.80 * typical_line_height
        )

    def is_wrapped_continuation(left: VisualRow, right: VisualRow) -> bool:
        lx1, ly1, lx2, ly2 = left.box
        rx1, ry1, rx2, ry2 = right.box
        lh, rh = max(1, ly2 - ly1), max(1, ry2 - ry1)
        line_height = min(lh, rh)
        vertical_gap = ry1 - ly2
        left_delta = abs(rx1 - lx1)
        overlap_x = max(0, min(lx2, rx2) - max(lx1, rx1))
        containment = overlap_x / min(max(1, lx2 - lx1), max(1, rx2 - rx1))
        return (
            left_delta <= 0.03 * capture_width
            and -0.25 * line_height <= vertical_gap <= 0.50 * line_height
            and containment >= 0.80
        )

    def same_visual_row(left: RawBlock, right: RawBlock) -> bool:
        # A tall original detection may already contain several visual lines.
        # Its whole bbox is never evidence that another detection is same-row.
        if left.multiline or right.multiline:
            return False
        lx1, ly1, lx2, ly2 = left.box
        rx1, ry1, rx2, ry2 = right.box
        lh, rh = max(1, ly2 - ly1), max(1, ry2 - ry1)
        vertical_overlap = max(0, min(ly2, ry2) - max(ly1, ry1))
        center_distance = abs((ly1 + ly2) / 2 - (ry1 + ry2) / 2)
        return (
            max(lh, rh) / min(lh, rh) <= 2.0
            and (
                vertical_overlap >= 0.70 * min(lh, rh)
                or center_distance <= 0.55 * ((lh + rh) / 2)
            )
        )

    def join_text(left: str, right: str) -> str:
        return left + right if contains_cjk(left) or contains_cjk(right) else left + " " + right

    # Phase 1: visual rows are classified entirely from original raw boxes.
    raw_rows: list[list[int]] = []
    for block in sorted(raw_blocks, key=lambda b: (b.box[1], b.box[0], b.raw_id)):
        row = next(
            (candidate for candidate in raw_rows if any(same_visual_row(raw_by_id[raw_id], block) for raw_id in candidate)),
            None,
        )
        if row is None:
            raw_rows.append([block.raw_id])
        else:
            row.append(block.raw_id)

    visual_rows: list[VisualRow] = []
    for row_ids in raw_rows:
        parts = sorted((raw_by_id[raw_id] for raw_id in row_ids), key=lambda b: (b.box[0], b.raw_id))
        text = ""
        for part in parts:
            text = part.text if not text else join_text(text, part.text)
        box = (
            min(part.box[0] for part in parts), min(part.box[1] for part in parts),
            max(part.box[2] for part in parts), max(part.box[3] for part in parts),
        )
        row = VisualRow(tuple(part.raw_id for part in parts), text, min(part.confidence for part in parts), box, parts[0].language)
        visual_rows.append(row)

    visual_rows.sort(key=lambda row: (row.box[1], row.box[0], row.raw_ids))
    for row_id, row in enumerate(visual_rows):
        logger.info("OCR ROW row=%s raw_ids=%s text=%r", row_id, list(row.raw_ids), row.text)

    # Speaker classification is complete before wrap assembly and is immutable.
    speaker_rows = {
        index for index in range(len(visual_rows) - 1)
        if is_speaker_name(visual_rows[index], visual_rows[index + 1])
    }
    visual_rows = [
        VisualRow(row.raw_ids, row.text, row.confidence, row.box, row.language, index in speaker_rows)
        for index, row in enumerate(visual_rows)
    ]
    for row in visual_rows:
        if row.is_speaker:
            logger.info("OCR SPEAKER raw_ids=%s text=%r", list(row.raw_ids), row.text)

    # Phase 2 only follows finalized rows. The wrap check always receives the
    # previous original row, never a bbox expanded by an earlier wrap.
    final_rows: list[VisualRow] = []
    wrap_tail: VisualRow | None = None
    for row in visual_rows:
        if wrap_tail and not wrap_tail.is_speaker and not row.is_speaker and is_wrapped_continuation(wrap_tail, row):
            upper = final_rows[-1]
            logger.info("OCR WRAP upper_row=%s lower_row=%s", list(wrap_tail.raw_ids), list(row.raw_ids))
            final_rows[-1] = VisualRow(
                upper.raw_ids + row.raw_ids,
                join_text(upper.text, row.text),
                min(upper.confidence, row.confidence),
                (min(upper.box[0], row.box[0]), min(upper.box[1], row.box[1]), max(upper.box[2], row.box[2]), max(upper.box[3], row.box[3])),
                upper.language,
            )
        else:
            final_rows.append(row)
        wrap_tail = row

    final = [OcrBlock(row.text, row.confidence, list(row.box), row.language, list(row.raw_ids)) for row in final_rows]
    for block in final:
        logger.info("OCR FINAL raw_ids=%s text=%r box=%s", list(block.raw_ids), block.source, block.box)
    return final


class PaddleOcrEngine:
    """Original PaddleOCR CPU backend kept as a reliable fallback."""

    name = "paddle_cpu"

    def __init__(self):
        self._engine = None
        self._lock = threading.RLock()
        self._last_diag: dict[str, Any] = {}

    def _get_engine(self):
        if self._engine is None:
            from paddleocr import PaddleOCR

            self._engine = PaddleOCR(
                use_doc_orientation_classify=False,
                use_doc_unwarping=False,
                use_textline_orientation=False,
                enable_mkldnn=False,
            )
        return self._engine

    @staticmethod
    def _to_plain_dict(result_obj):
        if isinstance(result_obj, dict):
            return result_obj
        try:
            return {k: result_obj[k] for k in ("rec_texts", "rec_scores", "rec_polys") if k in result_obj}
        except Exception:
            pass
        for attr in ("json", "to_json"):
            value = getattr(result_obj, attr, None)
            try:
                obj = value() if callable(value) else value
                if isinstance(obj, dict):
                    return obj.get("res", obj)
            except Exception:
                continue
        raise RuntimeError("Unsupported PaddleOCR result object")

    def recognize(self, frame_bgr: np.ndarray, source_lang: str, min_confidence: float) -> list[OcrBlock]:
        started = time.perf_counter()
        with self._lock:
            results = self._get_engine().predict(frame_bgr)

        blocks: list[OcrBlock] = []
        for result in results:
            data = self._to_plain_dict(result)
            texts = data.get("rec_texts") or []
            scores = data.get("rec_scores") or [1.0] * len(texts)
            polys = data.get("rec_polys") or []
            for i, text in enumerate(texts):
                text = str(text).strip()
                score = float(scores[i]) if i < len(scores) else 1.0
                if not text or score < min_confidence:
                    continue
                poly = polys[i] if i < len(polys) else None
                if poly is not None and len(poly):
                    arr = np.asarray(poly, dtype=float).reshape(-1, 2)
                    left, top = arr.min(axis=0)
                    right, bottom = arr.max(axis=0)
                    box = [int(left), int(top), int(right), int(bottom)]
                else:
                    box = [0, 0, int(frame_bgr.shape[1]), int(frame_bgr.shape[0])]
                blocks.append(OcrBlock(text, score, box, source_lang or "auto"))

        blocks.sort(key=lambda b: (b.box[1], b.box[0]))
        self._last_diag = {
            "backend": self.name,
            "provider": "PaddlePaddle CPU",
            "total_ocr_ms": round((time.perf_counter() - started) * 1000, 1),
            "blocks": len(blocks),
        }
        return blocks

    def diagnostics(self) -> dict[str, Any]:
        return dict(self._last_diag) or {"backend": self.name, "provider": "PaddlePaddle CPU"}


class RapidOcrDirectMLEngine:
    """RapidOCR PP-OCRv6 backend accelerated by ONNX Runtime DirectML.

    DirectML works with DirectX 12 GPUs, including AMD Radeon. RapidOCR 3.9+
    ships PP-OCRv6 ONNX models; the SMALL recognizer supports Chinese and
    Japanese in the same multilingual model.
    """

    name = "rapidocr_directml"

    def __init__(self):
        self._engine = None
        self._lock = threading.RLock()
        self._last_diag: dict[str, Any] = {}

    @staticmethod
    def _providers() -> list[str]:
        try:
            import onnxruntime as ort
            return list(ort.get_available_providers())
        except Exception:
            return []

    def _get_engine(self):
        if self._engine is not None:
            return self._engine

        providers = self._providers()
        if "DmlExecutionProvider" not in providers:
            raise RuntimeError(
                "DirectML is not available. Run INSTALL_RAPIDOCR_DIRECTML.bat and restart the server. "
                f"ONNX Runtime providers: {providers or 'none'}"
            )

        from rapidocr import EngineType, ModelType, OCRVersion, RapidOCR

        # PP-OCRv6 SMALL is multilingual and supports Chinese + Japanese.
        # Disable the 0/180-degree classifier for game screenshots: screen text
        # is normally upright and skipping this stage lowers latency.
        params = {
            "Global.use_cls": False,
            "Det.engine_type": EngineType.ONNXRUNTIME,
            "Rec.engine_type": EngineType.ONNXRUNTIME,
            "Det.model_type": ModelType.SMALL,
            "Rec.model_type": ModelType.SMALL,
            "Det.ocr_version": OCRVersion.PPOCRV6,
            "Rec.ocr_version": OCRVersion.PPOCRV6,
            "Rec.lang_type": "japan",
            "EngineConfig.onnxruntime.use_dml": True,
            # DirectML requires sequential execution and no memory pattern.
            # RapidOCR's ORT wrapper configures provider-specific session options.
            "EngineConfig.onnxruntime.intra_op_num_threads": 1,
            "EngineConfig.onnxruntime.inter_op_num_threads": 1,
        }
        self._engine = RapidOCR(params=params)
        try:
            warmup_image = np.zeros((64, 256, 3), dtype=np.uint8)
            self._engine(warmup_image, use_det=True, use_cls=False, use_rec=True)
        except Exception as exc:
            logging.getLogger(__name__).warning("RapidOCR warm-up failed: %s", exc)
        return self._engine

    def recognize(self, frame_bgr: np.ndarray, source_lang: str, min_confidence: float) -> list[OcrBlock]:
        started = time.perf_counter()
        with self._lock:
            # BGR ndarray is accepted by RapidOCR. The classifier is disabled to
            # avoid an extra model invocation on every phone screenshot.
            result = self._get_engine()(frame_bgr, use_det=True, use_cls=False, use_rec=True)

        raw_boxes = getattr(result, "boxes", None)
        raw_texts = getattr(result, "txts", None)
        raw_scores = getattr(result, "scores", None)
        boxes = tuple(raw_boxes) if raw_boxes is not None else ()
        texts = tuple(raw_texts) if raw_texts is not None else ()
        scores = tuple(raw_scores) if raw_scores is not None else ()

        blocks: list[OcrBlock] = []

        for i, text in enumerate(texts):
            text = str(text).strip()
            raw_score = scores[i] if i < len(scores) else None
            if raw_score is None:
                continue
            try:
                score = float(raw_score)
            except (TypeError, ValueError):
                continue
            if not text or score < min_confidence:
                continue

            poly = boxes[i] if i < len(boxes) else None
            if poly is not None and len(poly):
                arr = np.asarray(poly, dtype=float).reshape(-1, 2)
                left, top = arr.min(axis=0)
                right, bottom = arr.max(axis=0)
                box = [int(left), int(top), int(right), int(bottom)]
            else:
                box = [0, 0, int(frame_bgr.shape[1]), int(frame_bgr.shape[0])]
            blocks.append(OcrBlock(text, score, box, source_lang or "auto"))

        blocks.sort(key=lambda b: (b.box[1], b.box[0]))

        elapse_list = getattr(result, "elapse_list", None)
        diag: dict[str, Any] = {
            "backend": self.name,
            "provider": "DirectML",
            "available_providers": self._providers(),
            "total_ocr_ms": round((time.perf_counter() - started) * 1000, 1),
            "blocks": len(blocks),
        }
        if isinstance(elapse_list, (list, tuple)):
            # RapidOCR documents this as [det, cls, rec] seconds. With cls off,
            # versions may return 2 or 3 items; expose the raw values too.
            diag["rapidocr_stage_seconds"] = [
                None if v is None else round(float(v), 4) for v in elapse_list
            ]
            if len(elapse_list) == 3:
                detection_seconds, recognition_seconds = elapse_list[0], elapse_list[2]
            elif len(elapse_list) == 2:
                detection_seconds, recognition_seconds = elapse_list[0], elapse_list[1]
            else:
                detection_seconds = recognition_seconds = None
            if detection_seconds is not None:
                diag["detection_ms"] = round(float(detection_seconds) * 1000, 1)
            if recognition_seconds is not None:
                diag["recognition_ms"] = round(float(recognition_seconds) * 1000, 1)
        engine_elapsed = getattr(result, "elapse", None)
        if engine_elapsed is not None:
            try:
                diag["rapidocr_engine_ms"] = round(float(engine_elapsed) * 1000, 1)
            except (TypeError, ValueError):
                pass
        self._last_diag = diag
        return blocks

    def diagnostics(self) -> dict[str, Any]:
        base = {
            "backend": self.name,
            "provider": "DirectML",
            "available_providers": self._providers(),
        }
        base.update(self._last_diag)
        return base


class OcrEngineRouter:
    def __init__(self):
        self._engines = {
            "paddle_cpu": PaddleOcrEngine(),
            "rapidocr_directml": RapidOcrDirectMLEngine(),
        }
        self._active_name = "paddle_cpu"

    @staticmethod
    def _normalize_backend(value: str | None) -> str:
        value = str(value or "paddle_cpu").strip().lower()
        aliases = {
            "paddle": "paddle_cpu",
            "paddleocr": "paddle_cpu",
            "cpu": "paddle_cpu",
            "directml": "rapidocr_directml",
            "rapidocr": "rapidocr_directml",
            "rapidocr_dml": "rapidocr_directml",
        }
        return aliases.get(value, value)

    def recognize(
        self,
        frame_bgr: np.ndarray,
        source_lang: str,
        min_confidence: float,
        backend: str | None = None,
        fallback_to_paddle: bool = True,
    ) -> list[OcrBlock]:
        requested = self._normalize_backend(backend)
        engine = self._engines.get(requested)
        if engine is None:
            raise RuntimeError(f"Unknown OCR backend: {requested}")

        try:
            blocks = engine.recognize(frame_bgr, source_lang, min_confidence)
            self._active_name = requested
            return blocks
        except Exception as exc:
            if requested != "paddle_cpu" and fallback_to_paddle:
                fallback = self._engines["paddle_cpu"]
                blocks = fallback.recognize(frame_bgr, source_lang, min_confidence)
                self._active_name = "paddle_cpu"
                diag = fallback.diagnostics()
                diag["fallback_reason"] = str(exc)
                fallback._last_diag = diag
                return blocks
            raise

    def diagnostics(self) -> dict[str, Any]:
        return self._engines[self._active_name].diagnostics()

    def status(self) -> dict[str, Any]:
        return {
            "active_backend": self._active_name,
            "directml": self._engines["rapidocr_directml"].diagnostics(),
            "paddle": self._engines["paddle_cpu"].diagnostics(),
        }


ocr_engine = OcrEngineRouter()
