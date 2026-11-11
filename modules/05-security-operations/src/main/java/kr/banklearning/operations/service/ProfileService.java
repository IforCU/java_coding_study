package kr.banklearning.operations.service;

import java.util.List;
import kr.banklearning.operations.domain.AuditEvent;
import kr.banklearning.operations.domain.Profile;
import kr.banklearning.operations.domain.ProfileView;
import kr.banklearning.operations.repository.ProfileRepository;
import kr.banklearning.operations.util.PrivacyMasker;
import org.springframework.transaction.annotation.Transactional;

public class ProfileService {
    private final ProfileRepository repository;
    private final AuditService audit;

    public ProfileService(ProfileRepository repository, AuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    public ProfileView ownProfile(String username, String requestId) {
        Profile profile =
                repository
                        .findProfile(username)
                        .orElseThrow(() -> new BankProblem(404, "PROFILE_NOT_FOUND"));
        audit.record(username, "PROFILE_READ", "ALLOWED", requestId);
        return new ProfileView(
                profile.username(),
                profile.displayName(),
                PrivacyMasker.email(profile.email()),
                PrivacyMasker.account(profile.accountHint()),
                profile.note());
    }

    @Transactional
    public void updateNote(String username, String note, String requestId) {
        if (note == null || note.isBlank() || note.strip().length() > 100)
            throw new BankProblem(400, "INVALID_NOTE");
        if (repository.updateNote(username, note.strip()) != 1)
            throw new BankProblem(404, "PROFILE_NOT_FOUND");
        audit.record(username, "NOTE_UPDATE", "ALLOWED", requestId);
    }

    public List<AuditEvent> audit(String username, String requestId) {
        audit.record(username, "AUDIT_READ", "ALLOWED", requestId);
        return List.copyOf(repository.recentAudit());
    }

    public boolean ready() {
        return repository.ready();
    }
}
