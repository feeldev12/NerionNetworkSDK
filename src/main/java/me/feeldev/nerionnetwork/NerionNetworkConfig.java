package me.feeldev.nerionnetwork;

import java.io.File;

/**
 * Configuration for {@link NerionNetworkClient}. Everything is optional: {@code new NerionNetworkClient()}
 * uses the defaults.
 *
 * <pre>
 * NerionNetworkConfig config = NerionNetworkConfig.builder()
 *     .onConnected(() -&gt; log("Connected to the Nerion launcher"))
 *     .build();
 * </pre>
 */
public final class NerionNetworkConfig {

    private static final String CREDENTIAL_FILE_NAME = "launcher-api-credential.txt";

    private final File credentialFile;
    private final long pollIntervalMs;
    private final Runnable onConnected;
    private final Runnable onDisconnected;

    private NerionNetworkConfig(Builder b) {
        this.credentialFile = b.credentialFile != null ? b.credentialFile : defaultCredentialFile(new File("."));
        this.pollIntervalMs = b.pollIntervalMs;
        this.onConnected = b.onConnected;
        this.onDisconnected = b.onDisconnected;
    }

    public File getCredentialFile() { return credentialFile; }
    public long getPollIntervalMs() { return pollIntervalMs; }
    public Runnable getOnConnected() { return onConnected; }
    public Runnable getOnDisconnected() { return onDisconnected; }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Where the launcher writes its credentials for a game or server running in {@code gameDir}:
     * {@code plugins/NerionSDK/} when that folder has a {@code plugins/} folder (Bukkit/Paper),
     * {@code config/nerionsdk/} otherwise (Fabric, Forge, NeoForge). The launcher picks it the same way.
     */
    public static File defaultCredentialFile(File gameDir) {
        String folder = new File(gameDir, "plugins").isDirectory() ? "plugins/NerionSDK" : "config/nerionsdk";
        return new File(new File(gameDir, folder), CREDENTIAL_FILE_NAME);
    }

    public static final class Builder {

        private File credentialFile;
        private long pollIntervalMs = 5_000;
        private Runnable onConnected;
        private Runnable onDisconnected;

        private Builder() {}

        /**
         * Only needed when your code doesn't run from the game's own folder. By default it's
         * {@link #defaultCredentialFile(File)} of the working directory, where Minecraft and
         * Bukkit/Paper servers run.
         */
        public Builder credentialFile(File file) {
            this.credentialFile = file;
            return this;
        }

        public Builder pollIntervalMs(long ms) {
            this.pollIntervalMs = ms;
            return this;
        }

        public Builder onConnected(Runnable cb) {
            this.onConnected = cb;
            return this;
        }

        public Builder onDisconnected(Runnable cb) {
            this.onDisconnected = cb;
            return this;
        }

        public NerionNetworkConfig build() {
            return new NerionNetworkConfig(this);
        }
    }
}
