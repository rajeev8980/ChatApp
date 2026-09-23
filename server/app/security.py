"""Password hashing (bcrypt) + JWT tokens."""
import os
from datetime import datetime, timedelta, timezone

import bcrypt
import jwt

SECRET = os.environ.get("CHAT_SECRET", "dev-secret-change-me")
ALGO = "HS256"
TOKEN_DAYS = 7


def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()


def verify_password(password: str, hashed: str) -> bool:
    try:
        return bcrypt.checkpw(password.encode(), hashed.encode())
    except Exception:
        return False


def make_token(uid: str) -> str:
    exp = datetime.now(timezone.utc) + timedelta(days=TOKEN_DAYS)
    return jwt.encode({"sub": uid, "exp": exp}, SECRET, algorithm=ALGO)


def parse_token(token: str) -> str | None:
    try:
        payload = jwt.decode(token, SECRET, algorithms=[ALGO])
        return payload.get("sub")
    except Exception:
        return None


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()
