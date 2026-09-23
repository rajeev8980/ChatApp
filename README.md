# ChatApp — realtime chat (Android + own Python backend)

No Firebase. Android app (Kotlin + Jetpack Compose) talks to a FastAPI backend over REST + WebSocket.

## 1. Start the backend
```
cd server
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```
Windows: double-click `server\run.bat`. API: http://localhost:8000, docs: http://localhost:8000/docs.
- SQLite file DB: `server/chat.db` (auto-created). Uploads: `server/uploads/`.
- Set `CHAT_SECRET` env var in production.

## 2. Run the Android app
Open `ChatApp` folder in Android Studio (Gradle JDK 17 — see note below), Run `app` on an emulator.
The emulator reaches your PC via `http://10.0.2.2:8000` (default in `data/remote/ServerConfig.kt`).
Physical device: put PC + phone on same Wi-Fi and set `BASE_URL` to `http://<PC-LAN-IP>:8000/`.
Register two accounts (two emulators/devices) > People tab > chat > text + images, realtime both ways.

## Structure
```
server/app/
  main.py            # FastAPI app, CORS, /uploads static, /ws endpoint
  db.py              # SQLite (users, chats, chat_members, messages)
  security.py        # bcrypt + JWT
  ws.py              # socket registry + broadcast
  routers/auth.py    # register/login/me/presence
  routers/users.py   # user list
  routers/chats.py   # chats, messages, image upload
app/src/main/java/com/example/chatapp/
  data/remote/       # ApiService (Retrofit), Network, SocketManager (OkHttp WS), ServerConfig
  data/              # AuthRepository, ChatRepository, SessionManager (token store)
  ui/                # Compose screens + ViewModels (unchanged UX)
```

## Gradle JDK note
Needs JDK 17: `C:\Users\kundan kr\.jdks\jdk-17.0.20.1+1` (already downloaded).
Android Studio: Settings > Build Tools > Gradle > Gradle JDK > add that folder.
