from __future__ import annotations

# ECHOTRANSLATE_PRICING_PREFILTER_COUNTER_FIX_V1
from collections import Counter

# ECHOTRANSLATE_CHATGPT_PRICING_BRIDGE_V1

import json
import logging
import re
import uuid
from datetime import datetime, timezone
from urllib.parse import urlparse

import httpx
from fastapi import APIRouter, HTTPException, Request

from .config import load_config, save_config
from .db import connect
from .pricing import PricingInfo, PricingTier, save_pricing_agent_batch, safe_float, safe_int

logger = logging.getLogger(__name__)
router = APIRouter()

PROTOCOL = "ECHOTRANSLATE_PRICING_V1"
REQUEST_BEGIN = "<<<ECHOTRANSLATE_PRICING_REQUEST>>>"
REQUEST_END = "<<<END_ECHOTRANSLATE_PRICING_REQUEST>>>"
RESPONSE_BEGIN = "<<<ECHOTRANSLATE_PRICING_V1>>>"
RESPONSE_END = "<<<END_ECHOTRANSLATE_PRICING_V1>>>"
DEFAULT_BRIDGE_URL = "http://127.0.0.1:8639"
MODELS_PER_AGENT_REQUEST = 25

# ECHOTRANSLATE_PRICING_MODEL_PREFILTER_V1
# Pricing Agent is expensive in wall-clock time because ChatGPT researches official
# provider pages.  Do not send model IDs that are clearly irrelevant to text
# translation.  This is deliberately a negative filter, not a provider-specific
# allow-list: unknown text-model families remain eligible.
_PRICING_EXCLUDE_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    ("speech/audio/realtime", re.compile(
        r"(?:^|[-_/.])(asr|tts|speech|audio|s2s|realtime|voice)(?:$|[-_/.])", re.I
    )),
    ("vision/image/video", re.compile(
        r"(?:^|[-_/.])(image|vision|vl|omni|caption|captioner|video|wan|ocr)(?:$|[-_/.])", re.I
    )),
    ("embedding/rerank/moderation", re.compile(
        r"(?:^|[-_/.])(embedding|embeddings|embed|rerank|moderation|classifier|reward|guard)(?:$|[-_/.])", re.I
    )),
    ("coding", re.compile(
        r"(?:^|[-_/.])(coder|coding|codex|code)(?:$|[-_/.])", re.I
    )),
    ("reasoning-only", re.compile(
        r"(?:^|[-_/.])(thinking|reasoning|qwq|qvq|r1)(?:$|[-_/.])", re.I
    )),
    ("research", re.compile(
        r"(?:^|[-_/.])(research|deep[-_]?research)(?:$|[-_/.])", re.I
    )),
)


def _pricing_exclusion_reason(model: str) -> str | None:
    value = str(model or "").strip()
    for reason, pattern in _PRICING_EXCLUDE_PATTERNS:
        if pattern.search(value):
            return reason
    return None


def _filter_pricing_models(models: list[str]) -> tuple[list[str], list[dict[str, str]]]:
    eligible: list[str] = []
    skipped: list[dict[str, str]] = []
    seen: set[str] = set()
    for raw in models:
        model = str(raw or "").strip()
        if not model or model in seen:
            continue
        seen.add(model)
        reason = _pricing_exclusion_reason(model)
        if reason:
            skipped.append({"model": model, "reason": reason})
        else:
            eligible.append(model)
    return eligible, skipped


def _filter_summary(skipped: list[dict[str, str]]) -> dict[str, int]:
    return dict(sorted(Counter(str(item.get("reason") or "other") for item in skipped).items()))


def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def _provider_id(cfg: dict) -> str:
    profile = str(cfg.get("active_llm_profile") or "").strip()
    return f"profile:{profile}" if profile else f"base_url:{str(cfg.get('llm_base_url') or '').strip().rstrip('/')}"


def _active_profile(cfg: dict) -> tuple[str, dict]:
    name = str(cfg.get("active_llm_profile") or "").strip()
    profile = (cfg.get("llm_profiles") or {}).get(name)
    if not name or not isinstance(profile, dict):
        raise HTTPException(400, "Сначала выберите сохранённого LLM-провайдера")
    return name, dict(profile)


