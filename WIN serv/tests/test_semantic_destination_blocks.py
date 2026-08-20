# SEMANTIC_DESTINATION_BLOCKS_TESTS_V1
from __future__ import annotations

from app.ocr_engine import OcrBlock, build_visual_fragments
from app.screen_document import (
    ScreenDocument,
    ScreenDocumentError,
    destination_blocks_from_cache,
    destination_blocks_to_cache,
    validate_destination_payload,
)


def _block(text: str, box: list[int], confidence: float = 0.95) -> OcrBlock:
    return OcrBlock(text, confidence, box, "en")


def test_visual_fragments_merge_only_contiguous_same_row():
    merged = build_visual_fragments(
        [_block("I can't", [20, 100, 120, 140]), _block("believe", [130, 101, 220, 141])],
        screen_width=1000,
    )
    assert len(merged) == 1
    assert merged[0].source == "I can't believe"

    separate = build_visual_fragments(
        [_block("Continue", [20, 100, 170, 140]), _block("Settings", [700, 100, 850, 140])],
        screen_width=1000,
    )
    assert [block.source for block in separate] == ["Continue", "Settings"]


def test_visual_fragments_never_wrap_lines_semantically():
    blocks = build_visual_fragments(
        [
            _block("I can't believe", [20, 100, 300, 140]),
            _block("you actually came back.", [22, 146, 380, 186]),
        ],
        screen_width=1000,
    )
    assert len(blocks) == 2


def test_destination_validator_groups_sentence_and_separates_button():
    visual = [
        _block("I can't believe", [80, 610, 420, 650]),
        _block("you actually came back.", [82, 655, 510, 698]),
        _block("Continue", [720, 910, 910, 970]),
    ]
    document = ScreenDocument.from_ocr_blocks(
        visual,
        width=1080,
        height=2400,
        source_language="en",
        target_language="ru",
    )
    result = validate_destination_payload(
        {
            "destination_blocks": [
                {
                    "source_ids": ["f0", "f1"],
                    "role": "dialogue",
                    "translation": "Не могу поверить, что ты действительно вернулся.",
                },
                {"source_ids": ["f2"], "role": "button", "translation": "Продолжить"},
            ]
        },
        document,
    )
    assert result[0].source_ids == ["f0", "f1"]
    assert result[0].source == "I can't believe you actually came back."
    assert result[0].source_boxes == [(80, 610, 420, 650), (82, 655, 510, 698)]
    assert result[0].box == (80, 610, 510, 698)
    assert result[1].role == "button"


def test_destination_validator_rejects_missing_duplicate_and_nonconsecutive_ids():
    document = ScreenDocument.from_ocr_blocks(
        [_block("A", [0, 0, 20, 20]), _block("B", [0, 25, 20, 45]), _block("C", [0, 50, 20, 70])],
        width=100,
        height=100,
        source_language="en",
        target_language="ru",
    )
    bad_payloads = [
        {"destination_blocks": [{"source_ids": ["f0", "f1"], "translation": "x"}]},
        {
            "destination_blocks": [
                {"source_ids": ["f0"], "translation": "x"},
                {"source_ids": ["f0"], "translation": "y"},
                {"source_ids": ["f1", "f2"], "translation": "z"},
            ]
        },
        {
            "destination_blocks": [
                {"source_ids": ["f0", "f2"], "translation": "x"},
                {"source_ids": ["f1"], "translation": "y"},
            ]
        },
    ]
    for payload in bad_payloads:
        try:
            validate_destination_payload(payload, document)
        except ScreenDocumentError:
            pass
        else:
            raise AssertionError(f"invalid payload accepted: {payload}")


def test_screen_cache_round_trip_rebuilds_geometry_from_document():
    document = ScreenDocument.from_ocr_blocks(
        [_block("A", [10, 10, 40, 30]), _block("B", [10, 34, 40, 54])],
        width=100,
        height=100,
        source_language="en",
        target_language="ru",
    )
    destinations = validate_destination_payload(
        {"destination_blocks": [{"source_ids": ["f0", "f1"], "role": "dialogue", "translation": "AB"}]},
        document,
    )
    destinations[0].history_id = 123
    restored = destination_blocks_from_cache(destination_blocks_to_cache(destinations), document)
    assert restored[0].history_id == 123
    assert restored[0].source_boxes == [(10, 10, 40, 30), (10, 34, 40, 54)]


def test_screen_hash_includes_language_pair_and_geometry():
    base = [_block("Hello", [10, 10, 100, 40])]
    ru = ScreenDocument.from_ocr_blocks(base, width=200, height=100, source_language="en", target_language="ru")
    uk = ScreenDocument.from_ocr_blocks(base, width=200, height=100, source_language="en", target_language="uk")
    shifted = ScreenDocument.from_ocr_blocks(
        [_block("Hello", [11, 10, 101, 40])],
        width=200,
        height=100,
        source_language="en",
        target_language="ru",
    )
    assert ru.semantic_hash() != uk.semantic_hash()
    assert ru.semantic_hash() != shifted.semantic_hash()
