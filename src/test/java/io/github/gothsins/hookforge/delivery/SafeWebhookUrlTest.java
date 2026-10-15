package io.github.gothsins.hookforge.delivery;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafeWebhookUrlTest {
    @Test
    void onlyPublicIpv4AddressesPass() throws Exception {
        for (String blocked : new String[]{"127.0.0.1", "10.0.0.1", "172.16.0.1", "192.168.1.1",
                "169.254.169.254", "100.64.0.1", "192.0.2.1", "198.18.0.1", "203.0.113.1", "::1"}) {
            assertThat(SafeWebhookUrl.isPublicIpv4(InetAddress.getByName(blocked))).as(blocked).isFalse();
        }
        assertThat(SafeWebhookUrl.isPublicIpv4(InetAddress.getByName("8.8.8.8"))).isTrue();
    }

    @Test
    void rejectsUnsafeUrlsAndEffectiveLoopbackResolution() {
        for (String blocked : new String[]{"file:///etc/passwd", "http://user:pass@example.com",
                "http://example.com:8080/hook", "http://example.com/#fragment", "http://example.com\\@localhost"}) {
            assertThatThrownBy(() -> SafeWebhookUrl.parse(blocked)).as(blocked)
                    .isInstanceOf(UnsafeWebhookTarget.class);
        }
        assertThatThrownBy(() -> SafeWebhookUrl.publicDnsResolver().resolve("localhost"))
                .isInstanceOf(BlockedWebhookAddress.class);
    }

    @Test
    void httpClientRejectsPrivateTargetAtConnectionTime() throws Exception {
        try (WebhookHttpClient client = new WebhookHttpClient()) {
            for (String blocked : new String[]{"http://127.0.0.1/", "http://169.254.169.254/", "http://[::1]/"}) {
                assertThatThrownBy(() -> client.post(blocked, "{}".getBytes())).as(blocked)
                        .isInstanceOf(BlockedWebhookAddress.class);
            }
        }
    }
}