def _ensure_job_schema() -> None:
    with connect() as con:
        con.executescript(
            """
            CREATE TABLE IF NOT EXISTS llm_pricing_agent_jobs (
                request_id TEXT PRIMARY KEY,
                provider_id TEXT NOT NULL,
                provider_name TEXT NOT NULL,
                official_site TEXT NOT NULL,
                models_json TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'queued',
                error_text TEXT,
                created_at TEXT NOT NULL DEFAULT (datetime('now')),
                updated_at TEXT NOT NULL DEFAULT (datetime('now')),
                applied_at TEXT
            );
            CREATE INDEX IF NOT EXISTS idx_llm_pricing_agent_jobs_provider
            ON llm_pricing_agent_jobs(provider_id, created_at DESC);
            CREATE TABLE IF NOT EXISTS llm_pricing_agent_parts (
                part_request_id TEXT PRIMARY KEY,
                parent_request_id TEXT NOT NULL,
                part_index INTEGER NOT NULL,
                models_json TEXT NOT NULL,
                bridge_job_id TEXT,
                status TEXT NOT NULL DEFAULT 'queued',
                response_text TEXT,
                error_text TEXT,
                created_at TEXT NOT NULL DEFAULT (datetime('now')),
                updated_at TEXT NOT NULL DEFAULT (datetime('now')),
                UNIQUE(parent_request_id, part_index)
            );
            CREATE INDEX IF NOT EXISTS idx_llm_pricing_agent_parts_parent
            ON llm_pricing_agent_parts(parent_request_id, part_index);
            """
        )


def _decode_models(raw: object) -> list[str]:
    try:
        value = json.loads(str(raw or "[]"))
    except Exception:
        return []
    return [str(x) for x in value] if isinstance(value, list) else []


def _job(request_id: str) -> dict | None:
    _ensure_job_schema()
    with connect() as con:
        row = con.execute("SELECT * FROM llm_pricing_agent_jobs WHERE request_id=?", (request_id,)).fetchone()
    if row is None:
        return None
    item = dict(row)
    item["models"] = _decode_models(item.pop("models_json", "[]"))
    return item


def _parts(parent_request_id: str) -> list[dict]:
    _ensure_job_schema()
    with connect() as con:
        rows = con.execute(
            "SELECT * FROM llm_pricing_agent_parts WHERE parent_request_id=? ORDER BY part_index",
            (parent_request_id,),
        ).fetchall()
    result = []
    for row in rows:
        item = dict(row)
        item["models"] = _decode_models(item.pop("models_json", "[]"))
        result.append(item)
    return result


def _latest_job(provider_id: str) -> dict | None:
    _ensure_job_schema()
    with connect() as con:
        row = con.execute(
            "SELECT request_id FROM llm_pricing_agent_jobs WHERE provider_id=? ORDER BY created_at DESC LIMIT 1",
            (provider_id,),
        ).fetchone()
    return _job(str(row["request_id"])) if row is not None else None


def _save_job(request_id: str, provider_id: str, provider_name: str, official_site: str, models: list[str]) -> None:
    _ensure_job_schema()
    with connect() as con:
        con.execute(
            "INSERT INTO llm_pricing_agent_jobs(request_id,provider_id,provider_name,official_site,models_json,status) "
            "VALUES(?,?,?,?,?,'queued')",
            (request_id, provider_id, provider_name, official_site, json.dumps(models, ensure_ascii=False)),
        )


def _save_part(part_request_id: str, parent_request_id: str, part_index: int, models: list[str]) -> None:
    with connect() as con:
        con.execute(
            "INSERT OR IGNORE INTO llm_pricing_agent_parts(part_request_id,parent_request_id,part_index,models_json,status) "
            "VALUES(?,?,?,?, 'queued')",
            (part_request_id, parent_request_id, part_index, json.dumps(models, ensure_ascii=False)),
        )


def _update_job(request_id: str, *, status: str | None = None, error_text: str | None = None, applied: bool = False) -> None:
    fields = ["updated_at=datetime('now')"]
    params: list[object] = []
    if status is not None:
        fields.append("status=?"); params.append(status)
    if error_text is not None:
        fields.append("error_text=?"); params.append(error_text[:8000])
    if applied:
        fields.append("applied_at=datetime('now')")
    params.append(request_id)
    with connect() as con:
        con.execute(f"UPDATE llm_pricing_agent_jobs SET {', '.join(fields)} WHERE request_id=?", params)


