package com.nexoventas.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public final class ApiClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;
    private String accessToken;

    public ApiClient(String baseUrl) { this.baseUrl = baseUrl.replaceAll("/$", ""); }

    public void login(String username, String password) throws IOException, InterruptedException {
        String payload = json.writeValueAsString(new LoginRequest(username, password));
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build());
        if (response.statusCode() != 200) throw new IOException("No se pudo iniciar sesión (HTTP " + response.statusCode() + ")");
        accessToken = json.readValue(response.body(), LoginResponse.class).accessToken();
    }

    public List<Product> products() throws IOException, InterruptedException {
        requireToken();
        HttpResponse<String> response = send(authenticated("/api/v1/products").GET().build());
        if (response.statusCode() != 200) throw new IOException("No se pudieron cargar los productos (HTTP " + response.statusCode() + ")");
        return json.readValue(response.body(), new TypeReference<>() {});
    }

    public List<Product> lowStock() throws IOException, InterruptedException {
        requireToken();
        HttpResponse<String> response = send(authenticated("/api/v1/products/low-stock").GET().build());
        if (response.statusCode() != 200) throw new IOException("No se pudo consultar el stock (HTTP " + response.statusCode() + ")");
        return json.readValue(response.body(), new TypeReference<>() {});
    }

    private HttpRequest.Builder authenticated(String path) { return HttpRequest.newBuilder(uri(path)).header("Authorization", "Bearer " + accessToken); }
    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException { return http.send(request, HttpResponse.BodyHandlers.ofString()); }
    private URI uri(String path) { return URI.create(baseUrl + path); }
    private void requireToken() { if (accessToken == null) throw new IllegalStateException("Iniciá sesión primero"); }

    private record LoginRequest(String username, String password) {}
    private record LoginResponse(String accessToken, String tokenType) {}
    public record Product(String id, String sku, String name, String category, java.math.BigDecimal price, int stock, int minimumStock) {}
}
