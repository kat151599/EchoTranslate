from __future__ import annotations
import sqlite3
import threading
from pathlib import Path
from typing import Iterable
from .config import ROOT, load_config

_lock = threading.RLock()


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
        """)
        columns = {row[1] for row in con.execute("PRAGMA table_info(messages)")}
        source_columns = {row[1] for row in con.execute("PRAGMA table_info(history_sources)")}
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


def register_source(source_id: str, source_name: str) -> None:
    with _lock, connect() as con:
        con.execute(
            "INSERT OR IGNORE INTO history_sources(source_id, source_name) VALUES(?, COALESCE(NULLIF(?, ''), 'Default'))",
            (source_id, source_name),
        )
        if source_name:
            con.execute("UPDATE history_sources SET source_name=? WHERE source_id=?", (source_name, source_id))
            con.execute("UPDATE messages SET source_name=? WHERE source_id=?", (source_name, source_id))


def add_messages(source_id: str, source_name: str, session_id: str, rows: Iterable[tuple[str, str, float, str, str, str, str, int]]) -> list[int]:
    with _lock, connect() as con:
        ids: list[int] = []
        for row in rows:
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
) -> None:
    with _lock, connect() as con:
        con.execute(
            "INSERT INTO llm_test_results(provider_id, model, test_run_id, phrase_id, source_text, translation, duration_ms, error, source_id, session_id) "
            "VALUES(?,?,?,?,?,?,?,?,?,?)",
            (provider_id, model, test_run_id, phrase_id, source_text, translation, duration_ms, error, source_id, session_id),
        )


def llm_test_model_history(provider_id: str, model: str) -> list[dict]:
    with connect() as con:
        rows = con.execute(
            "SELECT test_run_id, created_at, phrase_id, source_text, translation, duration_ms, error "
            "FROM llm_test_results WHERE provider_id=? AND model=? ORDER BY id DESC",
            (provider_id, model),
        ).fetchall()
    return [dict(row) for row in rows]


def game_glossary(source_id: str) -> list[dict]:
    with connect() as con:
        rows = con.execute("SELECT id, source_text, translation, type, status FROM game_glossary WHERE source_id=? ORDER BY source_text", (source_id,)).fetchall()
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


def merge_game_glossary(source_id: str, entries: list[dict]) -> tuple[int, int]:
    added = conflicts = 0
    with _lock, connect() as con:
        for entry in entries:
            source = str(entry.get("source") or "").strip()
            translation = str(entry.get("translation") or "").strip()
            kind = str(entry.get("type") or "TERM").strip().upper()
            if not source or not translation or kind not in {"PERSON", "LOCATION", "ORG", "TITLE", "ITEM", "TERM"}:
                continue
            old = con.execute("SELECT translation, status FROM game_glossary WHERE source_id=? AND source_text=?", (source_id, source)).fetchone()
            if old is None:
                con.execute("INSERT INTO game_glossary(source_id, source_text, translation, type, status) VALUES(?,?,?,?, 'AUTO')", (source_id, source, translation, kind))
                added += 1
            elif old["status"] != "LOCKED" and old["translation"] != translation:
                con.execute("UPDATE game_glossary SET status='CONFLICT', updated_at=datetime('now') WHERE source_id=? AND source_text=?", (source_id, source))
                conflicts += 1
    return added, conflicts


def save_game_glossary(source_id: str, source_text: str, translation: str, kind: str, entry_id: int | None = None) -> None:
    with _lock, connect() as con:
        if entry_id is None:
            con.execute("INSERT INTO game_glossary(source_id, source_text, translation, type, status) VALUES(?,?,?,?, 'LOCKED') ON CONFLICT(source_id, source_text) DO UPDATE SET translation=excluded.translation, type=excluded.type, status='LOCKED', updated_at=datetime('now')", (source_id, source_text, translation, kind))
        else:
            con.execute("UPDATE game_glossary SET source_text=?, translation=?, type=?, status='LOCKED', updated_at=datetime('now') WHERE id=? AND source_id=?", (source_text, translation, kind, entry_id, source_id))


def delete_game_glossary(source_id: str, entry_id: int) -> None:
    with _lock, connect() as con:
        con.execute("DELETE FROM game_glossary WHERE source_id=? AND id=?", (source_id, entry_id))


def delete_history_message(source_id: str, message_id: int) -> bool:
    with _lock, connect() as con:
        return con.execute("DELETE FROM messages WHERE source_id=? AND id=?", (source_id, message_id)).rowcount > 0


def update_history_translation(source_id: str, message_id: int, translation: str) -> bool:
    with _lock, connect() as con:
        return con.execute(
            "UPDATE messages SET translation=? WHERE source_id=? AND id=?",
            (translation, source_id, message_id),
        ).rowcount > 0


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