def _update_part(part_request_id: str, *, status: str | None = None, bridge_job_id: str | None = None,
                 response_text: str | None = None, error_text: str | None = None) -> None:
    fields = ["updated_at=datetime('now')"]
    params: list[object] = []
    if status is not None:
        fields.append("status=?"); params.append(status)
    if bridge_job_id is not None:
        fields.append("bridge_job_id=?"); params.append(bridge_job_id)
    if response_text is not None:
        fields.append("response_text=?"); params.append(response_text)
    if error_text is not None:
        fields.append("error_text=?"); params.append(error_text[:8000])
    params.append(part_request_id)
    with connect() as con:
        con.execute(f"UPDATE llm_pricing_agent_parts SET {', '.join(fields)} WHERE part_request_id=?", params)


def _clean_url(value: object, *, required: bool = False) -> str:
    url = str(value or "").strip().rstrip("/")
    if not url:
        if required:
            raise HTTPException(400, "URL не указан")
        return ""
    parsed = urlparse(url)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise HTTPException(400, f"Некорректный URL: {url}")
    return url


def _source_matches_official(source_url: str | None, official_site: str) -> bool:
    if not source_url:
        return False
    source_host = (urlparse(source_url).hostname or "").lower()
    official_host = (urlparse(official_site).hostname or "").lower()
    if not source_host or not official_host:
        return False
    base = official_host[4:] if official_host.startswith("www.") else official_host
    return source_host == base or source_host.endswith("." + base)


def _bridge_headers(cfg: dict) -> dict[str, str]:
    token = str(cfg.get("chatgpt_bridge_token") or "").strip()
    return {"Authorization": f"Bearer {token}"} if token else {}


def _bridge_url(cfg: dict) -> str:
    return _clean_url(cfg.get("chatgpt_bridge_url") or DEFAULT_BRIDGE_URL, required=True)


def _pricing_chat_url(cfg: dict) -> str:
    url = _clean_url(cfg.get("pricing_chat_url"), required=True)
    if (urlparse(url).hostname or "").lower() != "chatgpt.com":
        raise HTTPException(400, "Pricing Agent должен быть chatgpt.com/c/... URL")
    return url


def _build_message(request_id: str, provider_id: str, provider_name: str, official_site: str, models: list[str]) -> str:
    body = {
        "protocol": PROTOCOL,
        "request_id": request_id,
        "provider_id": provider_id,
        "provider_name": provider_name,
        "official_site": official_site,
        "models": models,
    }
    return REQUEST_BEGIN + "\n" + json.dumps(body, ensure_ascii=False, indent=2) + "\n" + REQUEST_END


async def _refresh_provider_models(cfg: dict, provider_name: str, profile: dict) -> tuple[list[str], str]:
    existing: list[str] = []
    for raw in profile.get("llm_models") or []:
        model = str(raw or "").strip()
        if model and model not in existing:
            existing.append(model)
    base_url = str(profile.get("llm_base_url") or cfg.get("llm_base_url") or "").strip().rstrip("/")
    api_key = str(profile.get("llm_api_key") or cfg.get("llm_api_key") or "").strip()
    if not base_url.startswith(("http://", "https://")):
        if existing:
            return existing, "API URL некорректен; использован сохранённый список моделей"
        raise HTTPException(400, "У провайдера не указан корректный LLM API URL")
    headers = {"Authorization": f"Bearer {api_key}"} if api_key else {}
    warning = ""
    try:
        timeout = httpx.Timeout(12.0, connect=4.0)
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.get(f"{base_url}/models", headers=headers)
        response.raise_for_status()
        payload = response.json()
        fresh: list[str] = []
        for item in payload.get("data", []):
            if isinstance(item, dict):
                model = str(item.get("id") or "").strip()
                if model and model not in fresh:
                    fresh.append(model)
        if fresh:
            profiles = dict(cfg.get("llm_profiles") or {})
            updated = dict(profile)
            updated["llm_models"] = fresh
            profiles[provider_name] = updated
            save_config({"llm_profiles": profiles})
            return fresh, ""
        warning = "/models вернул пустой список; использован сохранённый список"
    except Exception as exc:
        warning = f"/models недоступен ({exc}); использован сохранённый список"
    if not existing:
        raise HTTPException(502, "Не удалось получить список моделей и локальный список пуст")
    return existing, warning


