from __future__ import annotations
import json
import re
import sqlite3
import threading
from pathlib import Path
from typing import Iterable
from .config import ROOT, load_config

_lock = threading.RLock()

# CJK_HISTORY_FILTER_V2
# A Russian/European target translation that still contains East-Asian script
# is treated as incomplete and must never become future context.
def contains_east_asian_script(text: str | None) -> bool:
    for ch in str(text or ""):
        code = ord(ch)
        if (
            0x3400 <= code <= 0x4DBF      # CJK Extension A
            or 0x4E00 <= code <= 0x9FFF   # CJK Unified Ideographs
            or 0xF900 <= code <= 0xFAFF   # CJK Compatibility Ideographs
            or 0x20000 <= code <= 0x2EBEF # CJK Extensions B-F/I
            or 0x30000 <= code <= 0x323AF # CJK Extensions G-H
            or 0x3040 <= code <= 0x309F   # Hiragana
            or 0x30A0 <= code <= 0x30FF   # Katakana
            or 0x31F0 <= code <= 0x31FF   # Katakana extensions
            or 0xAC00 <= code <= 0xD7AF   # Hangul syllables
        ):
            return True
    return False


def target_allows_east_asian_script(target_language: str | None) -> bool:
    language = str(target_language or "").strip().lower().replace("_", "-")
    return language.startswith(("zh", "ja", "ko"))


def translation_has_disallowed_east_asian_script(
    translation: str | None,
    target_language: str | None,
) -> bool:
    return (
        not target_allows_east_asian_script(target_language)
        and contains_east_asian_script(translation)
    )



def _db_path() -> Path:
    p = Path(load_config().get("database_path", "data/server.db"))
    if not p.is_absolute():
        p = ROOT / p
    p.parent.mkdir(parents=True, exist_ok=True)
    return p


def connect() -> sqlite3.Connection:
    con = sqlite3.connect(_db_path(), check_same_thread=False)
    con.row_factory = sqlite3.Row
    return con


