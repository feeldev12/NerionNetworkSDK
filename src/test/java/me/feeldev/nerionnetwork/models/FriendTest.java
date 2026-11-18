package me.feeldev.nerionnetwork.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FriendTest {

    private static final String BASE_FIELDS = "\"friendshipId\":\"f-1\","
            + "\"playerAccountId\":\"acc-1\","
            + "\"username\":\"someone\","
            + "\"playerName\":\"Someone\","
            + "\"mcUsername\":\"SomeoneMc\","
            + "\"avatarUrl\":null,"
            + "\"primarySkinUrl\":null,"
            + "\"online\":true,"
            + "\"currentInstanceId\":\"instance-1\","
            + "\"currentInstanceName\":\"My Modpack\","
            + "\"lastActiveAt\":1700000000,"
            + "\"favorite\":false,"
            + "\"socialLinks\":{}";

    @Test
    void fromJson_parsesJoinTargetFields_whenPresent_andReportsJoinable() {
        String json = "{" + BASE_FIELDS
                + ",\"joinTargetSource\":\"TUNNEL\""
                + ",\"joinTargetId\":\"tunnel-42\""
                + ",\"joinTargetInstanceId\":\"instance-1\""
                + "}";

        Friend friend = Friend.fromJson(json);

        assertEquals("TUNNEL", friend.getJoinTargetSource());
        assertEquals("tunnel-42", friend.getJoinTargetId());
        assertEquals("instance-1", friend.getJoinTargetInstanceId());
        assertTrue(friend.isJoinable());
    }

    @Test
    void fromJson_marksNotJoinable_whenJoinTargetFieldsAreAbsent() {
        String json = "{" + BASE_FIELDS + "}";

        Friend friend = Friend.fromJson(json);

        assertNull(friend.getJoinTargetSource());
        assertNull(friend.getJoinTargetId());
        assertNull(friend.getJoinTargetInstanceId());
        assertFalse(friend.isJoinable());
    }

    @Test
    void fromJson_parsesRoomJoinTarget() {
        String json = "{" + BASE_FIELDS
                + ",\"joinTargetSource\":\"ROOM\""
                + ",\"joinTargetId\":\"room-7\""
                + ",\"joinTargetInstanceId\":\"instance-1\""
                + "}";

        Friend friend = Friend.fromJson(json);

        assertEquals("ROOM", friend.getJoinTargetSource());
        assertEquals("room-7", friend.getJoinTargetId());
        assertTrue(friend.isJoinable());
    }

    @Test
    void legacyThirteenArgConstructor_defaultsJoinTargetFieldsToNull_andIsNotJoinable() {
        Friend friend = new Friend("f-1", "acc-1", "someone", "Someone", "SomeoneMc",
                null, null, true, "instance-1", "My Modpack", 1700000000L, false, "{}");

        assertNull(friend.getJoinTargetSource());
        assertNull(friend.getJoinTargetId());
        assertNull(friend.getJoinTargetInstanceId());
        assertFalse(friend.isJoinable());
    }

    @Test
    void sixteenArgConstructor_setsJoinTargetFieldsDirectly() {
        Friend friend = new Friend("f-1", "acc-1", "someone", "Someone", "SomeoneMc",
                null, null, true, "instance-1", "My Modpack", 1700000000L, false, "{}",
                "ROOM", "room-7", "instance-1");

        assertEquals("ROOM", friend.getJoinTargetSource());
        assertEquals("room-7", friend.getJoinTargetId());
        assertEquals("instance-1", friend.getJoinTargetInstanceId());
        assertTrue(friend.isJoinable());
    }
}
