from __future__ import annotations
import asyncio
from collections import deque
import hashlib
import hmac
import json
import os
from pathlib import Path
import sys
import threading
import time
import uuid
from types import SimpleNamespace
import cv2
import httpx
import logging
import numpy as np
from fastapi import FastAPI, UploadFile, File, Form, HTTPException, Request, Security, BackgroundTasks
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from .bootstrap import configure_paddlex_cache
from .config import ROOT, load_config, save_config
from .db import init_db, add_messages, recent_messages, list_sessions, clear_session, history_translation_hits, register_source, list_history_sources, source_history, history_source_name, history_source_info, set_history_source_image, clear_history_source, history_message, history_message_by_id, history_context_before, list_test_history_messages, add_llm_test_result, llm_test_model_history, game_glossary, glossary_state, glossary_scan_rows, update_glossary_state, merge_game_glossary, save_game_glossary, delete_game_glossary, delete_history_message, update_history_translation, update_history_message, create_pending_history_correction, pending_history_corrections, resolve_pending_history_correction, save_llm_test_run, latest_llm_test_run, llm_model_notes, save_llm_model_note
from .ocr_engine import ocr_engine, merge_ocr_blocks
from .token_budget import TokenBudget
from .prompting import glossary_text, game_glossary_text, target_text, build_messages
from .llm import translate_openai_compatible, extract_glossary_openai_compatible
from .model_testing import router as model_testing_router

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

terminal_lines: deque[str] = deque(maxlen=500)
_glossary_scans: set[str] = set()
_glossary_scan_lock = threading.Lock()


class TerminalLogHandler(logging.Handler):
    def emit(self, record: logging.LogRecord) -> None:
        try:
            terminal_lines.append(self.format(record))
        except Exception:
            self.handleError(record)


terminal_handler = TerminalLogHandler()
terminal_handler.setFormatter(logging.Formatter("%(asctime)s  %(levelname)s  %(name)s  %(message)s", "%H:%M:%S"))
logging.getLogger().addHandler(terminal_handler)


class HistoryCorrectionRequest(BaseModel):
    history_id: int
    proposed_source_text: str | None = None
    proposed_translation: str | None = None
    client_request_id: str | None = None

app = FastAPI(title="Overlay Translation Server", version="1.0.0")
app.include_router(model_testing_router)
templates = Jinja2Templates(directory=str(ROOT / "app" / "templates"))
GAME_IMAGES = ROOT / "uploads" / "game-images"
GAME_IMAGES.mkdir(parents=True, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=str(ROOT / "uploads")), name="uploads")
bearer_scheme = HTTPBearer(
    auto_error=False,
    description="Server API key из config.json. При пустом server_api_key авторизация отключена.",
)

@app.on_event("startup")
def startup():
    # Re-test after the server logger is active and before accepting requests.
    configure_paddlex_cache()
    init_db()


def check_auth(credentials: HTTPAuthorizationCredentials | None = Security(bearer_scheme)):
    key = str(load_config().get("server_api_key") or "").strip()
    if not key:
        return
    supplied = credentials.credentials if credentials and credentials.scheme.lower() == "bearer" else ""
    if not hmac.compare_digest(supplied, key):
        raise HTTPException(
            status_code=401,
            detail="Invalid server API key",
            headers={"WWW-Authenticate": "Bearer"},
        )


async def _maybe_scan_glossary(source_id: str, force: bool = False) -> dict:
    with _glossary_scan_lock:
        if source_id in _glossary_scans:
            raise RuntimeError("Анализ уже выполняется")
        _glossary_scans.add(source_id)
    try:
        state, rows = glossary_scan_rows(source_id)
        fresh = [row for row in rows if row["id"] > state["glossary_scanned_until_history_id"]]
        cfg = load_config()
        budget = TokenBudget(str(cfg.get("tokenizer_encoding", "o200k_base")))
        pending = sum(int(row.get("token_count") or budget.count(f"SOURCE: {row['source_text']}\nTRANSLATION: {row['translation']}")) for row in fresh)
        update_glossary_state(source_id, int(state["glossary_scanned_until_history_id"]), pending)
        logger.info("GLOSSARY PENDING source=%s tokens=%s", source_id, pending)
        if not fresh or (pending < 10000 and not force):
            return {"added": 0, "entries": []}
        checkpoint = max(row["id"] for row in fresh)
        logger.info("GLOSSARY SCAN START source=%s from=%s to=%s tokens=%s", source_id, state["glossary_scanned_until_history_id"], checkpoint, pending)
        existing = game_glossary(source_id)
        records = "\n".join(f"SOURCE: {row['source_text']}\nTRANSLATION: {row['translation']}" for row in rows)
        messages = [
            {"role": "system", "content": (
                "You curate a localization CONSISTENCY glossary, not a dictionary. Extract ONLY entries whose translation must remain canonical across the game.\n"
                "Allowed types: PERSON = proper character/person names; LOCATION = named places; ORG = named organizations/factions/companies; TERM = genuinely game-specific invented/lore/mechanic term that needs one fixed translation.\n"
                "TERM may include a unique named item, ability, rank or title only when it functions as a proper game term. Never include generic items, generic ranks, ordinary vocabulary, common nouns, verbs, adjectives, pronouns, UI words, idioms, descriptive phrases, dialogue fragments or full sentences.\n"
                "Do not add a word merely because it is capitalized. If unsure, OMIT it. Prefer an empty list over a questionable entry.\n"
                "For every entry return stable=true only when the same canonical translation should be reused verbatim. For TERM also return special_term=true only when it is genuinely game-specific.\n"
                "Return strict JSON: {\"entries\":[{\"source\":\"...\",\"translation\":\"...\",\"type\":\"PERSON|LOCATION|ORG|TERM\",\"confidence\":0.98,\"stable\":true,\"special_term\":false}]}."
            )},
            {"role": "user", "content": "EXISTING_GLOSSARY:\n" + "\n".join(f"{row['source_text']} => {row['translation']}" for row in existing) + "\n\nNEW_HISTORY:\n" + records},
        ]
        entries = await extract_glossary_openai_compatible(cfg=cfg, messages=messages)
        added, conflicts, added_entries = merge_game_glossary(source_id, entries)
        update_glossary_state(source_id, checkpoint, 0)
        logger.info("GLOSSARY SCAN DONE source=%s added=%s conflicts=%s", source_id, added, conflicts)
        return {"added": added, "entries": added_entries}
    except Exception as e:
        logger.exception("GLOSSARY SCAN FAILED source=%s reason=%s", source_id, e)
        raise
    finally:
        with _glossary_scan_lock:
            _glossary_scans.discard(source_id)


