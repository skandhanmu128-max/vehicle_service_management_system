package vsms.ui;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import vsms.db.DBConnection;
import vsms.db.DatabaseSetup;
import vsms.model.Booking;
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

public class Menu {

    private final Scanner sc = new Scanner(System.in);

    private final CustomerService customerService = new CustomerService();
    private final VehicleService vehicleService = new VehicleService();
    private final BookingService bookingService = new BookingService();
    private final ServiceHistoryService historyService = new ServiceHistoryService();
    private final InvoiceService invoiceService = new InvoiceService();

    public void run() {
        banner();
        boolean exit = false;
        while (!exit) {
            System.out.println();
            System.out.println("================ MAIN MENU ================");
            System.out.println("1. Customer Management");
            System.out.println("2. Vehicle Records");
            System.out.println("3. Service Booking");
            System.out.println("4. Service History");
            System.out.println("5. Invoice Generation");
            System.out.println("6. Database Setup / Connection Test");
            System.out.println("0. Exit");
            System.out.println("===========================================");
            String choice = ConsoleUtils.readLine(sc, "Select an option : ", true);
            switch (choice) {
                case "1":
                    customerMenu();
                    break;
                case "2":
                    vehicleMenu();
                    break;
                case "3":
                    bookingMenu();
                    break;
                case "4":
                    serviceHistoryMenu();
                    break;
                case "5":
                    invoiceMenu();
                    break;
                case "6":
                    databaseMenu();
                    break;
                case "0":
                    System.out.println("Goodbye!");
                    exit = true;
                    break;
                default:
                    System.out.println("  >> Invalid option: " + choice);
            }
        }
    }

    private void banner() {
        System.out.println("========================================================");
        System.out.println("        V E H I C L E   S E R V I C E   M A N A G E M E N T");
        System.out.println("========================================================");
        System.out.println("  Customer Records | Vehicle | Booking | History | Invoice");
        System.out.println("========================================================");
    }

    // ---------------------------------------------------------------
    // 1. CUSTOMER MANAGEMENT
    // ---------------------------------------------------------------

