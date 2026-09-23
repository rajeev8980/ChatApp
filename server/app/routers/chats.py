import json
import uuid
from pathlib import Path

from fastapi import APIRouter, Depends, File, HTTPException, UploadFile

from .. import db, ws
from ..schemas import DirectChatIn, GroupChatIn, SendMessageIn
from ..security import now_iso
from .common import current_user, public_user

chats_router = APIRouter(prefix="/api/chats", tags=["chats"])

UPLOAD_DIR = Path(__file__).resolve().parent.parent.parent / "uploads"
UPLOAD_DIR.mkdir(exist_ok=True)


def _members(chat_id: str) -> list[str]:
    rows = db.fetch_all("SELECT user_id FROM chat_members WHERE chat_id = ?", (chat_id,))
    return [r["user_id"] for r in rows]


def _chat_dict(row: dict) -> dict:
    return {
        "chatId": row["id"],
        "name": row["name"] or "",
        "isGroup": bool(row["is_group"]),
        "members": _members(row["id"]),
        "lastMessage": row["last_message"] or "",
        "lastMessageTime": row["last_message_time"] or "",
        "lastSenderId": row["last_sender_id"] or "",
        "photoUrl": row["photo_url"] or "",
        "createdBy": row["created_by"] or "",
    }


def _msg_dict(row: dict) -> dict:
    try:
        seen = json.loads(row.get("seen_by") or "[]")
    except Exception:
        seen = []
    return {
        "messageId": row["id"],
        "chatId": row["chat_id"],
        "senderId": row["sender_id"],
        "senderName": row["sender_name"] or "",
        "text": row["text"] or "",
        "imageUrl": row["image_url"] or "",
        "timestamp": row["timestamp"] or "",
        "seenBy": seen,
    }


def _require_member(chat_id: str, uid: str) -> dict:
    chat = db.fetch_one("SELECT * FROM chats WHERE id = ?", (chat_id,))
    if not chat:
        raise HTTPException(status_code=404, detail="Chat not found")
    mem = db.fetch_one(
        "SELECT 1 FROM chat_members WHERE chat_id = ? AND user_id = ?", (chat_id, uid)
    )
    if not mem:
        raise HTTPException(status_code=403, detail="Not a member")
    return chat


@chats_router.get("")
def my_chats(user: dict = Depends(current_user)):
    rows = db.fetch_all(
        """SELECT c.* FROM chats c JOIN chat_members m ON m.chat_id = c.id
           WHERE m.user_id = ? ORDER BY c.last_message_time DESC""",
        (user["id"],),
    )
    return [_chat_dict(r) for r in rows]


@chats_router.post("/direct")
def direct_chat(body: DirectChatIn, user: dict = Depends(current_user)):
    other = db.fetch_one("SELECT * FROM users WHERE id = ?", (body.otherUid,))
    if not other:
        raise HTTPException(status_code=404, detail="User not found")
    # reuse existing 1-1 chat
    mine = db.fetch_all("SELECT chat_id FROM chat_members WHERE user_id = ?", (user["id"],))
    for m in mine:
        chat = db.fetch_one(
            "SELECT * FROM chats WHERE id = ? AND is_group = 0", (m["chat_id"],)
        )
        if chat and set(_members(chat["id"])) == {user["id"], body.otherUid}:
            return _chat_dict(chat)
    cid = uuid.uuid4().hex
    db.execute(
        "INSERT INTO chats (id, name, is_group, photo_url, created_by, created_at)"
        " VALUES (?, ?, 0, ?, ?, ?)",
        (cid, other["name"], other["photo_url"] or "", user["id"], now_iso()),
    )
    db.execute("INSERT INTO chat_members (chat_id, user_id) VALUES (?, ?)", (cid, user["id"]))
    db.execute("INSERT INTO chat_members (chat_id, user_id) VALUES (?, ?)", (cid, body.otherUid))
    return _chat_dict(db.fetch_one("SELECT * FROM chats WHERE id = ?", (cid,)))


