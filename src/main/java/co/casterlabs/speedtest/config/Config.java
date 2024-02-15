package co.casterlabs.speedtest.config;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import lombok.ToString;

@ToString
@JsonClass(exposeAll = true)
public class Config {
    public boolean debug = false;
    public int port = 8080;
    public boolean isBehindProxy = false;
    public @Nullable SSLConfig ssl = new SSLConfig();

    public @Nullable String heartbeatUrl = null;
    public long heartbeatIntervalSeconds = 15;

}