def init_db() -> None:
    with _lock, connect() as con:
        con.executescript("""
        PRAGMA journal_mode=WAL;
        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT (datetime('now')),
            source_text TEXT NOT NULL,
            translation TEXT NOT NULL,
            confidence REAL,
            box_json TEXT,
            language TEXT,
            screen_hash TEXT
        );
        CREATE INDEX IF NOT EXISTS idx_messages_session_id ON messages(session_id, id DESC);
        CREATE TABLE IF NOT EXISTS screen_cache (
            source_id TEXT NOT NULL,
            session_id TEXT NOT NULL,
            screen_hash TEXT NOT NULL,
            response_json TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT (datetime('now')),
            PRIMARY KEY(source_id, session_id, screen_hash)
        );
        CREATE TABLE IF NOT EXISTS history_sources (
            source_id TEXT PRIMARY KEY,
            source_name TEXT NOT NULL,
            image_path TEXT
        );
        CREATE TABLE IF NOT EXISTS llm_test_results (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            provider_id TEXT NOT NULL,
            model TEXT NOT NULL,
            test_run_id TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT (datetime('now')),
            phrase_id INTEGER,
            source_text TEXT NOT NULL,
            translation TEXT,
            duration_ms REAL,
            error TEXT,
            source_id TEXT,
            session_id TEXT
        );
        CREATE INDEX IF NOT EXISTS idx_llm_test_results_lookup
            ON llm_test_results(provider_id, model, test_run_id, id DESC);
        CREATE TABLE IF NOT EXISTS llm_test_runs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            provider_id TEXT NOT NULL,
            test_run_id TEXT NOT NULL,
            mode TEXT NOT NULL,
            models_json TEXT NOT NULL,
            phrases_json TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT (datetime('now')),
            UNIQUE(provider_id, test_run_id)
        );
        CREATE INDEX IF NOT EXISTS idx_llm_test_runs_restore
            ON llm_test_runs(provider_id, mode, id DESC);
        CREATE TABLE IF NOT EXISTS llm_model_notes (
            provider_id TEXT NOT NULL,
            model TEXT NOT NULL,
            note TEXT NOT NULL DEFAULT '',
            updated_at TEXT NOT NULL DEFAULT (datetime('now')),
            PRIMARY KEY(provider_id, model)
        );
        CREATE TABLE IF NOT EXISTS llm_model_metadata (
            provider_id TEXT NOT NULL,
            model TEXT NOT NULL,
            input_price_per_m REAL,
            output_price_per_m REAL,
            context_length INTEGER,
            updated_at TEXT NOT NULL DEFAULT (datetime('now')),
            PRIMARY KEY(provider_id, model)
        );
        CREATE TABLE IF NOT EXISTS llm_benchmark_runs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            provider_id TEXT NOT NULL,
            run_id TEXT NOT NULL,
            mode TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT (datetime('now')),
            updated_at TEXT NOT NULL DEFAULT (datetime('now')),
            status TEXT NOT NULL DEFAULT 'running',
            settings_json TEXT NOT NULL,
            state_json TEXT NOT NULL,
            reserved_cost_usd REAL NOT NULL DEFAULT 0,
            estimated_actual_cost_usd REAL NOT NULL DEFAULT 0,
            UNIQUE(provider_id, run_id)
        );
        CREATE INDEX IF NOT EXISTS idx_llm_benchmark_runs_latest
            ON llm_benchmark_runs(provider_id, mode, id DESC);
        CREATE TABLE IF NOT EXISTS game_glossary (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            source_id TEXT NOT NULL,
            source_text TEXT NOT NULL,
            translation TEXT NOT NULL,
            type TEXT NOT NULL,
            status TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT (datetime('now')),
            updated_at TEXT NOT NULL DEFAULT (datetime('now')),
            UNIQUE(source_id, source_text)
        );
        CREATE TABLE IF NOT EXISTS glossary_source_state (
            source_id TEXT PRIMARY KEY,
            glossary_scanned_until_history_id INTEGER NOT NULL DEFAULT 0,
            glossary_pending_tokens INTEGER NOT NULL DEFAULT 0
        );
        CREATE TABLE IF NOT EXISTS pending_history_corrections (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            history_id INTEGER NOT NULL,
            source_id TEXT NOT NULL,
            session_id TEXT NOT NULL,
            original_source_text TEXT NOT NULL,
            original_translation TEXT NOT NULL,
            proposed_source_text TEXT,
            proposed_translation TEXT,
            created_at TEXT NOT NULL DEFAULT (datetime('now')),
            status TEXT NOT NULL DEFAULT 'pending',
            client_request_id TEXT
        );
        CREATE UNIQUE INDEX IF NOT EXISTS idx_pending_history_corrections_client_request
            ON pending_history_corrections(client_request_id) WHERE client_request_id IS NOT NULL;
        CREATE INDEX IF NOT EXISTS idx_pending_history_corrections_status
            ON pending_history_corrections(status, id DESC);
        """)
        columns = {row[1] for row in con.execute("PRAGMA table_info(messages)")}
        source_columns = {row[1] for row in con.execute("PRAGMA table_info(history_sources)")}
        test_result_columns = {row[1] for row in con.execute("PRAGMA table_info(llm_test_results)")}
        if "history_items" not in test_result_columns:
            con.execute("ALTER TABLE llm_test_results ADD COLUMN history_items INTEGER")
        if "history_tokens" not in test_result_columns:
            con.execute("ALTER TABLE llm_test_results ADD COLUMN history_tokens INTEGER")
        if "prompt_tokens" not in test_result_columns:
            con.execute("ALTER TABLE llm_test_results ADD COLUMN prompt_tokens INTEGER")
        if "image_path" not in source_columns:
            con.execute("ALTER TABLE history_sources ADD COLUMN image_path TEXT")
        if "target_language" not in columns:
            con.execute("ALTER TABLE messages ADD COLUMN target_language TEXT")
        if "source_id" not in columns:
            con.execute("ALTER TABLE messages ADD COLUMN source_id TEXT")
        if "source_name" not in columns:
            con.execute("ALTER TABLE messages ADD COLUMN source_name TEXT")
        if "token_count" not in columns:
            con.execute("ALTER TABLE messages ADD COLUMN token_count INTEGER")
        con.execute("UPDATE messages SET source_id='default' WHERE source_id IS NULL OR source_id='' ")
        con.execute("UPDATE messages SET source_name='Default' WHERE source_name IS NULL OR source_name='' ")
        con.execute(
            "INSERT OR IGNORE INTO history_sources(source_id, source_name) "
            "SELECT DISTINCT source_id, source_name FROM messages"
        )
        con.execute("INSERT OR IGNORE INTO history_sources(source_id, source_name) VALUES('default', 'Default')")
        cache_columns = {row[1] for row in con.execute("PRAGMA table_info(screen_cache)")}
        if "source_id" not in cache_columns:
            con.executescript("""
                CREATE TABLE screen_cache_migrated (
                    source_id TEXT NOT NULL,
                    session_id TEXT NOT NULL,
                    screen_hash TEXT NOT NULL,
                    response_json TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    PRIMARY KEY(source_id, session_id, screen_hash)
                );
                INSERT INTO screen_cache_migrated(source_id, session_id, screen_hash, response_json, created_at)
                SELECT 'default', session_id, screen_hash, response_json, created_at FROM screen_cache;
                DROP TABLE screen_cache;
                ALTER TABLE screen_cache_migrated RENAME TO screen_cache;
            """)
        con.execute(
            "CREATE INDEX IF NOT EXISTS idx_messages_translation_history "
            "ON messages(source_id, session_id, language, target_language, source_text, id DESC)"
        )
        con.execute("CREATE INDEX IF NOT EXISTS idx_messages_source_id ON messages(source_id, id DESC)")
        con.execute("CREATE INDEX IF NOT EXISTS idx_messages_glossary_scan ON messages(source_id, id, token_count)")


    # CJK_HISTORY_PURGE_V2: remove contaminated rows accumulated before this filter existed.
    with _lock, connect() as con:
        dirty_rows = con.execute(
            "SELECT id, source_id, session_id, screen_hash, translation, target_language FROM messages ORDER BY id"
        ).fetchall()
        dirty_rows = [
            row for row in dirty_rows
            if translation_has_disallowed_east_asian_script(row["translation"], row["target_language"])
        ]
        for row in dirty_rows:
            con.execute("DELETE FROM pending_history_corrections WHERE history_id=?", (int(row["id"]),))
            con.execute("DELETE FROM messages WHERE id=?", (int(row["id"]),))
            if row["screen_hash"]:
                con.execute(
                    "DELETE FROM screen_cache WHERE source_id=? AND session_id=? AND screen_hash=?",
                    (row["source_id"], row["session_id"], row["screen_hash"]),
                )



