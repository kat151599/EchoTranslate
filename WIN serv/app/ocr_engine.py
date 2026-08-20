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


# === SEMANTIC_VISUAL_FRAGMENTS_V1 ===
def build_visual_fragments(
    blocks: list[OcrBlock],
    *,
    screen_width: int | None = None,
    join_contiguous: bool = True,
) -> list[OcrBlock]:
    """Build only physically contiguous visual fragments; never join wrapped lines."""
    if not blocks:
        return []

    @dataclass(frozen=True)
    class RawFragment:
        raw_id: int
        text: str
        confidence: float
        box: tuple[int, int, int, int]
        language: str
        multiline: bool

    raw_input = tuple(
        RawFragment(
            raw_id=index,
            text=str(block.source or "").strip(),
            confidence=float(block.confidence),
            box=tuple(int(v) for v in block.box[:4]),
            language=block.language,
            multiline=False,
        )
        for index, block in enumerate(blocks)
        if str(block.source or "").strip() and len(block.box) >= 4
    )
    if not raw_input:
        return []

    heights = sorted(max(1, raw.box[3] - raw.box[1]) for raw in raw_input)
    typical_height = heights[(len(heights) - 1) // 2]
    raw_fragments = tuple(
        RawFragment(
            raw.raw_id,
            raw.text,
            raw.confidence,
            raw.box,
            raw.language,
            (raw.box[3] - raw.box[1]) > 1.7 * typical_height,
        )
        for raw in raw_input
    )
    width = max(1, int(screen_width or (max(raw.box[2] for raw in raw_fragments) - min(raw.box[0] for raw in raw_fragments))))

    def contains_cjk(text: str) -> bool:
        return any("\u3040" <= char <= "\u30ff" or "\u3400" <= char <= "\u9fff" for char in text)

    def join_text(left: str, right: str) -> str:
        return left + right if contains_cjk(left) or contains_cjk(right) else left + " " + right

    def vertical_match(left: RawFragment, right: RawFragment) -> bool:
        if left.multiline or right.multiline:
            return False
        lx1, ly1, lx2, ly2 = left.box
        rx1, ry1, rx2, ry2 = right.box
        lh, rh = max(1, ly2 - ly1), max(1, ry2 - ry1)
        if max(lh, rh) / min(lh, rh) > 1.8:
            return False
        overlap = max(0, min(ly2, ry2) - max(ly1, ry1))
        center_distance = abs((ly1 + ly2) / 2 - (ry1 + ry2) / 2)
        return overlap >= 0.68 * min(lh, rh) or center_distance <= 0.35 * ((lh + rh) / 2)

    def horizontal_gap(left: RawFragment, right: RawFragment) -> int:
        lx1, _, lx2, _ = left.box
        rx1, _, rx2, _ = right.box
        if lx2 < rx1:
            return rx1 - lx2
        if rx2 < lx1:
            return lx1 - rx2
        return 0

    def gap_limit(left: RawFragment, right: RawFragment) -> int:
        lh = max(1, left.box[3] - left.box[1])
        rh = max(1, right.box[3] - right.box[1])
        return max(8, int(min(2.0 * min(lh, rh), 0.04 * width)))

    ordered = sorted(raw_fragments, key=lambda raw: (raw.box[1], raw.box[0], raw.raw_id))
    if not join_contiguous:
        return [
            OcrBlock(raw.text, raw.confidence, list(raw.box), raw.language, [raw.raw_id])
            for raw in ordered
        ]

    rows: list[list[RawFragment]] = []
    for raw in ordered:
        candidates: list[tuple[int, int, list[RawFragment]]] = []
        for row in rows:
            aligned = [member for member in row if vertical_match(member, raw)]
            if not aligned:
                continue
            nearest_gap = min(horizontal_gap(member, raw) for member in aligned)
            nearest_limit = max(gap_limit(member, raw) for member in aligned)
            if nearest_gap <= nearest_limit:
                y_distance = min(abs((member.box[1] + member.box[3]) - (raw.box[1] + raw.box[3])) for member in aligned)
                candidates.append((nearest_gap, y_distance, row))
        if candidates:
            candidates.sort(key=lambda item: (item[0], item[1]))
            candidates[0][2].append(raw)
        else:
            rows.append([raw])

    visual: list[OcrBlock] = []
    for row in rows:
        parts = sorted(row, key=lambda raw: (raw.box[0], raw.raw_id))
        text = parts[0].text
        for part in parts[1:]:
            text = join_text(text, part.text)
        box = [
            min(part.box[0] for part in parts),
            min(part.box[1] for part in parts),
            max(part.box[2] for part in parts),
            max(part.box[3] for part in parts),
        ]
        visual.append(
            OcrBlock(
                source=text,
                confidence=min(part.confidence for part in parts),
                box=box,
                language=parts[0].language,
                raw_ids=[part.raw_id for part in parts],
            )
        )

    visual.sort(key=lambda block: (block.box[1], block.box[0], block.raw_ids))
    for block in visual:
        logger.info(
            "OCR VISUAL raw_ids=%s text=%r box=%s",
            block.raw_ids,
            block.source,
            block.box,
        )
    return visual


def merge_ocr_blocks(blocks: list[OcrBlock]) -> list[OcrBlock]:
    """Compatibility alias: semantic wrapping is deliberately no longer performed."""
    return build_visual_fragments(blocks, join_contiguous=True)


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