def _extract_payload(text: str) -> dict:
    raw = str(text or "")
    start = raw.find(RESPONSE_BEGIN)
    if start < 0:
        raise ValueError(f"Ответ не содержит маркер {RESPONSE_BEGIN}")
    start += len(RESPONSE_BEGIN)
    end = raw.find(RESPONSE_END, start)
    if end < 0:
        raise ValueError(f"Ответ не содержит маркер {RESPONSE_END}")
    body = raw[start:end].strip()
    try:
        payload = json.loads(body)
    except json.JSONDecodeError as exc:
        raise ValueError(f"Pricing Agent вернул невалидный JSON: {exc}") from exc
    if not isinstance(payload, dict):
        raise ValueError("Pricing Agent JSON должен быть объектом")
    return payload


def _number(value: object) -> float | None:
    result = safe_float(value)
    if result is not None and result > 1_000_000:
        raise ValueError("Цена выглядит некорректно большой")
    return result


def _validate_tiers(raw_tiers: object) -> list[PricingTier]:
    if raw_tiers is None:
        return []
    if not isinstance(raw_tiers, list):
        raise ValueError("tiers должен быть массивом")
    tiers: list[PricingTier] = []
    for raw in raw_tiers:
        if not isinstance(raw, dict):
            raise ValueError("Каждый tier должен быть объектом")
        try:
            start_i = int(raw.get("input_tokens_from"))
            end_i = int(raw["input_tokens_to"]) if raw.get("input_tokens_to") is not None else None
        except (TypeError, ValueError) as exc:
            raise ValueError("Некорректные границы pricing tier") from exc
        if start_i < 0 or (end_i is not None and end_i < start_i):
            raise ValueError("Некорректный диапазон pricing tier")
        input_price = _number(raw.get("input")); output_price = _number(raw.get("output"))
        if input_price is None or output_price is None:
            raise ValueError("Известный pricing tier обязан иметь input и output")
        tiers.append(PricingTier(
            input_tokens_from=start_i, input_tokens_to=end_i, input=input_price, output=output_price,
            cache_read=_number(raw.get("cache_read")), cache_write=_number(raw.get("cache_write")),
            thinking_input=_number(raw.get("thinking_input")), thinking_output=_number(raw.get("thinking_output")),
            thinking_cache_read=_number(raw.get("thinking_cache_read")), thinking_cache_write=_number(raw.get("thinking_cache_write")),
        ))
    tiers.sort(key=lambda t: t.input_tokens_from)
    previous_end: int | None = None
    for index, tier in enumerate(tiers):
        if index and previous_end is None:
            raise ValueError("После tier без верхней границы не может быть следующего tier")
        if previous_end is not None and tier.input_tokens_from <= previous_end:
            raise ValueError("Pricing tier ranges пересекаются")
        previous_end = tier.input_tokens_to
    return tiers


