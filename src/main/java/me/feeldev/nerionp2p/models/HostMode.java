package me.feeldev.nerionp2p.models;

/**
 * Hosting mode for {@code POST /v1/p2p/host} (doc §8.7).
 * <p>
 * {@code STANDBY} (default) — a long-lived loop that accepts joiners one after another over the
 * whole hosting session; use this for "open to LAN, friends drop in over time".
 * <p>
 * {@code SINGLE} — one hosting attempt with automatic relay fallback if P2P fails; does not
 * accept a second joiner the same way.
 * <p>
 * Prefer {@code STANDBY} unless you have a specific reason not to.
 */
public enum HostMode {
    STANDBY, SINGLE
}
