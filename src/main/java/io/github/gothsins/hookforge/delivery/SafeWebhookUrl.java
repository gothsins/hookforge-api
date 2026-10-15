package io.github.gothsins.hookforge.delivery;

import org.apache.hc.client5.http.DnsResolver;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;

final class SafeWebhookUrl {
    private SafeWebhookUrl() {}

    static URI parse(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new UnsafeWebhookTarget(DeliveryErrorCategory.INVALID_URL);
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                || uri.getRawAuthority() == null || value.indexOf('\\') >= 0
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new UnsafeWebhookTarget(DeliveryErrorCategory.INVALID_URL);
        }
        int port = uri.getPort();
        if (port != -1 && port != 80 && port != 443) {
            throw new UnsafeWebhookTarget(DeliveryErrorCategory.INVALID_URL);
        }
        return uri;
    }

    static DnsResolver publicDnsResolver() {
        return new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                InetAddress[] allowed = Arrays.stream(addresses).filter(SafeWebhookUrl::isPublicIpv4)
                        .toArray(InetAddress[]::new);
                if (allowed.length != addresses.length || allowed.length == 0) {
                    throw new BlockedWebhookAddress();
                }
                return allowed;
            }

            @Override
            public String resolveCanonicalHostname(String host) {
                return host;
            }
        };
    }

    static boolean isPublicIpv4(InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            return false; // IPv6 requires a separate global-address policy before enabling it.
        }
        byte[] bytes = address.getAddress();
        int a = Byte.toUnsignedInt(bytes[0]);
        int b = Byte.toUnsignedInt(bytes[1]);
        int c = Byte.toUnsignedInt(bytes[2]);
        if (a == 0 || a == 10 || a == 127 || a >= 224 ||
                (a == 100 && b >= 64 && b <= 127) ||
                (a == 169 && b == 254) ||
                (a == 172 && b >= 16 && b <= 31) ||
                (a == 192 && (b == 168 || (b == 0 && c == 0) || (b == 0 && c == 2))) ||
                (a == 198 && ((b == 18 || b == 19) || (b == 51 && c == 100))) ||
                (a == 203 && b == 0 && c == 113)) {
            return false;
        }
        return !address.isAnyLocalAddress() && !address.isLoopbackAddress()
                && !address.isLinkLocalAddress() && !address.isSiteLocalAddress()
                && !address.isMulticastAddress();
    }
}