def _validate_agent_payload(job: dict, expected_request_id: str, expected_models: list[str], payload: dict) -> tuple[list[PricingInfo], dict]:
    if str(payload.get("protocol") or "") != PROTOCOL:
        raise ValueError("Неверный protocol в ответе Pricing Agent")
    if str(payload.get("request_id") or "") != expected_request_id:
        raise ValueError("request_id ответа не совпадает с заданием")
    if str(payload.get("provider_id") or "") != str(job["provider_id"]):
        raise ValueError("provider_id ответа не совпадает с заданием")
    status = str(payload.get("status") or "").lower()
    if status == "failed":
        raise ValueError("Pricing Agent сообщил status=failed")
    if status not in {"complete", "partial"}:
        raise ValueError("Некорректный status ответа Pricing Agent")
    models_raw = payload.get("models")
    if not isinstance(models_raw, list):
        raise ValueError("models должен быть массивом")
    by_model: dict[str, dict] = {}
    for item in models_raw:
        if not isinstance(item, dict):
            raise ValueError("Каждая модель должна быть объектом")
        model = str(item.get("model") or "")
        if not model or model in by_model:
            raise ValueError("Пустой или повторяющийся model ID")
        by_model[model] = item
    if set(by_model) != set(expected_models) or len(by_model) != len(expected_models):
        missing = sorted(set(expected_models) - set(by_model)); extra = sorted(set(by_model) - set(expected_models))
        raise ValueError(f"Набор моделей ответа не совпадает: missing={missing[:10]} extra={extra[:10]}")

    checked_at = str(payload.get("checked_at") or _now_iso())
    infos: list[PricingInfo] = []
    known = partial = unknown = 0
    for model in expected_models:
        item = by_model[model]
        confidence = str(item.get("confidence") or "unknown").lower()
        if confidence not in {"official", "official_partial", "unknown"}:
            raise ValueError(f"{model}: некорректный confidence")
        billing = str(item.get("billing_mode") or "unknown").lower()
        if billing not in {"payg", "credits", "subscription", "unknown"}:
            raise ValueError(f"{model}: некорректный billing_mode")
        stored_billing = "payg" if billing == "unknown" else billing  # unknown must not bypass Money Guard
        currency = str(item.get("currency") or "USD").upper()
        source_url = str(item.get("source_url") or "").strip() or None
        if confidence != "unknown":
            if currency != "USD":
                raise ValueError(f"{model}: поддерживается только нормализованная валюта USD")
            if not _source_matches_official(source_url, str(job["official_site"])):
                raise ValueError(f"{model}: source_url не принадлежит официальному домену провайдера")

        tiers = _validate_tiers(item.get("tiers")) if confidence != "unknown" else []
        input_price = _number(item.get("input_price_per_m")) if confidence != "unknown" else None
        output_price = _number(item.get("output_price_per_m")) if confidence != "unknown" else None
        cache_read = _number(item.get("cache_read_price_per_m")) if confidence != "unknown" else None
        cache_write = _number(item.get("cache_write_price_per_m")) if confidence != "unknown" else None
        if tiers:
            input_price = tiers[0].input if input_price is None else input_price
            output_price = tiers[0].output if output_price is None else output_price
            cache_read = tiers[0].cache_read if cache_read is None else cache_read
            cache_write = tiers[0].cache_write if cache_write is None else cache_write

        requires_exact = bool(item.get("requires_exact_pricing", False))
        if confidence == "official_partial":
            requires_exact = True
        if confidence == "unknown":
            requires_exact = True; input_price = output_price = cache_read = cache_write = None; unknown += 1
        elif stored_billing == "payg" and (input_price is None or output_price is None):
            requires_exact = True; unknown += 1
        else:
            known += 1
            if requires_exact:
                partial += 1
        infos.append(PricingInfo(
            model=model, billing_mode=stored_billing, currency="USD",
            input_price_per_m=input_price, output_price_per_m=output_price,
            cache_read_price_per_m=cache_read, cache_write_price_per_m=cache_write,
            context_length=safe_int(item.get("context_length")), max_output_tokens=safe_int(item.get("max_output_tokens")),
            tiers=tiers, source="chatgpt_pricing_agent", source_url=source_url, checked_at=checked_at,
            confidence=confidence, requires_exact_pricing=requires_exact,
        ))
    return infos, {"known": known, "partial": partial, "unknown": unknown, "total": len(infos), "agent_status": status}


async def _submit_part(cfg: dict, parent_id: str, part_index: int, part_models: list[str], job: dict, chat_url: str) -> None:
    part_id = f"{parent_id}-p{part_index + 1:02d}"
    _save_part(part_id, parent_id, part_index, part_models)
    body = {
        "request_id": part_id,
        "kind": "echotranslate_pricing",
        "chat_url": chat_url,
        "message": _build_message(part_id, str(job["provider_id"]), str(job["provider_name"]), str(job["official_site"]), part_models),
    }
    timeout = httpx.Timeout(15.0, connect=4.0)
    async with httpx.AsyncClient(timeout=timeout) as client:
        response = await client.post(_bridge_url(cfg) + "/api/external/jobs", headers=_bridge_headers(cfg), json=body)
    response.raise_for_status()
    reply = response.json()
    if str(reply.get("request_id") or part_id) != part_id:
        raise RuntimeError("Family Bridge вернул другой request_id")
    status = str(reply.get("status") or "queued").lower()
    if status not in {"queued", "processing", "done"}:
        status = "queued"
    _update_part(part_id, status=status, bridge_job_id=str(reply.get("job_id") or reply.get("id") or part_id))