async def _scan_glossary_background(source_id: str) -> None:
    try:
        await _maybe_scan_glossary(source_id)
    except Exception:
        pass

@app.get("/health")
def health():
    cfg = load_config()
    return {"ok": True, "service": "overlay-translation-server", "model": cfg.get("llm_model"), "port": cfg.get("port")}


@app.get("/api/ocr/status")
def api_ocr_status():
    cfg = load_config()
    return {
        "configured_backend": cfg.get("ocr_backend", "paddle_cpu"),
        "fallback_to_paddle": bool(cfg.get("ocr_fallback_to_paddle", True)),
        **ocr_engine.status(),
    }


@app.post("/v1/history/corrections")
def submit_history_correction(payload: HistoryCorrectionRequest, _: None = Security(check_auth)):
    proposed_source_text = payload.proposed_source_text.strip() if payload.proposed_source_text is not None else None
    proposed_translation = payload.proposed_translation.strip() if payload.proposed_translation is not None else None
    if not proposed_source_text and not proposed_translation:
        raise HTTPException(400, "Provide proposed_source_text or proposed_translation")
    client_request_id = payload.client_request_id.strip() if payload.client_request_id else None
    correction = create_pending_history_correction(payload.history_id, proposed_source_text or None, proposed_translation or None, client_request_id)
    if correction is None:
        raise HTTPException(404, "History entry not found")
    return {"id": correction["id"], "status": correction["status"]}

