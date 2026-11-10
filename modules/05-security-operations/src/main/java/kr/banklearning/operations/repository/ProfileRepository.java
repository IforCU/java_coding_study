package kr.banklearning.operations.repository;

import java.util.List;
import java.util.Optional;
import kr.banklearning.operations.domain.AuditEvent;
import kr.banklearning.operations.domain.Profile;

public interface ProfileRepository {
    Optional<Profile> findProfile(String username);
    int updateNote(String username, String note);
    void appendAudit(AuditEvent event);
    List<AuditEvent> recentAudit();
    boolean ready();
}
