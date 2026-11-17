# Nerion Launcher Mod API — Reference

> Scope: the local HTTP control plane the Nerion launcher exposes to same-machine Minecraft mods
> and server plugins (`/v1/*`). This document assumes **zero prior context** on the launcher's
> internal codebase — everything you need to build against this API from a fabric/forge/neoforge
> mod or a bukkit/paper plugin is here.
>
> Audience: third-party mod/plugin developers integrating with the Nerion launcher. If you are
> working *inside* this repository and want the internal Rust architecture instead, see
> [`docs/architecture/tunnel-p2p-relay.md`](../architecture/tunnel-p2p-relay.md), §9.

## 1. What this API is for

When a player launches Minecraft (or a dedicated server) through the Nerion launcher, the launcher
starts a small local HTTP server on `127.0.0.1` and hands your mod/plugin a bearer token. Through
that server you can, on behalf of the player who launched you:

- Create/inspect/tear down a **tunnel** (a public `host:port` that maps back to the player's local
  Minecraft server, punched through NAT via P2P when possible, relayed otherwise).
- **Host** a session so other players can join it (`/v1/p2p/host`).
- **Join** someone else's session by invite code or tunnel ID (`/v1/p2p/join`).
- Poll live connection status (`/v1/p2p/status`).
- Ask the launcher for a free local port (`/v1/net/free-port`).
- List the player's **friends** and **invite one to your active session**
  (`/v1/social/friends`, `/v1/social/invite/{friendPlayerAccountId}`).

You never talk to the launcher's backend, STUN servers, or the relay server directly — the
launcher does all of that for you and exposes one simple, versioned, token-authenticated JSON API
instead.

## 2. Discovering the API — the credential file

The launcher writes a small credential file **before** it starts the Minecraft process (client or
dedicated server), so it is guaranteed to already exist by the time your mod/plugin's own
initialization code runs.

**Path convention** — the launcher picks one of two locations depending on which subdirectory
already exists in the game/server directory at launch time:

| Condition | Credential file path |
|---|---|
| `plugins/` directory exists (bukkit/paper-style server plugins) | `plugins/NerionSDK/launcher-api-credential.txt` |
| Otherwise (fabric/forge/neoforge-style client or server mods) | `config/nerionsdk/launcher-api-credential.txt` |

Both paths are relative to the Minecraft **game directory** (the same directory your mod/plugin
already runs from). This is the exact same directory-detection convention the launcher's older
`room-credential.txt` hand-off uses — the two files are unrelated and never collide (different
file names, different key vocabulary).

**File format** — plain `key=value` lines, one per line, UTF-8, no JSON, no quoting:

```properties
nerion.launcherApiUrl=http://127.0.0.1:52341
nerion.modToken=3f9c1a7e2b8d4f6a0c1e5b9d7a3f2c8e1b6d4a9f0c3e7b1d5a8f2c6e0b4d9a71
nerion.apiVersion=1
nerion.scopes=p2p.host,p2p.join,p2p.session,p2p.status,net.port,social.friends.read,social.friends.invite,social.friends.write
```

| Key | Meaning |
|---|---|
| `nerion.launcherApiUrl` | Base URL of the local API server, e.g. `http://127.0.0.1:52341`. The port is ephemeral and picked fresh every launch — **never hardcode a port**, always read it from this file. |
| `nerion.modToken` | Your bearer token (64 hex characters, 256 bits of entropy). Treat it as a secret for the lifetime of this process. |
| `nerion.apiVersion` | Currently `1`. Matches the `apiVersion` field `GET /v1/ping` also reports. |
| `nerion.scopes` | Comma-separated list of scopes this token is authorized for (see §5). Currently every session is issued the full scope set — there is no per-launch narrowing yet, but don't assume that stays true forever; read this field rather than hardcoding an assumed scope list. |

**Practical read pattern**: poll for the file's existence for a few seconds after your mod/plugin
initializes (it is written just before the JVM spawns, so it should already be there, but don't
assume zero latency on slow disks), then parse it once and cache `launcherApiUrl` + `modToken` for
the rest of the session. There is no need to re-read the file after that — the token doesn't
rotate mid-session.

**Cleanup**: the launcher deletes this file (best-effort) when the game/server process exits and
the token is revoked. Don't rely on the file's absence as a signal though — deletion is
best-effort, not guaranteed (e.g. if the disk is unavailable at that exact moment).

