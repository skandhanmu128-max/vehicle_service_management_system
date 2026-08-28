-- ============================================================
-- Vehicle Service Management System - Database Schema
-- Copyright (c) 2026
-- Run with:  mysql -u root -p < database/schema.sql
-- Generates a fresh database named vehicle_service_mgmt.
-- ============================================================

DROP DATABASE IF EXISTS vehicle_service_mgmt;
CREATE DATABASE vehicle_service_mgmt
    DEFAULT CHARACTER SET utf8mb4;

USE vehicle_service_mgmt;

-- ------------------------------------------------------------
-- 1. CUSTOMER  |  master record of registered customers
-- ------------------------------------------------------------
CREATE TABLE customer (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    phone       VARCHAR(20)  NOT NULL UNIQUE,
    email       VARCHAR(100),
    address     VARCHAR(255),
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 2. VEHICLE   |  vehicles registered under a customer
-- ------------------------------------------------------------
CREATE TABLE vehicle (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    customer_id   INT          NOT NULL,
    make          VARCHAR(50)  NOT NULL,
    model         VARCHAR(50)  NOT NULL,
    year          INT,
    license_plate VARCHAR(20)  NOT NULL UNIQUE,
    vin           VARCHAR(50),
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_customer FOREIGN KEY (customer_id)
        REFERENCES customer(id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 3. SERVICE_TYPE  |  catalog of services offered with base cost
-- ------------------------------------------------------------
CREATE TABLE service_type (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    base_cost   DECIMAL(10,2) NOT NULL
);

-- ------------------------------------------------------------
-- 4. BOOKING  |  a service appointment for a vehicle
-- ------------------------------------------------------------
CREATE TABLE booking (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id     INT NOT NULL,
    scheduled_date DATE NOT NULL,
    status         ENUM('BOOKED','IN_PROGRESS','COMPLETED','CANCELLED')
                   DEFAULT 'BOOKED',
    odometer       INT,
    technician     VARCHAR(100),
    notes          VARCHAR(255),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicle(id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 5. BOOKING_ITEM  |  line items added to a booking (services)
-- ------------------------------------------------------------
CREATE TABLE booking_item (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    booking_id      INT NOT NULL,
    service_type_id INT NOT NULL,
    cost            DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_item_booking FOREIGN KEY (booking_id)
        REFERENCES booking(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_service FOREIGN KEY (service_type_id)
        REFERENCES service_type(id)
);

-- ------------------------------------------------------------
-- 6. INVOICE  |  generated invoices for completed bookings
-- ------------------------------------------------------------
CREATE TABLE invoice (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    booking_id     INT NOT NULL,
    invoice_number VARCHAR(20) NOT NULL UNIQUE,
    total          DECIMAL(10,2) NOT NULL,
    pdf_path       VARCHAR(255),
    generated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_booking FOREIGN KEY (booking_id)
        REFERENCES booking(id) ON DELETE CASCADE
);

-- ------------------------------------------------------------
-- 7. SERVICE_HISTORY  |  read-only view of completed work
-- ------------------------------------------------------------
CREATE VIEW service_history AS
SELECT
    b.id              AS booking_id,
    b.scheduled_date  AS service_date,
    b.status,
    b.odometer,
    b.technician,
    c.id              AS customer_id,
    c.name            AS customer_name,
    v.id              AS vehicle_id,
    CONCAT(v.make, ' ', v.model) AS vehicle_name,
    v.license_plate,
    st.name           AS service_name,
    bi.cost
FROM booking b
JOIN vehicle       v  ON v.id  = b.vehicle_id
JOIN customer      c  ON c.id  = v.customer_id
JOIN booking_item  bi ON bi.booking_id = b.id
JOIN service_type  st ON st.id = bi.service_type_id
WHERE b.status IN ('COMPLETED','IN_PROGRESS');

-- ------------------------------------------------------------
-- Seed the service catalog with common services + base costs
-- ------------------------------------------------------------
INSERT INTO service_type (name, description, base_cost) VALUES
('Oil Change',          'Engine oil replacement and filter change', 1200.00),
('Tire Rotation',       'Rotate and balance all four tires',        400.00),
('Brake Pad Replacement','Replace front and rear brake pads',       2500.00),
('Engine Tuning',       'Spark plugs, filters and engine tuning',   1800.00),
('AC Service',          'AC gas refill and system check',           1500.00),
('Wheel Alignment',     'Computerised wheel alignment',              800.00),
('Battery Replacement', 'Replace car battery and terminals',        3500.00),
('Full Body Wash',      'Full exterior wash and wax polish',         600.00);