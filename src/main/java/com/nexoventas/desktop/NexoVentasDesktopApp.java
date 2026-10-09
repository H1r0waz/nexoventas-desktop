package com.nexoventas.desktop;

import javafx.application.*;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.List;

public class NexoVentasDesktopApp extends Application {
    private final String apiUrl = System.getenv().getOrDefault("NEXOVENTAS_API_URL", "https://nexoventas-api.onrender.com");
    private final ApiClient api = new ApiClient(apiUrl);
    private final Label status = new Label("Conectate para consultar la operación comercial.");
    private final TableView<ApiClient.Product> table = new TableView<>();
    private final Label productCount = metric("—", "Productos registrados");
    private final Label lowStockCount = metric("—", "Alertas de stock");

    @Override public void start(Stage stage) {
        stage.setTitle("NexoVentas Desktop · Gestión comercial");
        stage.getIcons().add(BrandAssets.windowIcon());
        stage.setMinWidth(980); stage.setMinHeight(650);
        Scene scene = new Scene(loginView(), 1180, 720);
        scene.getStylesheets().add(getClass().getResource("/com/nexoventas/desktop/styles.css").toExternalForm());
        stage.setScene(scene); stage.show();
    }

    private Parent loginView() {
        Label brand = new Label("NEXOVENTAS"); brand.getStyleClass().add("brand");
        Label title = new Label("Gestión comercial\nen tiempo real."); title.getStyleClass().add("login-title");
        Label subtitle = new Label("Cliente de escritorio Java que consume la API REST desplegada con Spring Boot, Render y Neon."); subtitle.getStyleClass().add("subtitle"); subtitle.setWrapText(true);
        Label eyebrow = new Label("CLIENTE DE ESCRITORIO"); eyebrow.getStyleClass().add("eyebrow");
        VBox intro = new VBox(18, BrandAssets.logoMark(74), eyebrow, brand, title, subtitle); intro.getStyleClass().add("intro");
        TextField user = new TextField(); user.setPromptText("Usuario");
        PasswordField password = new PasswordField(); password.setPromptText("Contraseña");
        Button login = new Button("Ingresar al panel"); login.setMaxWidth(Double.MAX_VALUE); login.getStyleClass().add("primary-button");
        Label hint = new Label("API: " + apiUrl); hint.getStyleClass().add("hint"); hint.setWrapText(true);
        Label security = new Label("●  Conexión segura mediante JWT"); security.getStyleClass().add("security-note");
        VBox form = new VBox(16, heading("Iniciar sesión", "Accedé a tu operación comercial."), user, password, login, security, hint); form.getStyleClass().add("login-card");
        login.setOnAction(e -> authenticate(user.getText(), password.getText(), login));
        password.setOnAction(e -> login.fire());
        HBox root = new HBox(intro, form); root.setAlignment(Pos.CENTER); root.setSpacing(80); root.getStyleClass().add("login-root");
        HBox.setHgrow(intro, Priority.ALWAYS); return root;
    }

    private void authenticate(String username, String password, Button login) {
        if (username.isBlank() || password.isBlank()) { showError("Ingresá usuario y contraseña."); return; }
        login.setDisable(true); login.setText("Conectando...");
        Task<Void> task = new Task<>() { @Override protected Void call() throws Exception { api.login(username, password); return null; } };
        task.setOnSucceeded(e -> ((Stage) login.getScene().getWindow()).getScene().setRoot(dashboard()));
        task.setOnFailed(e -> { login.setDisable(false); login.setText("Ingresar al panel"); showError("No se pudo iniciar sesión. Verificá las credenciales o esperá unos minutos si Render está iniciando."); });
        new Thread(task, "nexoventas-login").start();
    }