@chats_router.post("/group")
def group_chat(body: GroupChatIn, user: dict = Depends(current_user)):
    member_ids = [u for u in dict.fromkeys([user["id"]] + body.memberUids)]
    if len(member_ids) < 2:
        raise HTTPException(status_code=400, detail="Add at least one member")
    cid = uuid.uuid4().hex
    db.execute(
        "INSERT INTO chats (id, name, is_group, created_by, created_at)"
        " VALUES (?, ?, 1, ?, ?)",
        (cid, body.name.strip(), user["id"], now_iso()),
    )
    for uid in member_ids:
        db.execute(
            "INSERT OR IGNORE INTO chat_members (chat_id, user_id) VALUES (?, ?)", (cid, uid)
        )
    return _chat_dict(db.fetch_one("SELECT * FROM chats WHERE id = ?", (cid,)))


@chats_router.get("/{chat_id}")
def get_chat(chat_id: str, user: dict = Depends(current_user)):
    chat = _require_member(chat_id, user["id"])
    return _chat_dict(chat)


@chats_router.get("/{chat_id}/messages")
async def get_messages(chat_id: str, limit: int = 200, user: dict = Depends(current_user)):
    _require_member(chat_id, user["id"])
    rows = db.fetch_all(
        "SELECT * FROM messages WHERE chat_id = ? ORDER BY timestamp ASC LIMIT ?",
        (chat_id, min(limit, 500)),
    )
    # mark others' messages as seen by me
    touched = False
    for r in rows:
        if r["sender_id"] == user["id"]:
            continue
        try:
            seen = json.loads(r.get("seen_by") or "[]")
        except Exception:
            seen = []
        if user["id"] not in seen:
            seen.append(user["id"])
            db.execute("UPDATE messages SET seen_by = ? WHERE id = ?",
                       (json.dumps(seen), r["id"]))
            touched = True
    if touched:
        await ws.broadcast_to(
            _members(chat_id),
            {"type": "seen", "chatId": chat_id, "byUid": user["id"]},
        )
    rows = db.fetch_all(
        "SELECT * FROM messages WHERE chat_id = ? ORDER BY timestamp ASC LIMIT ?",
        (chat_id, min(limit, 500)),
    )
    return [_msg_dict(r) for r in rows]


async def _store_and_broadcast(chat_id: str, user: dict, text: str, image_url: str) -> dict:
    mid = uuid.uuid4().hex
    ts = now_iso()
    db.execute(
        "INSERT INTO messages (id, chat_id, sender_id, sender_name, text, image_url, timestamp)"
        " VALUES (?, ?, ?, ?, ?, ?, ?)",
        (mid, chat_id, user["id"], user["name"], text, image_url, ts),
    )
    preview = text[:120] if text else "\U0001f4f7 Photo"
    db.execute(
        "UPDATE chats SET last_message = ?, last_message_time = ?, last_sender_id = ?"
        " WHERE id = ?",
        (preview, ts, user["id"], chat_id),
    )
    msg = _msg_dict(db.fetch_one("SELECT * FROM messages WHERE id = ?", (mid,)))
    await ws.broadcast_to(_members(chat_id), {"type": "message", "chatId": chat_id, "message": msg})
    return msg


@chats_router.post("/{chat_id}/messages")
async def send_message(chat_id: str, body: SendMessageIn, user: dict = Depends(current_user)):
    _require_member(chat_id, user["id"])
    return await _store_and_broadcast(chat_id, user, body.text.strip(), "")


@chats_router.post("/{chat_id}/image")
async def send_image(
    chat_id: str, file: UploadFile = File(...), user: dict = Depends(current_user)
):
    _require_member(chat_id, user["id"])
    ext = Path(file.filename or "img.jpg").suffix.lower() or ".jpg"
    if ext not in {".jpg", ".jpeg", ".png", ".webp", ".gif"}:
        raise HTTPException(status_code=400, detail="Only image files allowed")
    if file.size and file.size > 10 * 1024 * 1024:
        raise HTTPException(status_code=400, detail="Max 10 MB")
    fname = f"{chat_id}_{uuid.uuid4().hex}{ext}"
    data = await file.read()
    if len(data) > 10 * 1024 * 1024:
        raise HTTPException(status_code=400, detail="Max 10 MB")
    (UPLOAD_DIR / fname).write_bytes(data)
    return await _store_and_broadcast(chat_id, user, "\U0001f4f7 Photo", f"/uploads/{fname}")
