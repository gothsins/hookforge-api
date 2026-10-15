package io.github.gothsins.hookforge.delivery;

import java.net.UnknownHostException;

final class BlockedWebhookAddress extends UnknownHostException {
    BlockedWebhookAddress() {
        super("Destination address is not public");
    }
}
