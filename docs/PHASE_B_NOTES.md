# Socialo — Phase B Implementation Notes

## New Entities

| Entity | Table | Purpose |
|---|---|---|
| `VerificationToken` | `verification_tokens` | Email verification codes (6-digit, 15 min) and password-reset links (UUID, 30 min) |
| `Appeal` | `appeals` | Suspension appeals submitted by blocked users |
| `UserBlock` | `user_blocks` | Records when user A blocks user B |
| `MessageReaction` | `message_reactions` | Emoji reactions on chat messages |

## Updated Entities

| Entity | New fields |
|---|---|
| `User` | `emailVerified`, `lastActiveAt` |
| `Message` | `mediaType`, `fileName`, `fileSize`, `replyTo` |
| `ConversationMember` | `lastReadMessageId` |

## New Endpoints

### Auth / Account
| Method | Path | Description |
|---|---|---|
| GET | `/verify-email` | Email verification code entry page |
| POST | `/verify-email` | Submit 6-digit code |
| POST | `/verify-email/resend` | Resend code |
| GET | `/reset-password?token=` | Reset password form |
| POST | `/reset-password` | Submit new password |
| GET | `/appeal` | Appeal form |
| POST | `/appeal` | Submit appeal |

### Block / Unblock
| Method | Path | Description |
|---|---|---|
| POST | `/block/{targetId}` | Block a user (form) |
| POST | `/unblock/{targetId}` | Unblock (Settings page) |
| POST | `/block-ajax/{targetId}` | Block (AJAX JSON) |
| POST | `/unblock-ajax/{targetId}` | Unblock (AJAX JSON) |

### Friend (AJAX)
| Method | Path | Description |
|---|---|---|
| POST | `/friends/request-ajax/{id}` | Send friend request — returns `{status}` |
| POST | `/friends/cancel-ajax/{id}` | Cancel request — returns `{status}` |

### Chat / Messenger
| Method | Path | Description |
|---|---|---|
| GET | `/chat/{id}/messages?before={msgId}` | Load 30 older messages (pagination) |
| POST | `/chat/{id}/group/add` | Add member to group (admin only) |
| POST | `/chat/{id}/group/remove` | Remove member (admin only) |
| POST | `/chat/{id}/group/leave` | Leave group |
| POST | `/chat/{id}/group/rename` | Rename group (admin only) |
| POST | `/chat/{id}/group/delete` | Delete group (admin only) |

### WebRTC
| Method | Path | Description |
|---|---|---|
| GET | `/api/webrtc/config` | Returns ICE server config (auth required) |

### Admin
| Method | Path | Description |
|---|---|---|
| POST | `/admin/appeal/approve/{id}` | Approve appeal (restore account + email) |
| POST | `/admin/appeal/reject/{id}` | Reject appeal (email sent) |

## WebSocket (STOMP)

**Endpoint:** `/ws` (SockJS fallback)

**Subscribe (client → server):**

| Destination | Description |
|---|---|
| `/app/chat.send` | Send text message |
| `/app/chat.typing` | Typing indicator |
| `/app/chat.seen` | Mark messages as read |
| `/app/chat.react` | Add/remove emoji reaction on message |
| `/app/chat.unsend` | Soft-delete a message |
| `/app/call.signal` | WebRTC call signaling |

**Receive (server → client):**

| Queue | Description |
|---|---|
| `/user/queue/messages` | New message, reaction update, unsend notification |
| `/user/queue/typing` | Typing indicator from other user |
| `/user/queue/receipts` | Seen receipt |
| `/user/queue/call` | WebRTC signal (invite, offer, answer, ice-candidate, end…) |
| `/topic/presence` | Online/offline broadcasts |

## How to Test with Two Browsers

1. Open browser A: `http://localhost:8081/login` — log in as user A.
2. Open browser B (incognito): same URL — log in as user B.
3. A opens `/chat`, starts a DM with B.
4. Type in A → see "typing…" in B instantly.
5. A sends message → appears in B without refresh.
6. B opens the chat → "Seen" appears in A.
7. Right-click a message → emoji reaction → both see it update.

### WebRTC Calls
1. A and B must be in the same DM conversation.
2. A clicks the phone/video icon in the chat header.
3. B sees the incoming call overlay on any open page.
4. B clicks Accept → both hear/see each other.
5. Test mute, camera off, and hang up.
6. Note: requires HTTPS or localhost. getUserMedia will fail on plain HTTP in production.

## Environment Variables

| Variable | Purpose |
|---|---|
| `MAIL_USERNAME` | Gmail address for sending emails |
| `MAIL_PASSWORD` | Gmail App Password (not your account password) |
| `TURN_URL` | TURN server URL for WebRTC in production |
| `TURN_USERNAME` | TURN username |
| `TURN_CREDENTIAL` | TURN credential |

If `MAIL_USERNAME` is empty, emails are printed to the console — safe for development.

## How to Create a Gmail App Password

1. Go to Google Account → Security → 2-Step Verification (must be on).
2. App passwords → create one for "Mail".
3. Set `MAIL_USERNAME=youraddress@gmail.com` and `MAIL_PASSWORD=the-16-char-app-password`.

## SQL changes (applied automatically by ddl-auto=update)

All new tables and columns are created automatically on startup. No manual SQL required.
