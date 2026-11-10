package kr.banklearning.operations.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(UUID id, String actor, String action, String outcome, String requestId, Instant occurredAt) {
    public UUID getId() { return id; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getOutcome() { return outcome; }
    public String getRequestId() { return requestId; }
    public Instant getOccurredAt() { return occurredAt; }
}
