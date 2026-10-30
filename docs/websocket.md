# WebSocket (realtime delivery)

WebSocket/STOMP is **only a realtime delivery channel**. Messages are still created with
`POST /api/v1/conversations/{conversationId}/messages`. PostgreSQL is the source of truth.

## Connection

- Endpoint: `ws://<host>/ws` (native WebSocket, no SockJS). The HTTP handshake does not require `Authorization`.
- Authentication happens in the STOMP `CONNECT` frame:

```
CONNECT
Authorization: Bearer <ACCESS_JWT>
```

Only `ACCESS` tokens with `status = ACTIVE` (CUSTOMER or PARTNER) are accepted; the same JWT rules as the REST API
apply (`userStatus` is ignored). `CONNECT` without a valid token is rejected with an `ERROR` frame.
The STOMP user name is the SONE user UUID.

## Allowed frames

- `CONNECT`, `SUBSCRIBE` to `/user/queue/messages` only, `UNSUBSCRIBE`, `DISCONNECT`.
- Client `SEND` frames and any other subscription destination are rejected.

## Subscription

```
SUBSCRIBE
destination:/user/queue/messages
```

After a message is persisted (transaction committed), the server sends it to both the conversation's customer and
partner, including the sender (so multiple sessions/devices stay in sync). Clients can deduplicate by `id`.

## Payload (`RealtimeMessageResponse`)

```json
{
  "id": "…",
  "conversationId": "…",
  "senderId": "…",
  "type": "TEXT",
  "content": "Hola",
  "createdAt": "2030-05-10T15:30:00Z"
}
```

## Notes

- Offline users are not an error: they do not receive the live event. After (re)connecting, sync the history with
  `GET /api/v1/conversations/{conversationId}/messages`.
- Realtime delivery is best-effort; a delivery failure never affects the persisted message or the REST response.
- Allowed origins: `sone.websocket.allowed-origin-patterns` (env `WEBSOCKET_ALLOWED_ORIGIN_PATTERNS`, default `*`).
  **Production must restrict this to the real client origins.**
- The in-memory simple broker assumes a single service instance.