## 3. Authentication

Every request to a scoped route (anything except `GET /v1/ping`) must include:

```
Authorization: Bearer <nerion.modToken value>
```

- **Missing or unrecognized token** → `401 invalid_token`.
- **Token doesn't carry the scope a route requires** → `403 missing_scope`.
- **Token revoked** (the launch that issued it exited) → `401 invalid_token`, identical to a token
  that was never valid. There is no separate "expired" vs "never existed" distinction — don't try
  to tell them apart.

You never send `instanceId`, `tunnelId` (for your OWN session), or any player identity in the
request — the token alone tells the launcher who is calling and which local game session owns the
request. The launcher resolves everything else server-side.

## 4. Base URL and versioning

Every route is prefixed `/v1`. The base URL comes from the credential file's
`nerion.launcherApiUrl` (e.g. `http://127.0.0.1:52341`). All requests and responses are JSON
(`Content-Type: application/json`), except that GET requests obviously send no body.

`GET /v1/ping` requires no token and tells you the current `apiVersion` plus which resource groups
(`capabilities`) are registered — call it first, before you've even finished parsing the
credential file's `apiVersion`, if you want to confirm connectivity.

## 5. Scopes

| Scope | Grants |
|---|---|
| `p2p.session` | Create/read your own tunnel session, read your own invite link. |
| `p2p.join` | Resolve someone else's invite code, join a session, stop your own joiner bridge. |
| `p2p.host` | Start hosting (P2P or relay-only), read relay port mappings, full teardown (`/v1/p2p/stop`). |
| `p2p.status` | Poll `/v1/p2p/status`. |
| `net.port` | Ask the launcher to find a free local port (`/v1/net/free-port`). |
| `social.friends.read` | List the caller's friends and friend requests (`/v1/social/friends`, `/v1/social/requests`, `/v1/social/requests/sent`). |
| `social.friends.invite` | Invite a friend to the caller's currently active play session (`/v1/social/invite/{friendPlayerAccountId}`). |
| `social.friends.write` | Every friend-request/friendship MUTATION: send/accept/reject/cancel a request, remove a friend, toggle favorite (§8.16-§8.20); also finding a player to send a request to (`GET /v1/social/search?q=`, answers `{"players": [{playerAccountId, username, displayName, avatarUrl}]}`). |

Every session issued today carries the full scope set — the `nerion.scopes` line in the credential
file will list all eight. Still, respect `403 missing_scope` responses rather than assuming a scope
you were granted at the start of a session remains granted forever; future launcher versions may
narrow scopes per-launch (e.g. a server plugin might only ever get `p2p.host`, never `p2p.join`).

## 6. Error envelope

Every non-2xx response has this exact shape:

```json
{
  "error": {
    "code": "invalid_token",
    "message": "missing or unrecognized bearer token"
  }
}
```

**Always key your error handling off `code`, never off `message`** — `message` is a free-form,
human-readable string that may change wording between launcher versions without notice.

| `code` | HTTP status | Meaning |
|---|---|---|
| `invalid_port` | 400 | A `lanPort`/`localPort` field was `0` or otherwise invalid. |
| `invalid_target` | 400 | A `/v1/p2p/join` or `/v1/p2p/resolve` request's target fields were malformed (see §8.9). |
| `invalid_token` | 401 | Missing, unrecognized, or revoked bearer token. |
| `missing_scope` | 403 | Token is valid but lacks the scope this route requires. |
| `no_active_session` | 404 | You called a route that needs a previously-created tunnel session (e.g. `GET /v1/p2p/session`) before ever creating one. |
| `p2p_session_active` | 409 | This launcher process already holds an active P2P/relay role — call `POST /v1/p2p/stop` first. |
| `tunnel_already_active` | 409 | The player's ACCOUNT already has an active tunnel (a backend-side limit, independent of this specific launcher process). |
| `not_playing_instance` | 403 | `POST /v1/social/invite/{friendPlayerAccountId}` was called while the caller has no active play session — you must actually be playing an instance to invite someone to it. |
| `backend_error` | 502 | The launcher's own backend call failed for a reason not covered above (network error, unexpected status). |
| `auth_unavailable` | 503 | The launcher can't currently authenticate to its own backend on your behalf (e.g. called too early during startup). Retry shortly. |
| `not_found` | 404 | Unknown route, or a request to an unversioned/unrecognized path. Also returned by `/v1/p2p/join` when the host already closed the session. |
| `host_not_ready` | 409 | `/v1/p2p/join` reached a session whose host hasn't started hosting yet (no `/v1/p2p/host` or `/v1/p2p/relay` so far). Wait a moment and join again. |
| `join_not_allowed` | 403 | `/v1/p2p/join` by `tunnelId` reached a `FRIENDS_ONLY` or `PRIVATE` session and you aren't the host's friend. Use an `UNLISTED`/`PUBLIC` session, or join with its invite code. |

