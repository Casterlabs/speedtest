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
import java.util.Random;

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
    private static final Random RANDOM = new Random();
    private static final int LIMIT = 200/*mb*/ * 1000 * 1000;

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
                                            .put("max", LIMIT)
                                            .put("recommendedDownload", LIMIT / 2) // 100mb
                                            .put("recommendedUpload", LIMIT / 40) // 5mb
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
                            int amount = Integer.parseInt(session.getQueryParameters().getOrDefault("size", Integer.toString(LIMIT)));

                            if (amount > LIMIT) {
                                yield errorResponse(
                                    StandardHttpStatus.BAD_REQUEST, "TOO_LARGE",
                                    "POST data is too large. Use GET to get (hah) the max download size."
                                );
                            }

                            yield HttpResponse.newFixedLengthResponse(
                                StandardHttpStatus.CREATED,
                                new InputStream() {
                                    private int remaining = amount;

                                    @Override
                                    public int read() throws IOException {
                                        if (this.remaining == 0) return -1;
                                        this.remaining -= 1;
                                        return RANDOM.nextInt();
                                    }

                                    @Override
                                    public int read(byte[] b, int off, int len) throws IOException {
                                        if (this.remaining == 0) return -1;
                                        len = Math.min(len, this.remaining);
                                        this.remaining -= len;
                                        RANDOM.nextBytes(b);
                                        return len;
                                    }
                                },
                                amount
                            );
                        }

                        case "/test/upload": {
                            long total = 0;
                            long read;
                            while ((read = session.getRequestBodyStream().skip(2048)) != -1) {
                                total += read;
                                if (total > LIMIT) {
                                    // Over the limit.
                                    yield errorResponse(
                                        StandardHttpStatus.BAD_REQUEST, "TOO_LARGE",
                                        "PUT data is too large. Use GET to get (hah) the max upload size."
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
