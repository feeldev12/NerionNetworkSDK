package me.feeldev.nerionp2p;

import java.io.File;

/**
 * Configuration for {@link NerionP2PClient}. Use the builder.
 *
 * <pre>
 * NerionP2PConfig config = NerionP2PConfig.builder()
 *     .credentialFile(new File("launcher-api-credential.txt"))
 *     .onCredentials((url, token) -&gt; { ... })
 *     .build();
 * </pre>
 */
public final class NerionP2PConfig {

    private final File credentialFile;
    private final long pollIntervalMs;
    private final Runnable onConnected;
    private final Runnable onDisconnected;

    private NerionP2PConfig(Builder b) {
        this.credentialFile = b.credentialFile;
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

    public static final class Builder {

        private File credentialFile = new File("launcher-api-credential.txt");
        private long pollIntervalMs = 5_000;
        private Runnable onConnected;
        private Runnable onDisconnected;

        private Builder() {}

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

        public NerionP2PConfig build() {
            if (credentialFile == null) {
                throw new IllegalArgumentException("credentialFile is required");
            }
            return new NerionP2PConfig(this);
        }
    }
}