Note: `not_hosting` is a reserved code that is **never actually returned**. `POST /v1/p2p/stop` and
`POST /v1/p2p/join/stop` are both safe to call even when nothing is running — they no-op instead
of erroring. Don't build error-handling logic around `not_hosting` ever appearing.

## 7. The relay-fallback contract trap — read this before you write your join logic

`POST /v1/p2p/join`'s success response always looks like one of these two shapes:

```json
{ "mode": "P2P", "localBridgePort": 51234, "publicHost": "relay7.nerionmc.com", "publicPort": 25589 }
```

```json
{ "mode": "RELAY", "localBridgePort": 0, "publicHost": "relay7.nerionmc.com", "publicPort": 25589 }
```

**`localBridgePort: 0` on a `RELAY` response is not a bug — it is the contract.** A P2P connection
gives you a real loopback TCP listener at `localBridgePort` (connect Minecraft to
`127.0.0.1:<localBridgePort>`). A relay-only connection has no local listener at all — you must
connect **directly** to `publicHost:publicPort` instead.

**If your code only ever reads `localBridgePort` and blindly does `127.0.0.1:<port>`, you will try
to connect to port 0 whenever P2P fails and relay kicks in.** Always branch on `mode`:

```
if response.mode == "P2P":
    connectTo("127.0.0.1", response.localBridgePort)
else: # "RELAY"
    connectTo(response.publicHost, response.publicPort)
```

Both `publicHost`/`publicPort` are included on **both** branches (not just `RELAY`) specifically so
you always have a fallback address available even after a successful P2P connection, without a
second API round-trip.

## 8. Route table

All bodies are JSON. Fields use `camelCase`. Optional fields may be omitted entirely (not just set
to `null`).

### 8.1 `GET /v1/ping` — unauthenticated discovery

No token required.

**Response `200`:**
```json
{ "apiVersion": 1, "capabilities": ["net", "p2p", "social"] }
```

### 8.2 `GET /v1/net/free-port` — scope `net.port`

Ask the launcher to find a free local TCP port, useful for picking a LAN port for a dedicated
server before it binds.

**Query params:** `?start=49152` (optional; the port-search floor to scan from upward, default
`49152`).

**Response `200`:**
```json
{ "port": 49153 }
```

### 8.3 `POST /v1/p2p/session` — scope `p2p.session` — create a tunnel WITHOUT hosting

Creates a backend tunnel row and marks it as your session's "own" tunnel, but does **not** start
any P2P/relay connection yet — pair this with `/v1/p2p/host` or `/v1/p2p/relay` afterward, or use
`/v1/p2p/host` alone (it creates a tunnel implicitly if you haven't called this first).

**Request body:**
```json
{ "visibility": "PRIVATE" }
```
`visibility` is optional; omit it to use the backend's default (`FRIENDS_ONLY`).

| `visibility` | Listed | Who can join |
|---|---|---|
| `PUBLIC` | Yes, for everyone | Anyone signed in to Nerion, by `tunnelId`. |
| `UNLISTED` | Never | Anyone who has the `tunnelId`. Made for rooms a mod or server hands out itself. |
| `FRIENDS_ONLY` | To the host's friends | The host's friends by `tunnelId`; anyone with the invite code. |
| `PRIVATE` | Never | Only with the invite code. |

The same values work for `POST /v1/p2p/visibility`.

**Response `201`:**
```json
{ "tunnelId": "abc123", "publicHost": "relay7.nerionmc.com", "publicPort": 25589, "relayPort": 7001 }
```

### 8.4 `GET /v1/p2p/session` — scope `p2p.session` — join info for MY OWN active tunnel

Requires you already have an active tunnel for this session (created via `/v1/p2p/session` or
`/v1/p2p/host`) — otherwise `404 no_active_session`.

**Response `200`:**
```json
{ "tunnelId": "abc123", "p2pSessionId": "sess-1", "publicHost": "relay7.nerionmc.com", "publicPort": 25589, "relayPort": 7001 }
```
`p2pSessionId` may be `null` if no joiner has connected via P2P signaling yet.

### 8.5 `GET /v1/p2p/invite` — scope `p2p.session` — invite link for MY OWN active tunnel

Requires an active tunnel, same precondition as §8.4. Works right after `/v1/p2p/session`, before
you start hosting.

**Response `200`:**
```json
{ "url": "https://nerionmc.com/join/tunnel/xyz", "token": "xyz", "maxUses": null, "usesCount": 2, "expiresAt": null }
```
`maxUses: null` means no limit, the default. Links don't expire today (`expiresAt` is always
`null`). Each resolve of the code (§8.6, or a join by `inviteCode`) counts one use, even when the
connection fails afterwards.

