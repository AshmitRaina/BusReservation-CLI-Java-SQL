CREATE TABLE buses (
    bus_number VARCHAR(10) PRIMARY KEY,
    source VARCHAR(50),
    destination VARCHAR(50),
    total_seats INT,
    available_seats INT
);

CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(50)
);

CREATE TABLE tickets (
    ticket_id VARCHAR(10) PRIMARY KEY,
    username VARCHAR(50),
    bus_number VARCHAR(10),
    seat_number VARCHAR(10),
    passenger_name VARCHAR(50),
    FOREIGN KEY (username) REFERENCES users(username),
    FOREIGN KEY (bus_number) REFERENCES buses(bus_number)
);
