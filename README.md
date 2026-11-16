# NerionP2PSDK

Java client for the Nerion Launcher's local mod API (`/v1/*`): host a world over P2P, join with an
invite code, invite friends and manage friend requests from your own mod or plugin.

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
    implementation 'com.github.feeldev12:NerionP2PSDK:1.4.5'
}
```

In a mod, also ship it inside your jar as a nested jar: `include` on Fabric (Loom), `jarJar` on Forge
and NeoForge. Other Nerion mods nest it too, and the loader keeps only the newest copy, so everyone
gets the version they were built against or a newer one. A plugin or a plain program can shade it.

Program against `NerionP2PClient` and the classes in `me.feeldev.nerionp2p.models`. Everything in
`me.feeldev.nerionp2p.internal` (except `P2PApiException`) is internal and may change.

## Connect

```java
NerionP2PClient client = new NerionP2PClient();
client.connect(); // non-blocking: waits in the background until the launcher is ready
```

To run code once it's connected, or to tune the client, pass a config:

```java
NerionP2PClient client = new NerionP2PClient(NerionP2PConfig.builder()
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

Every non-2xx response throws `P2PApiException`. Key your handling off `getErrorCode()`
(a typed `ErrorCode` enum with an `UNKNOWN` fallback), never off `getApiMessage()` — the message
text can change wording between launcher versions without notice.

```java
try {
    client.host(25565);
} catch (P2PApiException e) {
    if (e.getErrorCode() == ErrorCode.P2P_SESSION_ACTIVE) {
        client.stop(); // already hosting/joining — tear down first, then retry
    }
}
```

## Forward compatibility

Enum fields on responses (`ConnectionState`, `ConnectionRole`, `ConnectionMode`, `RelayStatus`,
`ErrorCode`) all have an `UNKNOWN` fallback and never throw on an unrecognized wire value — a
future launcher version can add new values without breaking apps built on an older SDK version.