@router.get("/llm-test/pricing-agent/config")
def pricing_agent_config():
    cfg = load_config(); name, profile = _active_profile(cfg); provider_id = _provider_id(cfg)
    latest = _latest_job(provider_id)
    if latest:
        latest = {key: latest.get(key) for key in ("request_id", "status", "error_text", "created_at", "updated_at", "applied_at")}
    return {
        "provider_id": provider_id, "provider_name": name,
        "official_site_url": str(profile.get("official_site_url") or ""),
        "chatgpt_bridge_url": str(cfg.get("chatgpt_bridge_url") or DEFAULT_BRIDGE_URL),
        "pricing_chat_url": str(cfg.get("pricing_chat_url") or ""),
        "chatgpt_bridge_token_configured": bool(str(cfg.get("chatgpt_bridge_token") or "").strip()),
        "latest_job": latest,
    }


@router.post("/llm-test/pricing-agent/config")
async def pricing_agent_save_config(request: Request):
    payload = await request.json(); cfg = load_config(); name, profile = _active_profile(cfg)
    official_site = _clean_url(payload.get("official_site_url"), required=True)
    bridge_url = _clean_url(payload.get("chatgpt_bridge_url") or DEFAULT_BRIDGE_URL, required=True)
    chat_url = _clean_url(payload.get("pricing_chat_url"), required=True)
    if (urlparse(chat_url).hostname or "").lower() != "chatgpt.com":
        raise HTTPException(400, "Pricing Agent URL должен вести на chatgpt.com")
    profiles = dict(cfg.get("llm_profiles") or {}); profile["official_site_url"] = official_site; profiles[name] = profile
    updates: dict[str, object] = {"llm_profiles": profiles, "chatgpt_bridge_url": bridge_url, "pricing_chat_url": chat_url}
    token = str(payload.get("chatgpt_bridge_token") or "").strip()
    if token:
        updates["chatgpt_bridge_token"] = token
    if bool(payload.get("clear_chatgpt_bridge_token", False)):
        updates["chatgpt_bridge_token"] = ""
    saved = save_config(updates)
    return {"ok": True, "official_site_url": official_site, "chatgpt_bridge_url": bridge_url, "pricing_chat_url": chat_url,
            "chatgpt_bridge_token_configured": bool(str(saved.get("chatgpt_bridge_token") or "").strip())}


@router.post("/llm-test/pricing-agent/refresh")
async def pricing_agent_refresh():
    cfg = load_config(); provider_name, profile = _active_profile(cfg); provider_id = _provider_id(cfg)
    official_site = _clean_url(profile.get("official_site_url"), required=True); chat_url = _pricing_chat_url(cfg)
    discovered_models, warning = await _refresh_provider_models(cfg, provider_name, profile)
    models, skipped_models = _filter_pricing_models(discovered_models)
    if not models:
        raise HTTPException(400, "После локальной фильтрации не осталось текстовых моделей для Pricing Agent")
    if skipped_models:
        filter_note = f"Локально исключено {len(skipped_models)} неподходящих моделей из {len(discovered_models)}"
        warning = (warning + "; " if warning else "") + filter_note
    parent_id = "pricing-" + uuid.uuid4().hex
    _save_job(parent_id, provider_id, provider_name, official_site, models)
    job = _job(parent_id); assert job is not None
    chunks = [models[i:i + MODELS_PER_AGENT_REQUEST] for i in range(0, len(models), MODELS_PER_AGENT_REQUEST)]
    try:
        # ECHOTRANSLATE_PRICING_LAZY_PART_SUBMIT_V1
        # Persist the whole plan, but submit only the first part.
        for index, part_models in enumerate(chunks):
            _save_part(f"{parent_id}-p{index + 1:02d}", parent_id, index, part_models)
        if chunks:
            await _submit_part(cfg, parent_id, 0, chunks[0], job, chat_url)
    except Exception as exc:
        _update_job(parent_id, status="error", error_text=f"Bridge submit failed: {exc}")
        raise HTTPException(502, f"Не удалось передать Pricing Agent batch: {exc}") from exc
    _update_job(parent_id, status="queued")
    return {
        "request_id": parent_id,
        "status": "queued",
        "models": len(models),
        "models_discovered": len(discovered_models),
        "models_skipped": len(skipped_models),
        "skipped_by_reason": _filter_summary(skipped_models),
        "parts": len(chunks),
        "models_warning": warning,
    }


