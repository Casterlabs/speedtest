package co.casterlabs.speedtest;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rhs.protocol.HttpStatus;
import co.casterlabs.rhs.protocol.StandardHttpStatus;
import co.casterlabs.rhs.server.HttpListener;
import co.casterlabs.rhs.server.HttpResponse;
import co.casterlabs.rhs.server.HttpServer;
import co.casterlabs.rhs.server.HttpServerBuilder;
import co.casterlabs.rhs.server.SSLUtil;
import co.casterlabs.rhs.session.HttpSession;
import co.casterlabs.rhs.session.TLSVersion;
import co.casterlabs.rhs.session.WebsocketListener;
import co.casterlabs.rhs.session.WebsocketSession;
import co.casterlabs.rhs.util.DropConnectionException;
import co.casterlabs.speedtest.config.SSLConfig;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.pem.util.PemUtils;

public class Daemon implements Closeable, HttpListener {
    private static final long TIME_LIMIT = TimeUnit.SECONDS.toMillis(25);
    private static final long ACTUAL_TIME_LIMIT = TIME_LIMIT + TimeUnit.SECONDS.toMillis(5); // Some leeway.

    public final HttpServer server;

    public Daemon(int port, boolean isBehindProxy, @Nullable SSLConfig ssl) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, UnrecoverableKeyException {
        HttpServerBuilder builder = new HttpServerBuilder()
            .withBehindProxy(isBehindProxy)
            .withPort(port);

        if (ssl != null && ssl.enabled) {
            X509ExtendedKeyManager keyManager = PemUtils.loadIdentityMaterial(Paths.get(ssl.trustChainFile), Paths.get(ssl.privateKeyFile));
            X509ExtendedTrustManager trustManager = PemUtils.loadTrustMaterial(Paths.get(ssl.certificateFile));

            SSLFactory factory = SSLFactory.builder()
                .withIdentityMaterial(keyManager)
                .withTrustMaterial(trustManager)
                .withCiphers(ssl.enabledCipherSuites) // Unsupported ciphers are automatically excluded.
                .withProtocols(TLSVersion.toRuntimeNames(ssl.tls))
                .build();
            SSLUtil.applyDHSize(ssl.dhSize);

            this.server = builder
                .withSsl(factory)
                .buildSecure(this);
        } else {

            this.server = builder.build(this);
        }
    }

    @Override
    public @Nullable HttpResponse serveHttpSession(HttpSession session) {
        try {
            String origin = session.getHeader("Origin");
            if (origin == null) origin = "*";

            return (switch (session.getMethod()) {
                case GET -> {
                    switch (session.getUri()) {
                        case "/":
                            yield HttpResponse.newFixedLengthResponse(
                                StandardHttpStatus.OK,
                                "<!DOCTYPE html>"
                                    + "<html>"
                                    + "Hello world!"
                                    + "<html>"
                            )
                                .setMimeType("text/html");

                        case "/test/service-data":
                            yield HttpResponse.newFixedLengthResponse(
                                StandardHttpStatus.OK,
                                new JsonObject()
                                    .put(
                                        "data",
                                        new JsonObject()
                                            .put("time_limit", TIME_LIMIT)
                                    )
                                    .putNull("error")
                            );

                        case "/test/ping":
                            yield HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK);

                        default:
                            yield HttpResponse.newFixedLengthResponse(StandardHttpStatus.NOT_FOUND);
                    }
                }

                case PATCH -> {
                    switch (session.getUri()) {
                        case "/test/download": {
                            yield HttpResponse.newFixedLengthResponse(
                                StandardHttpStatus.CREATED,
                                new InputStream() {
                                    private int remaining = Integer.MAX_VALUE;
                                    private long startedAt = System.currentTimeMillis();

                                    @Override
                                    public int read() throws IOException {
                                        if (System.currentTimeMillis() - this.startedAt > ACTUAL_TIME_LIMIT) {
                                            throw new DropConnectionException();
                                        }

                                        if (this.remaining == 0) return -1;

                                        this.remaining -= 1;
                                        return 0;
                                    }

                                    @Override
                                    public int read(byte[] b, int off, int len) throws IOException {
                                        if (System.currentTimeMillis() - this.startedAt > ACTUAL_TIME_LIMIT) {
                                            throw new DropConnectionException();
                                        }

                                        if (this.remaining == 0) return -1;

                                        len = Math.min(len, this.remaining);
                                        this.remaining -= len;
                                        return len;
                                    }
                                },
                                Integer.MAX_VALUE
                            );
                        }

                        case "/test/upload": {
                            long startedAt = System.currentTimeMillis();
                            while (session.getRequestBodyStream().skip(1100) != -1) {
                                if (System.currentTimeMillis() - startedAt > ACTUAL_TIME_LIMIT) {
                                    // Over the limit.
                                    yield errorResponse(
                                        StandardHttpStatus.BAD_REQUEST, "TOO_LONG",
                                        "You've been testing for too long."
                                    );
                                }
                            }

                            yield HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK);
                        }

                        default:
                            yield HttpResponse.newFixedLengthResponse(StandardHttpStatus.NOT_FOUND);
                    }
                }

                case OPTIONS -> HttpResponse.newFixedLengthResponse(StandardHttpStatus.NO_CONTENT);

                default -> errorResponse(
                    StandardHttpStatus.BAD_REQUEST, "NOT_IMPLEMENTED",
                    "Invalid HTTP method."
                );
            })
                .putHeader("Access-Control-Allow-Origin", origin)
                .putHeader("Access-Control-Allow-Methods", "OPTIONS, GET, PATCH")
                .putHeader("Access-Control-Allow-Headers", "Content-Type")
                .putHeader("Access-Control-Max-Age", "86400");
        } catch (Exception e) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public @Nullable WebsocketListener serveWebsocketSession(WebsocketSession session) {
        throw new DropConnectionException();
    }

    public void open() throws IOException {
        this.server.start();
    }

    @Override
    public void close() throws IOException {
        this.server.stop();
    }

    private static HttpResponse errorResponse(HttpStatus status, String code, String message) {
        return HttpResponse.newFixedLengthResponse(
            status,
            new JsonObject()
                .putNull("data")
                .put(
                    "error",
                    new JsonObject()
                        .put("code", code)
                        .put("message", message)
                )
        );
    }

}