def register_source(source_id: str, source_name: str) -> None:
    with _lock, connect() as con:
        con.execute(
            "INSERT OR IGNORE INTO history_sources(source_id, source_name) VALUES(?, COALESCE(NULLIF(?, ''), 'Default'))",
            (source_id, source_name),
        )
        if source_name:
            con.execute("UPDATE history_sources SET source_name=? WHERE source_id=?", (source_name, source_id))
            con.execute("UPDATE messages SET source_name=? WHERE source_id=?", (source_name, source_id))


def add_messages(source_id: str, source_name: str, session_id: str, rows: Iterable[tuple[str, str, float, str, str, str, str, int]]) -> list[int | None]:
    """Insert clean history rows; keep positional None for rejected CJK-residue rows."""
    with _lock, connect() as con:
        ids: list[int | None] = []
        for row in rows:
            translation = str(row[1] or "") if len(row) > 1 else ""
            target_language = str(row[5] or "") if len(row) > 5 else ""
            if translation_has_disallowed_east_asian_script(translation, target_language):
                # Keep output visible to the client, but never persist it as context.
                ids.append(None)
                continue
            cursor = con.execute(
                "INSERT INTO messages(source_id, source_name, session_id, source_text, translation, confidence, box_json, language, target_language, screen_hash, token_count) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                (source_id, source_name, session_id, *row),
            )
            ids.append(int(cursor.lastrowid))
        return ids



def recent_messages(source_id: str, session_id: str, limit: int = 500, exclude_id: int | None = None) -> list[dict]:
    with connect() as con:
        query = "SELECT id, created_at, source_text, translation, confidence, box_json, language FROM messages WHERE source_id=? AND session_id=?"
        params: list[object] = [source_id, session_id]
        if exclude_id is not None:
            query += " AND id<>?"
            params.append(exclude_id)
        rows = con.execute(query + " ORDER BY id DESC LIMIT ?", (*params, limit)).fetchall()
    return [dict(r) for r in rows]


def history_translation_hits(
    source_id: str, session_id: str, source_lang: str, target_lang: str, blocks: list,
) -> dict[int, tuple[int, str]]:
    """Fetch latest exact-text/near-identical-box translations in one SQL query."""
    if not blocks:
        return {}
    values = ",".join("(?,?,?,?,?,?)" for _ in blocks)
    params: list[object] = []
    for index, block in enumerate(blocks):
        x1, y1, x2, y2 = block.box
        params.extend((index, block.source, x1, y1, x2, y2))
    params.extend((source_id, session_id, source_lang, target_lang))
    query = f"""
        WITH requested(id, source_text, x1, y1, x2, y2) AS (VALUES {values})
        SELECT requested.id, messages.id AS history_id, messages.translation
        FROM requested
        JOIN messages ON messages.id = (
            SELECT candidate.id
            FROM messages AS candidate
            WHERE candidate.source_id = ?
              AND candidate.session_id = ?
              AND candidate.language = ?
              AND candidate.target_language = ?
              AND candidate.source_text = requested.source_text
              AND abs(json_extract(candidate.box_json, '$[0]') - requested.x1) <= 3
              AND abs(json_extract(candidate.box_json, '$[1]') - requested.y1) <= 3
              AND abs(json_extract(candidate.box_json, '$[2]') - requested.x2) <= 3
              AND abs(json_extract(candidate.box_json, '$[3]') - requested.y2) <= 3
            ORDER BY candidate.id DESC LIMIT 1
        )
    """
    with connect() as con:
        rows = con.execute(query, params).fetchall()
    return {int(row["id"]): (int(row["history_id"]), str(row["translation"])) for row in rows}


def list_sessions() -> list[dict]:
    with connect() as con:
        rows = con.execute("""
            SELECT source_id, session_id, COUNT(*) AS messages, MAX(created_at) AS last_seen
            FROM messages GROUP BY source_id, session_id ORDER BY MAX(id) DESC
        """).fetchall()
    return [dict(r) for r in rows]


def list_history_sources() -> list[dict]:
    with connect() as con:
        rows = con.execute("""
            SELECT m.source_id, COALESCE(s.source_name, MAX(m.source_name), 'Default') AS source_name, s.image_path,
                   SUM(length(m.source_text) + length(m.translation) + length(COALESCE(m.box_json, ''))) AS history_bytes
            FROM messages AS m
            LEFT JOIN history_sources AS s ON s.source_id = m.source_id
            GROUP BY m.source_id
            ORDER BY MAX(m.id) DESC
        """).fetchall()
    return [dict(r) for r in rows]