@app.post("/v1/screen/translate")
async def translate_screen(
    background_tasks: BackgroundTasks,
    image: UploadFile = File(...),
    source_lang: str = Form("auto"),
    target_lang: str = Form("ru"),
    session_id: str = Form("default"),
    app_package: str = Form(""),
    app_name: str = Form(""),
    _: None = Security(check_auth),
):
    total_start = time.perf_counter()
    request_id = os.urandom(4).hex()
    decode_ms = ocr_ms = merge_ms = context_ms = llm_ms = save_ms = 0.0
    logger.info("PHONE REQUEST RECEIVED id=%s at=%s", request_id, time.strftime("%Y-%m-%d %H:%M:%S"))
    logger.info(
        "REMOTE TRANSLATE REQUEST:\nsource_lang=%s\ntarget_lang=%s\nsession_id=%s\ncontent_type=%s",
        source_lang, target_lang, session_id, image.content_type,
    )
    cfg = load_config()
    if app_package.strip():
        source_id = app_package.strip()
        source_name = app_name.strip() or "Default"
        register_source(source_id, app_name.strip())
    else:
        source_id = "default"
        source_name = "Default"
        register_source(source_id, "")
    source_name = history_source_name(source_id) or source_name
    logger.info("HISTORY SOURCE id=%s name=%s", source_id, source_name)
    decode_start = time.perf_counter()
    raw = await image.read()
    if not raw:
        raise HTTPException(400, "Empty image")
    if len(raw) > 20 * 1024 * 1024:
        raise HTTPException(413, "Image is too large")
    arr = np.frombuffer(raw, np.uint8)
    frame = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    decode_ms = (time.perf_counter() - decode_start) * 1000
    if frame is None:
        raise HTTPException(400, "Unsupported/corrupted image")

    try:
        ocr_start = time.perf_counter()
        blocks = await asyncio.to_thread(
            ocr_engine.recognize,
            frame, source_lang, float(cfg.get("ocr_min_confidence", 0.45)),
            backend=cfg.get("ocr_backend", "paddle_cpu"),
            fallback_to_paddle=bool(cfg.get("ocr_fallback_to_paddle", True)),
        )
        ocr_ms = (time.perf_counter() - ocr_start) * 1000
    except Exception as e:
        raise HTTPException(503, f"OCR unavailable: {e}") from e
    raw_blocks_count = len(blocks)
    logger.info("OCR RAW BLOCKS: %s", raw_blocks_count)
    merge_start = time.perf_counter()
    if cfg.get("ocr_merge_horizontal_blocks", True):
        blocks = merge_ocr_blocks(blocks)
    merge_ms = (time.perf_counter() - merge_start) * 1000
    logger.info("OCR MERGED BLOCKS: %s", len(blocks))
    if not blocks:
        logger.info("PHONE RESPONSE RETURNED id=%s at=%s", request_id, time.strftime("%Y-%m-%d %H:%M:%S"))
        return {"blocks": [], "meta": {"session_id": session_id, "ocr_blocks": 0}}

    normalized = "\n".join(b.source.strip() for b in blocks)
    screen_hash = hashlib.sha256(normalized.encode("utf-8")).hexdigest()
    history_cache_enabled = bool(cfg.get("cache_identical_screen", True))
    logger.info("HISTORY CACHE enabled=%s", str(history_cache_enabled).lower())
    logger.info("HISTORY CACHE SOURCE id=%s", source_id)
    history_hits = history_translation_hits(source_id, session_id, source_lang, target_lang, blocks) if history_cache_enabled else {}
    hit_ids = sorted(history_hits)
    miss_ids = [index for index in range(len(blocks)) if index not in history_hits]
    logger.info("HISTORY CACHE HIT ids=%s", hit_ids)
    logger.info("HISTORY CACHE MISS ids=%s", miss_ids)
    miss_blocks = [blocks[index] for index in miss_ids]

    context_start = time.perf_counter()
    glossary = "\n\n".join(part for part in (glossary_text(cfg), game_glossary_text(game_glossary(source_id))) if part)
    logger.info("GLOSSARY source=%s entries=%s", source_id, len(game_glossary(source_id)))
    target = target_text(miss_blocks, source_lang, target_lang)
    logger.info("CONTEXT SOURCE id=%s", source_id)
    history_rows = recent_messages(source_id, session_id, 1000) if cfg.get("history_enabled", True) else []
    budget = TokenBudget(str(cfg.get("tokenizer_encoding", "o200k_base")))
    try:
        fitted = budget.fit_history(
            system_prompt=cfg["system_prompt"],
            glossary_text=glossary,
            target_text=target,
            history_newest_first=history_rows,
            history_token_limit=int(cfg.get("history_token_limit", 3000)),
        )
    except ValueError as e:
        raise HTTPException(422, str(e))

    messages = build_messages(cfg, fitted.history, glossary, target)
    context_ms = (time.perf_counter() - context_start) * 1000
    translations = {index: hit[1] for index, hit in history_hits.items()}
    history_ids = {index: hit[0] for index, hit in history_hits.items()}
    if miss_blocks and not str(cfg.get("llm_api_key") or "").strip() and "localhost" not in str(cfg.get("llm_base_url", "")) and "127.0.0.1" not in str(cfg.get("llm_base_url", "")):
        raise HTTPException(503, "LLM API key is not configured")
    if miss_blocks:
        try:
            logger.info(
                "CALLING LLM:\neffective_source_lang=%s\neffective_target_lang=%s\nblocks=%s\nhistory_items=%s\nhistory_tokens=%s\nhistory_token_limit=%s\nprompt_tokens=%s",
                source_lang, target_lang, len(miss_blocks), len(fitted.history),
                fitted.history_tokens, int(cfg.get("history_token_limit", 3000)), fitted.prompt_tokens,
            )
            logger.info("LLM BATCH BLOCKS: %s", len(miss_blocks))
            llm_start = time.perf_counter()
            try:
                miss_translations = await translate_openai_compatible(cfg=cfg, messages=messages)
            finally:
                llm_ms = (time.perf_counter() - llm_start) * 1000
            translations.update({miss_ids[index]: value for index, value in miss_translations.items() if index < len(miss_ids)})
            logger.info("LLM RESULT:\n%r", miss_translations)
        except Exception as e:
            logger.exception("LLM request failed in /v1/screen/translate")
            total_ms = (time.perf_counter() - total_start) * 1000
            logger.info(
                "TIMING id=%s status=502 decode=%.2fms ocr=%.2fms merge=%.2fms context=%.2fms llm=%.2fms save=%.2fms total=%.2fms",
                request_id, decode_ms, ocr_ms, merge_ms, context_ms, llm_ms, save_ms, total_ms,
            )
            raise HTTPException(502, f"LLM request failed: {e}") from e
    else:
        logger.info("LLM BATCH BLOCKS: 0")

    response_blocks = []
    db_rows = []
    for i, b in enumerate(blocks):
        translated = translations.get(i)
        if not translated:
            logger.warning(
                "SOURCE COPIED TO TRANSLATION: source=%r (LLM was called but returned no translation for block=%s)",
                b.source, i,
            )
            translated = b.source  # fail-soft for one omitted block
        response_blocks.append({
            "source": b.source,
            "translation": translated,
            "confidence": round(float(b.confidence), 5),
            "box": b.box,
            "language": source_lang,
            "history_id": history_ids.get(i),
        })
        logger.info("SAVE TRANSLATION:\nsource=%r\ntranslation=%r", b.source, translated)
        if i in miss_ids:
            db_rows.append((b.source, translated, float(b.confidence), json.dumps(b.box), source_lang, target_lang, screen_hash, budget.count(f"SOURCE: {b.source}\nTRANSLATION: {translated}")))

    save_start = time.perf_counter()
    inserted_ids = add_messages(source_id, source_name, session_id, db_rows)
    for block_index, history_id in zip(miss_ids, inserted_ids):
        response_blocks[block_index]["history_id"] = history_id
    save_ms = (time.perf_counter() - save_start) * 1000
    total_ms = (time.perf_counter() - total_start) * 1000
    response = {
        "blocks": response_blocks,
        "meta": {
            "session_id": session_id,
            "ocr_blocks": len(blocks),
            "history_items_used": len(fitted.history),
            "history_tokens_used": fitted.history_tokens,
            "history_token_limit": int(cfg.get("history_token_limit", 3000)),
            "estimated_prompt_tokens": fitted.prompt_tokens,
            "response_token_limit": int(cfg["max_output_tokens"]),
            "ocr": ocr_engine.diagnostics(),
            "timings": {
                "decode_ms": decode_ms,
                "ocr_ms": ocr_ms,
                "merge_ms": merge_ms,
                "context_ms": context_ms,
                "llm_ms": llm_ms,
                "save_ms": save_ms,
                "total_ms": total_ms,
            },
        },
    }
    background_tasks.add_task(_scan_glossary_background, source_id)
    logger.info(
        "TIMING id=%s status=200 decode=%.2fms ocr=%.2fms merge=%.2fms context=%.2fms llm=%.2fms save=%.2fms total=%.2fms",
        request_id, decode_ms, ocr_ms, merge_ms, context_ms, llm_ms, save_ms, total_ms,
    )
    logger.info("PHONE RESPONSE RETURNED id=%s at=%s", request_id, time.strftime("%Y-%m-%d %H:%M:%S"))
    return response


