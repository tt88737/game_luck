package com.gameluck.payment.service.reconciliation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class ReconciliationPlatformEventPager {

    private ReconciliationPlatformEventPager() {
    }

    public static void forEachPage(PageFetcher fetcher, int limit,
                                   Consumer<List<ReconciliationPlatformEventProjection>> consumer) {
        Objects.requireNonNull(fetcher, "fetcher");
        Objects.requireNonNull(consumer, "consumer");
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        Instant cursorTime = null;
        Long cursorId = null;
        while (true) {
            List<ReconciliationPlatformEventProjection> page = List.copyOf(
                Objects.requireNonNull(fetcher.fetch(cursorTime, cursorId, limit), "page"));
            if (page.isEmpty()) return;
            if (page.size() > limit) throw new IllegalStateException("Platform event page exceeds requested limit");
            for (ReconciliationPlatformEventProjection event : page) {
                if (cursorTime != null && compare(event, cursorTime, cursorId) <= 0) {
                    throw new IllegalStateException("Platform event pages must be strictly ordered by receivedTime,id");
                }
                cursorTime = event.receivedTime();
                cursorId = event.id();
            }
            consumer.accept(page);
        }
    }

    private static int compare(ReconciliationPlatformEventProjection event, Instant cursorTime, Long cursorId) {
        int timeComparison = event.receivedTime().compareTo(cursorTime);
        return timeComparison != 0 ? timeComparison : event.id().compareTo(cursorId);
    }

    @FunctionalInterface
    public interface PageFetcher {
        List<ReconciliationPlatformEventProjection> fetch(Instant cursorReceivedTime, Long cursorId, int limit);
    }
}