def source_history(source_id: str) -> list[dict]:
    with connect() as con:
        rows = con.execute(
            "SELECT id, created_at, source_text, translation FROM messages WHERE source_id=? ORDER BY id DESC",
            (source_id,),
        ).fetchall()
    return [dict(r) for r in rows]


def history_source_name(source_id: str) -> str | None:
    with connect() as con:
        row = con.execute("SELECT source_name FROM history_sources WHERE source_id=?", (source_id,)).fetchone()
    return str(row[0]) if row else None


def history_source_info(source_id: str) -> dict | None:
    with connect() as con:
        row = con.execute("SELECT source_name, image_path FROM history_sources WHERE source_id=?", (source_id,)).fetchone()
    return dict(row) if row else None


def set_history_source_image(source_id: str, image_path: str) -> str | None:
    with _lock, connect() as con:
        old = con.execute("SELECT image_path FROM history_sources WHERE source_id=?", (source_id,)).fetchone()
        con.execute("UPDATE history_sources SET image_path=? WHERE source_id=?", (image_path, source_id))
    return str(old[0]) if old and old[0] else None


def clear_history_source(source_id: str) -> None:
    with _lock, connect() as con:
        con.execute("DELETE FROM messages WHERE source_id=?", (source_id,))
        con.execute("DELETE FROM screen_cache WHERE source_id=?", (source_id,))


def history_message(source_id: str, message_id: int) -> dict | None:
    with connect() as con:
        row = con.execute(
            "SELECT id, session_id, source_text, translation, confidence, box_json, language, target_language, screen_hash FROM messages WHERE source_id=? AND id=?",
            (source_id, message_id),
        ).fetchone()
    return dict(row) if row else None


def history_message_by_id(message_id: int) -> dict | None:
    with connect() as con:
        row = con.execute(
            "SELECT id, source_id, session_id, source_text, language, target_language FROM messages WHERE id=?",
            (message_id,),
        ).fetchone()
    return dict(row) if row else None


def history_context_before(source_id: str, session_id: str, message_id: int, limit: int = 1000) -> list[dict]:
    with connect() as con:
        rows = con.execute(
            "SELECT id, created_at, source_text, translation, confidence, box_json, language "
            "FROM messages WHERE source_id=? AND session_id=? AND id<? ORDER BY id DESC LIMIT ?",
            (source_id, session_id, message_id, limit),
        ).fetchall()
    return [dict(row) for row in rows]


def list_test_history_messages() -> list[dict]:
    with connect() as con:
        rows = con.execute("""
            SELECT m.id, m.source_id, m.source_name, m.session_id, m.source_text
                   , m.translation
            FROM messages AS m ORDER BY m.id DESC
        """).fetchall()
    return [dict(row) for row in rows]


def add_llm_test_result(
    provider_id: str, model: str, test_run_id: str, phrase_id: int,
    source_text: str, translation: str | None, duration_ms: float | None,
    error: str | None, source_id: str, session_id: str,
    history_items: int | None = None, history_tokens: int | None = None,
    prompt_tokens: int | None = None,
) -> None:
    with _lock, connect() as con:
        con.execute(
            "INSERT INTO llm_test_results(provider_id, model, test_run_id, phrase_id, source_text, translation, duration_ms, error, source_id, session_id, history_items, history_tokens, prompt_tokens) "
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
            (
                provider_id, model, test_run_id, phrase_id, source_text, translation,
                duration_ms, error, source_id, session_id,
                history_items, history_tokens, prompt_tokens,
            ),
        )


def llm_test_model_history(provider_id: str, model: str) -> list[dict]:
    with connect() as con:
        rows = con.execute(
            "SELECT test_run_id, created_at, phrase_id, source_text, translation, duration_ms, error, "
            "history_items, history_tokens, prompt_tokens "
            "FROM llm_test_results WHERE provider_id=? AND model=? ORDER BY id DESC",
            (provider_id, model),
        ).fetchall()
    return [dict(row) for row in rows]


def save_llm_test_run(
    provider_id: str,
    test_run_id: str,
    mode: str,
    models: list[str],
    phrases: list[dict],
) -> None:
    with _lock, connect() as con:
        con.execute(
            "INSERT INTO llm_test_runs(provider_id, test_run_id, mode, models_json, phrases_json) "
            "VALUES(?,?,?,?,?) "
            "ON CONFLICT(provider_id, test_run_id) DO UPDATE SET "
            "mode=excluded.mode, models_json=excluded.models_json, phrases_json=excluded.phrases_json",
            (
                provider_id,
                test_run_id,
                mode,
                json.dumps(models, ensure_ascii=False),
                json.dumps(phrases, ensure_ascii=False),
            ),
        )


def _llm_test_run_results(con: sqlite3.Connection, provider_id: str, test_run_id: str) -> list[dict]:
    rows = con.execute(
        "SELECT model, phrase_id, source_text, translation, duration_ms, error, "
        "history_items, history_tokens, prompt_tokens "
        "FROM llm_test_results WHERE provider_id=? AND test_run_id=? ORDER BY id",
        (provider_id, test_run_id),
    ).fetchall()
    return [dict(row) for row in rows]


