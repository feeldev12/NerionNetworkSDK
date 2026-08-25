# NerionP2PSDK

Standalone Java client for the Nerion Launcher's local P2P mod API (`/v1/*`). Zero Minecraft
dependencies — safe to use from a fabric/forge/neoforge mod, a bukkit/paper plugin, or a plain
Java tool. Full contract: `docs/mod-api/api-reference.md` in the app-launcher repo.

## Install

```gradle
repositories { mavenLocal() }
dependencies { implementation "me.feeldev:nerion-p2p-sdk:1.3.0" }
```

## Connect

```java
NerionP2PConfig config = NerionP2PConfig.builder()
    .credentialFile(new File("config/nerionsdk/launcher-api-credential.txt"))
    .onConnected(() -> System.out.println("Connected to launcher API"))
    .build();

NerionP2PClient client = new NerionP2PClient(config);
client.connect(); // non-blocking, polls until the launcher writes the credential file
```

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