# ECHOTRANSLATE_PRICING_PARSER_ERROR_RECOVERY_V1
_RENDERED_PRICING_PARSER_ERRORS = (
    "Pricing response does not contain a parseable block",
    "Pricing response was found in ChatGPT, but rendered JSON could not be parsed",
)

def _is_rendered_pricing_parser_error(value: object) -> bool:
    text=str(value or "")
    return any(marker in text for marker in _RENDERED_PRICING_PARSER_ERRORS)

def _recover_rendered_pricing_parser_error(request_id: str) -> bool:
    parts=_parts(request_id)
    error_parts=[p for p in parts if p.get("status")=="error"]
    if not error_parts:
        return False
    if any(not _is_rendered_pricing_parser_error(p.get("error_text")) for p in error_parts):
        return False
    with connect() as con:
        for part in error_parts:
            con.execute(
                "UPDATE llm_pricing_agent_parts "
                "SET status='queued',error_text=NULL,response_text=NULL,updated_at=datetime('now') "
                "WHERE part_request_id=? AND status='error'",
                (str(part["part_request_id"]),),
            )
        con.execute(
            "UPDATE llm_pricing_agent_jobs "
            "SET status='queued',error_text=NULL,updated_at=datetime('now') "
            "WHERE request_id=? AND status='error'",
            (request_id,),
        )
    return True


async def _poll_part(cfg: dict, part: dict) -> dict:
    if part["status"] in {"done", "error"}:
        return part
    part_id = str(part["part_request_id"])
    timeout = httpx.Timeout(10.0, connect=3.0)
    async with httpx.AsyncClient(timeout=timeout) as client:
        response = await client.get(_bridge_url(cfg) + "/api/external/jobs/" + part_id, headers=_bridge_headers(cfg))
    response.raise_for_status(); remote = response.json()
    if str(remote.get("request_id") or part_id) != part_id:
        _update_part(part_id, status="error", error_text="Bridge poll returned wrong request_id")
    else:
        status = str(remote.get("status") or "queued").lower()
        if status in {"queued", "processing"}:
            _update_part(part_id, status=status)
        elif status in {"error", "failed"}:
            _update_part(part_id, status="error", error_text=str(remote.get("error_text") or remote.get("error") or "Family Bridge job failed"))
        elif status == "done":
            response_text = str(remote.get("response_text") or remote.get("response") or "")
            if response_text:
                _update_part(part_id, status="done", response_text=response_text)
            else:
                _update_part(part_id, status="error", error_text="Bridge status=done без response_text")
    return next(x for x in _parts(str(part["parent_request_id"])) if x["part_request_id"] == part_id)