def latest_llm_test_run(provider_id: str) -> dict | None:
    with connect() as con:
        row = con.execute(
            "SELECT test_run_id, created_at, models_json, phrases_json "
            "FROM llm_test_runs WHERE provider_id=? AND mode='all' ORDER BY id DESC LIMIT 1",
            (provider_id,),
        ).fetchone()
        if row is not None:
            return {
                "test_run_id": row["test_run_id"],
                "created_at": row["created_at"],
                "models": json.loads(row["models_json"] or "[]"),
                "phrases": json.loads(row["phrases_json"] or "[]"),
                "results": _llm_test_run_results(con, provider_id, row["test_run_id"]),
                "legacy": False,
            }

        # Compatibility with test runs created before llm_test_runs existed.
        legacy = con.execute(
            "SELECT r.test_run_id, MIN(r.created_at) AS created_at, MAX(r.id) AS last_id "
            "FROM llm_test_results AS r WHERE r.provider_id=? "
            "AND NOT EXISTS ("
            "SELECT 1 FROM llm_test_runs AS saved "
            "WHERE saved.provider_id=r.provider_id AND saved.test_run_id=r.test_run_id"
            ") GROUP BY r.test_run_id ORDER BY last_id DESC LIMIT 1",
            (provider_id,),
        ).fetchone()
        if legacy is None:
            return None
        results = _llm_test_run_results(con, provider_id, legacy["test_run_id"])
        models: list[str] = []
        phrase_ids: list[int] = []
        for item in results:
            model = str(item.get("model") or "")
            if model and model not in models:
                models.append(model)
            phrase_id = item.get("phrase_id")
            if phrase_id is not None and int(phrase_id) not in phrase_ids:
                phrase_ids.append(int(phrase_id))

        phrase_rows: dict[int, dict] = {}
        if phrase_ids:
            placeholders = ",".join("?" for _ in phrase_ids)
            rows = con.execute(
                f"SELECT id, source_id, source_name, session_id, source_text, translation "
                f"FROM messages WHERE id IN ({placeholders})",
                phrase_ids,
            ).fetchall()
            phrase_rows = {int(item["id"]): dict(item) for item in rows}
        phrases: list[dict] = []
        for phrase_id in phrase_ids:
            item = phrase_rows.get(phrase_id)
            if item is None:
                old_result = next((r for r in results if int(r.get("phrase_id") or -1) == phrase_id), None)
                item = {
                    "id": phrase_id,
                    "source_id": "",
                    "source_name": "",
                    "session_id": "",
                    "source_text": str((old_result or {}).get("source_text") or ""),
                    "translation": "",
                }
            phrases.append(item)
        return {
            "test_run_id": legacy["test_run_id"],
            "created_at": legacy["created_at"],
            "models": models,
            "phrases": phrases,
            "results": results,
            "legacy": True,
        }


def llm_model_notes(provider_id: str) -> dict[str, str]:
    with connect() as con:
        rows = con.execute(
            "SELECT model, note FROM llm_model_notes WHERE provider_id=? ORDER BY model",
            (provider_id,),
        ).fetchall()
    return {str(row["model"]): str(row["note"] or "") for row in rows}


def save_llm_model_note(provider_id: str, model: str, note: str) -> str:
    note = str(note).rstrip()
    with _lock, connect() as con:
        con.execute(
            "INSERT INTO llm_model_notes(provider_id, model, note, updated_at) VALUES(?,?,?,datetime('now')) "
            "ON CONFLICT(provider_id, model) DO UPDATE SET note=excluded.note, updated_at=datetime('now')",
            (provider_id, model, note),
        )
    return note


def game_glossary(source_id: str) -> list[dict]:
    with connect() as con:
        rows = con.execute("SELECT id, source_text, translation, type, status FROM game_glossary WHERE source_id=? ORDER BY created_at DESC, id DESC", (source_id,)).fetchall()
    return [dict(row) for row in rows]


def glossary_state(source_id: str) -> dict:
    with _lock, connect() as con:
        con.execute("INSERT OR IGNORE INTO glossary_source_state(source_id) VALUES(?)", (source_id,))
        row = con.execute("SELECT glossary_scanned_until_history_id, glossary_pending_tokens FROM glossary_source_state WHERE source_id=?", (source_id,)).fetchone()
    return dict(row)


def glossary_scan_rows(source_id: str) -> tuple[dict, list[dict]]:
    state = glossary_state(source_id)
    with connect() as con:
        rows = con.execute("SELECT id, source_text, translation, token_count FROM messages WHERE source_id=? AND id>? ORDER BY id", (source_id, state["glossary_scanned_until_history_id"])).fetchall()
        overlap = con.execute("SELECT id, source_text, translation FROM messages WHERE source_id=? AND id<=? ORDER BY id DESC LIMIT 5", (source_id, state["glossary_scanned_until_history_id"])).fetchall()
    return state, [dict(row) for row in reversed(overlap)] + [dict(row) for row in rows]


