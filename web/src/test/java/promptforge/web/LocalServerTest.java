package promptforge.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LocalServerTest {
    private static final Path HOME = TestProjects.create();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("promptforge.home", HOME::toString); }
    @LocalServerPort int port;
    @Test void realSessionCsrfAndChunkedBodyLimit() throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        String base = "http://127.0.0.1:" + port;
        var page = client.send(HttpRequest.newBuilder(URI.create(base + "/")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, page.statusCode());
        var tokenResponse = client.send(HttpRequest.newBuilder(URI.create(base + "/api/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
        var token = new ObjectMapper().readTree(tokenResponse.body());
        var preview = client.send(HttpRequest.newBuilder(URI.create(base + "/api/projects/demo/preview"))
                .header("Content-Type", "application/json").header(token.path("headerName").asText(), token.path("token").asText())
                .POST(HttpRequest.BodyPublishers.ofString(WebContractTest.DOCUMENT)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, preview.statusCode(), preview.body()); assertTrue(preview.body().contains("支付失敗再試"));
        var oversized = client.send(HttpRequest.newBuilder(URI.create(base + "/api/projects/demo/preview"))
                .header("Content-Type", "application/json").header(token.path("headerName").asText(), token.path("token").asText())
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new java.io.ByteArrayInputStream(new byte[70_000]))).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(413, oversized.statusCode(), oversized.body());
    }
}
