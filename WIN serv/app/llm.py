from __future__ import annotations
import asyncio
import json
import logging
import re
import time
import httpx


logger = logging.getLogger(__name__)


# ECHOTRANSLATE_PROVIDER_CONTENT_BLOCK_V1
PROVIDER_CONTENT_BLOCK_MESSAGE = (
    "Провайдер отклонил текст из-за ограничения контента. "
    "Попробуйте другую модель или другого провайдера."
)


class LLMProviderContentBlockedError(RuntimeError):
    def __init__(self, provider_code: str, provider_message: str = "") -> None:
        self.provider_code = str(provider_code or "data_inspection_failed")
        self.provider_message = str(provider_message or "")
        super().__init__(PROVIDER_CONTENT_BLOCK_MESSAGE)


def provider_error_code(response: httpx.Response) -> str:
    if response.status_code < 400:
        return ""
    try:
        payload = response.json()
    except Exception:
        return ""
    if not isinstance(payload, dict):
        return ""
    error = payload.get("error")
    if isinstance(error, dict):
        return str(error.get("code") or "").strip()
    return str(payload.get("code") or "").strip()


def raise_for_provider_content_block(response: httpx.Response) -> None:
    code = provider_error_code(response)
    if code != "data_inspection_failed":
        return
    provider_message = ""
    try:
        payload = response.json()
        error = payload.get("error") if isinstance(payload, dict) else None
        if isinstance(error, dict):
            provider_message = str(error.get("message") or "")
    except Exception:
        pass
    logger.warning(
        "LLM PROVIDER CONTENT BLOCK status=%s code=%s message=%s",
        response.status_code, code, provider_message,
    )
    raise LLMProviderContentBlockedError(code, provider_message)


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
            raise_for_provider_content_block(r)
            if r.status_code >= 400:
                # Some OpenAI-compatible local servers do not accept response_format.
                # Provider moderation errors are handled above and MUST NOT be retried
                # as a response_format compatibility problem.
                if r.status_code in (400, 422):
                    body.pop("response_format", None)
                    r = await post(attempt)
                    raise_for_provider_content_block(r)
            if r.status_code >= 400:
                logger.error(
                    "LLM HTTP ERROR status=%s body=%s",
                    r.status_code,
                    r.text,
                )
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
    raise_for_provider_content_block(response)
    response.raise_for_status()
    choices = response.json().get("choices") or []
    content = ((choices[0] if choices else {}).get("message") or {}).get("content")
    parsed = _extract_json(content)
    entries = parsed.get("entries") or []
    return [entry for entry in entries if isinstance(entry, dict)]

# === SEMANTIC_SCREEN_LLM_V1 ===
async def translate_screen_openai_compatible(*, cfg: dict, messages: list[dict]) -> dict:
    """Return the structured Semantic Destination Blocks payload for one whole screen."""
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
                logger.info("SEMANTIC LLM HTTP attempt=%s duration=%.2fms", attempt, duration_ms)
                if response is not None:
                    logger.info("SEMANTIC LLM HTTP attempt=%s status=%s", attempt, response.status_code)

        payload = None
        content = None
        for attempt in (1, 2):
            response = await post(attempt)
            raise_for_provider_content_block(response)
            if response.status_code >= 400 and response.status_code in (400, 422) and "response_format" in body:
                logger.warning(
                    "SEMANTIC LLM response_format rejected status=%s body=%s; retrying without response_format",
                    response.status_code,
                    response.text[:1000],
                )
                body.pop("response_format", None)
                response = await post(attempt)
                raise_for_provider_content_block(response)
            if response.status_code >= 400:
                logger.error("SEMANTIC LLM HTTP ERROR status=%s body=%s", response.status_code, response.text)
            response.raise_for_status()
            payload = response.json()
            choices = payload.get("choices") or []
            choice = choices[0] if choices else {}
            message = choice.get("message") or {}
            content = message.get("content")
            if isinstance(content, str) and content.strip():
                break
            logger.warning(
                "SEMANTIC LLM EMPTY RESPONSE model=%r finish_reason=%r usage=%r",
                payload.get("model"), choice.get("finish_reason"), payload.get("usage"),
            )
            if attempt == 1:
                await asyncio.sleep(0.15)

    parsed = _extract_json(content)
    if not isinstance(parsed, dict):
        raise ValueError("Semantic screen response is not a JSON object")
    return parsed
