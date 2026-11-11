package kr.banklearning.operations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kr.banklearning.operations.domain.AuditEvent;
import kr.banklearning.operations.domain.Profile;
import kr.banklearning.operations.repository.ProfileRepository;

public final class MemoryProfileRepository implements ProfileRepository {
    public final Map<String, Profile> profiles = new LinkedHashMap<>();
    public final List<AuditEvent> events = new ArrayList<>();
    public boolean fail, auditFail, ready = true;

    public Optional<Profile> findProfile(String username) {
        if (fail) throw failure();
        return Optional.ofNullable(profiles.get(username));
    }

    public int updateNote(String username, String note) {
        if (fail) throw failure();
        Profile current = profiles.get(username);
        if (current == null) return 0;
        profiles.put(
                username,
                new Profile(
                        current.username(),
                        current.displayName(),
                        current.email(),
                        current.accountHint(),
                        note));
        return 1;
    }

    public void appendAudit(AuditEvent event) {
        if (auditFail) throw failure();
        events.add(event);
    }

    public List<AuditEvent> recentAudit() {
        if (fail) throw failure();
        return List.copyOf(events);
    }

    public boolean ready() {
        return ready;
    }

    private static org.springframework.dao.DataAccessException failure() {
        return new org.springframework.dao.DataAccessResourceFailureException(
                "jdbc:secret username=password internal details");
    }
}
