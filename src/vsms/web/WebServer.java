package vsms.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vsms.db.DBConnection;
import vsms.db.DatabaseSetup;
import vsms.model.Booking;
import vsms.model.BookingItem;
import vsms.model.Customer;
import vsms.model.Invoice;
import vsms.model.ServiceHistoryEntry;
import vsms.model.ServiceType;
import vsms.model.Vehicle;
import vsms.service.BookingService;
import vsms.service.CustomerService;
import vsms.service.InvoiceService;
import vsms.service.ServiceHistoryService;
import vsms.service.VehicleService;
import vsms.dao.ServiceTypeDAO;

/**
 * Embedded HTTP Web Server running on http://localhost:8080
 * Uses built-in JDK com.sun.net.httpserver.HttpServer (Zero external dependencies).
 */
public class WebServer {

    private final int port;
    private HttpServer server;

    private final CustomerService customerService = new CustomerService();
    private final VehicleService vehicleService = new VehicleService();
    private final BookingService bookingService = new BookingService();
    private final ServiceHistoryService historyService = new ServiceHistoryService();
    private final InvoiceService invoiceService = new InvoiceService();
    private final ServiceTypeDAO serviceTypeDAO = new ServiceTypeDAO();

    public WebServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Serve Static Web Dashboard
        server.createContext("/", new StaticHandler());

