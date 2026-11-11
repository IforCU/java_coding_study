package kr.banklearning.operations.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import kr.banklearning.operations.MemoryProfileRepository;
import kr.banklearning.operations.domain.Profile;
import kr.banklearning.operations.util.PrivacyMasker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProfileServiceTest {
    private MemoryProfileRepository repository;
    private ProfileService service;

    @BeforeEach
    void setUp() {
        repository = new MemoryProfileRepository();
        repository.profiles.put(
                "learner",
                new Profile("learner", "학습사용자", "learner@example.test", "123456789012", "first"));
        repository.profiles.put(
                "admin",
                new Profile("admin", "학습관리자", "admin@example.test", "111122223333", "admin"));
        service =
                new ProfileService(
                        repository,
                        new AuditService(
                                repository,
                                Clock.fixed(
                                        Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC)));
    }

    @Test
    void profileMasksContactAndAccountAndAuditActor() {
        var profile = service.ownProfile("learner", "trace-1");
        assertEquals("l***@example.test", profile.email());
        assertEquals("****9012", profile.accountHint());
        var event = repository.events.get(0);
        assertEquals("l***", event.actor());
        assertEquals("trace-1", event.requestId());
        assertEquals("PROFILE_READ", event.action());
        assertEquals("ALLOWED", event.outcome());
    }

    @Test
    void noteUpdateUsesOnlyCurrentUsernameAndKeepsOtherProfile() {
        service.updateNote("learner", "  새 메모  ", "trace-2");
        assertEquals("새 메모", repository.profiles.get("learner").note());
        assertEquals("admin", repository.profiles.get("admin").note());
        assertEquals("NOTE_UPDATE", repository.events.get(0).action());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\n\t"})
    void invalidNoteNeverWrites(String note) {
        assertEquals(
                400,
                assertThrows(
                                BankProblem.class,
                                () -> service.updateNote("learner", note, "trace-3"))
                        .status());
        assertEquals("first", repository.profiles.get("learner").note());
        assertTrue(repository.events.isEmpty());
    }

    @Test
    void noteLengthBoundaryAndUnknownProfileAreTyped() {
        service.updateNote("learner", "가".repeat(100), "trace-4");
        assertThrows(
                BankProblem.class, () -> service.updateNote("learner", "가".repeat(101), "trace-5"));
        assertEquals(
                404,
                assertThrows(
                                BankProblem.class,
                                () -> service.updateNote("missing", "note", "trace-6"))
                        .status());
    }

    @Test
    void auditUnavailableDoesNotExposeFailureOrPreventPermittedProfileRead() {
        repository.auditFail = true;
        assertNotNull(service.ownProfile("learner", "trace-7"));
        assertDoesNotThrow(() -> service.updateNote("learner", "updated", "trace-8"));
        assertEquals("updated", repository.profiles.get("learner").note());
    }

    @Test
    void maskersDoNotRevealMalformedOrShortValues() {
        assertEquals("***", PrivacyMasker.email("bad-email"));
        assertEquals("****", PrivacyMasker.account("1234"));
        assertEquals("", PrivacyMasker.email(null));
        assertEquals("", PrivacyMasker.account(null));
        assertEquals("anonymous", PrivacyMasker.username(null));
    }
}
