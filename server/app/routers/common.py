"""Shared auth dependency + public user dict serializer."""
from fastapi import APIRouter, Depends, Header, HTTPException

from .. import db
from ..security import parse_token


def public_user(row: dict) -> dict:
    return {
        "uid": row["id"],
        "displayName": row["name"],
        "email": row["email"],
        "photoUrl": row.get("photo_url") or "",
        "online": bool(row.get("online")),
        "lastSeen": row.get("last_seen") or "",
    }


async def current_user(authorization: str = Header(default="")) -> dict:
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing token")
    uid = parse_token(authorization[7:])
    if not uid:
        raise HTTPException(status_code=401, detail="Invalid token")
    row = db.fetch_one("SELECT * FROM users WHERE id = ?", (uid,))
    if not row:
        raise HTTPException(status_code=401, detail="User not found")
    return row


router = APIRouter()
