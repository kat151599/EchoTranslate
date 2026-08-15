from __future__ import annotations
import asyncio
import json
import logging
import re
import time
import httpx


logger = logging.getLogger(__name__)


def _extract_json(text: str | None) -> dict:
    if text is None:
        text = ""
    text = text.strip()
    try:
        return json.loads(text)
    except Exception:
        m = re.search(r"\{.*\}", text, re.S)
        if not m:
            raise ValueError(f"LLM did not return JSON: {text[:300]}")
        return json.loads(m.group(0))


async def translate_openai_compatible(*, cfg: dict, messages: list[dict]) -> dict[int, str]:
    base = str(cfg["llm_base_url"]).rstrip("/")
    url = f"{base}/chat/completions"
    headers = {"Content-Type": "application/json"}
    if cfg.get("llm_api_key"):
        headers["Authorization"] = f"Bearer {cfg['llm_api_key']}"
    body = {
        "model": cfg["llm_model"],
        "messages": messages,
        "temperature": 0.2,
        "max_tokens": int(cfg["max_output_tokens"]),
        "response_format": {"type": "json_object"},
    }
    timeout = float(cfg.get("llm_timeout_seconds", 90))
    async with httpx.AsyncClient(timeout=timeout) as client:
        async def post(attempt: int) -> httpx.Response:
            started = time.perf_counter()
            response = None
            try:
                response = await client.post(url, headers=headers, json=body)
                return response
            finally:
                duration_ms = (time.perf_counter() - started) * 1000
                logger.info("LLM HTTP attempt=%s duration=%.2fms", attempt, duration_ms)
                if response is not None:
                    logger.info("LLM HTTP attempt=%s status=%s", attempt, response.status_code)

        payload = None
        content = None
        for attempt in (1, 2):
            r = await post(attempt)
            if r.status_code >= 400:
                # Some OpenAI-compatible local servers do not accept response_format.
                if r.status_code in (400, 422):
                    body.pop("response_format", None)
                    r = await post(attempt)
            r.raise_for_status()
            payload = r.json()
            choices = payload.get("choices") or []
            choice = choices[0] if choices else {}
            message = choice.get("message") or {}
            content = message.get("content")
            if content is not None and (not isinstance(content, str) or content.strip()):
                break

            extra_message_fields = {}
            for key, value in message.items():
                if key not in ("role", "content") and value is not None:
                    try:
                        length = len(value)
                    except TypeError:
                        length = len(str(value))
                    extra_message_fields[key] = {"present": True, "length": length}
            choice_metadata = {
                key: value
                for key, value in choice.items()
                if key != "message" and isinstance(value, (str, int, float, bool, type(None)))
            }
            logger.warning(
                "LLM EMPTY RESPONSE status=%s model=%r finish_reason=%r content=%r "
                "usage=%r choices=%s first_choice_metadata=%r message_extra_fields=%r",
                r.status_code,
                payload.get("model"),
                choice.get("finish_reason"),
                content,
                payload.get("usage"),
                len(choices),
                choice_metadata,
                extra_message_fields,
            )
            if attempt == 1:
                logger.warning("LLM EMPTY RESPONSE - RETRYING ONCE")
                await asyncio.sleep(0.15)
                continue
            break

    assert payload is not None
    parsed = _extract_json(content)
    out: dict[int, str] = {}
    for item in parsed.get("translations", []):
        out[int(item["id"])] = str(item["translation"]).strip()
    return out


async def extract_glossary_openai_compatible(*, cfg: dict, messages: list[dict]) -> list[dict]:
    base = str(cfg["llm_base_url"]).rstrip("/")
    headers = {"Content-Type": "application/json"}
    if cfg.get("llm_api_key"):
        headers["Authorization"] = f"Bearer {cfg['llm_api_key']}"
    body = {"model": cfg["llm_model"], "messages": messages, "temperature": 0, "max_tokens": int(cfg["max_output_tokens"]), "response_format": {"type": "json_object"}}
    async with httpx.AsyncClient(timeout=float(cfg.get("llm_timeout_seconds", 90))) as client:
        response = await client.post(f"{base}/chat/completions", headers=headers, json=body)
    response.raise_for_status()
    choices = response.json().get("choices") or []
    content = ((choices[0] if choices else {}).get("message") or {}).get("content")
    parsed = _extract_json(content)
    entries = parsed.get("entries") or []
    return [entry for entry in entries if isinstance(entry, dict)]
