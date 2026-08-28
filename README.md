# Vehicle Service Management System

A console-based **Java + JDBC + MySQL** application for managing vehicle servicing, customer
records, service bookings, service history and PDF invoice generation.

## Features

| Module | What it does |
| --- | --- |
| Customer Management | Register customers, view profile, search by phone, update, delete (cascades to vehicles/bookings) |
| Vehicle Records | Register vehicles under a customer, list, search by customer, update, delete |
| Service Booking | Create bookings with multiple services from a catalog, start / complete / cancel, status workflow `BOOKED -> IN_PROGRESS -> COMPLETED / CANCELLED` |
| Service History | Track completed + in-progress work by customer, vehicle or license plate via a SQL view |
| Invoice Generation | Auto-generates a **PDF invoice** (pure Java, no external PDF library) for completed bookings and stores the record in the DB |

## Tech Stack

- Java 8+ (JDK required)
- MySQL 5.7 / 8.x
- MySQL Connector/J 8.x (vendored in `lib/`)
- No external PDF library — the invoice PDF is generated with a small hand-written PDF writer

## Project Structure

```
├── src/vsms/
│   ├── Main.java              # entry point
│   ├── model/                 # Customer, Vehicle, ServiceType, Booking, BookingItem, Invoice
│   ├── dao/                   # JDBC data access objects
│   ├── service/               # business logic layer
│   ├── db/                    # DBConnection + one-time DatabaseSetup (DDL + seed)
│   ├── pdf/                   # PdfInvoiceGenerator (dependency-free PDF writer)
│   └── ui/                    # console menus + input helpers
├── database/schema.sql        # full database schema + seed data
├── lib/                       # MySQL Connector/J jar
├── db.properties              # connection settings (host/port/name/user/password)
├── compile.bat / run.bat      # build & run scripts (Windows)
└── invoices/                  # generated PDF invoices (git-ignored)
```

## Prerequisites

1. **JDK 8 or newer** — check with `java -version`
2. **MySQL server** running locally (or a remote host you can reach)

## Setup — 3 steps

### 1. Configure the database connection

Edit `db.properties` in the project root:

```properties
db.host=localhost
db.port=3306
db.name=vehicle_service_mgmt
db.user=root
db.password=your_password_here
```

### 2. Create the database (choose one)

Option A — from the app (easiest): start the app and choose menu option
**6. Database Setup**. It creates the database, all tables and seeds the service
catalog automatically.

Option B — from SQL file:

```bash
mysql -u root -p < database/schema.sql
```

### 3. Build & run

```
compile.bat
run.bat
```

or manually:

```
javac -encoding UTF-8 -cp "lib/*" -d out src/vsms/**/*.java
java -cp "out;lib/*;." vsms.Main
```

## Typical workflow

1. **Customer Management → Register new customer** (note the generated ID)
2. **Vehicle Records → Register vehicle** (owner = that customer ID)
3. **Service Booking → Create booking** (pick vehicle, date, services from catalog)
4. **Service Booking → Start service** then **Complete service** (add odometer/technician)
5. **Service History** — view completed work per customer / vehicle / plate
6. **Invoice Generation → Generate invoice (PDF)** — PDF is written to `invoices/` and
   opened automatically.

## Database Schema

Tables: `customer`, `vehicle`, `service_type`, `booking`, `booking_item`, `invoice`
plus the read-only view `service_history`. See `database/schema.sql` for the full DDL,
foreign keys and seed data.

## Deliverables

- Java application — this repository (`src/`, `compile.bat`, `run.bat`)
- Database schema — `database/schema.sql`
- GitHub repository — this repository

## License

For academic / learning purposes.