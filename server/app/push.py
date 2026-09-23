"""Push notifications via Firebase Cloud Messaging.

Activates when a service-account key is present:
  server/fcm-key.json  (gitignored, never commit)
or env CHAT_FCM_KEY pointing at the key file.
Without it, all functions are safe no-ops.
"""
import json
import os
from pathlib import Path

KEY_PATH = Path(
    os.environ.get(
        "CHAT_FCM_KEY",
        str(Path(__file__).resolve().parent.parent / "fcm-key.json"),
    )
)

_initialized = False


def _ensure() -> bool:
    global _initialized
    if _initialized:
        return True
    if not KEY_PATH.exists():
        return False
    try:
        with open(KEY_PATH, encoding="utf-8") as f:
            info = json.load(f)
        if info.get("type") != "service_account":
            return False
        import firebase_admin
        from firebase_admin import credentials

        try:
            firebase_admin.get_app()
        except ValueError:
            firebase_admin.initialize_app(credentials.Certificate(str(KEY_PATH)))
        _initialized = True
        return True
    except Exception:
        return False


def push_enabled() -> bool:
    return _ensure()


def send_to_tokens(tokens: list[str], title: str, body: str, chat_id: str) -> int:
    """Returns number of tokens the push was accepted for. 0 when disabled/failing."""
    tokens = [t for t in dict.fromkeys(tokens) if t]
    if not tokens or not _ensure():
        return 0
    try:
        from firebase_admin import messaging

        msg = messaging.MulticastMessage(
            tokens=tokens,
            notification=messaging.Notification(title=title, body=body[:200]),
            data={"chatId": chat_id},
            android=messaging.AndroidConfig(priority="high"),
        )
        resp = messaging.send_each_for_multicast(msg)
        return resp.success_count
    except Exception:
        return 0
