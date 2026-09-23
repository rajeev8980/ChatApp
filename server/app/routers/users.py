from fastapi import APIRouter, Depends, HTTPException

from .. import db
from .common import current_user, public_user

users_router = APIRouter(prefix="/api", tags=["users"])


@users_router.get("/users")
def list_users(user: dict = Depends(current_user)):
    rows = db.fetch_all(
        "SELECT * FROM users WHERE id != ? ORDER BY name COLLATE NOCASE", (user["id"],)
    )
    return [public_user(r) for r in rows]


@users_router.get("/users/{uid}")
def get_user(uid: str, user: dict = Depends(current_user)):
    row = db.fetch_one("SELECT * FROM users WHERE id = ?", (uid,))
    if not row:
        raise HTTPException(status_code=404, detail="User not found")
    return public_user(row)
