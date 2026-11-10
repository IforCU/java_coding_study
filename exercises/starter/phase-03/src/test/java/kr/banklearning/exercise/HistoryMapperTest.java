package kr.banklearning.exercise;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryMapperTest {
    private static final String RESOURCE = "exercise/HistoryMapper.xml";
    private static final String ACCOUNT_ID = "30000000-0000-0000-0000-000000000101";
    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-10-01T00:00:00+09:00");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-11-01T00:00:00+09:00");

    @ParameterizedTest(name = "from={0}, to={1}")
    @MethodSource("windows")
    void generatesRequiredAndOptionalFiltersWithOrderedBindings(
            OffsetDateTime from, OffsetDateTime to, List<String> expectedProperties) throws Exception {
        Map<String, Object> parameters = parameters(ACCOUNT_ID, from, to);
        MappedStatement statement = statement();
        BoundSql bound = statement.getBoundSql(parameters);
        String sql = normalized(bound.getSql());

        assertTrue(sql.contains("phase3.transaction_history"));
        assertTrue(sql.matches("(?is).*\\baccount_id\\b\\s*=.*\\?.*"), "계좌 조건이 필수입니다.");
        assertEquals(from != null, sql.matches("(?is).*occurred_at\\s*>=\\s*\\?.*"));
        assertEquals(to != null, sql.matches("(?is).*occurred_at\\s*<\\s*\\?.*"));
        assertFalse(sql.contains(ACCOUNT_ID));
        assertEquals(expectedProperties, bound.getParameterMappings().stream()
                .map(mapping -> mapping.getProperty()).toList());
        assertEquals(expectedProperties.size(), sql.chars().filter(character -> character == '?').count());

        Map<Integer, Object> actualBindings = bindings(statement, parameters, bound);
        assertEquals(expectedProperties.size(), actualBindings.size());
        for (int index = 0; index < expectedProperties.size(); index++) {
            assertEquals(parameters.get(expectedProperties.get(index)), actualBindings.get(index + 1));
        }
    }

    static Stream<Arguments> windows() {
        return Stream.of(
                Arguments.of(null, null, List.of("accountId")),
                Arguments.of(FROM, null, List.of("accountId", "from")),
                Arguments.of(null, TO, List.of("accountId", "to")),
                Arguments.of(FROM, TO, List.of("accountId", "from", "to")));
    }

    @Test
    void explicitNullDatesBehaveLikeOmittedDates() throws Exception {
        MappedStatement statement = statement();
        Map<String, Object> parameters = parameters(ACCOUNT_ID, null, null);
        String omitted = normalized(statement.getBoundSql(parameters).getSql());
        parameters.put("from", null);
        parameters.put("to", null);
        BoundSql bound = statement.getBoundSql(parameters);
        assertEquals(omitted, normalized(bound.getSql()));
        assertEquals(List.of("accountId"), bound.getParameterMappings().stream()
                .map(mapping -> mapping.getProperty()).toList());
    }

    @Test
    void treatsSqlLookingInputAsOneBoundValue() throws Exception {
        String supplied = ACCOUNT_ID + "' OR 1=1 --";
        Map<String, Object> parameters = parameters(supplied, null, null);
        MappedStatement statement = statement();
        BoundSql bound = statement.getBoundSql(parameters);
        assertFalse(bound.getSql().contains(supplied));
        assertFalse(bound.getSql().contains("OR 1=1"));
        assertEquals(Map.of(1, supplied), bindings(statement, parameters, bound));
    }

    @Test
    void mapperResourceDoesNotUseStringInterpolation() throws Exception {
        try (InputStream input = Resources.getResourceAsStream(RESOURCE)) {
            String xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(xml.contains("${"), "검색 값에 문자열 치환을 사용하지 않습니다.");
        }
    }

    private static MappedStatement statement() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(RESOURCE)) {
            new XMLMapperBuilder(input, configuration, RESOURCE, configuration.getSqlFragments()).parse();
        }
        return configuration.getMappedStatement("kr.banklearning.exercise.HistoryMapper.search");
    }

    private static Map<String, Object> parameters(String accountId, OffsetDateTime from, OffsetDateTime to) {
        Map<String, Object> values = new HashMap<>();
        values.put("accountId", accountId);
        if (from != null) values.put("from", from);
        if (to != null) values.put("to", to);
        return values;
    }

    private static Map<Integer, Object> bindings(
            MappedStatement statement, Map<String, Object> parameters, BoundSql bound) throws Exception {
        Map<Integer, Object> values = new TreeMap<>();
        ParameterMetaData metadata = parameterMetadata(bound);
        PreparedStatement recorder = (PreparedStatement) Proxy.newProxyInstance(
                HistoryMapperTest.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (proxy, method, arguments) -> {
                    if ("getParameterMetaData".equals(method.getName())) {
                        return metadata;
                    }
                    if (method.getName().startsWith("set") && arguments != null && arguments.length >= 2
                            && arguments[0] instanceof Integer index) {
                        values.put(index, arguments[1]);
                    }
                    return null;
                });
        new DefaultParameterHandler(statement, parameters, bound).setParameters(recorder);
        return values;
    }

    private static ParameterMetaData parameterMetadata(BoundSql bound) {
        List<String> properties = bound.getParameterMappings().stream()
                .map(mapping -> mapping.getProperty()).toList();
        return (ParameterMetaData) Proxy.newProxyInstance(
                HistoryMapperTest.class.getClassLoader(), new Class<?>[]{ParameterMetaData.class},
                (proxy, method, arguments) -> {
                    if ("getParameterCount".equals(method.getName())) return properties.size();
                    if ("isWrapperFor".equals(method.getName())) return false;
                    if (arguments == null || arguments.length != 1 || !(arguments[0] instanceof Integer index)) {
                        throw new SQLFeatureNotSupportedException(method.getName());
                    }
                    if (index < 1 || index > properties.size()) {
                        throw new SQLException("잘못된 바인딩 인덱스입니다.");
                    }
                    boolean uuid = "accountId".equals(properties.get(index - 1));
                    return switch (method.getName()) {
                        case "getParameterType" -> uuid ? Types.OTHER : Types.TIMESTAMP_WITH_TIMEZONE;
                        case "getParameterTypeName" -> uuid ? "uuid" : "timestamptz";
                        case "getParameterMode" -> ParameterMetaData.parameterModeIn;
                        case "isNullable" -> ParameterMetaData.parameterNullableUnknown;
                        case "getPrecision", "getScale" -> 0;
                        case "isSigned" -> false;
                        default -> throw new SQLFeatureNotSupportedException(method.getName());
                    };
                });
    }

    private static String normalized(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