def update_glossary_state(source_id: str, checkpoint: int, pending_tokens: int = 0) -> None:
    with _lock, connect() as con:
        con.execute("INSERT INTO glossary_source_state(source_id, glossary_scanned_until_history_id, glossary_pending_tokens) VALUES(?,?,?) ON CONFLICT(source_id) DO UPDATE SET glossary_scanned_until_history_id=excluded.glossary_scanned_until_history_id, glossary_pending_tokens=excluded.glossary_pending_tokens", (source_id, checkpoint, pending_tokens))


AUTO_GLOSSARY_TYPES = {"PERSON", "LOCATION", "ORG", "TERM"}
MANUAL_GLOSSARY_TYPES = AUTO_GLOSSARY_TYPES


def _glossary_term_pattern(term: str, *, ignore_case: bool = False) -> re.Pattern[str]:
    value = str(term or "").strip()
    if not value:
        return re.compile(r"(?!x)x")
    pattern = re.escape(value)
    if value[0].isalnum() or value[0] == "_":
        pattern = r"(?<!\w)" + pattern
    if value[-1].isalnum() or value[-1] == "_":
        pattern += r"(?!\w)"
    flags = re.UNICODE | (re.IGNORECASE if ignore_case else 0)
    return re.compile(pattern, flags)


def _bool_field(value: object) -> bool:
    if value is True:
        return True
    if isinstance(value, str):
        return value.strip().lower() in {"true", "1", "yes"}
    return False


def _float_field(value: object) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def _compact_glossary_value(value: str, max_chars: int = 96, max_words: int = 6) -> bool:
    text = str(value or "").strip()
    if not text or len(text) > max_chars or any(ch in text for ch in "\r\n\t"):
        return False
    words = re.findall(r"[^\W_]+(?:['’\-][^\W_]+)*", text, flags=re.UNICODE)
    if not words or len(words) > max_words:
        return False
    # Sentences and dialogue fragments do not belong in a consistency glossary.
    if "?" in text or "!" in text:
        return False
    if text.endswith(".") and len(words) > 2:
        return False
    return True


def _history_term_occurrences(con: sqlite3.Connection, source_id: str, term: str, stop_after: int = 2) -> int:
    pattern = _glossary_term_pattern(term, ignore_case=True)
    count = 0
    rows = con.execute(
        "SELECT source_text FROM messages WHERE source_id=? AND instr(lower(source_text), lower(?)) > 0 ORDER BY id DESC",
        (source_id, term),
    )
    for row in rows:
        if pattern.search(str(row["source_text"] or "")):
            count += 1
            if count >= stop_after:
                break
    return count


def _valid_auto_glossary_candidate(con: sqlite3.Connection, source_id: str, entry: dict) -> tuple[str, str, str] | None:
    source = str(entry.get("source") or "").strip()
    translation = str(entry.get("translation") or "").strip()
    kind = str(entry.get("type") or "").strip().upper()
    if kind not in AUTO_GLOSSARY_TYPES:
        return None
    if not _bool_field(entry.get("stable")):
        return None
    confidence = _float_field(entry.get("confidence"))
    required_confidence = 0.93 if kind == "TERM" else 0.88
    if confidence < required_confidence:
        return None
    if not _compact_glossary_value(source) or not _compact_glossary_value(translation, max_chars=120, max_words=8):
        return None
    if kind == "TERM":
        if not _bool_field(entry.get("special_term")):
            return None
        # A one-off word does not need a consistency glossary yet. When it appears
        # again, the full history check below will allow it automatically.
        if _history_term_occurrences(con, source_id, source, stop_after=2) < 2:
            return None
    return source, translation, kind


def _propagate_glossary_translation(
    con: sqlite3.Connection,
    source_id: str,
    old_source: str,
    new_source: str,
    old_translation: str,
    new_translation: str,
) -> tuple[int, int]:
    if not old_translation or old_translation == new_translation:
        return 0, 0

    source_patterns = []
    for term in (old_source, new_source):
        term = str(term or "").strip()
        if term and term not in {old_source if source_patterns else ""}:
            source_patterns.append(_glossary_term_pattern(term, ignore_case=True))
    translation_pattern = _glossary_term_pattern(old_translation, ignore_case=False)
    updated_rows = 0
    replaced_occurrences = 0

    rows = con.execute(
        "SELECT id, session_id, screen_hash, source_text, translation FROM messages WHERE source_id=? ORDER BY id",
        (source_id,),
    ).fetchall()
    for row in rows:
        source_text = str(row["source_text"] or "")
        if source_patterns and not any(pattern.search(source_text) for pattern in source_patterns):
            continue
        current_translation = str(row["translation"] or "")
        changed_translation, replacements = translation_pattern.subn(new_translation, current_translation)
        if not replacements or changed_translation == current_translation:
            continue
        con.execute(
            "UPDATE messages SET translation=?, token_count=NULL WHERE id=? AND source_id=?",
            (changed_translation, row["id"], source_id),
        )
        if row["screen_hash"]:
            con.execute(
                "DELETE FROM screen_cache WHERE source_id=? AND session_id=? AND screen_hash=?",
                (source_id, row["session_id"], row["screen_hash"]),
            )
        updated_rows += 1
        replaced_occurrences += int(replacements)
    return updated_rows, replaced_occurrences