@app.post("/admin/ocr-test")
async def admin_ocr_test(
    image: UploadFile = File(...),
    source_lang: str = Form("auto"),
    target_lang: str = Form("ru"),
    translate: bool = Form(False),
):
    """Run OCR independently of the Android endpoint, history, and screen cache."""
    cfg = load_config()
    raw = await image.read()
    if not raw:
        raise HTTPException(400, "Empty image")
    if len(raw) > 20 * 1024 * 1024:
        raise HTTPException(413, "Image is too large")
    frame = cv2.imdecode(np.frombuffer(raw, np.uint8), cv2.IMREAD_COLOR)
    if frame is None:
        raise HTTPException(400, "Unsupported/corrupted image")
    try:
        blocks = await asyncio.to_thread(
            ocr_engine.recognize,
            frame, source_lang, float(cfg.get("ocr_min_confidence", 0.45)),
            backend=cfg.get("ocr_backend", "paddle_cpu"),
            fallback_to_paddle=bool(cfg.get("ocr_fallback_to_paddle", True)),
        )
        if cfg.get("ocr_merge_horizontal_blocks", True):
            blocks = merge_ocr_blocks(blocks)
    except Exception as e:
        raise HTTPException(503, f"OCR unavailable: {e}") from e

    translations: dict[int, str] = {}
    if translate and blocks:
        if not str(cfg.get("llm_api_key") or "").strip() and "localhost" not in str(cfg.get("llm_base_url", "")) and "127.0.0.1" not in str(cfg.get("llm_base_url", "")):
            raise HTTPException(503, "LLM API key is not configured")
        messages = build_messages(
            cfg,
            [],
            glossary_text(cfg),
            target_text(blocks, source_lang, target_lang),
        )
        try:
            translations = await translate_openai_compatible(cfg=cfg, messages=messages)
        except Exception as e:
            raise HTTPException(502, f"LLM request failed: {e}") from e

    return {
        "blocks": [
            {
                "source": block.source,
                "translation": translations.get(index) if translate else None,
                "confidence": round(float(block.confidence), 5),
                "box": block.box,
                "language": block.language,
            }
            for index, block in enumerate(blocks)
        ],
        "meta": {"ocr_blocks": len(blocks), "width": int(frame.shape[1]), "height": int(frame.shape[0]), "translated": translate, "ocr": ocr_engine.diagnostics()},
    }

class SettingsUpdate(BaseModel):
    server_api_key: str | None = None
    target_lang: str | None = None
    ocr_min_confidence: float | None = None
    ocr_backend: str | None = None
    ocr_fallback_to_paddle: bool | None = None
    ocr_merge_horizontal_blocks: bool | None = None
    llm_base_url: str | None = None
    llm_api_key: str | None = None
    llm_model: str | None = None
    history_token_limit: int | None = None
    max_output_tokens: int | None = None
    tokenizer_encoding: str | None = None
    history_enabled: bool | None = None
    glossary_enabled: bool | None = None
    system_prompt: str | None = None
    glossary: dict[str, str] | None = None
    cache_identical_screen: bool | None = None

