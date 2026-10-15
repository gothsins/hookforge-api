package io.github.gothsins.hookforge.delivery;

final class UnsafeWebhookTarget extends RuntimeException {
    private final DeliveryErrorCategory category;

    UnsafeWebhookTarget(DeliveryErrorCategory category) {
        this.category = category;
    }

    DeliveryErrorCategory category() {
        return category;
    }
}