        // API Endpoints
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/setup", new SetupHandler());
        server.createContext("/api/customers", new CustomersHandler());
        server.createContext("/api/vehicles", new VehiclesHandler());
        server.createContext("/api/servicetypes", new ServiceTypesHandler());
        server.createContext("/api/bookings", new BookingsHandler());
        server.createContext("/api/history", new HistoryHandler());
        server.createContext("/api/invoices", new InvoicesHandler());

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("========================================================");
        System.out.println("   VEHICLE SERVICE MANAGEMENT SYSTEM - WEB DASHBOARD    ");
        System.out.println("========================================================");
        System.out.println(" Web Server running at: http://localhost:" + port);
        System.out.println(" Open your browser and navigate to http://localhost:" + port);
        System.out.println("========================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ------------------------------------------------------------------------
    // Static HTML/CSS/JS Dashboard Handler
    // ------------------------------------------------------------------------
    private class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                sendResponse(exchange, 200, "text/html; charset=UTF-8", getDashboardHtml().getBytes(StandardCharsets.UTF_8));
                return;
            }
            if (path.startsWith("/invoices/")) {
                String fileName = path.substring("/invoices/".length());
                File file = new File("invoices", fileName);
                if (file.exists() && file.isFile()) {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    sendResponse(exchange, 200, "application/pdf", bytes);
                    return;
                }
            }
            sendResponse(exchange, 404, "text/plain", "404 Not Found".getBytes(StandardCharsets.UTF_8));
        }
    }

    // ------------------------------------------------------------------------
    // DB Status & Setup
    // ------------------------------------------------------------------------
    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            boolean connected = false;
            String error = "";
            try (java.sql.Connection conn = DBConnection.getConnection()) {
                connected = conn != null && !conn.isClosed();
            } catch (Exception e) {
                error = e.getMessage();
            }
            String json = "{\"connected\":" + connected + ",\"database\":\"" + escapeJson(DBConnection.databaseName()) + "\",\"error\":\"" + escapeJson(error) + "\"}";
            sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private class SetupHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}".getBytes(StandardCharsets.UTF_8));
                return;
            }
            boolean success = DatabaseSetup.setupDatabase();
            String json = "{\"success\":" + success + "}";
            sendResponse(exchange, success ? 200 : 500, "application/json", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    // ------------------------------------------------------------------------
    // Customers Handler
    // ------------------------------------------------------------------------
    private class CustomersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            try {
                if ("GET".equalsIgnoreCase(method)) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("phone=")) {
                        String phone = query.substring("phone=".length());
                        Customer c = customerService.findByPhone(phone);
                        if (c == null) {
                            sendResponse(exchange, 404, "application/json", "{\"error\":\"Customer not found\"}".getBytes(StandardCharsets.UTF_8));
                        } else {
                            sendResponse(exchange, 200, "application/json", customerToJson(c).getBytes(StandardCharsets.UTF_8));
                        }
                    } else if (query != null && query.startsWith("id=")) {
                        int id = Integer.parseInt(query.substring("id=".length()));
                        Customer c = customerService.findById(id);
                        if (c == null) {
                            sendResponse(exchange, 404, "application/json", "{\"error\":\"Customer not found\"}".getBytes(StandardCharsets.UTF_8));
                        } else {
                            sendResponse(exchange, 200, "application/json", customerToJson(c).getBytes(StandardCharsets.UTF_8));
                        }
                    } else {
                        List<Customer> list = customerService.findAll();
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < list.size(); i++) {
                            sb.append(customerToJson(list.get(i)));
                            if (i < list.size() - 1) sb.append(",");
                        }
                        sb.append("]");
                        sendResponse(exchange, 200, "application/json", sb.toString().getBytes(StandardCharsets.UTF_8));
                    }
                } else if ("POST".equalsIgnoreCase(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    Customer c = customerService.register(
                            map.get("name"),
                            map.get("phone"),
                            map.get("email"),
                            map.get("address")
                    );
                    sendResponse(exchange, 201, "application/json", customerToJson(c).getBytes(StandardCharsets.UTF_8));
                } else if ("PUT".equalsIgnoreCase(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    Customer c = new Customer(
                            Integer.parseInt(map.get("id")),
                            map.get("name"),
                            map.get("phone"),
                            map.get("email"),
                            map.get("address")
                    );
                    customerService.update(c);
                    sendResponse(exchange, 200, "application/json", "{\"success\":true}".getBytes(StandardCharsets.UTF_8));
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("id=")) {
                        int id = Integer.parseInt(query.substring("id=".length()));
                        boolean deleted = customerService.delete(id);
                        sendResponse(exchange, 200, "application/json", ("{\"success\":" + deleted + "}").getBytes(StandardCharsets.UTF_8));
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"error\":\"Missing id parameter\"}".getBytes(StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception e) {
                sendResponse(exchange, 400, "application/json", ("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}").getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    // ------------------------------------------------------------------------
    // Vehicles Handler
    // ------------------------------------------------------------------------
    private class VehiclesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            try {
                if ("GET".equalsIgnoreCase(method)) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("customerId=")) {
                        int customerId = Integer.parseInt(query.substring("customerId=".length()));
                        List<Vehicle> list = vehicleService.findByCustomer(customerId);
                        sendResponse(exchange, 200, "application/json", vehiclesToJson(list).getBytes(StandardCharsets.UTF_8));
                    } else {
                        List<Vehicle> list = vehicleService.findAll();
                        sendResponse(exchange, 200, "application/json", vehiclesToJson(list).getBytes(StandardCharsets.UTF_8));
                    }
                } else if ("POST".equalsIgnoreCase(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    Vehicle v = vehicleService.register(
                            Integer.parseInt(map.get("customerId")),
                            map.get("make"),
                            map.get("model"),
                            Integer.parseInt(map.getOrDefault("year", "0")),
                            map.get("licensePlate"),
                            map.get("vin")
                    );
                    sendResponse(exchange, 201, "application/json", vehicleToJson(v).getBytes(StandardCharsets.UTF_8));
                } else if ("PUT".equalsIgnoreCase(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    Vehicle v = new Vehicle(
                            Integer.parseInt(map.get("id")),
                            Integer.parseInt(map.get("customerId")),
                            map.get("make"),
                            map.get("model"),
                            Integer.parseInt(map.getOrDefault("year", "0")),
                            map.get("licensePlate"),
                            map.get("vin")
                    );
                    vehicleService.update(v);
                    sendResponse(exchange, 200, "application/json", "{\"success\":true}".getBytes(StandardCharsets.UTF_8));
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("id=")) {
                        int id = Integer.parseInt(query.substring("id=".length()));
                        boolean deleted = vehicleService.delete(id);
                        sendResponse(exchange, 200, "application/json", ("{\"success\":" + deleted + "}").getBytes(StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception e) {
                sendResponse(exchange, 400, "application/json", ("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}").getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    // ------------------------------------------------------------------------
    // Service Types Handler
    // ------------------------------------------------------------------------
    private class ServiceTypesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<ServiceType> list = serviceTypeDAO.findAll();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    ServiceType st = list.get(i);
                    sb.append(String.format("{\"id\":%d,\"name\":\"%s\",\"description\":\"%s\",\"baseCost\":%.2f}",
                            st.getId(), escapeJson(st.getName()), escapeJson(st.getDescription()), st.getBaseCost()));
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendResponse(exchange, 200, "application/json", sb.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", ("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}").getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    // ------------------------------------------------------------------------
    // Bookings Handler
    // ------------------------------------------------------------------------
    private class BookingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            try {
                if ("GET".equalsIgnoreCase(method)) {
                    List<Booking> list = bookingService.findAll();
                    sendResponse(exchange, 200, "application/json", bookingsToJson(list).getBytes(StandardCharsets.UTF_8));
                } else if ("POST".equalsIgnoreCase(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    int vehicleId = Integer.parseInt(map.get("vehicleId"));
                    LocalDate date = LocalDate.parse(map.get("scheduledDate"));
                    String notes = map.get("notes");
                    
                    String serviceTypeIdsStr = map.get("serviceTypeIds");
                    List<Integer> serviceTypeIds = new ArrayList<>();
                    if (serviceTypeIdsStr != null && !serviceTypeIdsStr.isBlank()) {
                        for (String part : serviceTypeIdsStr.split(",")) {
                            serviceTypeIds.add(Integer.parseInt(part.trim()));
                        }
                    }

                    Booking b = bookingService.createBooking(vehicleId, date, notes, serviceTypeIds);
                    sendResponse(exchange, 201, "application/json", bookingToJson(b).getBytes(StandardCharsets.UTF_8));
                } else if ("PUT".equalsIgnoreCase(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    int id = Integer.parseInt(map.get("id"));
                    String action = map.get("action");

                    if ("start".equalsIgnoreCase(action)) {
                        bookingService.startService(id);
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}".getBytes(StandardCharsets.UTF_8));
                    } else if ("complete".equalsIgnoreCase(action)) {
                        int odometer = Integer.parseInt(map.get("odometer"));
                        String technician = map.get("technician");
                        bookingService.complete(id, odometer, technician);
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}".getBytes(StandardCharsets.UTF_8));
                    } else if ("cancel".equalsIgnoreCase(action)) {
                        bookingService.cancel(id);
                        sendResponse(exchange, 200, "application/json", "{\"success\":true}".getBytes(StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception e) {
                sendResponse(exchange, 400, "application/json", ("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}").getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    // ------------------------------------------------------------------------
    // Service History Handler
    // ------------------------------------------------------------------------
    private class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String query = exchange.getRequestURI().getQuery();
                List<ServiceHistoryEntry> list;
                if (query != null && query.startsWith("plate=")) {
                    list = historyService.historyByPlate(query.substring("plate=".length()));
                } else if (query != null && query.startsWith("customerId=")) {
                    list = historyService.historyByCustomer(Integer.parseInt(query.substring("customerId=".length())));
                } else if (query != null && query.startsWith("vehicleId=")) {
                    list = historyService.historyByVehicle(Integer.parseInt(query.substring("vehicleId=".length())));
                } else {
                    list = historyService.allHistory();
                }
                sendResponse(exchange, 200, "application/json", historyToJson(list).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                sendResponse(exchange, 500, "application/json", ("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}").getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    // ------------------------------------------------------------------------
    // Invoices Handler
    // ------------------------------------------------------------------------
    private class InvoicesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            try {
                if ("POST".equalsIgnoreCase(method)) {
                    String body = readBody(exchange);
                    Map<String, String> map = parseJson(body);
                    int bookingId = Integer.parseInt(map.get("bookingId"));
                    Invoice inv = invoiceService.generateInvoice(bookingId);
                    sendResponse(exchange, 201, "application/json", invoiceToJson(inv).getBytes(StandardCharsets.UTF_8));
                } else if ("GET".equalsIgnoreCase(method)) {
                    List<Invoice> list = invoiceService.findAll();
                    sendResponse(exchange, 200, "application/json", invoicesToJson(list).getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                sendResponse(exchange, 400, "application/json", ("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}").getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------
    private void sendResponse(HttpExchange exchange, int status, String contentType, byte[] data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
        
        String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String val = kv[1].trim().replace("\"", "");
                map.put(key, val);
            }
        }
        return map;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String customerToJson(Customer c) {
        return String.format("{\"id\":%d,\"name\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\",\"address\":\"%s\"}",
                c.getId(), escapeJson(c.getName()), escapeJson(c.getPhone()), escapeJson(c.getEmail()), escapeJson(c.getAddress()));
    }

    private String vehiclesToJson(List<Vehicle> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(vehicleToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String vehicleToJson(Vehicle v) {
        return String.format("{\"id\":%d,\"customerId\":%d,\"make\":\"%s\",\"model\":\"%s\",\"year\":%d,\"licensePlate\":\"%s\",\"vin\":\"%s\"}",
                v.getId(), v.getCustomerId(), escapeJson(v.getMake()), escapeJson(v.getModel()), v.getYear(), escapeJson(v.getLicensePlate()), escapeJson(v.getVin()));
    }

    private String bookingsToJson(List<Booking> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(bookingToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String bookingToJson(Booking b) {
        StringBuilder itemsJson = new StringBuilder("[");
        List<BookingItem> items = b.getItems();
        for (int i = 0; i < items.size(); i++) {
            BookingItem item = items.get(i);
            itemsJson.append(String.format("{\"id\":%d,\"bookingId\":%d,\"serviceTypeId\":%d,\"serviceName\":\"%s\",\"cost\":%.2f}",
                    item.getId(), item.getBookingId(), item.getServiceTypeId(), escapeJson(item.getServiceName()), item.getCost()));
            if (i < items.size() - 1) itemsJson.append(",");
        }
        itemsJson.append("]");

        return String.format("{\"id\":%d,\"vehicleId\":%d,\"scheduledDate\":\"%s\",\"status\":\"%s\",\"odometer\":%s,\"technician\":\"%s\",\"notes\":\"%s\",\"total\":%.2f,\"items\":%s}",
                b.getId(), b.getVehicleId(), b.getScheduledDate(), b.getStatus(),
                b.getOdometer() != null ? b.getOdometer() : "null",
                escapeJson(b.getTechnician()), escapeJson(b.getNotes()), bookingService.calculateTotal(b), itemsJson.toString());
    }

    private String historyToJson(List<ServiceHistoryEntry> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            ServiceHistoryEntry h = list.get(i);
            sb.append(String.format("{\"bookingId\":%d,\"serviceDate\":\"%s\",\"status\":\"%s\",\"odometer\":%s,\"technician\":\"%s\",\"customerId\":%d,\"customerName\":\"%s\",\"vehicleId\":%d,\"vehicleName\":\"%s\",\"licensePlate\":\"%s\",\"serviceName\":\"%s\",\"cost\":%.2f}",
                    h.getBookingId(), h.getServiceDate(), h.getStatus(),
                    h.getOdometer() != null ? h.getOdometer() : "null",
                    escapeJson(h.getTechnician()), h.getCustomerId(), escapeJson(h.getCustomerName()),
                    h.getVehicleId(), escapeJson(h.getVehicleName()), escapeJson(h.getLicensePlate()),
                    escapeJson(h.getServiceName()), h.getCost()));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String invoicesToJson(List<Invoice> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(invoiceToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String invoiceToJson(Invoice inv) {
        return String.format("{\"id\":%d,\"bookingId\":%d,\"invoiceNumber\":\"%s\",\"total\":%.2f,\"pdfPath\":\"%s\"}",
                inv.getId(), inv.getBookingId(), escapeJson(inv.getInvoiceNumber()), inv.getTotal(), escapeJson(inv.getPdfPath().replace("\\", "/")));
    }

    // ------------------------------------------------------------------------
    // Glassmorphism Dashboard UI HTML Template
    // ------------------------------------------------------------------------
    private String getDashboardHtml() {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "<title>Vehicle Service Management System</title>\n"
                + "<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">\n"
                + "<style>\n"
                + "  :root {\n"
                + "    --bg: #0f172a;\n"
                + "    --panel-bg: rgba(30, 41, 59, 0.7);\n"
                + "    --accent: #38bdf8;\n"
                + "    --accent-hover: #0284c7;\n"
                + "    --text: #f8fafc;\n"
                + "    --text-muted: #94a3b8;\n"
                + "    --border: rgba(255, 255, 255, 0.1);\n"
                + "    --success: #22c55e;\n"
                + "    --warning: #f59e0b;\n"
                + "    --danger: #ef4444;\n"
                + "  }\n"
                + "  * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }\n"
                + "  body { background: radial-gradient(circle at top left, #1e293b, #0f172a); color: var(--text); min-height: 100vh; display: flex; }\n"
                + "  sidebar { width: 260px; background: rgba(15, 23, 42, 0.9); border-right: 1px solid var(--border); backdrop-filter: blur(12px); padding: 24px 16px; display: flex; flex-direction: column; gap: 24px; }\n"
                + "  .brand { font-size: 20px; font-weight: 700; color: var(--accent); display: flex; align-items: center; gap: 10px; }\n"
                + "  nav { display: flex; flex-direction: column; gap: 8px; }\n"
                + "  nav button { background: none; border: none; color: var(--text-muted); padding: 12px 16px; border-radius: 8px; text-align: left; font-size: 14px; font-weight: 500; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 12px; }\n"
                + "  nav button:hover, nav button.active { background: rgba(56, 189, 248, 0.15); color: var(--accent); }\n"
                + "  main { flex: 1; padding: 32px; overflow-y: auto; }\n"
                + "  .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; }\n"
                + "  .header h1 { font-size: 24px; font-weight: 600; }\n"
                + "  .badge { padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; }\n"
                + "  .badge.connected { background: rgba(34, 197, 94, 0.15); color: var(--success); border: 1px solid rgba(34, 197, 94, 0.3); }\n"
                + "  .badge.disconnected { background: rgba(239, 68, 68, 0.15); color: var(--danger); border: 1px solid rgba(239, 68, 68, 0.3); }\n"
                + "  .card { background: var(--panel-bg); border: 1px solid var(--border); border-radius: 12px; backdrop-filter: blur(16px); padding: 24px; margin-bottom: 24px; box-shadow: 0 8px 32px rgba(0,0,0,0.3); }\n"
                + "  .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }\n"
                + "  .grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }\n"
                + "  .stat-card { background: rgba(30, 41, 59, 0.5); border: 1px solid var(--border); border-radius: 10px; padding: 20px; text-align: center; }\n"
                + "  .stat-card .val { font-size: 28px; font-weight: 700; color: var(--accent); margin-top: 8px; }\n"
                + "  table { width: 100%; border-collapse: collapse; margin-top: 16px; font-size: 14px; }\n"
                + "  th { text-align: left; padding: 12px; color: var(--text-muted); font-weight: 500; border-bottom: 1px solid var(--border); }\n"
                + "  td { padding: 14px 12px; border-bottom: 1px solid rgba(255,255,255,0.05); }\n"
                + "  tr:hover td { background: rgba(255,255,255,0.02); }\n"
                + "  .btn { background: var(--accent); color: #0f172a; border: none; padding: 10px 18px; border-radius: 6px; font-weight: 600; cursor: pointer; transition: all 0.2s; font-size: 13px; }\n"
                + "  .btn:hover { background: var(--accent-hover); transform: translateY(-1px); }\n"
                + "  .btn-sm { padding: 6px 12px; font-size: 12px; }\n"
                + "  .btn-outline { background: none; border: 1px solid var(--accent); color: var(--accent); }\n"
                + "  .btn-outline:hover { background: rgba(56,189,248,0.1); color: var(--accent); }\n"
                + "  .form-group { margin-bottom: 16px; }\n"
                + "  .form-group label { display: block; margin-bottom: 6px; font-size: 13px; color: var(--text-muted); }\n"
                + "  .form-group input, .form-group select, .form-group textarea { width: 100%; background: rgba(15,23,42,0.6); border: 1px solid var(--border); color: var(--text); padding: 10px 12px; border-radius: 6px; outline: none; font-size: 14px; }\n"
                + "  .form-group input:focus, .form-group select:focus { border-color: var(--accent); }\n"
                + "  .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); backdrop-filter: blur(4px); display: none; justify-content: center; align-items: center; z-index: 100; }\n"
                + "  .modal { background: #1e293b; border: 1px solid var(--border); width: 500px; max-width: 90%; border-radius: 12px; padding: 24px; }\n"
                + "  .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }\n"
                + "  .status-tag { padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: 600; text-transform: uppercase; }\n"
                + "  .status-BOOKED { background: rgba(56, 189, 248, 0.2); color: var(--accent); }\n"
                + "  .status-IN_PROGRESS { background: rgba(245, 158, 11, 0.2); color: var(--warning); }\n"
                + "  .status-COMPLETED { background: rgba(34, 197, 94, 0.2); color: var(--success); }\n"
                + "  .status-CANCELLED { background: rgba(239, 68, 68, 0.2); color: var(--danger); }\n"
                + "</style>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <sidebar>\n"
                + "    <div class=\"brand\">🚘 VSMS Pro</div>\n"
                + "    <nav>\n"
                + "      <button class=\"active\" onclick=\"showTab('dashboard')\">📊 Dashboard</button>\n"
                + "      <button onclick=\"showTab('customers')\">👥 Customers</button>\n"
                + "      <button onclick=\"showTab('vehicles')\">🚗 Vehicles</button>\n"
                + "      <button onclick=\"showTab('bookings')\">📅 Bookings</button>\n"
                + "      <button onclick=\"showTab('history')\">📜 Service History</button>\n"
                + "      <button onclick=\"showTab('invoices')\">🧾 Invoices</button>\n"
                + "      <button onclick=\"showTab('db')\">⚙️ Database</button>\n"
                + "    </nav>\n"
                + "  </sidebar>\n"
                + "  <main>\n"
                + "    <div class=\"header\">\n"
                + "      <h1 id=\"page-title\">Dashboard</h1>\n"
                + "      <div id=\"db-badge\" class=\"badge disconnected\">Checking DB...</div>\n"
                + "    </div>\n"
                + "\n"
                + "    <!-- DASHBOARD TAB -->\n"
                + "    <div id=\"tab-dashboard\" class=\"tab-content\">\n"
                + "      <div class=\"grid-3\">\n"
                + "        <div class=\"stat-card\"><div>Total Customers</div><div class=\"val\" id=\"stat-customers\">0</div></div>\n"
                + "        <div class=\"stat-card\"><div>Registered Vehicles</div><div class=\"val\" id=\"stat-vehicles\">0</div></div>\n"
                + "        <div class=\"stat-card\"><div>Total Bookings</div><div class=\"val\" id=\"stat-bookings\">0</div></div>\n"
                + "      </div>\n"
                + "      <div class=\"card\" style=\"margin-top:24px;\">\n"
                + "        <h3>Recent Service Bookings</h3>\n"
                + "        <table id=\"recent-bookings-table\">\n"
                + "          <thead><tr><th>ID</th><th>Vehicle ID</th><th>Date</th><th>Status</th><th>Total</th></tr></thead>\n"
                + "          <tbody></tbody>\n"
                + "        </table>\n"
                + "      </div>\n"
                + "    </div>\n"
                + "\n"
                + "    <!-- CUSTOMERS TAB -->\n"
                + "    <div id=\"tab-customers\" class=\"tab-content\" style=\"display:none;\">\n"
                + "      <div class=\"card\">\n"
                + "        <div style=\"display:flex; justify-content:space-between; align-items:center;\">\n"
                + "          <h3>Customer Records</h3>\n"
                + "          <button class=\"btn\" onclick=\"openCustomerModal()\">+ Add Customer</button>\n"
                + "        </div>\n"
                + "        <table id=\"customers-table\">\n"
                + "          <thead><tr><th>ID</th><th>Name</th><th>Phone</th><th>Email</th><th>Address</th><th>Actions</th></tr></thead>\n"
                + "          <tbody></tbody>\n"
                + "        </table>\n"
                + "      </div>\n"
                + "    </div>\n"
                + "\n"
                + "    <!-- VEHICLES TAB -->\n"
                + "    <div id=\"tab-vehicles\" class=\"tab-content\" style=\"display:none;\">\n"
                + "      <div class=\"card\">\n"
                + "        <div style=\"display:flex; justify-content:space-between; align-items:center;\">\n"
                + "          <h3>Vehicle Management</h3>\n"
                + "          <button class=\"btn\" onclick=\"openVehicleModal()\">+ Add Vehicle</button>\n"
                + "        </div>\n"
                + "        <table id=\"vehicles-table\">\n"
                + "          <thead><tr><th>ID</th><th>Customer ID</th><th>Make / Model</th><th>Year</th><th>License Plate</th><th>VIN</th><th>Actions</th></tr></thead>\n"
                + "          <tbody></tbody>\n"
                + "        </table>\n"
                + "      </div>\n"
                + "    </div>\n"
                + "\n"
                + "    <!-- BOOKINGS TAB -->\n"
                + "    <div id=\"tab-bookings\" class=\"tab-content\" style=\"display:none;\">\n"
                + "      <div class=\"card\">\n"
                + "        <div style=\"display:flex; justify-content:space-between; align-items:center;\">\n"
                + "          <h3>Service Bookings</h3>\n"
                + "          <button class=\"btn\" onclick=\"openBookingModal()\">+ New Booking</button>\n"
                + "        </div>\n"
                + "        <table id=\"bookings-table\">\n"
                + "          <thead><tr><th>ID</th><th>Vehicle ID</th><th>Date</th><th>Status</th><th>Odometer</th><th>Technician</th><th>Total</th><th>Actions</th></tr></thead>\n"
                + "          <tbody></tbody>\n"
                + "        </table>\n"
                + "      </div>\n"
                + "    </div>\n"
                + "\n"
                + "    <!-- HISTORY TAB -->\n"
                + "    <div id=\"tab-history\" class=\"tab-content\" style=\"display:none;\">\n"
                + "      <div class=\"card\">\n"
                + "        <h3>Service History</h3>\n"
                + "        <table id=\"history-table\">\n"
                + "          <thead><tr><th>Date</th><th>Customer</th><th>Vehicle</th><th>Plate</th><th>Service Rendered</th><th>Cost</th><th>Technician</th></tr></thead>\n"
                + "          <tbody></tbody>\n"
                + "        </table>\n"
                + "      </div>\n"
                + "    </div>\n"
                + "\n"
                + "    <!-- INVOICES TAB -->\n"
                + "    <div id=\"tab-invoices\" class=\"tab-content\" style=\"display:none;\">\n"
                + "      <div class=\"card\">\n"
                + "        <h3>Generated PDF Invoices</h3>\n"
                + "        <table id=\"invoices-table\">\n"
                + "          <thead><tr><th>Invoice No</th><th>Booking ID</th><th>Total</th><th>Action</th></tr></thead>\n"
                + "          <tbody></tbody>\n"
                + "        </table>\n"
                + "      </div>\n"
                + "    </div>\n"
                + "\n"
                + "    <!-- DATABASE TAB -->\n"
                + "    <div id=\"tab-db\" class=\"tab-content\" style=\"display:none;\">\n"
                + "      <div class=\"card\">\n"
                + "        <h3>Database Setup & Connection</h3>\n"
                + "        <p style=\"color:var(--text-muted); margin: 12px 0;\">Initialize database schema, tables, and seed service catalog with 1-click setup.</p>\n"
                + "        <button class=\"btn\" onclick=\"runSetup()\">⚡ Run 1-Click Database Setup</button>\n"
                + "        <div id=\"setup-log\" style=\"margin-top: 20px; font-family: monospace; background: rgba(0,0,0,0.5); padding: 12px; border-radius: 6px; display: none;\"></div>\n"
                + "      </div>\n"
                + "    </div>\n"
                + "  </main>\n"
                + "\n"
                + "  <!-- MODALS -->\n"
                + "  <div id=\"customer-modal\" class=\"modal-overlay\">\n"
                + "    <div class=\"modal\">\n"
                + "      <div class=\"modal-header\"><h3>Add Customer</h3><button onclick=\"closeModals()\" style=\"background:none;border:none;color:white;cursor:pointer;\">✕</button></div>\n"
                + "      <div class=\"form-group\"><label>Full Name</label><input id=\"c-name\"></div>\n"
                + "      <div class=\"form-group\"><label>Phone</label><input id=\"c-phone\"></div>\n"
                + "      <div class=\"form-group\"><label>Email</label><input id=\"c-email\"></div>\n"
                + "      <div class=\"form-group\"><label>Address</label><input id=\"c-address\"></div>\n"
                + "      <button class=\"btn\" style=\"width:100%;\" onclick=\"saveCustomer()\">Save Customer</button>\n"
                + "    </div>\n"
                + "  </div>\n"
                + "\n"
                + "  <div id=\"vehicle-modal\" class=\"modal-overlay\">\n"
                + "    <div class=\"modal\">\n"
                + "      <div class=\"modal-header\"><h3>Add Vehicle</h3><button onclick=\"closeModals()\" style=\"background:none;border:none;color:white;cursor:pointer;\">✕</button></div>\n"
                + "      <div class=\"form-group\"><label>Customer ID</label><input id=\"v-cust-id\" type=\"number\"></div>\n"
                + "      <div class=\"form-group\"><label>Make</label><input id=\"v-make\" placeholder=\"e.g. Honda\"></div>\n"
                + "      <div class=\"form-group\"><label>Model</label><input id=\"v-model\" placeholder=\"e.g. City\"></div>\n"
                + "      <div class=\"form-group\"><label>Year</label><input id=\"v-year\" type=\"number\" value=\"2022\"></div>\n"
                + "      <div class=\"form-group\"><label>License Plate</label><input id=\"v-plate\" placeholder=\"e.g. KA-01-AB-1234\"></div>\n"
                + "      <div class=\"form-group\"><label>VIN</label><input id=\"v-vin\"></div>\n"
                + "      <button class=\"btn\" style=\"width:100%;\" onclick=\"saveVehicle()\">Save Vehicle</button>\n"
                + "    </div>\n"
                + "  </div>\n"
                + "\n"
                + "  <div id=\"booking-modal\" class=\"modal-overlay\">\n"
                + "    <div class=\"modal\">\n"
                + "      <div class=\"modal-header\"><h3>New Service Booking</h3><button onclick=\"closeModals()\" style=\"background:none;border:none;color:white;cursor:pointer;\">✕</button></div>\n"
                + "      <div class=\"form-group\"><label>Vehicle ID</label><input id=\"b-veh-id\" type=\"number\"></div>\n"
                + "      <div class=\"form-group\"><label>Scheduled Date</label><input id=\"b-date\" type=\"date\"></div>\n"
                + "      <div class=\"form-group\"><label>Select Services (Hold Ctrl to select multiple)</label><select id=\"b-services\" multiple style=\"height:100px;\"></select></div>\n"
                + "      <div class=\"form-group\"><label>Notes</label><input id=\"b-notes\"></div>\n"
                + "      <button class=\"btn\" style=\"width:100%;\" onclick=\"saveBooking()\">Create Booking</button>\n"
                + "    </div>\n"
                + "  </div>\n"
                + "\n"
                + "  <script>\n"
                + "    const API = 'http://localhost:8080/api';\n"
                + "    checkStatus();\n"
                + "    loadDashboard();\n"
                + "\n"
                + "    async function checkStatus() {\n"
                + "      try {\n"
                + "        const res = await fetch(API + '/status');\n"
                + "        const data = await res.json();\n"
                + "        const badge = document.getElementById('db-badge');\n"
                + "        if (data.connected) {\n"
                + "          badge.className = 'badge connected';\n"
                + "          badge.innerText = 'Connected: ' + data.database;\n"
                + "        } else {\n"
                + "          badge.className = 'badge disconnected';\n"
                + "          badge.innerText = 'DB Offline (Click Database to setup)';\n"
                + "        }\n"
                + "      } catch (e) {\n"
                + "        document.getElementById('db-badge').innerText = 'Server Error';\n"
                + "      }\n"
                + "    }\n"
                + "\n"
                + "    function showTab(name) {\n"
                + "      document.querySelectorAll('.tab-content').forEach(el => el.style.display = 'none');\n"
                + "      document.querySelectorAll('nav button').forEach(el => el.classList.remove('active'));\n"
                + "      document.getElementById('tab-' + name).style.display = 'block';\n"
                + "      document.getElementById('page-title').innerText = name.toUpperCase();\n"
                + "      if (name === 'dashboard') loadDashboard();\n"
                + "      if (name === 'customers') loadCustomers();\n"
                + "      if (name === 'vehicles') loadVehicles();\n"
                + "      if (name === 'bookings') loadBookings();\n"
                + "      if (name === 'history') loadHistory();\n"
                + "      if (name === 'invoices') loadInvoices();\n"
                + "    }\n"
                + "\n"
                + "    async function loadDashboard() {\n"
                + "      checkStatus();\n"
                + "      const [c, v, b] = await Promise.all([\n"
                + "        fetch(API + '/customers').then(r => r.json()).catch(()=>[]),\n"
                + "        fetch(API + '/vehicles').then(r => r.json()).catch(()=>[]),\n"
                + "        fetch(API + '/bookings').then(r => r.json()).catch(()=>[])\n"
                + "      ]);\n"
                + "      document.getElementById('stat-customers').innerText = c.length || 0;\n"
                + "      document.getElementById('stat-vehicles').innerText = v.length || 0;\n"
                + "      document.getElementById('stat-bookings').innerText = b.length || 0;\n"
                + "      renderBookingsTable('recent-bookings-table', (b || []).slice(0, 5));\n"
                + "    }\n"
                + "\n"
                + "    async function loadCustomers() {\n"
                + "      const res = await fetch(API + '/customers');\n"
                + "      const data = await res.json();\n"
                + "      const tbody = document.querySelector('#customers-table tbody');\n"
                + "      tbody.innerHTML = data.map(c => `<tr><td>${c.id}</td><td>${c.name}</td><td>${c.phone}</td><td>${c.email||'-'}</td><td>${c.address||'-'}</td><td><button class=\"btn btn-sm btn-outline\" onclick=\"deleteCustomer(${c.id})\">Delete</button></td></tr>`).join('');\n"
                + "    }\n"
                + "\n"
                + "    async function loadVehicles() {\n"
                + "      const res = await fetch(API + '/vehicles');\n"
                + "      const data = await res.json();\n"
                + "      const tbody = document.querySelector('#vehicles-table tbody');\n"
                + "      tbody.innerHTML = data.map(v => `<tr><td>${v.id}</td><td>${v.customerId}</td><td>${v.make} ${v.model}</td><td>${v.year}</td><td>${v.licensePlate}</td><td>${v.vin||'-'}</td><td><button class=\"btn btn-sm btn-outline\" onclick=\"deleteVehicle(${v.id})\">Delete</button></td></tr>`).join('');\n"
                + "    }\n"
                + "\n"
                + "    async function loadBookings() {\n"
                + "      const res = await fetch(API + '/bookings');\n"
                + "      const data = await res.json();\n"
                + "      renderBookingsTable('bookings-table', data, true);\n"
                + "    }\n"
                + "\n"
                + "    function renderBookingsTable(id, data, actions=false) {\n"
                + "      const tbody = document.querySelector('#' + id + ' tbody');\n"
                + "      tbody.innerHTML = data.map(b => {\n"
                + "        let act = '';\n"
                + "        if (actions) {\n"
                + "          if (b.status === 'BOOKED') act = `<button class=\"btn btn-sm\" onclick=\"startService(${b.id})\">Start</button>`;\n"
                + "          if (b.status === 'IN_PROGRESS') act = `<button class=\"btn btn-sm\" onclick=\"completeService(${b.id})\">Complete</button>`;\n"
                + "          if (b.status === 'COMPLETED') act = `<button class=\"btn btn-sm btn-outline\" onclick=\"generateInvoice(${b.id})\">Generate Invoice</button>`;\n"
                + "        }\n"
                + "        return `<tr><td>B-${b.id}</td><td>V-${b.vehicleId}</td><td>${b.scheduledDate}</td><td><span class=\"status-tag status-${b.status}\">${b.status}</span></td>${actions ? `<td>${b.odometer||'-'}</td><td>${b.technician||'-'}</td>`:''}<td>₹${b.total.toFixed(2)}</td>${actions ? `<td>${act}</td>`:''}</tr>`;\n"
                + "      }).join('');\n"
                + "    }\n"
                + "\n"
                + "    async function loadHistory() {\n"
                + "      const res = await fetch(API + '/history');\n"
                + "      const data = await res.json();\n"
                + "      const tbody = document.querySelector('#history-table tbody');\n"
                + "      tbody.innerHTML = data.map(h => `<tr><td>${h.serviceDate}</td><td>${h.customerName}</td><td>${h.vehicleName}</td><td>${h.licensePlate}</td><td>${h.serviceName}</td><td>₹${h.cost.toFixed(2)}</td><td>${h.technician||'-'}</td></tr>`).join('');\n"
                + "    }\n"
                + "\n"
                + "    async function loadInvoices() {\n"
                + "      const res = await fetch(API + '/invoices');\n"
                + "      const data = await res.json();\n"
                + "      const tbody = document.querySelector('#invoices-table tbody');\n"
                + "      tbody.innerHTML = data.map(i => `<tr><td>${i.invoiceNumber}</td><td>B-${i.bookingId}</td><td>₹${i.total.toFixed(2)}</td><td><a href=\"/${i.pdfPath}\" target=\"_blank\" class=\"btn btn-sm btn-outline\">PDF Preview</a></td></tr>`).join('');\n"
                + "    }\n"
                + "\n"
                + "    async function runSetup() {\n"
                + "      const log = document.getElementById('setup-log');\n"
                + "      log.style.display = 'block'; log.innerText = 'Running setup...';\n"
                + "      const res = await fetch(API + '/setup', { method: 'POST' });\n"
                + "      const data = await res.json();\n"
                + "      if (data.success) { log.innerText = '✅ Setup complete! Database and seed data initialized.'; checkStatus(); }\n"
                + "      else { log.innerText = '❌ Setup failed. Check MySQL running state.'; }\n"
                + "    }\n"
                + "\n"
                + "    function closeModals() { document.querySelectorAll('.modal-overlay').forEach(m => m.style.display = 'none'); }\n"
                + "    function openCustomerModal() { document.getElementById('customer-modal').style.display = 'flex'; }\n"
                + "    function openVehicleModal() { document.getElementById('vehicle-modal').style.display = 'flex'; }\n"
                + "    async function openBookingModal() {\n"
                + "      const res = await fetch(API + '/servicetypes');\n"
                + "      const types = await res.json();\n"
                + "      const sel = document.getElementById('b-services');\n"
                + "      sel.innerHTML = types.map(t => `<option value=\"${t.id}\">${t.name} (₹${t.baseCost})</option>`).join('');\n"
                + "      document.getElementById('b-date').valueAsDate = new Date();\n"
                + "      document.getElementById('booking-modal').style.display = 'flex';\n"
                + "    }\n"
                + "\n"
                + "    async function saveCustomer() {\n"
                + "      await fetch(API + '/customers', { method: 'POST', body: JSON.stringify({\n"
                + "        name: document.getElementById('c-name').value,\n"
                + "        phone: document.getElementById('c-phone').value,\n"
                + "        email: document.getElementById('c-email').value,\n"
                + "        address: document.getElementById('c-address').value\n"
                + "      })}); closeModals(); loadCustomers();\n"
                + "    }\n"
                + "    async function saveVehicle() {\n"
                + "      await fetch(API + '/vehicles', { method: 'POST', body: JSON.stringify({\n"
                + "        customerId: document.getElementById('v-cust-id').value,\n"
                + "        make: document.getElementById('v-make').value,\n"
                + "        model: document.getElementById('v-model').value,\n"
                + "        year: document.getElementById('v-year').value,\n"
                + "        licensePlate: document.getElementById('v-plate').value,\n"
                + "        vin: document.getElementById('v-vin').value\n"
                + "      })}); closeModals(); loadVehicles();\n"
                + "    }\n"
                + "    async function saveBooking() {\n"
                + "      const sel = Array.from(document.getElementById('b-services').selectedOptions).map(o => o.value).join(',');\n"
                + "      await fetch(API + '/bookings', { method: 'POST', body: JSON.stringify({\n"
                + "        vehicleId: document.getElementById('b-veh-id').value,\n"
                + "        scheduledDate: document.getElementById('b-date').value,\n"
                + "        serviceTypeIds: sel,\n"
                + "        notes: document.getElementById('b-notes').value\n"
                + "      })}); closeModals(); loadBookings();\n"
                + "    }\n"
                + "    async function startService(id) {\n"
                + "      await fetch(API + '/bookings', { method: 'PUT', body: JSON.stringify({ id: id, action: 'start' })});\n"
                + "      loadBookings();\n"
                + "    }\n"
                + "    async function completeService(id) {\n"
                + "      const odo = prompt('Enter final Odometer reading (km):', '15000');\n"
                + "      const tech = prompt('Enter Technician Name:', 'Alex Master Tech');\n"
                + "      if (odo && tech) {\n"
                + "        await fetch(API + '/bookings', { method: 'PUT', body: JSON.stringify({ id: id, action: 'complete', odometer: odo, technician: tech })});\n"
                + "        loadBookings();\n"
                + "      }\n"
                + "    }\n"
                + "    async function generateInvoice(id) {\n"
                + "      const res = await fetch(API + '/invoices', { method: 'POST', body: JSON.stringify({ bookingId: id })});\n"
                + "      const data = await res.json();\n"
                + "      alert('Invoice generated: ' + data.invoiceNumber);\n"
                + "      showTab('invoices');\n"
                + "    }\n"
                + "    async function deleteCustomer(id) { if (confirm('Delete customer?')) { await fetch(API + '/customers?id=' + id, { method: 'DELETE' }); loadCustomers(); } }\n"
                + "    async function deleteVehicle(id) { if (confirm('Delete vehicle?')) { await fetch(API + '/vehicles?id=' + id, { method: 'DELETE' }); loadVehicles(); } }\n"
                + "  </script>\n"
                + "</body>\n"
                + "</html>";
    }
}
