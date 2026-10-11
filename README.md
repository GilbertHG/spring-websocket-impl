# Team chat over STOMP (Spring Boot)

Demo of the usual Spring production pattern: **STOMP over native WebSocket**, JWT on the **CONNECT** frame, rooms, typing, and presence.

Demo users: `alice`, `bob`, `carol` — password `password`.

```bash
./gradlew bootRun
```

Open http://localhost:8080

- `POST /api/auth/login` issues a JWT
- Browser connects to `ws://localhost:8080/ws` and sends `Authorization: Bearer …` on STOMP CONNECT
- Chat send: `/app/rooms.{roomId}.chat`
- Broadcast: `/topic/rooms.{roomId}`
- Errors: `/user/queue/errors`

The in-memory simple broker and presence map are for a **single node**. A cluster should use a STOMP broker relay (for example RabbitMQ) and shared presence (for example Redis). Production JWT should be RS256/JWKS from an identity provider, not a shared HMAC secret.
