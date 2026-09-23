import uuid

from fastapi import APIRouter, Depends, HTTPException

from .. import db
from ..schemas import DeviceIn, LoginIn, PresenceIn, RegisterIn, UpdateMeIn
from ..security import hash_password, make_token, now_iso, verify_password
from .common import current_user, public_user

auth_router = APIRouter(prefix="/api/auth", tags=["auth"])


@auth_router.post("/register")
def register(body: RegisterIn):
    email = body.email.strip().lower()
    if db.fetch_one("SELECT id FROM users WHERE email = ?", (email,)):
        raise HTTPException(status_code=400, detail="Email already registered")
    uid = uuid.uuid4().hex
    db.execute(
        "INSERT INTO users (id, name, email, password_hash, online, created_at)"
        " VALUES (?, ?, ?, ?, 1, ?)",
        (uid, body.name.strip(), email, hash_password(body.password), now_iso()),
    )
    row = db.fetch_one("SELECT * FROM users WHERE id = ?", (uid,))
    return {"token": make_token(uid), "user": public_user(row)}


@auth_router.post("/login")
def login(body: LoginIn):
    row = db.fetch_one(
        "SELECT * FROM users WHERE email = ?", (body.email.strip().lower(),)
    )
    if not row or not verify_password(body.password, row["password_hash"]):
        raise HTTPException(status_code=401, detail="Invalid email or password")
    db.execute("UPDATE users SET online = 1 WHERE id = ?", (row["id"],))
    row = db.fetch_one("SELECT * FROM users WHERE id = ?", (row["id"],))
    return {"token": make_token(row["id"]), "user": public_user(row)}


me_router = APIRouter(prefix="/api", tags=["me"])


@me_router.get("/me")
def me(user: dict = Depends(current_user)):
    return public_user(user)


@me_router.put("/me")
def update_me(body: UpdateMeIn, user: dict = Depends(current_user)):
    db.execute("UPDATE users SET name = ? WHERE id = ?", (body.displayName.strip(), user["id"]))
    row = db.fetch_one("SELECT * FROM users WHERE id = ?", (user["id"],))
    return public_user(row)


@me_router.post("/presence")
def presence(body: PresenceIn, user: dict = Depends(current_user)):
    if body.online:
        db.execute("UPDATE users SET online = 1 WHERE id = ?", (user["id"],))
    else:
        db.execute(
            "UPDATE users SET online = 0, last_seen = ? WHERE id = ?",
            (now_iso(), user["id"]),
        )
    return {"ok": True}


@me_router.post("/device")
def device(body: DeviceIn, user: dict = Depends(current_user)):
    db.execute("UPDATE users SET fcm_token = ? WHERE id = ?",
               (body.fcmToken.strip(), user["id"]))
    return {"ok": True}
