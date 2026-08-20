from __future__ import annotations

from dataclasses import dataclass, field
import hashlib
import json
from typing import Any, Iterable

PIPELINE_VERSION = "semantic-destination-blocks-v1"
ALLOWED_ROLES = {
    "dialogue",
    "narration",
    "subtitle",
    "speaker",
    "button",
    "choice",
    "menu_item",
    "label",
    "title",
    "status",
    "counter",
    "unknown",
}


class ScreenDocumentError(ValueError):
    pass


@dataclass(frozen=True)
class ScreenFragment:
    id: str
    order: int
    text: str
    box: tuple[int, int, int, int]
    confidence: float
    language: str
    raw_ids: tuple[int, ...] = ()

    def prompt_dict(self) -> dict[str, Any]:
        return {
            "id": self.id,
            "text": self.text,
            "box": list(self.box),
            "order": self.order,
        }


@dataclass
class DestinationBlock:
    id: str
    source_ids: list[str]
    source: str
    translation: str
    role: str
    box: tuple[int, int, int, int]
    source_boxes: list[tuple[int, int, int, int]]
    confidence: float
    language: str
    history_id: int | None = None

    def response_dict(self) -> dict[str, Any]:
        return {
            "id": self.id,
            "source_ids": list(self.source_ids),
            "source": self.source,
            "translation": self.translation,
            "role": self.role,
            "box": list(self.box),
            "source_boxes": [list(box) for box in self.source_boxes],
            "confidence": round(float(self.confidence), 5),
            "language": self.language,
            "history_id": self.history_id,
        }

    def cache_dict(self) -> dict[str, Any]:
            # Reference cache only: never duplicate translation text here.
            # The live translation is always read from messages by history_id on a HIT.
            return {
                "id": self.id,
                "source_ids": list(self.source_ids),
                "role": self.role,
                "history_id": self.history_id,
            }


@dataclass(frozen=True)
class ScreenDocument:
    width: int
    height: int
    source_language: str
    target_language: str
    fragments: tuple[ScreenFragment, ...] = field(default_factory=tuple)

    @classmethod
    def from_ocr_blocks(
        cls,
        blocks: Iterable[Any],
        *,
        width: int,
        height: int,
        source_language: str,
        target_language: str,
    ) -> "ScreenDocument":
        fragments: list[ScreenFragment] = []
        for order, block in enumerate(blocks):
            box_values = tuple(int(v) for v in list(block.box)[:4])
            if len(box_values) != 4:
                raise ScreenDocumentError(f"OCR fragment {order} has invalid box")
            text = str(block.source or "").strip()
            if not text:
                continue
            raw_ids = tuple(int(v) for v in (getattr(block, "raw_ids", None) or [order]))
            fragments.append(
                ScreenFragment(
                    id=f"f{len(fragments)}",
                    order=len(fragments),
                    text=text,
                    box=box_values,
                    confidence=float(getattr(block, "confidence", 1.0)),
                    language=str(getattr(block, "language", source_language) or source_language),
                    raw_ids=raw_ids,
                )
            )
        return cls(
            width=max(1, int(width)),
            height=max(1, int(height)),
            source_language=str(source_language or "auto"),
            target_language=str(target_language or "ru"),
            fragments=tuple(fragments),
        )

    @property
    def fragment_by_id(self) -> dict[str, ScreenFragment]:
        return {fragment.id: fragment for fragment in self.fragments}

    def prompt_dict(self) -> dict[str, Any]:
        return {
            "pipeline": PIPELINE_VERSION,
            "screen_size": [self.width, self.height],
            "source_language": self.source_language,
            "target_language": self.target_language,
            "screen_fragments": [fragment.prompt_dict() for fragment in self.fragments],
        }

    def semantic_hash(self) -> str:
        # Full-screen cache: text, near-identical geometry, language pair and screen size must match.
        # Whitespace inside OCR text is normalized, but fragment boundaries are not.
        payload = {
            "v": PIPELINE_VERSION,
            "size": [self.width, self.height],
            "source_language": self.source_language.strip().lower(),
            "target_language": self.target_language.strip().lower(),
            "fragments": [
                {
                    "text": " ".join(fragment.text.split()),
                    # 4 px quantization ignores harmless OCR jitter while still
                    # invalidating cache when layout changes meaningfully.
                    "box": [int(round(value / 4.0) * 4) for value in fragment.box],
                }
                for fragment in self.fragments
            ],
        }
        packed = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
        return hashlib.sha256(packed.encode("utf-8")).hexdigest()

    def materialize(
        self,
        *,
        destination_id: str,
        source_ids: list[str],
        role: str,
        translation: str,
        history_id: int | None = None,
    ) -> DestinationBlock:
        by_id = self.fragment_by_id
        fragments = [by_id[source_id] for source_id in source_ids]
        source_boxes = [fragment.box for fragment in fragments]
        left = min(box[0] for box in source_boxes)
        top = min(box[1] for box in source_boxes)
        right = max(box[2] for box in source_boxes)
        bottom = max(box[3] for box in source_boxes)
        return DestinationBlock(
            id=destination_id,
            source_ids=list(source_ids),
            source=_join_source_fragments(fragments, self.source_language),
            translation=str(translation).strip(),
            role=_normalize_role(role),
            box=(left, top, right, bottom),
            source_boxes=source_boxes,
            confidence=min(fragment.confidence for fragment in fragments),
            language=self.source_language,
            history_id=history_id,
        )