@app.get("/admin", response_class=HTMLResponse)
def admin(request: Request):
    cfg = load_config().copy()
    cfg["llm_profile_names"] = sorted((cfg.get("llm_profiles") or {}).keys())
    active_profile = (cfg.get("llm_profiles") or {}).get(cfg.get("active_llm_profile"), {})
    active_models = [str(model) for model in active_profile.get("llm_models", []) if str(model)]
    if not active_models and active_profile.get("llm_model"):
        active_models = [str(active_profile["llm_model"])]
    active_pings = dict(active_profile.get("llm_model_pings_ms") or {})
    cfg["active_llm_models"] = sorted(active_models, key=lambda model: (active_pings.get(model, float("inf")), model.lower()))
    cfg["llm_api_key_configured"] = bool(str(cfg.get("llm_api_key") or "").strip())
    cfg["server_api_key_configured"] = bool(str(cfg.get("server_api_key") or "").strip())
    # Do not expose secrets into page source.
    cfg["llm_api_key"] = "" if cfg.get("llm_api_key") else ""
    cfg["server_api_key"] = "" if cfg.get("server_api_key") else ""
    return templates.TemplateResponse("admin.html", {"request": request, "cfg": cfg, "sessions": list_sessions(), "pending_corrections": pending_history_corrections()})


@app.post("/admin/history-corrections/{correction_id}/{action}")
def resolve_admin_history_correction(correction_id: int, action: str):
    if action not in {"accept", "reject"}:
        raise HTTPException(404, "Unknown correction action")
    if not resolve_pending_history_correction(correction_id, action == "accept"):
        raise HTTPException(404, "Pending correction or history entry not found")
    return {"ok": True, "id": correction_id, "status": "accepted" if action == "accept" else "rejected"}


@app.get("/history", response_class=HTMLResponse)
def history(request: Request):
    return templates.TemplateResponse("history.html", {"request": request, "sources": list_history_sources()})


@app.get("/history/{source_id}", response_class=HTMLResponse)
def history_source(request: Request, source_id: str):
    source_name = history_source_name(source_id)
    if source_name is None:
        raise HTTPException(404, "History source not found")
    return templates.TemplateResponse("history_source.html", {"request": request, "source_id": source_id, "source_name": source_name, "source": history_source_info(source_id), "rows": source_history(source_id)})


@app.post("/history/{source_id}/image")
async def history_source_image(source_id: str, image: UploadFile = File(...)):
    if image.content_type not in {"image/jpeg", "image/png", "image/webp"}:
        raise HTTPException(400, "Поддерживаются JPG, PNG и WebP")
    data = await image.read()
    if not data or len(data) > 5 * 1024 * 1024:
        raise HTTPException(413, "Размер изображения должен быть не больше 5 MB")
    extension = {"image/jpeg": ".jpg", "image/png": ".png", "image/webp": ".webp"}[image.content_type]
    relative = f"game-images/{uuid.uuid4().hex}{extension}"
    destination = (ROOT / "uploads" / relative).resolve()
    destination.write_bytes(data)
    old = set_history_source_image(source_id, relative)
    if old:
        old_path = (ROOT / "uploads" / old).resolve()
        if old_path.parent == GAME_IMAGES and old_path.exists():
            old_path.unlink()
    return HTMLResponse(f'<meta http-equiv="refresh" content="0;url=/history/{source_id}">')


@app.get("/history/{source_id}/glossary", response_class=HTMLResponse)
def history_glossary(request: Request, source_id: str):
    source_name = history_source_name(source_id)
    if source_name is None:
        raise HTTPException(404, "History source not found")
    state = glossary_state(source_id)
    return templates.TemplateResponse("glossary.html", {"request": request, "source_id": source_id, "source_name": source_name, "entries": game_glossary(source_id), "state": state})


@app.post("/history/{source_id}/glossary/scan")
async def history_glossary_scan(source_id: str):
    if history_source_name(source_id) is None:
        raise HTTPException(404, "History source not found")
    try:
        return await _maybe_scan_glossary(source_id, force=True)
    except RuntimeError as e:
        raise HTTPException(409, str(e)) from e
    except Exception as e:
        raise HTTPException(502, f"Ошибка анализа: {e}") from e


@app.post("/history/{source_id}/glossary/save")
async def history_glossary_save(source_id: str, request: Request):
    form = await request.form()
    source_text, translation = str(form.get("source_text") or "").strip(), str(form.get("translation") or "").strip()
    kind = str(form.get("type") or "TERM").strip().upper()
    if not source_text or not translation or kind not in {"PERSON", "LOCATION", "ORG", "TERM"}:
        raise HTTPException(400, "Заполните оригинал, перевод и корректный тип")
    try:
        entry = save_game_glossary(source_id, source_text, translation, kind, int(form["id"]) if form.get("id") else None)
    except Exception as e:
        raise HTTPException(409, f"Не удалось сохранить запись: {e}") from e
    if entry is None:
        raise HTTPException(404, "Glossary entry not found")
    return {"ok": True, "entry": entry, "updated_history": int(entry.get("history_updates", 0)), "updated_occurrences": int(entry.get("history_replacements", 0))}


