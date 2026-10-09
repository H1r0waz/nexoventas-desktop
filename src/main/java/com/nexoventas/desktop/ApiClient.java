package com.nexoventas.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.List;

public final class ApiClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;
    private String accessToken;

    public ApiClient(String baseUrl) { this.baseUrl = baseUrl.replaceAll("/$", ""); }

    public void login(String username, String password) throws IOException, InterruptedException {
        String payload = json.writeValueAsString(new LoginRequest(username, password));
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/api/v1/auth/login")).timeout(Duration.ofSeconds(30))
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

    public List<Customer> customers() throws IOException, InterruptedException {
        HttpResponse<String> response = send(authenticated("/api/v1/customers").GET().build());
        if (response.statusCode() != 200) throw new IOException("No se pudieron cargar los clientes (HTTP " + response.statusCode() + ")");
        return json.readValue(response.body(), new TypeReference<>() {});
    }

    public int salesCount() throws IOException, InterruptedException {
        HttpResponse<String> response = send(authenticated("/api/v1/sales").GET().build());
        if (response.statusCode() != 200) throw new IOException("No se pudieron cargar las ventas (HTTP " + response.statusCode() + ")");
        return json.readTree(response.body()).size();
    }

    public void createProduct(String sku, String name, String category, BigDecimal price, int stock, int minimumStock) throws IOException, InterruptedException {
        create("/api/v1/products", new ProductRequest(sku, name, category, price, stock, minimumStock));
    }

    public void createCustomer(String name, String taxId, String email, String phone) throws IOException, InterruptedException {
        create("/api/v1/customers", new CustomerRequest(name, taxId.isBlank() ? null : taxId, email.isBlank() ? null : email, phone.isBlank() ? null : phone));
    }

    public void createSale(String customerId, String productId, int quantity) throws IOException, InterruptedException {
        create("/api/v1/sales", new SaleRequest(customerId, List.of(new SaleLineRequest(productId, quantity))));
    }

    private void create(String path, Object payload) throws IOException, InterruptedException {
        HttpResponse<String> response = send(authenticated(path).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build());
        if (response.statusCode() != 201) throw new IOException("La operación no se pudo completar (HTTP " + response.statusCode() + ")");
    }

    private HttpRequest.Builder authenticated(String path) { return HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + accessToken); }
    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException { return http.send(request, HttpResponse.BodyHandlers.ofString()); }
    private URI uri(String path) { return URI.create(baseUrl + path); }
    private void requireToken() { if (accessToken == null) throw new IllegalStateException("Iniciá sesión primero"); }

    private record LoginRequest(String username, String password) {}
    private record LoginResponse(String accessToken, String tokenType) {}
    private record ProductRequest(String sku, String name, String category, BigDecimal price, int stock, int minimumStock) {}
    private record CustomerRequest(String name, String taxId, String email, String phone) {}
    private record SaleRequest(String customerId, List<SaleLineRequest> items) {}
    private record SaleLineRequest(String productId, int quantity) {}
    public record Product(String id, String sku, String name, String category, BigDecimal price, int stock, int minimumStock) {
        @Override public String toString() { return sku + " · " + name; }
    }
    public record Customer(String id, String name, String taxId, String email, String phone) {
        @Override public String toString() { return name; }
    }
}
