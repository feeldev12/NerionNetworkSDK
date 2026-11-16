package me.feeldev.nerionp2p.models;

import me.feeldev.nerionp2p.internal.Json;
import me.feeldev.nerionp2p.internal.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Response from {@code GET /v1/social/friends} (doc §8.14) — the caller's full friends list. */
public final class FriendsResponse {

    private final List<Friend> friends;

    public FriendsResponse(List<Friend> friends) {
        this.friends = friends;
    }

    public List<Friend> getFriends() {
        return friends;
    }

    public static FriendsResponse fromJson(String json) {
        List<Friend> friends = new ArrayList<>();
        for (JsonObject friend : Json.parseObject(json).getObjectList("friends")) {
            friends.add(Friend.fromJson(friend));
        }
        return new FriendsResponse(friends);
    }
}
