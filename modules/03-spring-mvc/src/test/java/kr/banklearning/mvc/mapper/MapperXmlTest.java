package kr.banklearning.mvc.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MapperXmlTest {
    private Configuration configuration;
    private static final String NAMESPACE = "kr.banklearning.mvc.mapper.BankMapper.";

    @BeforeEach
    void loadMapper() throws Exception {
        configuration = new Configuration();
        String resource = "kr/banklearning/mvc/mapper/BankMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments())
                    .parse();
        }
    }

    @Test
    void noDateFiltersEmitOnlyAccountBinding() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("accountId", "00000000-0000-0000-0000-000000000001");
        BoundSql sql =
                configuration.getMappedStatement(NAMESPACE + "history").getBoundSql(parameters);
        assertEquals(
                List.of("accountId"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
        assertFalse(sql.getSql().contains("occurred_at >="));
        assertFalse(sql.getSql().contains("occurred_at <"));
    }

    @Test
    void bothDateFiltersUseBoundPlaceholdersAndStableOrdering() {
        BoundSql sql =
                configuration
                        .getMappedStatement(NAMESPACE + "history")
                        .getBoundSql(
                                Map.of(
                                        "accountId",
                                        "untrusted-value",
                                        "fromInclusive",
                                        OffsetDateTime.parse("2026-10-01T00:00:00+09:00"),
                                        "toExclusive",
                                        OffsetDateTime.parse("2026-10-26T00:00:00+09:00")));
        assertEquals(
                List.of("accountId", "fromInclusive", "toExclusive"),
                sql.getParameterMappings().stream().map(mapping -> mapping.getProperty()).toList());
        String text = sql.getSql().replaceAll("\\s+", " ");
        assertTrue(text.contains("occurred_at >= ?"));
        assertTrue(text.contains("occurred_at < ?"));
        assertTrue(text.contains("ORDER BY occurred_at, id"));
        assertFalse(text.contains("untrusted-value"));
    }

    @Test
    void constructorMappingsPreserveAmountAndTimestampTypes() {
        var map = configuration.getResultMap(NAMESPACE + "transactionView");
        assertEquals(6, map.getConstructorResultMappings().size());
        assertEquals(
                java.math.BigDecimal.class,
                map.getConstructorResultMappings().get(3).getJavaType());
        assertEquals(OffsetDateTime.class, map.getConstructorResultMappings().get(5).getJavaType());
        assertTrue(map.getConstructorResultMappings().get(0).getFlags().contains(ResultFlag.ID));
    }

    @Test
    void optionalAccountCustomerFilterIsDynamicAndBound() {
        BoundSql all =
                configuration
                        .getMappedStatement(NAMESPACE + "accounts")
                        .getBoundSql(new HashMap<>());
        BoundSql filtered =
                configuration
                        .getMappedStatement(NAMESPACE + "accounts")
                        .getBoundSql(Map.of("customerId", "unsafe-value"));
        assertFalse(all.getSql().contains("WHERE"));
        assertTrue(filtered.getSql().contains("WHERE"));
        assertEquals("customerId", filtered.getParameterMappings().get(0).getProperty());
        assertFalse(filtered.getSql().contains("unsafe-value"));
    }
}