### 8.6 `POST /v1/p2p/resolve` — scope `p2p.join` — resolve SOMEONE ELSE'S invite code

Look up what an invite code points to before deciding to join it (e.g. to show the player a
confirmation dialog with the owner's name).

**Request body:**
```json
{ "inviteCode": "xyz" }
```

**Response `200`:**
```json
{
  "tunnelId": "abc123",
  "type": "TUNNEL",
  "instanceId": "inst-1",
  "instanceName": "SkyBlock",
  "ownerDisplayName": "Steve",
  "ownerAvatarUrl": "https://..."
}
```

### 8.7 `POST /v1/p2p/host` — scope `p2p.host` — create-or-reuse a tunnel and start hosting

The main "open my world for others" call.

**Request body:**
```json
{
  "lanPort": 25565,
  "extraPorts": [{ "port": 8123, "protocol": "tcp" }],
  "mode": "STANDBY",
  "tunnelId": "abc123"
}
```

| Field | Required | Meaning |
|---|---|---|
| `lanPort` | yes | The local port your Minecraft server is actually listening on. Must be nonzero — `0` → `400 invalid_port`. |
| `extraPorts` | no | Additional TCP/UDP ports to tunnel alongside the main port (e.g. a Dynmap web port). Each entry is `{port, protocol}` where `protocol` is `"tcp"` or `"udp"`. |
| `mode` | no | `"STANDBY"` (default) — a long-lived loop that accepts joiners one after another over the whole hosting session; use this for "open to LAN, friends drop in over time". `"SINGLE"` — one hosting attempt with automatic relay fallback if P2P fails, does not accept a second joiner the same way. Prefer `STANDBY` unless you have a specific reason not to. |
| `tunnelId` | no | Reuse a tunnel you already created via `/v1/p2p/session`. Must match YOUR session's own active tunnel — passing an arbitrary/foreign ID returns `400 invalid_target`. Omit it to let the launcher create a fresh tunnel. |

**Response `202`** (hosting is starting asynchronously — poll `/v1/p2p/status` to see it come up):
```json
{ "tunnelId": "abc123", "publicHost": "relay7.nerionmc.com", "publicPort": 25589, "relayPort": 7001, "state": "STARTING" }
```

**Conflict**: if this launcher process is already hosting or joining another session, you get
`409 p2p_session_active` — call `POST /v1/p2p/stop` first.

### 8.8 `POST /v1/p2p/relay` — scope `p2p.host` — relay-only, skip P2P entirely

Use this if you specifically want to guarantee a relay connection without attempting P2P at all
(e.g. you already know P2P won't work in this environment). Requires an active tunnel (§8.4's
precondition) — call `/v1/p2p/session` first, or use `/v1/p2p/host` instead if you want the
launcher to create the tunnel for you.

**Request body:**
```json
{ "localPort": 25565, "extraPorts": [{ "port": 8123, "protocol": "tcp" }] }
```

**Response `200`:**
```json
{
  "publicHost": "relay7.nerionmc.com",
  "publicPort": 25589,
  "extraPorts": [{ "localPort": 8123, "protocol": "tcp" }]
}
```

### 8.9 `GET /v1/p2p/ports` — scope `p2p.host` — current relay extra-port mappings

**Response `200`:**
```json
{ "ports": [{ "localPort": 8123, "publicPort": 30412 }] }
```

### 8.10 `POST /v1/p2p/join` — scope `p2p.join` — join SOMEONE ELSE'S session

Provide **either** `inviteCode` **or** `tunnelId` (optionally with `shareCode`) — never both, and
never neither. Both missing or both present → `400 invalid_target`.

**Request body (invite code form):**
```json
{ "inviteCode": "xyz" }
```
`inviteCode` is the invite's `token`, not its URL (the Java SDK accepts either and sends the
token). The code lets anyone in, friend or not, whatever the session's visibility.

