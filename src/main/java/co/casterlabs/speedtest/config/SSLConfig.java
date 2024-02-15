package co.casterlabs.speedtest.config;

import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rhs.session.TLSVersion;

@JsonClass(exposeAll = true)
public class SSLConfig {
    public boolean enabled = false;

    public TLSVersion[] tls = {
            TLSVersion.TLSv1_2,
            TLSVersion.TLSv1_3
    };

    public String[] enabledCipherSuites = {
            "TLS_ECDHE_PSK_WITH_AES_128_CCM_SHA256",
            "TLS_ECDHE_PSK_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_PSK_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_PSK_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_DHE_PSK_WITH_AES_256_CCM",
            "TLS_DHE_PSK_WITH_AES_128_CCM",
            "TLS_DHE_RSA_WITH_AES_256_CCM",
            "TLS_DHE_RSA_WITH_AES_128_CCM",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_AES_128_CCM_SHA256",
            "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_AES_128_GCM_SHA256",
            "TLS_DHE_PSK_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_PSK_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"
    }; // Null = All Available

    public int dhSize = 2048;

    public String certificateFile = "ssl/crt.pem";
    public String privateKeyFile = "ssl/key.pem";
    public String trustChainFile = "ssl/chain.pem";

}
