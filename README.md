# Vehicle Service Management System

A full-featured **Java + JDBC + MySQL** application for managing vehicle servicing, customer records, service bookings, service history tracking, and automated PDF invoice generation. Includes both a **Web Dashboard (`http://localhost:8080`)** and an **Interactive CLI Interface**.

## Features

| Module | What it does |
| --- | --- |
| **Web Dashboard** | Modern glassmorphism UI served at `http://localhost:8080` with 1-click database setup and real-time management |
| **Customer Management** | Register customers, view profile, search by phone, update, delete (cascades to vehicles/bookings) |
| **Vehicle Records** | Register vehicles under a customer, list, search by customer, update, delete |
| **Service Booking** | Create bookings with multiple services from catalog, status workflow `BOOKED -> IN_PROGRESS -> COMPLETED / CANCELLED` |
| **Service History** | Track completed + in-progress work by customer, vehicle or license plate via a SQL view |
| **Invoice Generation** | Auto-generates a **PDF invoice** (pure Java, no external PDF library) for completed bookings and stores the record in the DB |

## Tech Stack

- **Java 8+** (JDK required)
- **MySQL 5.7 / 8.x**
- **MySQL Connector/J 8.x** (vendored in `lib/`)
- **Embedded Web Server** (`com.sun.net.httpserver.HttpServer` on port `8080`)
- **Pure Java PDF Writer** — Zero external PDF library required

## Project Structure

```
├── src/vsms/
│   ├── Main.java              # Entry point (Launches Web Server + CLI)
│   ├── web/                   # WebServer (HTTP endpoints + Web Dashboard UI)
│   ├── model/                 # Customer, Vehicle, ServiceType, Booking, BookingItem, Invoice
│   ├── dao/                   # JDBC Data Access Objects
│   ├── service/               # Business logic layer
│   ├── db/                    # DBConnection + one-time DatabaseSetup (DDL + seed)
│   ├── pdf/                   # PdfInvoiceGenerator (dependency-free PDF writer)
│   └── ui/                    # Console menus + input helpers
├── database/schema.sql        # Full database schema + seed data
├── lib/                       # MySQL Connector/J jar
├── db.properties              # Connection settings (host/port/name/user/password)
├── compile.bat / run.bat      # Build & run scripts (Windows)
└── invoices/                  # Generated PDF invoices (git-ignored)
```

## Setup & Quick Start — 3 Steps

### 1. Configure the database connection

Edit `db.properties` in the project root:

```properties
db.host=localhost
db.port=3306
db.name=vehicle_service_mgmt
db.user=root
db.password=your_password_here
```

### 2. Build & Run

```cmd
compile.bat
run.bat
```

Or manually:

```bash
javac -encoding UTF-8 -cp "lib/*" -d out src/vsms/*.java src/vsms/*/*.java
java -cp "out;lib/*;." vsms.Main
```

### 3. Open Web Dashboard

Navigate to **[http://localhost:8080](http://localhost:8080)** in your web browser.

Alternatively, use option **6. Database Setup** in the console menu to initialize database tables and seed data automatically.

## License

For academic / learning purposes.