**Request body (tunnel ID form):**
```json
{ "tunnelId": "abc123", "shareCode": "friend-share-1" }
```

By `tunnelId`, anyone can join a `PUBLIC` or `UNLISTED` session; a `FRIENDS_ONLY` one only takes
the host's friends (`403 join_not_allowed`). While the host hasn't started hosting yet you get
`409 host_not_ready` instead of an address nothing listens on: wait a moment and call again (the
Java SDK's `joinByTunnelIdWhenReady` does it for you).

**Response `200`:** see §7 for the full shape and the relay-fallback contract trap. Read §7 before
implementing this route's caller.

**Timing**: the launcher tries P2P first, the same way Nerion's own invites do. It waits a couple
of seconds for the host's first P2P offer, punches through, and answers with `mode: "P2P"` and
`localBridgePort`. If P2P can't connect, it answers with `mode: "RELAY"` instead. Over relay it also
waits (up to a few seconds) for the host to report its relay extra ports, so voice chat and any
other extra port reach the host too. You don't need your own retry loop around this call: by the
time it returns, the launcher already did the waiting.

Behind strict NATs the P2P attempt alone can take ~40 s before the launcher settles on relay, so
give this request at least 60 s (the Java SDK does) and make it off the game thread. Timing out
sooner and calling again starts a second join while the first is still running: the game ends up
on relay while the first attempt opens an unused P2P connection that takes the extra ports.

### 8.11 `POST /v1/p2p/join/stop` — scope `p2p.join` — stop ONLY the joiner connection

Tears down your own joiner-side bridge. Does not affect anyone else's hosting session — safe to
call even if you were never actually connected (idempotent, always succeeds).

**Response `200`:** `{}`

### 8.12 `GET /v1/p2p/status` — scope `p2p.status` — poll-safe status

Safe to poll repeatedly — this route has no side effects, calling it never changes anything.

**Response `200` (no active session):**
```json
{ "state": "IDLE", "role": null, "mode": null, "tunnelId": null, "localBridgePort": null, "lanPort": null, "relayStatus": "IDLE" }
```

**Response `200` (hosting, connected via P2P):**
```json
{
  "state": "CONNECTED",
  "role": "HOST",
  "mode": "P2P",
  "tunnelId": "abc123",
  "localBridgePort": 25565,
  "lanPort": 25565,
  "relayStatus": "ONLINE"
}
```

| Field | Values |
|---|---|
| `state` | `IDLE`, `DETECTING_NAT`, `SIGNALING`, `PUNCHING`, `HANDSHAKING`, `CONNECTED`, `FALLBACK_PENDING`, `FALLBACK`, `ERROR` |
| `role` | `HOST`, `JOINER`, or `null` |
| `mode` | `P2P`, `RELAY`, or `null` |
| `lanPort` | Only populated for `role: "HOST"` — the local Minecraft port being forwarded. Always `null` for `role: "JOINER"`. |
| `relayStatus` | The independent relay-sidecar connection status: `IDLE`, `CONNECTING`, `ONLINE`, `STOPPED`, `ERROR`. This is a SEPARATE state machine from `state` — the host keeps a relay connection alive as a safety net even while `state: "CONNECTED"` via P2P, so don't assume `relayStatus` mirrors `state`. |

### 8.13 `POST /v1/p2p/stop` — scope `p2p.host` — full teardown

Stops hosting entirely (kills the standby loop, all joiner bridges connected to you, and the relay
sidecar). Idempotent — safe to call even if nothing was running.

**Response `200`:** `{}`

### 8.14 `GET /v1/social/friends` — scope `social.friends.read`

Lists the caller's friends. Same shape whether the caller is a client mod or a server plugin — the
launcher resolves the friends list off the token's identity alone, exactly like every other scoped
route.

**Response `200`:**
```json
{
  "friends": [
    {
      "friendshipId": "f-1",
      "playerAccountId": "acc-42",
      "username": "steve123",
      "playerName": "Steve",
      "mcUsername": "Steve",
      "avatarUrl": "https://...",
      "primarySkinUrl": "https://...",
      "online": true,
      "currentInstanceId": "inst-9",
      "currentInstanceName": "SkyBlock",
      "lastActiveAt": 1735689600,
      "favorite": false,
      "socialLinks": {}
    }
  ]
}
```

`currentInstanceId`/`currentInstanceName` are `null` when the friend isn't currently playing
anything. `lastActiveAt` is a Unix timestamp (seconds); `null` if never recorded.

### 8.15 `POST /v1/social/invite/{friendPlayerAccountId}` — scope `social.friends.invite`

Invites the given friend to join the CALLER's currently active play session. No request body — the
launcher resolves both "who is calling" (the bearer token) and "which instance they're inviting the
friend to" (the caller's own active play session, tracked server-side) without either being supplied
explicitly, the same "never send your own instanceId" shape every `/v1/p2p/*` route already follows.

**Response `200`:** `{}`

**Errors:**
- `403 not_playing_instance` — the caller has no active play session right now. You must actually be
  playing an instance before you can invite someone to it.
- `404 backend_error` (wrapping the backend's own 404) — `friendPlayerAccountId` doesn't resolve to a
  real player account.

Note: this route does not itself verify the two players are actually friends — that's enforced
server-side by `FriendshipService`, surfaced here as a generic `backend_error` if it ever rejects the
pair for a reason not covered by the table above.

### 8.16 `GET /v1/social/requests` / `GET /v1/social/requests/sent` — scope `social.friends.read`

Lists friend requests the caller has RECEIVED (`/v1/social/requests`) or SENT
(`/v1/social/requests/sent`). Both wrap their list the same way `/v1/social/friends` does.

**`GET /v1/social/requests` response `200`:**
```json
{
  "requests": [
    { "requestId": "req-1", "fromPlayerAccountId": "acc-7", "fromPlayerName": "Alex", "fromAvatarUrl": "https://...", "fromPrimarySkinUrl": "https://...", "sentAt": 1735689600 }
  ]
}
```

**`GET /v1/social/requests/sent` response `200`:**
```json
{
  "requests": [
    { "requestId": "req-2", "fromPlayerAccountId": "acc-9", "fromPlayerName": "Jordan", "sentAt": 1735689650 }
  ]
}
```

Quirk to know about: the sent-requests entries use the SAME `fromPlayerAccountId`/`fromPlayerName`/
`fromAvatarUrl` key names as received requests, even though for a SENT request those values describe
the person you sent it TO, not someone it's "from" — this is the backend's own wire shape, not a bug
in this route. Don't be misled by the key names when rendering a sent-requests list.

### 8.17 `POST /v1/social/requests` — scope `social.friends.write`

Sends a friend request to the given player.

**Request body:**
```json
{ "playerAccountId": "acc-7" }
```

**Response `200`:** `{}`

**Errors:** `400 invalid_target` if the body is missing/malformed; `backend_error` for anything the
backend rejects (already friends, already a pending request, unknown account, etc. — none of these
get a dedicated code, key off the backend's message if you need to distinguish them, though per §6
you should generally avoid relying on `message` text).

### 8.18 `POST /v1/social/requests/{requestId}/accept` — scope `social.friends.write`

Accepts a friend request the caller RECEIVED. `requestId` comes from `GET /v1/social/requests`.

**Response `200`:** `{}`

### 8.19 `POST /v1/social/requests/{requestId}/reject` — scope `social.friends.write`

Rejects a friend request the caller RECEIVED. Same `requestId` source as §8.18.

**Response `200`:** `{}`

### 8.20 `POST /v1/social/requests/{requestId}/cancel` — scope `social.friends.write`

Cancels a friend request the caller SENT (not one they received — use §8.19 for that).
`requestId` comes from `GET /v1/social/requests/sent`. Wraps LauncherBackend's
`DELETE /friends/requests/{id}` — translated to a `POST .../cancel` verb here to keep this
mod-API's OWN surface internally consistent (compare `POST /v1/p2p/join/stop`) rather than
introducing `DELETE` as a one-off REST style.

**Response `200`:** `{}`

### 8.21 `POST /v1/social/friends/{friendPlayerAccountId}/remove` — scope `social.friends.write`

Removes an existing friend. `friendPlayerAccountId` is the same id `/v1/social/friends` returns per
entry. Wraps LauncherBackend's `DELETE /friends/{friendPlayerAccountId}` — same verb-translation
rationale as §8.20.

**Response `200`:** `{}`

### 8.22 `POST /v1/social/friends/{friendshipId}/favorite` — scope `social.friends.write`

Toggles favorite status on an existing friendship (favorite → not-favorite, or the reverse — this
route doesn't take a target state, it flips whatever the current one is). `friendshipId` is the same
id `/v1/social/friends` returns per entry as `friendshipId` (NOT the same value as
`friendPlayerAccountId` used in §8.21 — don't mix the two up).

**Response `200`:** `{}`

## 9. Example: a client mod's typical hosting flow

```
1. Read credential file → { launcherApiUrl, modToken }
2. GET  {launcherApiUrl}/v1/ping                      (optional sanity check, no token needed)
3. POST {launcherApiUrl}/v1/p2p/host  {"lanPort": 25565}
   → 202 { tunnelId, publicHost, publicPort, state: "STARTING" }
4. Poll GET {launcherApiUrl}/v1/p2p/status every ~2s until state != "IDLE"/"DETECTING_NAT"/...
5. GET  {launcherApiUrl}/v1/p2p/invite  → share `url` with friends
6. ... player plays ...
7. POST {launcherApiUrl}/v1/p2p/stop    (on world close / server shutdown)
```

## 10. Example: a client mod's typical joining flow

```
1. Read credential file → { launcherApiUrl, modToken }
2. POST {launcherApiUrl}/v1/p2p/join  {"inviteCode": "<code the player pasted>"}
   → 200 { mode: "P2P", localBridgePort: 51234, publicHost, publicPort }
      or  { mode: "RELAY", localBridgePort: 0, publicHost, publicPort }
3. Branch on `mode` exactly as described in §7 — connect Minecraft accordingly.
4. ... player plays ...
5. POST {launcherApiUrl}/v1/p2p/join/stop  (on disconnect)
```

## 10.5. Example: inviting a friend to your session

```
1. Read credential file → { launcherApiUrl, modToken }
2. GET  {launcherApiUrl}/v1/social/friends  → render the list, highlight `online: true` entries
3. Player picks a friend → POST {launcherApiUrl}/v1/social/invite/{friendPlayerAccountId}
   → 200 {}  (the friend gets a notification; you are not told whether they accept)
```

Requires the caller to already be playing an instance (typically true for a client mod, since it's
running inside the Minecraft process the launcher just started) — see `403 not_playing_instance`
in §8.15 if not.

## 10.6. Example: managing friend requests

```
1. Read credential file → { launcherApiUrl, modToken }
2. GET  {launcherApiUrl}/v1/social/requests  → render incoming requests
3. Player accepts one → POST {launcherApiUrl}/v1/social/requests/{requestId}/accept
   → 200 {}  (they're now friends — GET /v1/social/friends will include them from here on)

... sending your own request ...
4. POST {launcherApiUrl}/v1/social/requests  {"playerAccountId": "acc-7"}
   → 200 {}
5. GET  {launcherApiUrl}/v1/social/requests/sent  → confirm it shows up, or
   POST {launcherApiUrl}/v1/social/requests/{requestId}/cancel  to withdraw it
```

## 11. Cross-loader parity

The contract above is identical whether you are a fabric/forge/neoforge **client mod** or a
bukkit/paper **server plugin** — same routes, same request/response shapes, same error codes. The
only difference is which credential-file path convention applies to your loader (§2's table) and,
practically, which routes make sense to call (a server plugin typically only ever calls
`/v1/p2p/host`/`/v1/p2p/status`/`/v1/p2p/stop`; a client mod typically only ever calls
`/v1/p2p/join`/`/v1/p2p/join/stop`/`/v1/p2p/status` — but the API itself does not enforce this
split; nothing stops a client mod from hosting too).

## 12. See also

- [`docs/mod-api/e2e-checklist.md`](e2e-checklist.md) — manual, curl-driven end-to-end
  verification checklist for confirming a launcher build actually serves this API correctly.
- [`docs/architecture/tunnel-p2p-relay.md`](../architecture/tunnel-p2p-relay.md) — internal Rust
  architecture (P2P engine, relay protocol, backend bookkeeping) this API is a thin HTTP wrapper
  over. Only relevant if you are modifying the launcher itself, not building a mod against it.
