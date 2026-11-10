import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HttpCheck {
    public static void main(String[] arguments) {
        try {
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            var request = HttpRequest.newBuilder(URI.create(arguments[0]))
                    .timeout(Duration.ofSeconds(8)).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) System.exit(1);
        } catch (Exception exception) {
            System.exit(1);
        }
    }
}
