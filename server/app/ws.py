"""WebSocket connection registry + broadcast helpers."""
import json
from fastapi import WebSocket

# user_id -> set of active sockets
_connections: dict[str, set[WebSocket]] = {}


async def connect(uid: str, ws: WebSocket) -> None:
    await ws.accept()
    _connections.setdefault(uid, set()).add(ws)


def disconnect(uid: str, ws: WebSocket) -> None:
    sockets = _connections.get(uid)
    if sockets and ws in sockets:
        sockets.remove(ws)
        if not sockets:
            _connections.pop(uid, None)


async def send_to(uid: str, event: dict) -> None:
    for ws in list(_connections.get(uid, set())):
        try:
            await ws.send_text(json.dumps(event))
        except Exception:
            disconnect(uid, ws)


async def broadcast_to(user_ids: list[str], event: dict) -> None:
    for uid in user_ids:
        await send_to(uid, event)
