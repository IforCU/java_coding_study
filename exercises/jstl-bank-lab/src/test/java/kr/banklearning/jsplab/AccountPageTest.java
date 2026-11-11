package kr.banklearning.jsplab;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="lab.phase", matches="02")
class AccountPageTest {
    private static LabServer server;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @BeforeAll static void start() throws Exception { server = new LabServer(0); }
    @AfterAll static void stop() throws Exception { if (server != null) server.close(); }

    private static HttpResponse<String> response(String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(server.url() + path))
                .timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String page(String query) throws Exception {
        HttpResponse<String> response = response("/accounts" + query);
        assertEquals(200, response.statusCode(), "JSP 컴파일 및 렌더링 성공");
        return response.body();
    }

    private static long rows(String html) {
        return Pattern.compile("class=\"account-row\"").matcher(html).results().count();
    }

    private static void count(String html, String id, int expected) {
        assertTrue(Pattern.compile("id=\"" + id + "\">\\s*" + expected + "\\s*</span>")
                .matcher(html).find(), id + " should be " + expected);
    }

    @Test void jspCompilesAndStaticResourcesLoad() throws Exception {
        assertTrue(page("").contains("내 계좌 조회"));
        assertEquals(200, response("/lab.css").statusCode());
    }

    @Test void viewCannotBeRequestedDirectly() throws Exception {
        assertEquals(404, response("/WEB-INF/views/accounts.jsp").statusCode());
    }

    @Test @EnabledIfSystemProperty(named="jstl.contract", matches="true")
    void allAccountsAndFormatting() throws Exception {
        String html = page("");
        assertEquals(4, rows(html));
        count(html, "total-count", 4); count(html, "result-count", 4);
        assertTrue(html.contains("1,250,000원"));
        assertTrue(html.contains("350,000원"));
        assertTrue(html.contains("50,000원"));
        assertTrue(html.contains("0원"));
        assertTrue(html.contains("110-***-0001"));
        assertTrue(html.contains("정상"));
        assertTrue(html.contains("거래 제한")); assertTrue(html.contains("해지"));
    }

    @Test @EnabledIfSystemProperty(named="jstl.contract", matches="true")
    void categoryFiltersAndContiguousNumbers() throws Exception {
        String deposit = page("?type=DEPOSIT");
        assertEquals(2, rows(deposit)); count(deposit, "total-count", 4); count(deposit, "result-count", 2);
        assertFalse(deposit.contains("여행 적금"));
        String savings = page("?type=SAVINGS");
        assertEquals(2, rows(savings)); count(savings, "result-count", 2);
        assertTrue(Pattern.compile("class=\"account-row\"><td>1</td>").matcher(savings).find());
        assertTrue(Pattern.compile("class=\"account-row\"><td>2</td>").matcher(savings).find());
        assertFalse(savings.contains("생활비 통장"));
    }

    @Test @EnabledIfSystemProperty(named="jstl.contract", matches="true")
    void emptySourceAndUnmatchedFilter() throws Exception {
        for (String query : new String[]{"?demo=empty", "?type=UNKNOWN"}) {
            String html = page(query);
            assertEquals(0, rows(html));
            assertTrue(html.contains("id=\"empty-message\">조건에 맞는 계좌가 없습니다."));
            assertFalse(html.contains("id=\"result-count\""));
            count(html, "total-count", query.contains("empty") ? 0 : 4);
        }
    }

    @Test @EnabledIfSystemProperty(named="jstl.contract", matches="true")
    void greetingTrimsAndEscapes() throws Exception {
        assertTrue(page("").contains("학습자님, 계좌를 확인하세요."));
        assertTrue(page("?name=%20%20").contains("학습자님, 계좌를 확인하세요."));
        String name = URLEncoder.encode("  <script>alert(1)</script>  ", StandardCharsets.UTF_8);
        String html = page("?name=" + name);
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;님, 계좌를 확인하세요."));
        assertTrue(html.contains("비상금 &lt;안전&gt;"));
        assertFalse(html.contains("비상금 <안전>"));
    }

    @Test @EnabledIfSystemProperty(named="jstl.contract", matches="true")
    void generatedLinkWorks() throws Exception {
        String html = page("");
        var matcher = Pattern.compile("id=\"empty-link\" href=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find());
        String href = matcher.group(1).replace("&amp;", "&");
        assertTrue(href.contains("demo=empty"));
        HttpResponse<String> response = response(href);
        assertEquals(200, response.statusCode()); assertEquals(0, rows(response.body()));
    }
}