# ECHOTRANSLATE_PRICING_POLL_30S_STOP_V1
@router.get("/llm-test/pricing-agent/jobs/{request_id}")
async def pricing_agent_job(request_id: str):
    job = _job(request_id)
    if job is None:
        raise HTTPException(404, "Pricing job не найден")
    if job["status"] == "applied":
        return {"request_id": request_id, "status": "applied", "applied_at": job.get("applied_at")}
    if job["status"] == "cancelled":
        return {"request_id": request_id, "status": "cancelled", "error": job.get("error_text") or "Остановлено пользователем"}
    if job["status"] == "error":
        if _is_rendered_pricing_parser_error(job.get("error_text")) and _recover_rendered_pricing_parser_error(request_id):
            job = _job(request_id)
            assert job is not None
        else:
            return {"request_id": request_id, "status": "error", "error": job.get("error_text")}

    cfg = load_config()
    current_parts = _parts(request_id)
    poll_errors: list[str] = []

    active = next((p for p in current_parts if p["status"] not in {"done", "error", "cancelled"}), None)
    if active is not None:
        try:
            if active.get("bridge_job_id"):
                await _poll_part(cfg, active)
            else:
                await _submit_part(cfg, request_id, int(active["part_index"]), list(active["models"]), job, _pricing_chat_url(cfg))
        except Exception as exc:
            poll_errors.append(f"{active['part_request_id']}: {exc}")

    current_parts = _parts(request_id)
    next_part = next((p for p in current_parts if p["status"] not in {"done", "error", "cancelled"}), None)
    if next_part is not None and not next_part.get("bridge_job_id"):
        try:
            await _submit_part(cfg, request_id, int(next_part["part_index"]), list(next_part["models"]), job, _pricing_chat_url(cfg))
        except Exception as exc:
            poll_errors.append(f"{next_part['part_request_id']}: submit failed: {exc}")
        current_parts = _parts(request_id)

    cancelled = [p for p in current_parts if p["status"] == "cancelled"]
    errors = [p for p in current_parts if p["status"] == "error"]
    done = [p for p in current_parts if p["status"] == "done"]
    if cancelled:
        _update_job(request_id, status="cancelled", error_text="Остановлено пользователем")
        return {"request_id": request_id, "status": "cancelled", "parts_done": len(done), "parts_total": len(current_parts)}
    if errors:
        message = "; ".join(str(p.get("error_text") or p["part_request_id"]) for p in errors[:5])
        _update_job(request_id, status="error", error_text=message)
        return {"request_id": request_id, "status": "error", "error": message, "parts_done": len(done), "parts_total": len(current_parts)}
    if len(done) != len(current_parts):
        status_value = "processing" if any(p["status"] == "processing" for p in current_parts) else "queued"
        _update_job(request_id, status=status_value)
        return {"request_id": request_id, "status": status_value, "parts_done": len(done), "parts_total": len(current_parts), "poll_error": "; ".join(poll_errors[:3]) if poll_errors else ""}

    all_infos: list[PricingInfo] = []
    totals = {"known": 0, "partial": 0, "unknown": 0, "total": 0}
    try:
        for part in current_parts:
            payload = _extract_payload(str(part.get("response_text") or ""))
            infos, summary = _validate_agent_payload(job, str(part["part_request_id"]), list(part["models"]), payload)
            all_infos.extend(infos)
            for key in totals:
                totals[key] += int(summary.get(key) or 0)
        expected = set(job["models"]); got = [info.model for info in all_infos]
        if len(got) != len(expected) or set(got) != expected:
            raise ValueError("После объединения частей набор моделей не совпадает с исходным")
        saved = save_pricing_agent_batch(str(job["provider_id"]), all_infos)
    except Exception as exc:
        logger.exception("PRICING AGENT BATCH REJECTED request_id=%s", request_id)
        _update_job(request_id, status="error", error_text=str(exc))
        return {"request_id": request_id, "status": "error", "error": str(exc), "parts_done": len(done), "parts_total": len(current_parts)}

    _update_job(request_id, status="applied", error_text="", applied=True)
    return {"request_id": request_id, "status": "applied", "summary": {**totals, **saved}, "parts_done": len(done), "parts_total": len(current_parts)}


@router.post("/llm-test/pricing-agent/jobs/{request_id}/stop")
async def pricing_agent_stop(request_id: str):
    job = _job(request_id)
    if job is None:
        raise HTTPException(404, "Pricing job не найден")
    if job["status"] in {"applied", "error", "cancelled"}:
        return {"request_id": request_id, "status": job["status"]}

    cfg = load_config()
    remote_errors: list[str] = []
    for part in _parts(request_id):
        if part["status"] in {"done", "error", "cancelled"}:
            continue
        if part.get("bridge_job_id"):
            try:
                timeout = httpx.Timeout(8.0, connect=3.0)
                async with httpx.AsyncClient(timeout=timeout) as client:
                    response = await client.post(_bridge_url(cfg) + "/api/external/jobs/" + str(part["part_request_id"]) + "/cancel", headers=_bridge_headers(cfg))
                response.raise_for_status()
            except Exception as exc:
                remote_errors.append(f"{part['part_request_id']}: {exc}")
        _update_part(str(part["part_request_id"]), status="cancelled", error_text="Остановлено пользователем")

    detail = "Остановлено пользователем"
    if remote_errors:
        detail += "; Bridge cancel warnings: " + "; ".join(remote_errors[:3])
    _update_job(request_id, status="cancelled", error_text=detail)
    return {"request_id": request_id, "status": "cancelled", "warning": "; ".join(remote_errors[:3])}
