package me.feeldev.nerionp2p.internal;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

/**
 * Polls a launcher-api-credential.txt file on a fixed interval until it contains
 * a non-blank {@code nerion.launcherApiUrl} and {@code nerion.modToken}, then
 * hands them off to the given callback exactly once and stops itself.
 */
public final class CredentialFileWatcher {

    private final File credentialFile;
    private final long intervalMs;
    private final Logger logger;
    private final BiConsumer<String, String> onCredentialsFound;

    private Timer timer;

    public CredentialFileWatcher(File credentialFile, long intervalMs, Logger logger,
                                  BiConsumer<String, String> onCredentialsFound) {
        this.credentialFile = credentialFile;
        this.intervalMs = intervalMs;
        this.logger = logger;
        this.onCredentialsFound = onCredentialsFound;
    }

    public void start() {
        timer = new Timer("nerion-p2p-credential-watcher", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                poll();
            }
        }, intervalMs, intervalMs);
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void poll() {
        if (!credentialFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(credentialFile)) {
            Properties props = new Properties();
            props.load(reader);

            String url = props.getProperty("nerion.launcherApiUrl", "").trim();
            String token = props.getProperty("nerion.modToken", "").trim();

            if (!url.isEmpty() && !token.isEmpty()) {
                stop();
                onCredentialsFound.accept(url, token);
            }
        } catch (IOException e) {
            logger.warning("[NerionP2P] Failed to read credential file '" + credentialFile + "': " + e.getMessage());
        } catch (Exception e) {
            logger.warning("[NerionP2P] Unexpected error reading credential file: " + e.getMessage());
        }
    }
}
