package me.feeldev.nerionnetwork.models;

/**
 * Who can find and join a session (doc §8.3).
 * <ul>
 *   <li>{@code PUBLIC} — listed for everyone; anyone signed in to Nerion can join.</li>
 *   <li>{@code UNLISTED} — never listed; anyone who has the tunnel ID can join. Made for rooms a
 *       mod or server hands out itself (a minigame lobby passing the tunnel ID to its players).</li>
 *   <li>{@code FRIENDS_ONLY} (default) — the host's friends, or anyone with the invite code.</li>
 *   <li>{@code PRIVATE} — not shown to friends either; only with the invite code.</li>
 * </ul>
 */
public enum Visibility {
    PUBLIC, UNLISTED, FRIENDS_ONLY, PRIVATE
}