    private Parent dashboard() {
        Label brand = new Label("NEXOVENTAS"); brand.getStyleClass().add("brand");
        Button refresh = new Button("Actualizar datos"); refresh.getStyleClass().add("secondary-button"); refresh.setOnAction(e -> loadProducts(refresh));
        Label desktop = new Label("DESKTOP"); desktop.getStyleClass().add("desktop-pill");
        HBox identity = new HBox(10, BrandAssets.logoMark(32), brand, desktop); identity.setAlignment(Pos.CENTER_LEFT);
        HBox top = new HBox(identity, new Region(), refresh); HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS); top.setAlignment(Pos.CENTER_LEFT); top.getStyleClass().add("topbar");
        configureTable();
        HBox metrics = new HBox(18, productCount, lowStockCount); metrics.getStyleClass().add("metrics");
        VBox content = new VBox(22, heading("Resumen operativo", "Productos e inventario sincronizados con la API."), metrics, status, table); content.getStyleClass().add("content"); VBox.setVgrow(table, Priority.ALWAYS);
        BorderPane root = new BorderPane(content); root.setTop(top); root.getStyleClass().add("dashboard");
        Platform.runLater(() -> loadProducts(refresh)); return root;
    }

    private void configureTable() {
        if (!table.getColumns().isEmpty()) return;
        table.getColumns().add(column("SKU", "sku", 140)); table.getColumns().add(column("Producto", "name", 280)); table.getColumns().add(column("Categoría", "category", 180));
        TableColumn<ApiClient.Product, Number> stock = new TableColumn<>("Stock"); stock.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().stock())); stock.setPrefWidth(110);
        TableColumn<ApiClient.Product, Number> minimum = new TableColumn<>("Mínimo"); minimum.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().minimumStock())); minimum.setPrefWidth(110);
        TableColumn<ApiClient.Product, String> price = new TableColumn<>("Precio"); price.setCellValueFactory(p -> new SimpleStringProperty("$ " + p.getValue().price())); price.setPrefWidth(150);
        table.getColumns().addAll(stock, minimum, price); table.setPlaceholder(new Label("No hay productos para mostrar."));
    }
    private TableColumn<ApiClient.Product, String> column(String title, String property, int width) {
        TableColumn<ApiClient.Product, String> column = new TableColumn<>(title);
        column.setCellValueFactory(product -> new SimpleStringProperty(switch (property) {
            case "sku" -> product.getValue().sku(); case "name" -> product.getValue().name(); default -> product.getValue().category();
        }));
        column.setPrefWidth(width); return column;
    }
    private void loadProducts(Button refresh) {
        refresh.setDisable(true); status.setText("Actualizando datos desde la API...");
        Task<List<ApiClient.Product>> task = new Task<>() { @Override protected List<ApiClient.Product> call() throws Exception { return api.products(); } };
        task.setOnSucceeded(e -> { List<ApiClient.Product> products = task.getValue(); table.setItems(FXCollections.observableArrayList(products)); productCount.setText(products.size() + "\nProductos registrados"); refresh.setDisable(false); status.setText("●  Datos sincronizados correctamente."); status.getStyleClass().setAll("status-success"); loadLowStock(); });
        task.setOnFailed(e -> { refresh.setDisable(false); status.setText("●  No se pudieron actualizar los datos. Revisá la conexión."); status.getStyleClass().setAll("status-error"); }); new Thread(task, "nexoventas-products").start();
    }
    private void loadLowStock() { Task<List<ApiClient.Product>> task = new Task<>() { @Override protected List<ApiClient.Product> call() throws Exception { return api.lowStock(); } }; task.setOnSucceeded(e -> lowStockCount.setText(task.getValue().size() + "\nAlertas de stock")); new Thread(task, "nexoventas-low-stock").start(); }
    private Label metric(String value, String caption) { Label label = new Label(value + "\n" + caption); label.getStyleClass().add("metric"); return label; }
    private VBox heading(String title, String subtitle) { Label h = new Label(title); h.getStyleClass().add("page-title"); Label s = new Label(subtitle); s.getStyleClass().add("subtitle"); return new VBox(6, h, s); }
    private void showError(String message) { new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait(); }
    public static void main(String[] args) { launch(args); }
}
