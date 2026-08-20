from __future__ import annotations


def glossary_text(cfg: dict) -> str:
    if not cfg.get("glossary_enabled", True):
        return ""
    g = cfg.get("glossary") or {}
    if not g:
        return ""
    lines = [f"{k} => {v}" for k, v in g.items()]
    return "GLOSSARY:\n" + "\n".join(lines)


def game_glossary_text(entries: list[dict]) -> str:
    if not entries:
        return ""
    return "GAME_GLOSSARY:\n" + "\n".join(f"{entry['source_text']} => {entry['translation']}" for entry in entries)


def target_text(blocks: list, source_lang: str, target_lang: str) -> str:
    lines = [f"{i} {b.source}" for i, b in enumerate(blocks)]
    return (
        f"SOURCE_LANGUAGE: {source_lang}\nTARGET_LANGUAGE: {target_lang}\n"
        "TARGET_BLOCKS:\n" + "\n".join(lines)
    )


def build_messages(cfg: dict, history: list[dict], glossary: str, target: str) -> list[dict]:
    hist = "\n\n".join(
        f"SOURCE: {r['source_text']}\nTRANSLATION: {r['translation']}" for r in history
    )
    user_parts = []
    if glossary:
        user_parts.append(glossary)
    if hist:
        user_parts.append("HISTORY (context only; do not output it):\n" + hist)
    user_parts.append(target)
    user_parts.append('OUTPUT SCHEMA: {"translations":[{"id":0,"translation":"..."}]}')
    return [
        {"role": "system", "content": cfg["system_prompt"]},
        {"role": "user", "content": "\n\n".join(user_parts)},
    ]

# === SEMANTIC_SCREEN_PROMPT_V1 ===
SEMANTIC_SCREEN_SYSTEM = """SEMANTIC SCREEN MODE (production screenshot translation):
For this request, SCREEN_FRAGMENTS is the current translation target and replaces any earlier TARGET_BLOCKS wording in the base localization prompt.
OCR fragments are physical detections, not mandatory translation boundaries.
First infer logical destination blocks from the whole current screen, then translate each destination block.
Use HISTORY only as context; never output or regroup history.
The DESTINATION BLOCK output schema supplied in the user message supersedes any earlier translations/id output schema for this request.
Return JSON only. Do not include reasoning or commentary."""

SEMANTIC_SCREEN_CONTRACT = """DESTINATION BLOCK RULES:
- Every input source id must appear exactly once in the output.
- Never invent, omit, or duplicate source ids.
- source_ids inside one destination block must stay in reading order and must be consecutive in SCREEN_FRAGMENTS.
- Merge neighboring fragments only when they are parts of the same sentence or the same logical text element.
- Do NOT merge prose/dialogue with speaker names, buttons, choices, menu items, labels, titles, status text, counters, or other independent UI elements.
- Keep separate independent buttons/menu items/choices even when they are on the same visual row.
- role must be one of: dialogue, narration, subtitle, speaker, button, choice, menu_item, label, title, status, counter, unknown.
- Coordinates are input evidence only. Never return coordinates.
- Translate the complete logical text represented by each source_ids group.
OUTPUT SCHEMA:
{\"destination_blocks\":[{\"source_ids\":[\"f0\",\"f1\"],\"role\":\"dialogue\",\"translation\":\"...\"}]}"""


def screen_document_text(document) -> str:
    import json
    payload = document.prompt_dict()
    return "SCREEN_DOCUMENT:\n" + json.dumps(payload, ensure_ascii=False, separators=(",", ":"))


def build_screen_messages(
    cfg: dict,
    history: list[dict],
    glossary: str,
    screen_document: str,
    *,
    repair_error: str | None = None,
    previous_response: dict | None = None,
    vision_image_data_url: str | None = None,
) -> list[dict]:
    import json
    hist = "\n\n".join(
        f"SOURCE: {r['source_text']}\nTRANSLATION: {r['translation']}" for r in history
    )
    user_parts = []
    if glossary:
        user_parts.append(glossary)
    if hist:
        user_parts.append("HISTORY (context only; do not output it):\n" + hist)
    user_parts.append(screen_document)
    user_parts.append(SEMANTIC_SCREEN_CONTRACT)
    if repair_error:
        user_parts.append(
            "VALIDATOR REJECTED THE PREVIOUS RESPONSE. Return a corrected full response for the same SCREEN_FRAGMENTS.\n"
            f"VALIDATION_ERROR: {repair_error}\n"
            "PREVIOUS_RESPONSE:\n" + json.dumps(previous_response or {}, ensure_ascii=False, separators=(",", ":"))
        )
    user_text = "\n\n".join(user_parts)
    system_text = str(cfg["system_prompt"]).rstrip() + "\n\n" + SEMANTIC_SCREEN_SYSTEM
    if vision_image_data_url:
        user_content = [
            {"type": "text", "text": user_text},
            {"type": "image_url", "image_url": {"url": vision_image_data_url}},
        ]
    else:
        user_content = user_text
    return [
        {"role": "system", "content": system_text},
        {"role": "user", "content": user_content},
    ]
