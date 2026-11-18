# Nerion Network SDK

Java client for the Nerion Launcher's local mod API (`/v1/*`): host a world (P2P, or through the
relay when P2P can't connect), join with an invite code, invite friends and manage friend requests from your own mod or plugin.

- No dependencies and no Minecraft classes: works in Fabric, Forge and NeoForge mods, Bukkit/Paper
  plugins and plain Java tools. Java 17+.
- Only works while the game was started by the Nerion Launcher: it opens the local API and leaves
  its address and a key in the game's folder, where the client finds them by itself.
- Full contract of the API: [`docs/api-reference.md`](docs/api-reference.md).

## Install

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.feeldev12:NerionNetworkSDK:2.0.0'
}
```

**In a mod**, don't ship the SDK (no `include`, `jarJar` or shading): the jar is itself a mod with
no code, **Nerion Network SDK** (`nerionnetwork`), and the Nerion launcher puts it in every instance.
One copy per game, shared by every mod, always the latest. Declare it as a dependency:

- Fabric (`fabric.mod.json`): `"depends": { "nerionnetwork": ">=2.0.0" }`
- NeoForge / Forge (`mods.toml`): a dependency on `modId = "nerionnetwork"` with `versionRange = "[2.0.0,)"` and
  `mandatory = true` (Forge rejects a dependency without it)

To try your mod from your IDE, add it to the game you run there too (Loom: `localRuntime`,
ForgeGradle/NeoGradle: `runtimeOnly`).

**In a plugin or a plain Java program**, use `implementation` and shade it into your jar.

From 2.0 on, releases only add things: nothing public is removed or changed, so a mod built for
2.0 keeps working with any newer 2.x.

Program against `NerionNetworkClient` and the classes in `me.feeldev.nerionnetwork.models`. Everything in
`me.feeldev.nerionnetwork.internal` (except `NetworkApiException`) is internal and may change.

## Connect

```java
NerionNetworkClient client = new NerionNetworkClient();
client.connect(); // non-blocking: waits in the background until the launcher is ready
```

To run code once it's connected, or to tune the client, pass a config:

```java
NerionNetworkClient client = new NerionNetworkClient(NerionNetworkConfig.builder()
    .onConnected(() -> System.out.println("Connected to the Nerion launcher"))
    .build());
```

The launcher's credentials live in `config/nerionsdk/` for mods and in `plugins/NerionSDK/` for
Bukkit/Paper servers, and the client picks the right one from the working directory. Only if your
code doesn't run from the game's own folder, point it there with `.credentialFile(...)`.

## Host a session (open to LAN)

```java
HostResult host = client.host(25565); // STANDBY mode by default
StatusResponse status = client.awaitState(
    Duration.ofSeconds(15), Duration.ofSeconds(2),
    ConnectionState.CONNECTED, ConnectionState.FALLBACK, ConnectionState.ERROR);

if (status.isConnected()) {
    InviteInfo invite = client.getInvite();
    System.out.println("Share this: " + invite.getUrl());
}

// later, on world close / server shutdown:
client.stop();
```

## Join someone else's session

```java
JoinResult join = client.joinByInviteCode(inviteCodeFromPlayer);

// ALWAYS branch on mode — a RELAY response has localBridgePort == 0, that's the contract, not a bug.
String host = join.getConnectHost();
int port = join.getConnectPort();
// connectMinecraftTo(host, port);

// on disconnect:
client.stopJoining();
```

## Rooms with players who aren't friends

For a minigame lobby that hands a room to a group of players, open the session as `UNLISTED`: it
never shows up in any list, and anyone who has the tunnel ID can join.

```java
// the player chosen to host
SessionInfo session = client.createSession(Visibility.UNLISTED);
client.host(lanPort);
sendToYourLobbyServer(session.getTunnelId());

// everyone else, once your server passes them the tunnel ID
JoinResult join = client.joinByTunnelIdWhenReady(tunnelId, Duration.ofSeconds(30));
```

`joinByTunnelIdWhenReady` waits while the host's world is still opening (`host_not_ready`) and
throws any other error right away. With a plain `joinByTunnelId`, check `e.isHostNotReady()`.

Invite codes work for anyone too: `getInvite()` works right after `createSession()`, has no use
limit by default and doesn't expire. `joinByInviteCode` takes its token or its whole URL. Every
join with the code counts one use, even when the connection fails afterwards.

## Invite a friend to your session

```java
List<Friend> friends = client.getFriends().getFriends();

for (Friend friend : friends) {
    if (friend.isOnline()) {
        System.out.println(friend.getPlayerName() + " is online");
    }
}

// requires the caller to already be playing an instance (true for a client mod, since it's
// running inside the Minecraft process the launcher just started)
client.inviteFriendToSession(friends.get(0).getPlayerAccountId());
```

## Players of the session

Everyone who joined the session you host or joined, including those who left, so you can show
them and invite them back even if they aren't friends (like "recent players" in Steam). Any player
of the session can invite; only the host reports joins/leaves and bans. Needs a launcher with
`client.ping().supportsSessionPlayers()`.

```java
// Host: from your server's player join/leave hooks while hosting
client.reportPlayerJoined(uuid, name);
client.reportPlayerLeft(uuid, name);

// Anyone in the session
for (SessionParticipant p : client.getSessionParticipants().getParticipants()) {
    String state = p.isInSession() ? "playing" : "left at " + p.getLeftAt();
    System.out.println(p.getMinecraftName() + " — " + state);
    if (!p.isInSession() && p.canInvite()) {
        client.inviteToSession(p.getPlayerAccountId()); // friend or not: they were in this session
    }
}

// Host: ban from the session (also kick/ban them from your game server)
client.setPlayerBanned(p.getPlayerAccountId(), true);
client.setPlayerBannedByUuid(uuid, name, true);      // players without a Nerion account
```

Errors to handle: `isInviteRateLimited()` (3 invites per hour to the same non-friend),
`isPlayerBanned()`, `isInvalidInvitee()`, `isNotTunnelParticipant()`, and `isTunnelBanned()` when
the host banned you.

## Manage friend requests

```java
for (FriendRequest req : client.getFriendRequests().getRequests()) {
    System.out.println(req.getFromPlayerName() + " wants to be friends");
    client.acceptFriendRequest(req.getRequestId());
}

client.sendFriendRequest(someOtherPlayerAccountId);

// check on it later, or withdraw it:
for (FriendRequest sent : client.getSentFriendRequests().getRequests()) {
    client.cancelFriendRequest(sent.getRequestId());
}

// once you're friends:
client.toggleFavorite(friend.getFriendshipId()); // NOT friend.getPlayerAccountId()
client.removeFriend(friend.getPlayerAccountId());
```

## Error handling

Every non-2xx response throws `NetworkApiException`. Key your handling off `getErrorCode()`
(a typed `ErrorCode` enum with an `UNKNOWN` fallback), never off `getApiMessage()` — the message
text can change wording between launcher versions without notice.

```java
try {
    client.host(25565);
} catch (NetworkApiException e) {
    if (e.getErrorCode() == ErrorCode.P2P_SESSION_ACTIVE) {
        client.stop(); // already hosting/joining — tear down first, then retry
    }
}
```

## Forward compatibility

Enum fields on responses (`ConnectionState`, `ConnectionRole`, `ConnectionMode`, `RelayStatus`,
`ErrorCode`) all have an `UNKNOWN` fallback and never throw on an unrecognized wire value — a
future launcher version can add new values without breaking apps built on an older SDK version.
