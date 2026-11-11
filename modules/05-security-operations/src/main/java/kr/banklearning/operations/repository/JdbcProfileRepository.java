package kr.banklearning.operations.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import kr.banklearning.operations.domain.AuditEvent;
import kr.banklearning.operations.domain.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcProfileRepository implements ProfileRepository {
    private final JdbcTemplate jdbc;

    public JdbcProfileRepository(DataSource dataSource) {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.setQueryTimeout(3);
    }

    @Override
    public Optional<Profile> findProfile(String username) {
        return jdbc
                .query(
                        "SELECT username, display_name, email, account_hint, note FROM phase5.profile WHERE username = ?",
                        (result, row) ->
                                new Profile(
                                        result.getString("username"),
                                        result.getString("display_name"),
                                        result.getString("email"),
                                        result.getString("account_hint"),
                                        result.getString("note")),
                        username)
                .stream()
                .findFirst();
    }

    @Override
    public int updateNote(String username, String note) {
        return jdbc.update("UPDATE phase5.profile SET note = ? WHERE username = ?", note, username);
    }

    @Override
    public void appendAudit(AuditEvent event) {
        jdbc.update(
                "INSERT INTO phase5.audit_event (id, actor, action, outcome, request_id, occurred_at) VALUES (?, ?, ?, ?, ?, ?)",
                event.id(),
                event.actor(),
                event.action(),
                event.outcome(),
                event.requestId(),
                Timestamp.from(event.occurredAt()));
    }

    @Override
    public List<AuditEvent> recentAudit() {
        return List.copyOf(
                jdbc.query(
                        "SELECT id, actor, action, outcome, request_id, occurred_at FROM phase5.audit_event ORDER BY occurred_at DESC, id DESC LIMIT 50",
                        (result, row) ->
                                new AuditEvent(
                                        result.getObject("id", UUID.class),
                                        result.getString("actor"),
                                        result.getString("action"),
                                        result.getString("outcome"),
                                        result.getString("request_id"),
                                        result.getTimestamp("occurred_at").toInstant())));
    }

    @Override
    public boolean ready() {
        try {
            return Boolean.TRUE.equals(
                    jdbc.queryForObject(
                            "SELECT (SELECT count(*) FROM phase5.profile) >= 0 AND (SELECT count(*) FROM phase5.audit_event) >= 0",
                            Boolean.class));
        } catch (org.springframework.dao.DataAccessException exception) {
            return false;
        }
    }
}
