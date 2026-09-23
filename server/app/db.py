"""SQLite file database (chat.db next to the server folder)."""
import sqlite3
import threading
from pathlib import Path

DB_PATH = Path(__file__).resolve().parent.parent / "chat.db"

_lock = threading.Lock()
_conn = sqlite3.connect(str(DB_PATH), check_same_thread=False)
_conn.row_factory = sqlite3.Row


def init_db() -> None:
    with _lock, _conn:
        _conn.executescript(
            """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                photo_url TEXT DEFAULT '',
                online INTEGER DEFAULT 0,
                last_seen TEXT DEFAULT '',
                created_at TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS chats (
                id TEXT PRIMARY KEY,
                name TEXT DEFAULT '',
                is_group INTEGER DEFAULT 0,
                photo_url TEXT DEFAULT '',
                created_by TEXT DEFAULT '',
                last_message TEXT DEFAULT '',
                last_message_time TEXT DEFAULT '',
                last_sender_id TEXT DEFAULT '',
                created_at TEXT NOT NULL
            );
            CREATE TABLE IF NOT EXISTS chat_members (
                chat_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                PRIMARY KEY (chat_id, user_id)
            );
            CREATE TABLE IF NOT EXISTS messages (
                id TEXT PRIMARY KEY,
                chat_id TEXT NOT NULL,
                sender_id TEXT NOT NULL,
                sender_name TEXT DEFAULT '',
                text TEXT DEFAULT '',
                image_url TEXT DEFAULT '',
                timestamp TEXT NOT NULL
            );
            CREATE INDEX IF NOT EXISTS idx_messages_chat ON messages(chat_id, timestamp);
            CREATE INDEX IF NOT EXISTS idx_members_user ON chat_members(user_id);
            """
        )


def fetch_all(query: str, params: tuple = ()) -> list[dict]:
    with _lock:
        cur = _conn.execute(query, params)
        return [dict(r) for r in cur.fetchall()]


def fetch_one(query: str, params: tuple = ()) -> dict | None:
    with _lock:
        cur = _conn.execute(query, params)
        row = cur.fetchone()
        return dict(row) if row else None


def execute(query: str, params: tuple = ()) -> None:
    with _lock, _conn:
        _conn.execute(query, params)