def merge_game_glossary(source_id: str, entries: list[dict]) -> tuple[int, int, list[dict]]:
    added = conflicts = 0
    added_entries: list[dict] = []
    with _lock, connect() as con:
        for entry in entries:
            candidate = _valid_auto_glossary_candidate(con, source_id, entry)
            if candidate is None:
                continue
            source, translation, kind = candidate
            old = con.execute(
                "SELECT translation, status FROM game_glossary WHERE source_id=? AND source_text=?",
                (source_id, source),
            ).fetchone()
            if old is None:
                cursor = con.execute(
                    "INSERT INTO game_glossary(source_id, source_text, translation, type, status) VALUES(?,?,?,?, 'AUTO')",
                    (source_id, source, translation, kind),
                )
                added += 1
                added_entries.append({
                    "id": int(cursor.lastrowid),
                    "source_text": source,
                    "translation": translation,
                    "type": kind,
                    "status": "AUTO",
                })
            elif old["status"] != "LOCKED" and old["translation"] != translation:
                con.execute(
                    "UPDATE game_glossary SET status='CONFLICT', updated_at=datetime('now') WHERE source_id=? AND source_text=?",
                    (source_id, source),
                )
                conflicts += 1
    return added, conflicts, added_entries


def save_game_glossary(source_id: str, source_text: str, translation: str, kind: str, entry_id: int | None = None) -> dict | None:
    source_text = str(source_text or "").strip()
    translation = str(translation or "").strip()
    kind = str(kind or "").strip().upper()
    if not source_text or not translation or kind not in MANUAL_GLOSSARY_TYPES:
        raise ValueError("Некорректная запись глоссария")

    with _lock, connect() as con:
        previous = None
        if entry_id is None:
            previous = con.execute(
                "SELECT id, source_text, translation FROM game_glossary WHERE source_id=? AND source_text=?",
                (source_id, source_text),
            ).fetchone()
            con.execute(
                "INSERT INTO game_glossary(source_id, source_text, translation, type, status) "
                "VALUES(?,?,?,?, 'LOCKED') ON CONFLICT(source_id, source_text) DO UPDATE SET "
                "translation=excluded.translation, type=excluded.type, status='LOCKED', updated_at=datetime('now')",
                (source_id, source_text, translation, kind),
            )
            row = con.execute(
                "SELECT id, source_text, translation, type, status FROM game_glossary WHERE source_id=? AND source_text=?",
                (source_id, source_text),
            ).fetchone()
        else:
            previous = con.execute(
                "SELECT id, source_text, translation FROM game_glossary WHERE id=? AND source_id=?",
                (entry_id, source_id),
            ).fetchone()
            if previous is None:
                return None
            if not con.execute(
                "UPDATE game_glossary SET source_text=?, translation=?, type=?, status='LOCKED', updated_at=datetime('now') "
                "WHERE id=? AND source_id=?",
                (source_text, translation, kind, entry_id, source_id),
            ).rowcount:
                return None
            row = con.execute(
                "SELECT id, source_text, translation, type, status FROM game_glossary WHERE id=? AND source_id=?",
                (entry_id, source_id),
            ).fetchone()

        updated_rows = replaced_occurrences = 0
        if previous is not None and str(previous["translation"]) != translation:
            updated_rows, replaced_occurrences = _propagate_glossary_translation(
                con,
                source_id,
                str(previous["source_text"]),
                source_text,
                str(previous["translation"]),
                translation,
            )

    if row is None:
        return None
    result = dict(row)
    result["history_updates"] = updated_rows
    result["history_replacements"] = replaced_occurrences
    return result


def delete_game_glossary(source_id: str, entry_id: int) -> bool:
    with _lock, connect() as con:
        return con.execute("DELETE FROM game_glossary WHERE source_id=? AND id=?", (source_id, entry_id)).rowcount > 0


def delete_history_message(source_id: str, message_id: int) -> bool:
    with _lock, connect() as con:
        return con.execute("DELETE FROM messages WHERE source_id=? AND id=?", (source_id, message_id)).rowcount > 0


def update_history_translation(source_id: str, message_id: int, translation: str) -> bool:
    with _lock, connect() as con:
        row = con.execute(
            "SELECT session_id, screen_hash, target_language FROM messages WHERE source_id=? AND id=?",
            (source_id, message_id),
        ).fetchone()
        if row is None:
            return False
        if translation_has_disallowed_east_asian_script(translation, row["target_language"]):
            con.execute("DELETE FROM pending_history_corrections WHERE history_id=?", (message_id,))
            con.execute("DELETE FROM messages WHERE source_id=? AND id=?", (source_id, message_id))
            if row["screen_hash"]:
                con.execute(
                    "DELETE FROM screen_cache WHERE source_id=? AND session_id=? AND screen_hash=?",
                    (source_id, row["session_id"], row["screen_hash"]),
                )
            return True
        con.execute(
            "UPDATE messages SET translation=? WHERE source_id=? AND id=?",
            (translation, source_id, message_id),
        )
        if row["screen_hash"]:
            con.execute(
                "DELETE FROM screen_cache WHERE source_id=? AND session_id=? AND screen_hash=?",
                (source_id, row["session_id"], row["screen_hash"]),
            )
        return True



