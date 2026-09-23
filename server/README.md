# ChatApp backend (FastAPI + SQLite file DB)

## Run
```
cd server
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
Windows: double-click `run.bat`. API at http://localhost:8000, docs at http://localhost:8000/docs.

## Data
- SQLite file `server/chat.db` (auto-created), uploads in `server/uploads/`.
- Set `CHAT_SECRET` env var in production (default is a dev secret).

## API
- `POST /api/auth/register {name,email,password}` -> `{token, user}`
- `POST /api/auth/login {email,password}` -> `{token, user}`
- `GET /api/me`, `PUT /api/me {displayName}`, `POST /api/presence {online}`
- `GET /api/users` (all except self)
- `GET /api/chats`, `POST /api/chats/direct {otherUid}`, `POST /api/chats/group {name, memberUids}`
- `GET /api/chats/{id}/messages`, `POST /api/chats/{id}/messages {text}`
- `POST /api/chats/{id}/image` (multipart `file`)
- `WS /ws?token=...` receives `{"type":"message","chatId","message"}` in realtime

Auth: `Authorization: Bearer <token>` header on all `/api/*` except register/login/health.
Android emulator reaches this PC via `http://10.0.2.2:8000`.
