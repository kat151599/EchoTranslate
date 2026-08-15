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