@app.post("/history/{source_id}/glossary/{entry_id}/delete")
def history_glossary_delete(source_id: str, entry_id: int):
    if not delete_game_glossary(source_id, entry_id):
        raise HTTPException(404, "Glossary entry not found")
    return {"ok": True}


def _llm_test_provider_id(cfg: dict) -> str:
    profile = str(cfg.get("active_llm_profile") or "").strip()
    return f"profile:{profile}" if profile else f"base_url:{str(cfg.get('llm_base_url') or '').strip().rstrip('/')}"


@app.post("/history/{source_id}/clear")
def history_clear_source(source_id: str):
    clear_history_source(source_id)
    return {"ok": True}


@app.post("/history/{source_id}/{message_id}/delete")
def history_delete_message(source_id: str, message_id: int):
    if not delete_history_message(source_id, message_id):
        raise HTTPException(404, "History entry not found")
    return {"ok": True}


@app.post("/history/{source_id}/{message_id}/edit")
async def history_edit_message(source_id: str, message_id: int, request: Request):
    form = await request.form()
    source_text, translation = str(form.get("source_text") or "").strip(), str(form.get("translation") or "").strip()
    if not source_text or not translation:
        raise HTTPException(400, "Оригинал и перевод не могут быть пустыми")
    if not update_history_message(source_id, message_id, source_text, translation):
        raise HTTPException(404, "History entry not found")
    return {"ok": True, "source_text": source_text, "translation": translation}


@app.post("/history/{source_id}/{message_id}/retranslate")
async def history_retranslate_message(source_id: str, message_id: int):
    row = history_message(source_id, message_id)
    if row is None:
        raise HTTPException(404, "History entry not found")
    cfg = load_config()
    source_lang = str(row.get("language") or "auto")
    target_lang = str(row.get("target_language") or cfg.get("target_lang", "ru"))
    target = target_text([SimpleNamespace(source=row["source_text"])], source_lang, target_lang)
    history_rows = recent_messages(source_id, row["session_id"], 1000, exclude_id=message_id) if cfg.get("history_enabled", True) else []
    budget = TokenBudget(str(cfg.get("tokenizer_encoding", "o200k_base")))
    try:
        fitted = budget.fit_history(
            system_prompt=cfg["system_prompt"],
            glossary_text=glossary_text(cfg),
            target_text=target,
            history_newest_first=history_rows,
            history_token_limit=int(cfg.get("history_token_limit", 3000)),
        )
        messages = build_messages(cfg, fitted.history, glossary_text(cfg), target)
        if not str(cfg.get("llm_api_key") or "").strip() and "localhost" not in str(cfg.get("llm_base_url", "")) and "127.0.0.1" not in str(cfg.get("llm_base_url", "")):
            raise HTTPException(503, "LLM API key is not configured")
        translations = await translate_openai_compatible(cfg=cfg, messages=messages)
        translation = str(translations.get(0) or "").strip()
        if not translation:
            raise ValueError("LLM returned no translation")
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("LLM retranslate failed source_id=%s message_id=%s", source_id, message_id)
        raise HTTPException(502, f"LLM request failed: {e}") from e
    if not update_history_translation(source_id, message_id, translation):
        raise HTTPException(404, "History entry not found")
    return {"ok": True, "translation": translation}


@app.post("/v1/history/{history_id}/retranslate")
async def mobile_history_retranslate(history_id: int, _: None = Security(check_auth)):
    logger.info("MOBILE RETRANSLATE history_id=%s", history_id)
    row = history_message_by_id(history_id)
    if row is None:
        raise HTTPException(404, "History entry not found")
    cfg = load_config()
    source_id = row["source_id"]
    session_id = row["session_id"]
    source_lang = str(row.get("language") or "auto")
    target_lang = str(row.get("target_language") or cfg.get("target_lang", "ru"))
    target = target_text([SimpleNamespace(source=row["source_text"])], source_lang, target_lang)
    context_rows = history_context_before(source_id, session_id, history_id, 1000) if cfg.get("history_enabled", True) else []
    glossary = "\n\n".join(part for part in (glossary_text(cfg), game_glossary_text(game_glossary(source_id))) if part)
    try:
        fitted = TokenBudget(str(cfg.get("tokenizer_encoding", "o200k_base"))).fit_history(
            system_prompt=cfg["system_prompt"], glossary_text=glossary, target_text=target,
            history_newest_first=context_rows, history_token_limit=int(cfg.get("history_token_limit", 3000)),
        )
        if not str(cfg.get("llm_api_key") or "").strip() and "localhost" not in str(cfg.get("llm_base_url", "")) and "127.0.0.1" not in str(cfg.get("llm_base_url", "")):
            raise HTTPException(503, "LLM API key is not configured")
        translations = await translate_openai_compatible(
            cfg=cfg, messages=build_messages(cfg, fitted.history, glossary, target),
        )
        translation = str(translations.get(0) or "").strip()
        if not translation:
            raise ValueError("LLM returned no translation")
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("MOBILE RETRANSLATE failed history_id=%s", history_id)
        raise HTTPException(502, f"LLM request failed: {e}") from e
    if not update_history_translation(source_id, history_id, translation):
        raise HTTPException(404, "History entry not found")
    logger.info("MOBILE RETRANSLATE OK history_id=%s", history_id)
    return {"history_id": history_id, "source": row["source_text"], "translation": translation}


