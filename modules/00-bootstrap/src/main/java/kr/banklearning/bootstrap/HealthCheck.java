package kr.banklearning.bootstrap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** 별도 curl 설치 없이 JDK로 컨테이너의 HTTP readiness를 확인한다. */
public final class HealthCheck {
    private HealthCheck() { }

    public static void main(String[] args) {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/health/ready"))
                    .timeout(Duration.ofSeconds(8)).GET().build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) {
                System.exit(1);
            }
        } catch (Exception exception) {
            System.exit(1);
        }
    }
}
