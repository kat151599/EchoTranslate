from __future__ import annotations
import json
import os
import threading
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
CONFIG_PATH = ROOT / "config.json"
EXAMPLE_PATH = ROOT / "config.example.json"
_lock = threading.RLock()


def _load_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def ensure_config() -> None:
    if not CONFIG_PATH.exists():
        CONFIG_PATH.write_text(EXAMPLE_PATH.read_text(encoding="utf-8"), encoding="utf-8")


def _apply_defaults(cfg: dict[str, Any]) -> dict[str, Any]:
    cfg.setdefault("ocr_backend", "rapidocr_directml")
    cfg.setdefault("ocr_fallback_to_paddle", True)
    cfg.setdefault("ocr_min_confidence", 0.70)
    cfg.setdefault("ocr_merge_horizontal_blocks", True)
    if not cfg.get("system_prompt_profiles"):
        cfg["system_prompt_profiles"] = [{"id": "default", "name": "Default", "prompt": str(cfg.get("system_prompt") or "")}]
    cfg.setdefault("active_system_prompt_profile_id", cfg["system_prompt_profiles"][0]["id"])
    active = next((p for p in cfg["system_prompt_profiles"] if p.get("id") == cfg["active_system_prompt_profile_id"]), cfg["system_prompt_profiles"][0])
    cfg["active_system_prompt_profile_id"] = active["id"]
    cfg["system_prompt"] = str(active.get("prompt") or "")
    return cfg


def load_config() -> dict[str, Any]:
    ensure_config()
    with _lock:
        cfg = _apply_defaults(_load_json(CONFIG_PATH))
    if os.getenv("OVERLAY_SERVER_API_KEY"):
        cfg["server_api_key"] = os.environ["OVERLAY_SERVER_API_KEY"]
    if os.getenv("OVERLAY_LLM_API_KEY"):
        cfg["llm_api_key"] = os.environ["OVERLAY_LLM_API_KEY"]
    return cfg


def save_config(updates: dict[str, Any]) -> dict[str, Any]:
    ensure_config()
    with _lock:
        cfg = _apply_defaults(_load_json(CONFIG_PATH))
        allowed = {
            "server_api_key", "source_lang", "target_lang", "ocr_min_confidence",
            "ocr_backend", "ocr_fallback_to_paddle", "ocr_merge_horizontal_blocks",
            "llm_base_url", "llm_api_key", "llm_model", "llm_timeout_seconds",
            "llm_profiles", "active_llm_profile",
            "max_request_tokens", "max_output_tokens", "tokenizer_encoding",
            "history_enabled", "glossary_enabled", "system_prompt", "glossary",
            "cache_identical_screen", "system_prompt_profiles", "active_system_prompt_profile_id"
        }
        for k, v in updates.items():
            if k in allowed:
                cfg[k] = v
        if "system_prompt" in updates:
            for profile in cfg["system_prompt_profiles"]:
                if profile.get("id") == cfg["active_system_prompt_profile_id"]:
                    profile["prompt"] = str(updates["system_prompt"])
                    break
        cfg = _apply_defaults(cfg)
        temporary_path = CONFIG_PATH.with_suffix(".json.tmp")
        temporary_path.write_text(json.dumps(cfg, ensure_ascii=False, indent=2), encoding="utf-8")
        temporary_path.replace(CONFIG_PATH)
    return cfg