@app.delete("/v1/history/{history_id}")
def mobile_history_delete(history_id: int, _: None = Security(check_auth)):
    logger.info("MOBILE DELETE history_id=%s", history_id)
    row = history_message_by_id(history_id)
    if row is None or not delete_history_message(row["source_id"], history_id):
        raise HTTPException(404, "History entry not found")
    logger.info("MOBILE DELETE OK history_id=%s", history_id)
    return {"ok": True, "history_id": history_id}


@app.get("/api/admin/terminal")
def admin_terminal():
    return {"lines": list(terminal_lines)}


@app.get("/admin/translators/new", response_class=HTMLResponse)
def admin_new_translator(request: Request):
    return templates.TemplateResponse("add_translator.html", {"request": request})


@app.post("/api/admin/llm-models")
async def admin_llm_models(request: Request):
    form = await request.form()
    base_url = str(form.get("llm_base_url", "")).strip().rstrip("/")
    api_key = str(form.get("llm_api_key", "")).strip()
    if not base_url.startswith(("http://", "https://")):
        raise HTTPException(400, "Укажите корректный адрес LLM API")
    headers = {"Authorization": f"Bearer {api_key}"} if api_key else {}
    try:
        async with httpx.AsyncClient(timeout=15) as client:
            response = await client.get(f"{base_url}/models", headers=headers)
        response.raise_for_status()
        data = response.json().get("data", [])
        models = [str(item["id"]) for item in data if isinstance(item, dict) and item.get("id")]
    except (httpx.HTTPError, ValueError, KeyError) as e:
        raise HTTPException(502, f"Не удалось получить список моделей: {e}") from e
    return {"models": models}


@app.post("/api/admin/provider-models")
async def admin_provider_models(request: Request):
    form = await request.form()
    name = str(form.get("llm_profile_name", "")).strip()
    cfg = load_config()
    profiles = dict(cfg.get("llm_profiles") or {})
    profile = profiles.get(name)
    if not isinstance(profile, dict):
        raise HTTPException(400, "Выберите сохранённого провайдера")
    base_url = str(profile.get("llm_base_url", "")).strip().rstrip("/")
    api_key = str(profile.get("llm_api_key", "")).strip()
    if not base_url.startswith(("http://", "https://")):
        raise HTTPException(400, "У провайдера не указан корректный адрес LLM API")
    headers = {"Authorization": f"Bearer {api_key}"} if api_key else {}
    try:
        async with httpx.AsyncClient(timeout=15) as client:
            response = await client.get(f"{base_url}/models", headers=headers)
        response.raise_for_status()
        data = response.json().get("data", [])
        models = sorted(str(item["id"]) for item in data if isinstance(item, dict) and item.get("id"))
    except (httpx.HTTPError, ValueError, KeyError) as e:
        raise HTTPException(502, f"Не удалось получить модели провайдера: {e}") from e
    profile["llm_models"] = models
    profiles[name] = profile
    save_config({"llm_profiles": profiles})
    pings = dict(profile.get("llm_model_pings_ms") or {})
    models.sort(key=lambda model: (pings.get(model, float("inf")), model.lower()))
    return {"models": models, "pings_ms": pings}


@app.post("/admin/translators/new")
async def admin_save_translator(request: Request):
    form = await request.form()
    name = str(form.get("profile_name", "")).strip()
    base_url = str(form.get("llm_base_url", "")).strip().rstrip("/")
    model = str(form.get("llm_model", "")).strip()
    api_key = str(form.get("llm_api_key", "")).strip()
    try:
        models = json.loads(str(form.get("llm_models", "[]")))
    except json.JSONDecodeError:
        models = []
    models = [str(item) for item in models if str(item)]
    if model not in models:
        models.append(model)
    if not name or not base_url or not model:
        raise HTTPException(400, "Укажите имя переводчика, адрес API и модель")
    cfg = load_config()
    profiles = dict(cfg.get("llm_profiles") or {})
    profiles[name] = {
        "llm_base_url": base_url,
        "llm_model": model,
        "llm_api_key": api_key,
        "llm_models": models,
    }
    save_config({
        "llm_profiles": profiles,
        "active_llm_profile": name,
        "llm_base_url": base_url,
        "llm_model": model,
        "llm_api_key": api_key,
    })
    return HTMLResponse('<meta http-equiv="refresh" content="0;url=/admin">')


@app.post("/admin/translators/select")
async def admin_select_translator(request: Request):
    form = await request.form()
    name = str(form.get("llm_profile_name", "")).strip()
    model = str(form.get("llm_model", "")).strip()
    cfg = load_config()
    profile = (cfg.get("llm_profiles") or {}).get(name)
    if not isinstance(profile, dict):
        raise HTTPException(400, "Выберите сохранённый переводчик")
    available_models = [str(item) for item in profile.get("llm_models", []) if str(item)]
    if not available_models:
        available_models = [str(profile.get("llm_model", "")).strip()]
    if model not in available_models:
        # The provider dropdown submits the currently displayed model, which
        # may belong to the previously selected provider.  Select this
        # provider's saved/default model in that case.
        model = str(profile.get("llm_model", "")).strip()
        if model not in available_models:
            model = available_models[0]
    save_config({
        "active_llm_profile": name,
        "llm_base_url": str(profile.get("llm_base_url", "")).strip(),
        "llm_model": model,
        "llm_api_key": str(profile.get("llm_api_key", "")).strip(),
    })
    return HTMLResponse('<meta http-equiv="refresh" content="0;url=/admin">')

