package me.feeldev.nerionnetwork.models;

import me.feeldev.nerionnetwork.internal.Json;
import me.feeldev.nerionnetwork.internal.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Response from {@code GET /v1/social/requests} or {@code GET /v1/social/requests/sent} (doc
 * §8.16) — both endpoints wrap their list under the same {@code "requests"} key with the same
 * {@link FriendRequest} element shape, so one response type covers both.
 */
public final class FriendRequestsResponse {

    private final List<FriendRequest> requests;

    public FriendRequestsResponse(List<FriendRequest> requests) {
        this.requests = requests;
    }

    public List<FriendRequest> getRequests() {
        return requests;
    }

    public static FriendRequestsResponse fromJson(String json) {
        List<FriendRequest> requests = new ArrayList<>();
        for (JsonObject request : Json.parseObject(json).getObjectList("requests")) {
            requests.add(FriendRequest.fromJson(request));
        }
        return new FriendRequestsResponse(requests);
    }
}
