package com.daniel.sonemessagingservice.message.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import com.daniel.sonemessagingservice.message.application.exception.DailyMessageLimitExceededException;
import com.daniel.sonemessagingservice.message.domain.repository.MessageRepository;

/** Per-conversation daily limit (UTC day). Every Message counts as one, regardless of its type. */
final class DailyMessageLimit {

    static final int LIMIT = 200;

    private DailyMessageLimit() {
    }

    static void ensureNotReached(MessageRepository repository, UUID conversationId, Instant now) {
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        Instant dayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant nextDayStart = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        if (repository.countByConversationIdCreatedBetween(conversationId, dayStart, nextDayStart) >= LIMIT) {
            throw new DailyMessageLimitExceededException();
        }
    }
}