@app.post("/admin/settings")
async def admin_settings(request: Request):
    form = await request.form()
    cfg = load_config()
    updates = {
        "target_lang": str(form.get("target_lang", cfg.get("target_lang", "ru"))).strip(),
        "ocr_min_confidence": float(form.get("ocr_min_confidence", cfg.get("ocr_min_confidence", .45))),
        "ocr_backend": str(form.get("ocr_backend", cfg.get("ocr_backend", "paddle_cpu"))).strip(),
        "ocr_fallback_to_paddle": form.get("ocr_fallback_to_paddle") == "on",
        "ocr_merge_horizontal_blocks": form.get("ocr_merge_horizontal_blocks") == "on",
        "history_token_limit": max(0, int(form.get("history_token_limit", cfg.get("history_token_limit", 3000)))),
        "max_output_tokens": int(form.get("max_output_tokens", cfg.get("max_output_tokens", 1200))),
        "tokenizer_encoding": str(form.get("tokenizer_encoding", cfg.get("tokenizer_encoding", "o200k_base"))).strip(),
        "history_enabled": form.get("history_enabled") == "on",
        "glossary_enabled": form.get("glossary_enabled") == "on",
        "cache_identical_screen": form.get("cache_identical_screen") == "on",
        "system_prompt": str(form.get("system_prompt", cfg.get("system_prompt", ""))),
        "active_system_prompt_profile_id": str(form.get("active_system_prompt_profile_id", cfg.get("active_system_prompt_profile_id", ""))),
    }
    if form.get("clear_server_api_key") == "on":
        updates["server_api_key"] = ""
    elif str(form.get("server_api_key", "")).strip():
        updates["server_api_key"] = str(form["server_api_key"]).strip()
    glossary_raw = str(form.get("glossary", "")).strip()
    if glossary_raw:
        g = {}
        for line in glossary_raw.splitlines():
            if "=>" in line:
                a, b = line.split("=>", 1)
                g[a.strip()] = b.strip()
        updates["glossary"] = g
    save_config(updates)
    return HTMLResponse('<meta http-equiv="refresh" content="0;url=/admin">')


@app.post("/admin/prompt-profile/{action}")
async def admin_prompt_profile(action: str, request: Request):
    payload = await request.json()
    cfg = load_config()
    profiles = list(cfg.get("system_prompt_profiles") or [])
    active = str(cfg.get("active_system_prompt_profile_id") or "")
    name = str(payload.get("name") or "").strip()
    if action == "new":
        if not name or any(p.get("name") == name for p in profiles):
            raise HTTPException(400, "Имя профиля должно быть уникальным")
        profile = next(p for p in profiles if p.get("id") == active)
        profiles.append({"id": uuid.uuid4().hex, "name": name, "prompt": profile.get("prompt", "")})
        active = profiles[-1]["id"]
    elif action == "rename":
        if not name or any(p.get("name") == name and p.get("id") != active for p in profiles):
            raise HTTPException(400, "Имя профиля должно быть уникальным")
        next(p for p in profiles if p.get("id") == active)["name"] = name
    elif action == "delete":
        if len(profiles) <= 1:
            raise HTTPException(400, "Нельзя удалить последний профиль")
        profiles = [p for p in profiles if p.get("id") != active]
        active = profiles[0]["id"]
    elif action == "select":
        active = str(payload.get("id") or "")
        if not any(p.get("id") == active for p in profiles):
            raise HTTPException(400, "Профиль не найден")
    else:
        raise HTTPException(404, "Unknown prompt profile action")
    save_config({"system_prompt_profiles": profiles, "active_system_prompt_profile_id": active})
    return {"ok": True}

@app.post("/admin/session/{source_id}/{session_id}/clear")
def admin_clear_session(source_id: str, session_id: str):
    clear_session(source_id, session_id)
    return HTMLResponse('<meta http-equiv="refresh" content="0;url=/admin">')

@app.post("/admin/server/{action}")
def admin_server_action(action: str):
    if action not in {"restart", "stop"}:
        raise HTTPException(404, "Unknown server action")

    def finish_action():
        time.sleep(0.5)
        if action == "restart":
            os.execv(sys.executable, [sys.executable, str(ROOT / "run_server.py")])
        os._exit(0)

    threading.Thread(target=finish_action, daemon=True).start()
    return HTMLResponse(f"Server will {action} now.")

@app.get("/api/sessions")
def api_sessions():
    return list_sessions()

@app.get("/api/session/{session_id}")
def api_session(session_id: str):
    rows = recent_messages("default", session_id, 500)
    rows.reverse()
    return rows


@app.get("/api/session/{source_id}/{session_id}")
def api_source_session(source_id: str, session_id: str):
    rows = recent_messages(source_id, session_id, 500)
    rows.reverse()
    return rows
