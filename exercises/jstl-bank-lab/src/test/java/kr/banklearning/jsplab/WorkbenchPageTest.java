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

@EnabledIfSystemProperty(named="lab.phase", matches="0[3-6]")
class WorkbenchPageTest {
    private static LabServer server;
    private static final String PHASE = System.getProperty("lab.phase");
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @BeforeAll static void start() throws Exception { server = new LabServer(0); }
    @AfterAll static void stop() throws Exception { if (server != null) server.close(); }

    private static HttpResponse<String> get(String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(server.url() + path))
                .timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private static String page(String query) throws Exception {
        var response = get("/workbench" + query);
        assertEquals(200, response.statusCode(), "JSP 컴파일·렌더링");
        return response.body();
    }
    private static void rows(String html, int expected) {
        assertEquals(expected, Pattern.compile("class=\"record-row\"").matcher(html).results().count());
        assertTrue(Pattern.compile("id=\"record-count\">\\s*" + expected + "\\s*</span>").matcher(html).find());
    }

    @Test void jspCompilesAndPrivateViewIsProtected() throws Exception {
        assertTrue(page("").contains("BANK VIEW LAB / PHASE " + PHASE));
        assertEquals(200, get("/lab.css").statusCode());
        assertEquals(404, get("/WEB-INF/views/workbench.jsp").statusCode());
    }

    @Test @EnabledIfSystemProperty(named="jstl.contract", matches="true")
    void completeDefaultResult() throws Exception {
        String html = page("");
        rows(html, switch (PHASE) { case "03", "04" -> 3; default -> 2; });
        switch (PHASE) {
            case "03" -> {
                assertTrue(html.contains("2026-11-01 09:00"));
                assertTrue(html.contains("1,500,000원"));
                assertTrue(html.contains("급여 &lt;정산&gt;"));
            }
            case "04" -> {
                assertTrue(html.contains("100,000원"));
                assertTrue(html.contains("완료된 이체입니다."));
                assertTrue(html.contains("처리 결과 확인 전 재전송하지 마세요."));
                assertTrue(html.contains("취소 요청은 환급 완료가 아닙니다."));
                var link = Pattern.compile("class=\"detail-link\" href=\"([^\"]+)\"").matcher(html);
                assertTrue(link.find());
                var result = get(link.group(1).replace("&amp;", "&"));
                assertEquals(200, result.statusCode());
                assertTrue(result.body().contains("id=\"selected-id\">T01</p>"));
            }
            case "05" -> {
                assertTrue(html.contains("110-***-0001"));
                assertTrue(html.contains("req-lab-02"));
                assertTrue(html.contains("조회 &lt;확인&gt;"));
            }
            case "06" -> {
                assertTrue(html.contains("1,250,000원"));
                assertTrue(html.contains("생활비 &lt;통장&gt;"));
                assertTrue(html.contains("id=\"request-id\">api-lab-01</span>"));
            }
            default -> fail("Unsupported lab phase");
        }
    }

    @Test @EnabledIfSystemProperty(named="jstl.contract", matches="true")
    void emptyResult() throws Exception {
        String html = page("?demo=empty");
        rows(html, 0);
        assertTrue(html.contains("id=\"empty-message\""));
        assertFalse(html.contains("id=\"api-error\""));
        assertFalse(html.contains("id=\"denied-message\""));
    }

    @Test @EnabledIfSystemProperty(named="jstl.contract", matches="true")
    void filteredOrRejectedScenario() throws Exception {
        switch (PHASE) {
            case "03" -> {
                String query = URLEncoder.encode("  급여  ", StandardCharsets.UTF_8);
                rows(page("?kind=DEPOSIT&q=" + query), 1);
                rows(page("?kind=WITHDRAWAL&q=" + query), 0);
                rows(page("?q=missing"), 0);
                rows(page("?kind=UNKNOWN"), 0);
            }
            case "04" -> {
                String html = page("?status=PENDING"); rows(html, 1);
                assertTrue(html.contains("T02")); assertFalse(html.contains("T01"));
                assertFalse(html.contains("환급 완료"));
                rows(page("?status=UNKNOWN"), 0);
            }
            case "05" -> {
                String html = page("?demo=denied");
                assertTrue(html.contains("감사 내역을 볼 수 없습니다."));
                assertFalse(html.contains("class=\"record-row\""));
                assertFalse(html.contains("req-lab-01"));
                assertFalse(html.contains("id=\"empty-message\""));
                assertFalse(html.contains("id=\"record-count\""));
            }
            case "06" -> {
                String html = page("?demo=unavailable");
                assertTrue(html.contains("id=\"api-error\">잠시 후 다시 조회해 주세요.</p>"));
                assertTrue(html.contains("id=\"request-id\">api-lab-01</span>"));
                assertFalse(html.contains("class=\"record-row\""));
                assertFalse(html.contains("id=\"empty-message\""));
                assertFalse(html.contains("id=\"record-count\""));
            }
            default -> fail("Unsupported lab phase");
        }
    }

    @Test @EnabledIfSystemProperty(named="jstl.contract", matches="true")
    void dynamicOutputIsEscaped() throws Exception {
        String value = URLEncoder.encode("<script>alert(1)</script>", StandardCharsets.UTF_8);
        String html = page("?id=" + value + "&note=" + value);
        assertFalse(html.contains("<script>"));
        if (PHASE.equals("04") || PHASE.equals("05")) {
            assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        } else {
            assertTrue(html.contains(PHASE.equals("03") ? "급여 &lt;정산&gt;" : "생활비 &lt;통장&gt;"));
        }
    }
}