def _normalize_role(value: object) -> str:
    role = str(value or "unknown").strip().lower()
    return role if role in ALLOWED_ROLES else "unknown"


def _is_compact_language(language: str, text: str) -> bool:
    primary = language.strip().lower().replace("_", "-").split("-", 1)[0]
    if primary in {"zh", "ja", "th"}:
        return True
    if primary and primary != "auto":
        return False
    compact = "".join(ch for ch in text if not ch.isspace())
    if not compact:
        return False
    cjk = sum(1 for ch in compact if "\u3040" <= ch <= "\u30ff" or "\u3400" <= ch <= "\u9fff")
    return cjk * 2 >= len(compact)


def _join_source_fragments(fragments: list[ScreenFragment], language: str) -> str:
    values = [fragment.text.strip() for fragment in fragments if fragment.text.strip()]
    if not values:
        return ""
    compact = _is_compact_language(language, "".join(values))
    result = values[0]
    for value in values[1:]:
        if compact:
            result += value
        elif result.endswith("\u00ad"):
            result = result[:-1] + value
        elif result.endswith("-"):
            result += value
        else:
            result += " " + value
    return result.strip()


def _validate_group_geometry(document: ScreenDocument, source_ids: list[str]) -> None:
    if len(source_ids) < 2:
        return
    by_id = document.fragment_by_id
    for previous_id, current_id in zip(source_ids, source_ids[1:]):
        previous = by_id[previous_id]
        current = by_id[current_id]
        px1, py1, px2, py2 = previous.box
        cx1, cy1, cx2, cy2 = current.box
        ph = max(1, py2 - py1)
        ch = max(1, cy2 - cy1)
        line_height = max(ph, ch)
        vertical_gap = max(0, cy1 - py2, py1 - cy2)
        horizontal_gap = max(0, cx1 - px2, px1 - cx2)
        y_overlap = max(0, min(py2, cy2) - max(py1, cy1))
        same_row = y_overlap >= 0.35 * min(ph, ch)

        if same_row:
            max_horizontal = max(8 * line_height, int(document.width * 0.30))
            if horizontal_gap > max_horizontal:
                raise ScreenDocumentError(
                    f"group {source_ids!r} spans implausibly distant same-row fragments"
                )
        else:
            max_vertical = max(6 * line_height, int(document.height * 0.18))
            if vertical_gap > max_vertical:
                raise ScreenDocumentError(
                    f"group {source_ids!r} spans implausibly distant rows"
                )


def validate_destination_payload(payload: dict[str, Any], document: ScreenDocument) -> list[DestinationBlock]:
    if not isinstance(payload, dict):
        raise ScreenDocumentError("LLM response is not an object")
    raw_blocks = payload.get("destination_blocks")
    if not isinstance(raw_blocks, list) or not raw_blocks:
        raise ScreenDocumentError("LLM response has no destination_blocks")

    valid_ids = [fragment.id for fragment in document.fragments]
    valid_set = set(valid_ids)
    order = {fragment.id: fragment.order for fragment in document.fragments}
    used: list[str] = []
    destinations: list[DestinationBlock] = []

    for destination_index, raw in enumerate(raw_blocks):
        if not isinstance(raw, dict):
            raise ScreenDocumentError(f"destination_blocks[{destination_index}] is not an object")
        source_ids_raw = raw.get("source_ids")
        if not isinstance(source_ids_raw, list) or not source_ids_raw:
            raise ScreenDocumentError(f"destination_blocks[{destination_index}] has empty source_ids")
        source_ids = [str(value) for value in source_ids_raw]
        if len(set(source_ids)) != len(source_ids):
            raise ScreenDocumentError(f"destination_blocks[{destination_index}] duplicates source_ids")
        unknown = [source_id for source_id in source_ids if source_id not in valid_set]
        if unknown:
            raise ScreenDocumentError(f"LLM invented source ids: {unknown}")
        positions = [order[source_id] for source_id in source_ids]
        if positions != sorted(positions):
            raise ScreenDocumentError(f"source_ids are not in reading order: {source_ids}")
        if positions != list(range(positions[0], positions[0] + len(positions))):
            raise ScreenDocumentError(f"source_ids are not consecutive: {source_ids}")
        _validate_group_geometry(document, source_ids)
        translation = str(raw.get("translation") or "").strip()
        if not translation:
            raise ScreenDocumentError(f"destination_blocks[{destination_index}] has empty translation")
        used.extend(source_ids)
        destinations.append(
            document.materialize(
                destination_id=f"d{destination_index}",
                source_ids=source_ids,
                role=str(raw.get("role") or "unknown"),
                translation=translation,
            )
        )

    if len(used) != len(set(used)):
        duplicates = sorted({source_id for source_id in used if used.count(source_id) > 1})
        raise ScreenDocumentError(f"source ids used more than once: {duplicates}")
    missing = [source_id for source_id in valid_ids if source_id not in set(used)]
    if missing:
        raise ScreenDocumentError(f"source ids missing from destination blocks: {missing}")
    if set(used) != valid_set:
        raise ScreenDocumentError("destination source id coverage does not match input")
    return destinations

