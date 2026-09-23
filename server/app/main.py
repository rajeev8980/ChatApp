from fastapi import Depends, FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from . import db, ws
from .routers.auth import auth_router, me_router
from .routers.chats import UPLOAD_DIR, chats_router
from .routers.common import current_user
from .routers.users import users_router
from .security import parse_token

db.init_db()

app = FastAPI(title="ChatApp API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router)
app.include_router(me_router)
app.include_router(users_router)
app.include_router(chats_router)
app.mount("/uploads", StaticFiles(directory=str(UPLOAD_DIR)), name="uploads")


@app.get("/api/health")
def health():
    return {"ok": True}


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket, token: str = ""):
    uid = parse_token(token)
    if not uid:
        await websocket.close(code=4401)
        return
    row = db.fetch_one("SELECT id FROM users WHERE id = ?", (uid,))
    if not row:
        await websocket.close(code=4401)
        return
    await ws.connect(uid, websocket)
    try:
        while True:
            await websocket.receive_text()  # keep-alive / ignore client msgs
    except WebSocketDisconnect:
        ws.disconnect(uid, websocket)
    except Exception:
        ws.disconnect(uid, websocket)
