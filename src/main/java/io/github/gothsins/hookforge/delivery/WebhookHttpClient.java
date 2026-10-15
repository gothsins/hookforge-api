package io.github.gothsins.hookforge.delivery;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.io.ManagedHttpClientConnectionFactory;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WebhookHttpClient implements AutoCloseable {
    private final CloseableHttpClient client;
    private final ScheduledExecutorService deadlines = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "webhook-deadline");
        thread.setDaemon(true);
        return thread;
    });

    public WebhookHttpClient() {
        var connections = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(SafeWebhookUrl.publicDnsResolver())
                .setConnectionFactory(ManagedHttpClientConnectionFactory.builder()
                        .http1Config(Http1Config.custom().setMaxHeaderCount(64)
                                .setMaxLineLength(8192).build()).build())
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(3))
                        .setSocketTimeout(Timeout.ofSeconds(5)).build())
                .build();
        client = HttpClients.custom()
                .setConnectionManager(connections)
                .setRoutePlanner(new DefaultRoutePlanner(null))
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(1))
                        .setResponseTimeout(Timeout.ofSeconds(5)).build())
                .disableRedirectHandling()
                .disableAutomaticRetries()
                .disableCookieManagement()
                .build();
    }

    public int post(String url, byte[] json) throws IOException {
        URI uri = SafeWebhookUrl.parse(url);
        if (json.length > 64 * 1024) {
            throw new UnsafeWebhookTarget(DeliveryErrorCategory.PAYLOAD_TOO_LARGE);
        }
        HttpPost request = new HttpPost(uri);
        request.setHeader("Accept", "application/json");
        request.setHeader("Connection", "close");
        request.setEntity(new ByteArrayEntity(json, ContentType.APPLICATION_JSON));
        AtomicBoolean expired = new AtomicBoolean();
        var deadline = deadlines.schedule(() -> {
            expired.set(true);
            request.cancel();
        }, 10, TimeUnit.SECONDS);
        try {
            ClassicHttpResponse response = client.executeOpen(null, request, null);
            try {
                // The response body is never read or stored. Immediate close prevents unbounded downloads.
                if (expired.get()) {
                    throw new SocketTimeoutException("Webhook deadline exceeded");
                }
                return response.getCode();
            } finally {
                CloseableHttpResponse.adapt(response).close(CloseMode.IMMEDIATE);
            }
        } catch (IOException exception) {
            if (expired.get()) {
                throw new SocketTimeoutException("Webhook deadline exceeded");
            }
            throw exception;
        } finally {
            deadline.cancel(false);
        }
    }

    @Override
    public void close() throws IOException {
        deadlines.shutdownNow();
        client.close();
    }
}