def update_history_message(source_id: str, message_id: int, source_text: str, translation: str) -> bool:
    """Edit one history row; delete it instead if the corrected translation still contains forbidden CJK residue."""
    with _lock, connect() as con:
        row = con.execute(
            "SELECT session_id, screen_hash, target_language FROM messages WHERE source_id=? AND id=?",
            (source_id, message_id),
        ).fetchone()
        if row is None:
            return False
        if translation_has_disallowed_east_asian_script(translation, row["target_language"]):
            con.execute("DELETE FROM pending_history_corrections WHERE history_id=?", (message_id,))
            con.execute("DELETE FROM messages WHERE source_id=? AND id=?", (source_id, message_id))
        else:
            con.execute(
                "UPDATE messages SET source_text=?, translation=?, token_count=NULL WHERE source_id=? AND id=?",
                (source_text, translation, source_id, message_id),
            )
        if row["screen_hash"]:
            con.execute(
                "DELETE FROM screen_cache WHERE source_id=? AND session_id=? AND screen_hash=?",
                (source_id, row["session_id"], row["screen_hash"]),
            )
    return True



def create_pending_history_correction(history_id: int, proposed_source_text: str | None, proposed_translation: str | None, client_request_id: str | None) -> dict | None:
    with _lock, connect() as con:
        row = con.execute("SELECT source_id, session_id, source_text, translation FROM messages WHERE id=?", (history_id,)).fetchone()
        if row is None:
            return None
        if client_request_id:
            existing = con.execute("SELECT id, status FROM pending_history_corrections WHERE client_request_id=?", (client_request_id,)).fetchone()
            if existing:
                return dict(existing)
        cursor = con.execute(
            "INSERT INTO pending_history_corrections(history_id, source_id, session_id, original_source_text, original_translation, proposed_source_text, proposed_translation, client_request_id) VALUES(?,?,?,?,?,?,?,?)",
            (history_id, row["source_id"], row["session_id"], row["source_text"], row["translation"], proposed_source_text, proposed_translation, client_request_id),
        )
        return {"id": int(cursor.lastrowid), "status": "pending"}


def pending_history_corrections() -> list[dict]:
    with connect() as con:
        rows = con.execute("""
            SELECT c.id, c.history_id, c.source_id, c.session_id, c.original_source_text, c.original_translation,
                   c.proposed_source_text, c.proposed_translation, c.created_at,
                   COALESCE(s.source_name, c.source_id) AS source_name,
                   COALESCE(m.source_text, c.original_source_text) AS current_source_text,
                   COALESCE(m.translation, c.original_translation) AS current_translation
            FROM pending_history_corrections AS c
            LEFT JOIN history_sources AS s ON s.source_id=c.source_id
            LEFT JOIN messages AS m ON m.id=c.history_id
            WHERE c.status='pending' ORDER BY c.id DESC
        """).fetchall()
    return [dict(row) for row in rows]


def resolve_pending_history_correction(correction_id: int, accept: bool) -> bool:
    with _lock, connect() as con:
        correction = con.execute("SELECT * FROM pending_history_corrections WHERE id=? AND status='pending'", (correction_id,)).fetchone()
        if correction is None:
            return False
        if accept:
            row = con.execute("SELECT session_id, screen_hash, source_text, translation FROM messages WHERE id=? AND source_id=?", (correction["history_id"], correction["source_id"])).fetchone()
            if row is None:
                return False
            source_text = str(correction["proposed_source_text"] or row["source_text"]).strip() or row["source_text"]
            translation = str(correction["proposed_translation"] or row["translation"]).strip() or row["translation"]
            con.execute("UPDATE messages SET source_text=?, translation=? WHERE id=? AND source_id=?", (source_text, translation, correction["history_id"], correction["source_id"]))
            if row["screen_hash"]:
                con.execute("DELETE FROM screen_cache WHERE source_id=? AND session_id=? AND screen_hash=?", (correction["source_id"], row["session_id"], row["screen_hash"]))
        con.execute("UPDATE pending_history_corrections SET status=? WHERE id=?", ("accepted" if accept else "rejected", correction_id))
    return True


def clear_session(source_id: str, session_id: str) -> None:
    with _lock, connect() as con:
        con.execute("DELETE FROM messages WHERE source_id=? AND session_id=?", (source_id, session_id))
        con.execute("DELETE FROM screen_cache WHERE source_id=? AND session_id=?", (source_id, session_id))


def get_cached(source_id: str, session_id: str, screen_hash: str) -> str | None:
    with connect() as con:
        row = con.execute("SELECT response_json FROM screen_cache WHERE source_id=? AND session_id=? AND screen_hash=?", (source_id, session_id, screen_hash)).fetchone()
    return row[0] if row else None


def put_cached(source_id: str, session_id: str, screen_hash: str, response_json: str) -> None:
    with _lock, connect() as con:
        con.execute(
            "INSERT OR REPLACE INTO screen_cache(source_id, session_id, screen_hash, response_json) VALUES(?,?,?,?)",
            (source_id, session_id, screen_hash, response_json),
        )
