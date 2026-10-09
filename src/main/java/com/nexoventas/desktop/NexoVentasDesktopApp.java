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
import javafx.stage.Stage;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NexoVentasDesktopApp extends Application {
    private final String apiUrl = System.getenv().getOrDefault("NEXOVENTAS_API_URL", "https://nexoventas-api.onrender.com");
    private final ApiClient api = new ApiClient(apiUrl);
    private final Label status = new Label("Conectate para consultar la operación comercial.");
    private final TableView<ApiClient.Product> table = new TableView<>();
    private final TableView<ApiClient.Sale> salesTable = new TableView<>();
    private final javafx.collections.ObservableList<ApiClient.Customer> customers = FXCollections.observableArrayList();
    private final Label productCount = metric("—", "Productos registrados");
    private final Label lowStockCount = metric("—", "Alertas de stock");
    private final Label customerCount = metric("—", "Clientes registrados");
    private final Label salesCount = metric("—", "Ventas registradas");

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
        task.setOnFailed(e -> {
            login.setDisable(false); login.setText("Ingresar al panel");
            String reason = task.getException() == null ? "Error desconocido" : task.getException().getMessage();
            showError("No se pudo iniciar sesión.\n\n" + reason + "\n\nVerificá las credenciales o esperá unos minutos si Render está iniciando.");
        });
        new Thread(task, "nexoventas-login").start();
    }

    private Parent dashboard() {
        Label brand = new Label("NEXOVENTAS"); brand.getStyleClass().add("brand");
        Button refresh = new Button("Actualizar datos"); refresh.getStyleClass().add("secondary-button"); refresh.setOnAction(e -> refreshDashboard(refresh));
        Button newProduct = new Button("+ Producto"); newProduct.getStyleClass().add("primary-compact"); newProduct.setOnAction(e -> openProductDialog());
        Button newCustomer = new Button("+ Cliente"); newCustomer.getStyleClass().add("secondary-button"); newCustomer.setOnAction(e -> openCustomerDialog());
        Button newSale = new Button("+ Venta"); newSale.getStyleClass().add("secondary-button"); newSale.setOnAction(e -> openSaleDialog());
        Label desktop = new Label("DESKTOP"); desktop.getStyleClass().add("desktop-pill");
        HBox identity = new HBox(10, BrandAssets.logoMark(32), brand, desktop); identity.setAlignment(Pos.CENTER_LEFT);
        HBox actions = new HBox(10, newProduct, newCustomer, newSale, refresh); actions.setAlignment(Pos.CENTER_RIGHT);
        HBox top = new HBox(identity, new Region(), actions); HBox.setHgrow(top.getChildren().get(1), Priority.ALWAYS); top.setAlignment(Pos.CENTER_LEFT); top.getStyleClass().add("topbar");
        configureTable(); configureSalesTable();
        HBox metrics = new HBox(18, productCount, lowStockCount, customerCount, salesCount); metrics.getStyleClass().add("metrics");
        Tab productsTab = new Tab("Inventario", table); productsTab.setClosable(false);
        Tab salesTab = new Tab("Ventas", salesTable); salesTab.setClosable(false);
        TabPane dataTabs = new TabPane(productsTab, salesTab); dataTabs.getStyleClass().add("data-tabs");
        VBox content = new VBox(22, heading("Resumen operativo", "Inventario, clientes y ventas sincronizados con la API."), metrics, status, dataTabs); content.getStyleClass().add("content"); VBox.setVgrow(dataTabs, Priority.ALWAYS);
        BorderPane root = new BorderPane(content); root.setTop(top); root.getStyleClass().add("dashboard");
        Platform.runLater(() -> refreshDashboard(refresh)); return root;
    }

    private void configureTable() {
        if (!table.getColumns().isEmpty()) return;
        table.getColumns().add(column("SKU", "sku", 140)); table.getColumns().add(column("Producto", "name", 280)); table.getColumns().add(column("Categoría", "category", 180));
        TableColumn<ApiClient.Product, Number> stock = new TableColumn<>("Stock"); stock.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().stock())); stock.setPrefWidth(110);
        TableColumn<ApiClient.Product, Number> minimum = new TableColumn<>("Mínimo"); minimum.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().minimumStock())); minimum.setPrefWidth(110);
        TableColumn<ApiClient.Product, String> price = new TableColumn<>("Precio"); price.setCellValueFactory(p -> new SimpleStringProperty("$ " + p.getValue().price())); price.setPrefWidth(150);
        table.getColumns().addAll(stock, minimum, price); table.setPlaceholder(new Label("No hay productos para mostrar."));
    }
    private void configureSalesTable() {
        if (!salesTable.getColumns().isEmpty()) return;
        TableColumn<ApiClient.Sale, String> date = new TableColumn<>("Fecha"); date.setCellValueFactory(s -> new SimpleStringProperty(formatDate(s.getValue().createdAt()))); date.setPrefWidth(170);
        TableColumn<ApiClient.Sale, String> customer = new TableColumn<>("Cliente"); customer.setCellValueFactory(s -> new SimpleStringProperty(s.getValue().customer() == null ? "—" : s.getValue().customer().name())); customer.setPrefWidth(245);
        TableColumn<ApiClient.Sale, String> items = new TableColumn<>("Detalle"); items.setCellValueFactory(s -> new SimpleStringProperty(saleDetail(s.getValue()))); items.setPrefWidth(430);
        TableColumn<ApiClient.Sale, String> total = new TableColumn<>("Total"); total.setCellValueFactory(s -> new SimpleStringProperty("$ " + s.getValue().total())); total.setPrefWidth(165);
        salesTable.getColumns().addAll(date, customer, items, total); salesTable.setPlaceholder(new Label("Todavía no hay ventas registradas."));
    }
    private TableColumn<ApiClient.Product, String> column(String title, String property, int width) {
        TableColumn<ApiClient.Product, String> column = new TableColumn<>(title);
        column.setCellValueFactory(product -> new SimpleStringProperty(switch (property) {
            case "sku" -> product.getValue().sku(); case "name" -> product.getValue().name(); default -> product.getValue().category();
        }));
        column.setPrefWidth(width); return column;
    }
    private void refreshDashboard(Button refresh) {
        if (refresh != null) refresh.setDisable(true);
        status.setText("Actualizando datos desde la API...");
        loadProducts(refresh); loadCustomers(); loadSales();
    }
    private void loadProducts(Button refresh) {
        Task<List<ApiClient.Product>> task = new Task<>() { @Override protected List<ApiClient.Product> call() throws Exception { return api.products(); } };
        task.setOnSucceeded(e -> { List<ApiClient.Product> products = task.getValue(); table.setItems(FXCollections.observableArrayList(products)); productCount.setText(products.size() + "\nProductos registrados"); if (refresh != null) refresh.setDisable(false); status.setText("●  Datos sincronizados correctamente."); status.getStyleClass().setAll("status-success"); loadLowStock(); });
        task.setOnFailed(e -> { if (refresh != null) refresh.setDisable(false); status.setText("●  No se pudieron actualizar los datos. Revisá la conexión."); status.getStyleClass().setAll("status-error"); }); new Thread(task, "nexoventas-products").start();
    }
    private void loadCustomers() {
        Task<List<ApiClient.Customer>> task = new Task<>() { @Override protected List<ApiClient.Customer> call() throws Exception { return api.customers(); } };
        task.setOnSucceeded(e -> { customers.setAll(task.getValue()); customerCount.setText(customers.size() + "\nClientes registrados"); });
        new Thread(task, "nexoventas-customers").start();
    }
    private void loadSales() {
        Task<List<ApiClient.Sale>> task = new Task<>() { @Override protected List<ApiClient.Sale> call() throws Exception { return api.sales(); } };
        task.setOnSucceeded(e -> { List<ApiClient.Sale> sales = task.getValue(); salesTable.setItems(FXCollections.observableArrayList(sales)); salesCount.setText(sales.size() + "\nVentas registradas"); });
        new Thread(task, "nexoventas-sales").start();
    }
    private void loadLowStock() { Task<List<ApiClient.Product>> task = new Task<>() { @Override protected List<ApiClient.Product> call() throws Exception { return api.lowStock(); } }; task.setOnSucceeded(e -> lowStockCount.setText(task.getValue().size() + "\nAlertas de stock")); new Thread(task, "nexoventas-low-stock").start(); }

    private void openProductDialog() {
        Dialog<ButtonType> dialog = formDialog("Nuevo producto", "Registrá un producto para actualizar el catálogo.");
        TextField sku = field("SKU, ej. TEC-001"), name = field("Nombre del producto"), category = field("Categoría");
        TextField price = field("Precio, ej. 25000"), stock = field("Stock inicial"), minimum = field("Stock mínimo");
        dialog.getDialogPane().setContent(formGrid(new String[] {"SKU", "Nombre", "Categoría", "Precio", "Stock", "Mínimo"}, sku, name, category, price, stock, minimum));
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try { runMutation("Producto registrado correctamente.", () -> api.createProduct(sku.getText().trim(), name.getText().trim(), category.getText().trim(), new BigDecimal(price.getText().trim()), Integer.parseInt(stock.getText().trim()), Integer.parseInt(minimum.getText().trim()))); }
        catch (NumberFormatException e) { showError("Precio, stock y mínimo deben ser números válidos."); }
    }

    private void openCustomerDialog() {
        Dialog<ButtonType> dialog = formDialog("Nuevo cliente", "Registrá un cliente para poder crear ventas.");
        TextField name = field("Nombre o razón social"), taxId = field("CUIT/DNI (opcional)"), email = field("Email (opcional)"), phone = field("Teléfono (opcional)");
        dialog.getDialogPane().setContent(formGrid(new String[] {"Nombre", "CUIT/DNI", "Email", "Teléfono"}, name, taxId, email, phone));
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        runMutation("Cliente registrado correctamente.", () -> api.createCustomer(name.getText().trim(), taxId.getText().trim(), email.getText().trim(), phone.getText().trim()));
    }

    private void openSaleDialog() {
        if (customers.isEmpty()) { showError("Primero registrá al menos un cliente."); return; }
        if (table.getItems().isEmpty()) { showError("Primero registrá al menos un producto."); return; }
        Dialog<ButtonType> dialog = formDialog("Registrar venta", "La venta descuenta el stock automáticamente.");
        ComboBox<ApiClient.Customer> customer = new ComboBox<>(customers); customer.setMaxWidth(Double.MAX_VALUE); customer.setPromptText("Seleccioná un cliente");
        ComboBox<ApiClient.Product> product = new ComboBox<>(table.getItems()); product.setMaxWidth(Double.MAX_VALUE); product.setPromptText("Seleccioná un producto");
        TextField quantity = field("Cantidad");
        dialog.getDialogPane().setContent(formGrid(new String[] {"Cliente", "Producto", "Cantidad"}, customer, product, quantity));
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        if (customer.getValue() == null || product.getValue() == null) { showError("Seleccioná un cliente y un producto."); return; }
        try { runMutation("Venta registrada. El inventario fue actualizado.", () -> api.createSale(customer.getValue().id(), product.getValue().id(), Integer.parseInt(quantity.getText().trim()))); }
        catch (NumberFormatException e) { showError("La cantidad debe ser un número entero válido."); }
    }

    private Dialog<ButtonType> formDialog(String title, String header) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(title); dialog.setHeaderText(header); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK); dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/nexoventas/desktop/styles.css").toExternalForm()); return dialog;
    }
    private TextField field(String prompt) { TextField input = new TextField(); input.setPromptText(prompt); return input; }
    private GridPane formGrid(String[] labels, Control... controls) { GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(10, 0, 0, 0)); for (int i = 0; i < labels.length; i++) { Label label = new Label(labels[i]); label.getStyleClass().add("form-label"); grid.add(label, 0, i); grid.add(controls[i], 1, i); GridPane.setHgrow(controls[i], Priority.ALWAYS); } return grid; }
    private void runMutation(String success, ApiOperation operation) {
        Task<Void> task = new Task<>() { @Override protected Void call() throws Exception { operation.run(); return null; } };
        task.setOnSucceeded(e -> { status.setText("●  " + success); status.getStyleClass().setAll("status-success"); refreshDashboard(null); });
        task.setOnFailed(e -> showError(task.getException() == null ? "No se pudo completar la operación." : task.getException().getMessage())); new Thread(task, "nexoventas-mutation").start();
    }
    @FunctionalInterface private interface ApiOperation { void run() throws Exception; }
    private Label metric(String value, String caption) { Label label = new Label(value + "\n" + caption); label.getStyleClass().add("metric"); return label; }
    private String saleDetail(ApiClient.Sale sale) {
        if (sale.items() == null || sale.items().isEmpty()) return "Sin artículos";
        return sale.items().stream().map(item -> item.quantity() + " × " + (item.product() == null ? "Producto" : item.product().name())).reduce((left, right) -> left + " · " + right).orElse("Sin artículos");
    }
    private String formatDate(String value) {
        try { return OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
        catch (Exception ignored) { return value == null ? "—" : value; }
    }
    private VBox heading(String title, String subtitle) { Label h = new Label(title); h.getStyleClass().add("page-title"); Label s = new Label(subtitle); s.getStyleClass().add("subtitle"); return new VBox(6, h, s); }
    private void showError(String message) { new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait(); }
    public static void main(String[] args) { launch(args); }
}
