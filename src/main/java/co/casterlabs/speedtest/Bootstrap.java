package co.casterlabs.speedtest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import co.casterlabs.speedtest.config.Config;
import co.casterlabs.speedtest.config.FileWatcher;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class Bootstrap {
    private static final File CONFIG_FILE = new File("config.json");

    public static Config config = new Config();
    public static Daemon daemon;
    public static Heartbeat heartbeat;

    public static void main(String[] args) throws IOException {
        System.setProperty("fastloggingframework.wrapsystem", "true");
        FastLoggingFramework.setColorEnabled(false);

        reload();

        try {
            // Defaults...
            Files.writeString(CONFIG_FILE.toPath(), Rson.DEFAULT.toJson(config).toString(true));
        } catch (IOException ignored) {}

        new FileWatcher(CONFIG_FILE) {
            @Override
            public void onChange() {
                try {
                    reload();
                    FastLogger.logStatic("Reloaded config!");
                } catch (Throwable t) {
                    FastLogger.logStatic(LogLevel.SEVERE, "Unable to reload config file:\n%s", t);
                }
            }
        }.start();
    }

    private static void reload() throws IOException {
        if (!CONFIG_FILE.exists()) {
            FastLogger.logStatic("Config file doesn't exist, creating a new file. Modify it and restart ST.");
            Files.writeString(CONFIG_FILE.toPath(), Rson.DEFAULT.toJson(new Config()).toString(true));
            System.exit(1);
        }

        Config config;

        try {
            config = Rson.DEFAULT.fromJson(Files.readString(CONFIG_FILE.toPath()), Config.class);
        } catch (JsonParseException e) {
            FastLogger.logStatic(LogLevel.SEVERE, "Unable to parse config file, is it malformed?\n%s", e);
            return;
        }

//        boolean isNew = ST.config == null;
        Bootstrap.config = config;

        // Reconfigure heartbeats.
        if (heartbeat != null) {
            heartbeat.close();
            heartbeat = null;
        }

        if (config.heartbeatUrl != null && config.heartbeatIntervalSeconds > 0) {
            heartbeat = new Heartbeat();
            heartbeat.start();
        }

        // Start the daemon if necessary.
        if (daemon == null) {
            try {
                daemon = new Daemon(config.port, config.isBehindProxy, config.ssl);
                daemon.open();
            } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | IOException e) {
                throw new IOException("Unable to start server:", e);
            }
        } else {
            if (config.port != config.port) {
                FastLogger.logStatic(
                    LogLevel.WARNING,
                    "ST does not support changing the HTTP server port while running. You will need to fully restart for this to take effect."
                );
            }
        }

        // Logging
        FastLoggingFramework.setDefaultLevel(config.debug ? LogLevel.DEBUG : LogLevel.INFO);
        daemon.server.getLogger().setCurrentLevel(FastLoggingFramework.getDefaultLevel());
    }

}