# === SCREEN_CACHE_HISTORY_REF_V1 ===
def destination_cache_history_ids(raw: str) -> list[int]:
    """Return ordered history ids stored by a Semantic Destination screen cache entry."""
    payload = json.loads(raw)
    if payload.get("version") != PIPELINE_VERSION:
        raise ScreenDocumentError("screen cache version mismatch")
    raw_blocks = payload.get("destination_blocks")
    if not isinstance(raw_blocks, list) or not raw_blocks:
        raise ScreenDocumentError("screen cache has no destination blocks")
    history_ids: list[int] = []
    for index, item in enumerate(raw_blocks):
        if not isinstance(item, dict):
            raise ScreenDocumentError(f"screen cache destination {index} is not an object")
        history_id = item.get("history_id")
        if history_id is None:
            raise ScreenDocumentError(f"screen cache destination {index} has no history_id")
        try:
            history_ids.append(int(history_id))
        except (TypeError, ValueError) as exc:
            raise ScreenDocumentError(f"screen cache destination {index} has invalid history_id") from exc
    if len(history_ids) != len(set(history_ids)):
        raise ScreenDocumentError("screen cache duplicates history_id")
    return history_ids


def destination_blocks_to_cache(blocks: list[DestinationBlock]) -> str:
    if any(block.history_id is None for block in blocks):
        raise ScreenDocumentError("screen cache requires persisted history_id for every destination")
    return json.dumps(
        {
            "version": PIPELINE_VERSION,
            "cache_mode": "history_reference_v1",
            "destination_blocks": [block.cache_dict() for block in blocks],
        },
        ensure_ascii=False,
        separators=(",", ":"),
    )


def destination_blocks_from_cache(
    raw: str,
    document: ScreenDocument,
    translations_by_history_id: dict[int, str],
) -> list[DestinationBlock]:
    payload = json.loads(raw)
    if payload.get("version") != PIPELINE_VERSION:
        raise ScreenDocumentError("screen cache version mismatch")
    raw_blocks = payload.get("destination_blocks")
    if not isinstance(raw_blocks, list) or not raw_blocks:
        raise ScreenDocumentError("screen cache has no destination blocks")

    # Compatibility: old v1 cache rows may still contain an embedded `translation`.
    # It is intentionally ignored. The live value from messages always wins.
    live_payload: list[dict[str, Any]] = []
    history_ids: list[int] = []
    for index, item in enumerate(raw_blocks):
        if not isinstance(item, dict):
            raise ScreenDocumentError(f"screen cache destination {index} is not an object")
        history_id = item.get("history_id")
        if history_id is None:
            raise ScreenDocumentError(f"screen cache destination {index} has no history_id")
        try:
            history_id = int(history_id)
        except (TypeError, ValueError) as exc:
            raise ScreenDocumentError(f"screen cache destination {index} has invalid history_id") from exc
        translation = str(translations_by_history_id.get(history_id) or "").strip()
        if not translation:
            raise ScreenDocumentError(f"history row {history_id} is missing or has empty translation")
        history_ids.append(history_id)
        live_payload.append({
            "source_ids": item.get("source_ids"),
            "role": item.get("role"),
            "translation": translation,
        })

    if len(history_ids) != len(set(history_ids)):
        raise ScreenDocumentError("screen cache duplicates history_id")

    # Rebuild source/geometry from the CURRENT ScreenDocument and validate the grouping again.
    validated = validate_destination_payload({"destination_blocks": live_payload}, document)
    for block, history_id in zip(validated, history_ids):
        block.history_id = history_id
    return validated
