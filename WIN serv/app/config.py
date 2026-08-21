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
    # Retire the old global prompt ceiling. Only HISTORY has its own input budget.
    cfg.pop("max_request_tokens", None)
    cfg.setdefault("history_token_limit", 3000)
    # CHATGPT_PRICING_AGENT_CONFIG_V1
    cfg.setdefault("chatgpt_bridge_url", "http://127.0.0.1:8639")
    cfg.setdefault("pricing_chat_url", "")
    cfg.setdefault("chatgpt_bridge_token", "")
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
    if os.getenv("ECHOTRANSLATE_CHATGPT_BRIDGE_URL"):
        cfg["chatgpt_bridge_url"] = os.environ["ECHOTRANSLATE_CHATGPT_BRIDGE_URL"]
    if os.getenv("ECHOTRANSLATE_PRICING_CHAT_URL"):
        cfg["pricing_chat_url"] = os.environ["ECHOTRANSLATE_PRICING_CHAT_URL"]
    if os.getenv("ECHOTRANSLATE_CHATGPT_BRIDGE_TOKEN"):
        cfg["chatgpt_bridge_token"] = os.environ["ECHOTRANSLATE_CHATGPT_BRIDGE_TOKEN"]
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
            "history_token_limit", "max_output_tokens", "tokenizer_encoding",
            "history_enabled", "glossary_enabled", "system_prompt", "glossary",
            "cache_identical_screen", "system_prompt_profiles", "active_system_prompt_profile_id",
            "chatgpt_bridge_url", "pricing_chat_url", "chatgpt_bridge_token"
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