    private void customerMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("----- CUSTOMER MANAGEMENT -----");
            System.out.println("1. Register new customer");
            System.out.println("2. View customer profile");
            System.out.println("3. Find customer by phone");
            System.out.println("4. List all customers");
            System.out.println("5. Update customer");
            System.out.println("6. Delete customer");
            System.out.println("0. Back to main menu");
            String choice = ConsoleUtils.readLine(sc, "Select an option : ", true);
            switch (choice) {
                case "1":
                    registerCustomer();
                    break;
                case "2":
                    viewCustomer();
                    break;
                case "3":
                    findCustomerByPhone();
                    break;
                case "4":
                    listCustomers();
                    break;
                case "5":
                    updateCustomer();
                    break;
                case "6":
                    deleteCustomer();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("  >> Invalid option: " + choice);
            }
        }
    }

    private void registerCustomer() {
        System.out.println("\n--- Register New Customer ---");
        String name = ConsoleUtils.readLine(sc, "Full name : ", true);
        String phone = ConsoleUtils.readLine(sc, "Phone     : ", true);
        String email = ConsoleUtils.readOptional(sc, "Email     : ");
        String address = ConsoleUtils.readOptional(sc, "Address   : ");
        try {
            Customer c = customerService.register(name, phone, email, address);
            System.out.println("  >> Customer registered! ID = " + c.getId());
        } catch (IllegalArgumentException | SQLException e) {
            System.out.println("  >> Failed: " + e.getMessage());
        }
        ConsoleUtils.pause(sc);
    }

    private void viewCustomer() {
        Integer id = ConsoleUtils.readOptionalInt(sc, "\nCustomer ID : ");
        if (id == null) {
            System.out.println("  >> No ID entered, cancelled.");
            return;
        }
        try {
            Customer c = customerService.findById(id);
            if (c == null) {
                System.out.println("  >> Customer not found.");
            } else {
                printCustomer(c);
                List<Vehicle> vehicles = customerService.getVehicles(id);
                System.out.println("\n  Registered vehicles: " + vehicles.size());
                if (!vehicles.isEmpty()) {
                    printVehicles(vehicles, true);
                }
            }
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    private void findCustomerByPhone() {
        String phone = ConsoleUtils.readLine(sc, "\nPhone number : ", true);
        try {
            Customer c = customerService.findByPhone(phone);
            if (c == null) {
                System.out.println("  >> No customer found with phone " + phone);
            } else {
                printCustomer(c);
                List<Vehicle> vehicles = customerService.getVehicles(c.getId());
                System.out.println("\n  Registered vehicles: " + vehicles.size());
                if (!vehicles.isEmpty()) {
                    printVehicles(vehicles, true);
                }
            }
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    private void listCustomers() {
        System.out.println();
        try {
            List<Customer> customers = customerService.findAll();
            if (customers.isEmpty()) {
                System.out.println("  >> No customers registered yet.");
            } else {
                System.out.printf("%-5s %-28s %-16s %-28s %-6s%n",
                        "ID", "NAME", "PHONE", "EMAIL", "VEHICLES");
                System.out.println("------------------------------------------------------------------");
                for (Customer c : customers) {
                    int vehicles = customerService.countVehicles(c.getId());
                    System.out.printf("%-5d %-28s %-16s %-28s %-6d%n",
                            c.getId(),
                            npe(c.getName()),
                            npe(c.getPhone()),
                            npe(c.getEmail()),
                            vehicles);
                }
            }
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    private void updateCustomer() {
        Integer id = ConsoleUtils.readOptionalInt(sc, "\nCustomer ID to update : ");
        if (id == null) {
            return;
        }
        try {
            Customer c = customerService.findById(id);
            if (c == null) {
                System.out.println("  >> Customer not found.");
                return;
            }
            System.out.println("  Leave a field blank to keep the current value.");
            String name = ConsoleUtils.readLine(sc, "Name (" + npe(c.getName()) + ") : ", false);
            if (!name.isEmpty()) {
                c.setName(name);
            }
            String phone = ConsoleUtils.readLine(sc, "Phone (" + npe(c.getPhone()) + ") : ", false);
            if (!phone.isEmpty()) {
                c.setPhone(phone);
            }
            String email = ConsoleUtils.readLine(sc, "Email (" + npe(c.getEmail()) + ") : ", false);
            c.setEmail(email.isEmpty() ? c.getEmail() : email);
            String address = ConsoleUtils.readLine(sc, "Address (" + npe(c.getAddress()) + ") : ", false);
            c.setAddress(address.isEmpty() ? c.getAddress() : address);

            customerService.update(c);
            System.out.println("  >> Customer updated.");
        } catch (IllegalArgumentException | SQLException e) {
            System.out.println("  >> Failed: " + e.getMessage());
        }
        ConsoleUtils.pause(sc);
    }

    private void deleteCustomer() {
        Integer id = ConsoleUtils.readOptionalInt(sc, "\nCustomer ID to delete : ");
        if (id == null) {
            return;
        }
        try {
            String confirm = ConsoleUtils.readLine(sc, "Delete customer " + id
                    + " and all linked vehicles/bookings (y/n) ? ", true);
            if (!"y".equalsIgnoreCase(confirm)) {
                System.out.println("  >> Deletion cancelled.");
                return;
            }
            if (customerService.delete(id)) {
                System.out.println("  >> Customer deleted.");
            } else {
                System.out.println("  >> Customer not found.");
            }
        } catch (IllegalStateException | SQLException e) {
            System.out.println("  >> Failed: " + e.getMessage());
        }
        ConsoleUtils.pause(sc);
    }

    // ---------------------------------------------------------------
    // 2. VEHICLE RECORDS
    // ---------------------------------------------------------------

    private void vehicleMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("----- VEHICLE RECORDS -----");
            System.out.println("1. Register vehicle");
            System.out.println("2. List all vehicles");
            System.out.println("3. List vehicles of a customer");
            System.out.println("4. Update vehicle");
            System.out.println("5. Delete vehicle");
            System.out.println("0. Back to main menu");
            String choice = ConsoleUtils.readLine(sc, "Select an option : ", true);
            switch (choice) {
                case "1":
                    registerVehicle();
                    break;
                case "2":
                    listVehicles();
                    break;
                case "3":
                    listVehiclesOfCustomer();
                    break;
                case "4":
                    updateVehicle();
                    break;
                case "5":
                    deleteVehicle();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("  >> Invalid option: " + choice);
            }
        }
    }

    private void registerVehicle() {
        System.out.println("\n--- Register Vehicle ---");
        Integer customerId = ConsoleUtils.readOptionalInt(sc, "Owner (customer ID) : ");
        if (customerId == null) {
            System.out.println("  >> No ID entered, cancelled.");
            return;
        }
        try {
            Customer owner = customerService.findById(customerId);
            if (owner == null) {
                System.out.println("  >> Customer not found. Register the customer first.");
                return;
            }
            printCustomer(owner);
            String make = ConsoleUtils.readLine(sc, "Make (e.g. Toyota)    : ", true);
            String model = ConsoleUtils.readLine(sc, "Model (e.g. Innova)   : ", true);
            Integer year = ConsoleUtils.readOptionalInt(sc, "Year                  : ");
            String plate = ConsoleUtils.readLine(sc, "License plate         : ", true);
            String vin = ConsoleUtils.readOptional(sc, "VIN                   : ");

            Vehicle v = vehicleService.register(customerId, make, model,
                    year == null ? 0 : year, plate, vin);
            System.out.println("  >> Vehicle registered! ID = " + v.getId());
        } catch (IllegalArgumentException | SQLException e) {
            System.out.println("  >> Failed: " + e.getMessage());
        }
        ConsoleUtils.pause(sc);
    }

    private void listVehicles() {
        System.out.println();
        try {
            List<Vehicle> vehicles = vehicleService.findAll();
            if (vehicles.isEmpty()) {
                System.out.println("  >> No vehicles registered yet.");
                return;
            }
            System.out.printf("%-6s %-6s %-30s %-16s %-6s %-22s%n",
                    "ID", "OWNER", "VEHICLE", "PLATE", "YEAR", "OWNER NAME");
            System.out.println("--------------------------------------------------------------------------------------");
            for (Vehicle v : vehicles) {
                Customer owner = vehicleService.getOwner(v.getCustomerId());
                System.out.printf("%-6d %-6d %-30s %-16s %-6d %-22s%n",
                        v.getId(),
                        v.getCustomerId(),
                        v.displayName(),
                        npe(v.getLicensePlate()),
                        v.getYear(),
                        npe(owner == null ? "" : owner.getName()));
            }
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    private void listVehiclesOfCustomer() {
        Integer customerId = ConsoleUtils.readOptionalInt(sc, "\nCustomer ID : ");
        if (customerId == null) {
            return;
        }
        try {
            Customer owner = customerService.findById(customerId);
            if (owner == null) {
                System.out.println("  >> Customer not found.");
                return;
            }
            System.out.println("\n  Vehicles of " + owner.getName() + ":");
            List<Vehicle> vehicles = vehicleService.findByCustomer(customerId);
            if (vehicles.isEmpty()) {
                System.out.println("  >> No vehicles registered for this customer.");
            } else {
                printVehicles(vehicles, false);
            }
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    private void updateVehicle() {
        Integer id = ConsoleUtils.readOptionalInt(sc, "\nVehicle ID to update : ");
        if (id == null) {
            return;
        }
        try {
            Vehicle v = vehicleService.findById(id);
            if (v == null) {
                System.out.println("  >> Vehicle not found.");
                return;
            }
            System.out.println("  Leave a field blank to keep the current value.");
            String make = ConsoleUtils.readLine(sc, "Make (" + npe(v.getMake()) + ") : ", false);
            if (!make.isEmpty()) {
                v.setMake(make);
            }
            String model = ConsoleUtils.readLine(sc, "Model (" + npe(v.getModel()) + ") : ", false);
            if (!model.isEmpty()) {
                v.setModel(model);
            }
            String year = ConsoleUtils.readLine(sc, "Year (" + (v.getYear() > 0 ? v.getYear() : "-") + ") : ", false);
            if (!year.isEmpty()) {
                v.setYear(Integer.parseInt(year));
            }
            String plate = ConsoleUtils.readLine(sc, "Plate (" + npe(v.getLicensePlate()) + ") : ", false);
            if (!plate.isEmpty()) {
                v.setLicensePlate(plate);
            }
            String vin = ConsoleUtils.readLine(sc, "VIN (" + npe(v.getVin()) + ") : ", false);
            if (!vin.isEmpty()) {
                v.setVin(vin);
            }
            vehicleService.update(v);
            System.out.println("  >> Vehicle updated.");
        } catch (IllegalArgumentException | SQLException e) {
            System.out.println("  >> Failed: " + e.getMessage());
        }
        ConsoleUtils.pause(sc);
    }

    private void deleteVehicle() {
        Integer id = ConsoleUtils.readOptionalInt(sc, "\nVehicle ID to delete : ");
        if (id == null) {
            return;
        }
        try {
            String confirm = ConsoleUtils.readLine(sc,
                    "Delete vehicle " + id + " and its bookings (y/n) ? ", true);
            if (!"y".equalsIgnoreCase(confirm)) {
                System.out.println("  >> Deletion cancelled.");
                return;
            }
            if (vehicleService.delete(id)) {
                System.out.println("  >> Vehicle deleted.");
            } else {
                System.out.println("  >> Vehicle not found.");
            }
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    // ---------------------------------------------------------------
    // 3. SERVICE BOOKING
    // ---------------------------------------------------------------

    private void bookingMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("----- SERVICE BOOKING -----");
            System.out.println("1. Create booking");
            System.out.println("2. List all bookings");
            System.out.println("3. List bookings by status");
            System.out.println("4. List bookings for a vehicle");
            System.out.println("5. Start service (IN_PROGRESS)");
            System.out.println("6. Complete service (COMPLETED)");
            System.out.println("7. Cancel booking");
            System.out.println("0. Back to main menu");
            String choice = ConsoleUtils.readLine(sc, "Select an option : ", true);
            switch (choice) {
                case "1":
                    createBooking();
                    break;
                case "2":
                    listBookings(null);
                    break;
                case "3":
                    listBookingByStatus();
                    break;
                case "4":
                    listBookingsForVehicle();
                    break;
                case "5":
                    startService();
                    break;
                case "6":
                    completeService();
                    break;
                case "7":
                    cancelBooking();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("  >> Invalid option: " + choice);
            }
        }
    }

    private void createBooking() {
        System.out.println("\n--- Create Service Booking ---");
        try {
            List<Vehicle> vehicles = vehicleService.findAll();
            if (vehicles.isEmpty()) {
                System.out.println("  >> Register a vehicle first before booking a service.");
                return;
            }
            printVehicles(vehicles, true);
            Integer vehicleId = ConsoleUtils.readOptionalInt(sc, "Vehicle ID : ");
            if (vehicleId == null) {
                System.out.println("  >> No ID entered, cancelled.");
                return;
            }
            LocalDate date = ConsoleUtils.readDate(sc, "Scheduled date");
            String notes = ConsoleUtils.readOptional(sc, "Notes : ");

            List<ServiceType> catalog = bookingService.serviceCatalog();
            System.out.println("\n  Available services:");
            System.out.printf("%-4s %-24s %-42s %s%n", "ID", "SERVICE", "DESCRIPTION", "COST");
            System.out.println("------------------------------------------------------------------------------");
            for (ServiceType st : catalog) {
                System.out.printf("%-4d %-24s %-42s Rs. %s%n",
                        st.getId(), st.getName(), npe(st.getDescription()), st.getBaseCost());
            }
            String ids = ConsoleUtils.readLine(sc, "\nService IDs (comma separated, e.g. 1,3,5) : ", true);
            List<Integer> selected = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .distinct()
                    .collect(Collectors.toList());
            if (selected.isEmpty()) {
                System.out.println("  >> At least one service is required.");
                return;
            }

            Booking b = bookingService.createBooking(vehicleId, date, notes, selected);
            System.out.println("  >> Booking created! ID = " + b.getId()
                    + " | Date = " + b.getScheduledDate()
                    + " | Total = Rs. " + bookingService.calculateTotal(b));
        } catch (IllegalArgumentException | SQLException e) {
            System.out.println("  >> Failed: " + e.getMessage());
        }
        ConsoleUtils.pause(sc);
    }

    private void listAllBookings(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            System.out.println("  >> No bookings found.");
            return;
        }
        System.out.printf("%-5s %-8s %-12s %-14s %-10s %-12s %-10s%n",
                "ID", "VEH#", "DATE", "STATUS", "ODOMETER", "TECHNICIAN", "TOTAL");
        System.out.println("----------------------------------------------------------------------------------------");
        for (Booking b : bookings) {
            System.out.printf("%-5d %-8s %-12s %-14s %-10s %-12s Rs. %s%n",
                    b.getId(),
                    "#" + b.getVehicleId(),
                    b.getScheduledDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")),
                    b.getStatus(),
                    b.getOdometer() == null ? "-" : b.getOdometer() + " km",
                    npe(b.getTechnician()),
                    bookingService.calculateTotal(b));
        }
    }

    private void listBookings(String status) {
        System.out.println();
        try {
            List<Booking> bookings = (status == null)
                    ? bookingService.findAll()
                    : bookingService.findByStatus(status);
            listAllBookings(bookings);
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    private void listBookingByStatus() {
        System.out.println("\nStatus: 1=BOOKED  2=IN_PROGRESS  3=COMPLETED  4=CANCELLED");
        Integer code = ConsoleUtils.readOptionalInt(sc, "Select status code : ");
        if (code == null) {
            return;
        }
        String[] statuses = {Booking.STATUS_BOOKED, Booking.STATUS_IN_PROGRESS,
                Booking.STATUS_COMPLETED, Booking.STATUS_CANCELLED};
        if (code < 1 || code > 4) {
            System.out.println("  >> Invalid status code.");
            return;
        }
        listBookings(statuses[code - 1]);
    }

    private void listBookingsForVehicle() {
        Integer vehicleId = ConsoleUtils.readOptionalInt(sc, "\nVehicle ID : ");
        if (vehicleId == null) {
            return;
        }
        try {
            List<Booking> bookings = bookingService.findByVehicle(vehicleId);
            System.out.println();
            listAllBookings(bookings);
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    private void startService() {
        System.out.println();
        try {
            List<Booking> booked = bookingService.findByStatus(Booking.STATUS_BOOKED);
            listAllBookings(booked);
            Integer id = ConsoleUtils.readOptionalInt(sc, "\nBooking ID to start : ");
            if (id == null) {
                return;
            }
            bookingService.startService(id);
            System.out.println("  >> Booking " + id + " is now IN_PROGRESS.");
        } catch (IllegalStateException | SQLException e) {
            System.out.println("  >> Failed: " + e.getMessage());
        }
        ConsoleUtils.pause(sc);
    }

    private void completeService() {
        System.out.println();
        try {
            List<Booking> active = bookingService.findByStatus(Booking.STATUS_IN_PROGRESS);
            listAllBookings(active);
            Integer id = ConsoleUtils.readOptionalInt(sc, "\nBooking ID to complete : ");
            if (id == null) {
                return;
            }
            Integer odometer = ConsoleUtils.readOptionalInt(sc, "Odometer reading (km) : ");
            String technician = ConsoleUtils.readOptional(sc, "Technician name : ");
            bookingService.complete(id, odometer == null ? 0 : odometer, technician);
            System.out.println("  >> Booking " + id + " marked COMPLETED.");
            System.out.println("  >> Generate the invoice from the Invoice menu.");
        } catch (IllegalStateException | SQLException e) {
            System.out.println("  >> Failed: " + e.getMessage());
        }
        ConsoleUtils.pause(sc);
    }

    private void cancelBooking() {
        System.out.println();
        try {
            List<Booking> active = new ArrayList<>();
            active.addAll(bookingService.findByStatus(Booking.STATUS_BOOKED));
            active.addAll(bookingService.findByStatus(Booking.STATUS_IN_PROGRESS));
            listAllBookings(active);
            Integer id = ConsoleUtils.readOptionalInt(sc, "\nBooking ID to cancel : ");
            if (id == null) {
                return;
            }
            bookingService.cancel(id);
            System.out.println("  >> Booking " + id + " cancelled.");
        } catch (IllegalStateException | SQLException e) {
            System.out.println("  >> Failed: " + e.getMessage());
        }
        ConsoleUtils.pause(sc);
    }

    // ---------------------------------------------------------------
    // 4. SERVICE HISTORY
    // ---------------------------------------------------------------

    private void serviceHistoryMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("----- SERVICE HISTORY -----");
            System.out.println("1. View complete history");
            System.out.println("2. History by customer");
            System.out.println("3. History by vehicle");
            System.out.println("4. History by license plate");
            System.out.println("0. Back to main menu");
            String choice = ConsoleUtils.readLine(sc, "Select an option : ", true);
            switch (choice) {
                case "1":
                    showHistory();
                    break;
                case "2":
                    historyByCustomer();
                    break;
                case "3":
                    historyByVehicle();
                    break;
                case "4":
                    historyByPlate();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("  >> Invalid option: " + choice);
            }
        }
    }

    private void showHistory() {
        System.out.println();
        try {
            printHistory(historyService.allHistory());
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    private void historyByCustomer() {
        Integer id = ConsoleUtils.readOptionalInt(sc, "\nCustomer ID : ");
        if (id == null) {
            return;
        }
        System.out.println();
        try {
            printHistory(historyService.historyByCustomer(id));
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    private void historyByVehicle() {
        Integer id = ConsoleUtils.readOptionalInt(sc, "\nVehicle ID : ");
        if (id == null) {
            return;
        }
        System.out.println();
        try {
            printHistory(historyService.historyByVehicle(id));
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    private void historyByPlate() {
        String plate = ConsoleUtils.readLine(sc, "\nLicense plate : ", true);
        System.out.println();
        try {
            printHistory(historyService.historyByPlate(plate));
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    private void printHistory(List<ServiceHistoryEntry> entries) {
        if (entries.isEmpty()) {
            System.out.println("  >> No service history found.");
            return;
        }
        System.out.printf("%-5s %-12s %-10s %-26s %-14s %-20s %-9s%n",
                "BK#", "DATE", "STATUS", "CUSTOMER", "VEHICLE", "SERVICE", "AMOUNT");
        System.out.println("-----------------------------------------------------------------------------------------------");
        for (ServiceHistoryEntry e : entries) {
            System.out.printf("%-5d %-12s %-10s %-26s %-14s %-20s Rs. %s%n",
                    e.getBookingId(),
                    e.getServiceDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")),
                    e.getStatus(),
                    truncate(e.getCustomerName(), 26),
                    e.getLicensePlate(),
                    truncate(e.getServiceName(), 20),
                    e.getCost());
        }
        System.out.println("-----------------------------------------------------------------------------------------------");
        System.out.println("  TOTAL SPENT : Rs. " + historyService.totalSpent(entries));
    }

    // ---------------------------------------------------------------
    // 5. INVOICES
    // ---------------------------------------------------------------

    private void invoiceMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("----- INVOICE GENERATION -----");
            System.out.println("1. Generate invoice (PDF) for a completed booking");
            System.out.println("2. List all invoices");
            System.out.println("3. View / open an invoice");
            System.out.println("0. Back to main menu");
            String choice = ConsoleUtils.readLine(sc, "Select an option : ", true);
            switch (choice) {
                case "1":
                    generateInvoice();
                    break;
                case "2":
                    listInvoices();
                    break;
                case "3":
                    viewInvoice();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("  >> Invalid option: " + choice);
            }
        }
    }

    private void generateInvoice() {
        System.out.println("\n--- Generate Invoice ---");
        try {
            List<Booking> completed = bookingService.findByStatus(Booking.STATUS_COMPLETED)
                    .stream()
                    .filter(b -> {
                        try {
                            return !bookingService.hasInvoice(b.getId());
                        } catch (SQLException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            if (completed.isEmpty()) {
                System.out.println("  >> No completed bookings waiting for an invoice.");
                return;
            }
            listAllBookings(completed);
            Integer bookingId = ConsoleUtils.readOptionalInt(sc, "\nBooking ID : ");
            if (bookingId == null) {
                return;
            }
            Invoice invoice = invoiceService.generateInvoice(bookingId);
            System.out.println("  >> Invoice generated!");
            System.out.println("     Number : " + invoice.getInvoiceNumber());
            System.out.println("     Total  : Rs. " + invoice.getTotal());
            System.out.println("     Path   : " + invoice.getPdfPath());
            if (invoiceService.openPdf(invoice)) {
                System.out.println("     PDF opened in the default viewer.");
            }
        } catch (IllegalStateException | SQLException | java.io.IOException e) {
            System.out.println("  >> Failed: " + e.getMessage());
        }
        ConsoleUtils.pause(sc);
    }

    private void listInvoices() {
        System.out.println();
        try {
            List<Invoice> invoices = invoiceService.findAll();
            if (invoices.isEmpty()) {
                System.out.println("  >> No invoices generated yet.");
            } else {
                System.out.printf("%-12s %-8s %-12s %-12s %s%n",
                        "NUMBER", "BK#", "TOTAL", "DATE", "PATH");
                System.out.println("--------------------------------------------------------------------------");
                for (Invoice inv : invoices) {
                    System.out.printf("%-12s %-8d Rs.%-9s %-12s %s%n",
                            inv.getInvoiceNumber(),
                            inv.getBookingId(),
                            inv.getTotal(),
                            inv.getGeneratedAt().toLocalDate(),
                            npe(inv.getPdfPath()));
                }
            }
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    private void viewInvoice() {
        String number = ConsoleUtils.readLine(sc, "\nInvoice number (e.g. INV-0001) : ", true);
        try {
            Invoice invoice = invoiceService.findByNumber(number);
            if (invoice == null) {
                System.out.println("  >> Invoice not found.");
                return;
            }
            System.out.println("  " + invoice);
            if (invoiceService.openPdf(invoice)) {
                System.out.println("  >> PDF opened in the default viewer.");
            } else {
                System.out.println("  >> PDF file not found on disk: " + invoice.getPdfPath());
            }
        } catch (SQLException e) {
            sqlError(e);
        }
        ConsoleUtils.pause(sc);
    }

    // ---------------------------------------------------------------
    // 6. DATABASE
    // ---------------------------------------------------------------

    private void databaseMenu() {
        System.out.println();
        System.out.println("----- DATABASE SETUP / CONNECTION TEST -----");
        if (DatabaseSetup.setupDatabase()) {
            System.out.println("[test] Testing main connection ...");
            try (java.sql.Connection conn = DBConnection.getConnection()) {
                System.out.println("[test] Connected to " + DBConnection.databaseName()
                        + " using " + conn.getMetaData().getDatabaseProductName());
            } catch (SQLException e) {
                System.out.println("[test] FAILED: " + e.getMessage());
            }
        }
        ConsoleUtils.pause(sc);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void printCustomer(Customer c) {
        System.out.println("------------------------------------------");
        System.out.println("  Customer ID  : " + c.getId());
        System.out.println("  Name         : " + npe(c.getName()));
        System.out.println("  Phone        : " + npe(c.getPhone()));
        System.out.println("  Email        : " + npe(c.getEmail()));
        System.out.println("  Address      : " + npe(c.getAddress()));
        System.out.println("------------------------------------------");
    }

    private void printVehicles(List<Vehicle> vehicles, boolean header) {
        if (header) {
            System.out.printf("%-6s %-6s %-32s %-16s %-6s%n", "ID", "OWNER", "VEHICLE", "PLATE", "YEAR");
            System.out.println("-----------------------------------------------------------------");
        }
        for (Vehicle v : vehicles) {
            System.out.printf("%-6d %-6d %-32s %-16s %-6d%n",
                    v.getId(), v.getCustomerId(), v.displayName(),
                    npe(v.getLicensePlate()), v.getYear());
        }
    }

    private void sqlError(SQLException e) {
        System.out.println("  >> Database error: " + e.getMessage());
        System.out.println("     Run option 6 (Database Setup) to check the connection.");
    }

    private static String npe(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
