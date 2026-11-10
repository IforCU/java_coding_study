package kr.banklearning.operations.service;

import java.time.Clock;
import java.util.UUID;
import kr.banklearning.operations.domain.AuditEvent;
import kr.banklearning.operations.repository.ProfileRepository;
import kr.banklearning.operations.util.PrivacyMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class AuditService {
    private static final Logger LOG = LoggerFactory.getLogger(AuditService.class);
    private final ProfileRepository repository;
    private final Clock clock;
    private final TransactionTemplate transaction;
    public AuditService(ProfileRepository repository, Clock clock) { this(repository, clock, null); }
    public AuditService(ProfileRepository repository, Clock clock, PlatformTransactionManager manager) {
        this.repository = repository; this.clock = clock;
        transaction = manager == null ? null : new TransactionTemplate(manager);
        if (transaction != null) { transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); transaction.setTimeout(3); }
    }
    public void record(String actor, String action, String outcome, String requestId) {
        String trace = requestId != null && requestId.matches("[A-Za-z0-9-]{1,64}") ? requestId : UUID.randomUUID().toString();
        AuditEvent event = new AuditEvent(UUID.randomUUID(), PrivacyMasker.username(actor), action, outcome, trace, clock.instant());
        try {
            if (transaction == null) repository.appendAudit(event);
            else transaction.executeWithoutResult(status -> repository.appendAudit(event));
        } catch (RuntimeException exception) {
            LOG.warn("audit_write_failed requestId={}", trace);
        }
    }
}
