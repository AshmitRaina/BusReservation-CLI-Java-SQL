// DBConnection.java
import java.sql.*;
import java.util.*;
import java.util.List; // <-- this line is important

 class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/mydb";
    private static final String USER = "root";
    private static final String PASSWORD = "2025";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
class User {
    String username;
    String password;
    List<Ticket> tickets = new ArrayList<>();

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void displayTickets() {
        if (tickets.isEmpty()) {
            System.out.println("No tickets booked.");
        } else {
            for (Ticket ticket : tickets) {
                ticket.displayTicketDetails();
                System.out.println("--------------");
            }
        }
    }
}

class Ticket {
    String ticketID;
    String busNumber;
    String seatNumber;
    String passengerName;

    public Ticket(String ticketID, String busNumber, String seatNumber, String passengerName) {
        this.ticketID = ticketID;
        this.busNumber = busNumber;
        this.seatNumber = seatNumber;
        this.passengerName = passengerName;
    }

    public void displayTicketDetails() {
        System.out.println("Ticket ID: " + ticketID);
        System.out.println("Bus Number: " + busNumber);
        System.out.println("Seat Number: " + seatNumber);
        System.out.println("Passenger Name: " + passengerName);
    }
}

public class Main {
    private static Scanner scanner = new Scanner(System.in);
    private static User currentUser = null;

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n====== Bus Reservation System ======");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Show Available Buses");
            System.out.println("4. Book Ticket");
            System.out.println("5. View My Tickets");
            System.out.println("6. Cancel Ticket");
            System.out.println("7. Logout");
            System.out.println("8. Exit");
            System.out.print("Choose option: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1 -> register();
                case 2 -> login();
                case 3 -> showAvailableBuses();
                case 4 -> bookTicket();
                case 5 -> viewMyTickets();
                case 6 -> cancelTicket();
                case 7 -> logout();
                case 8 -> {
                    System.out.println("Exiting system. Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static void register() {
        System.out.print("Enter new username: ");
        String username = scanner.next();
        System.out.print("Enter password: ");
        String password = scanner.next();

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("Username already exists.");
                return;
            }

            ps = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            System.out.println("Registration successful!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static void login() {
        System.out.print("Enter username: ");
        String username = scanner.next();
        System.out.print("Enter password: ");
        String password = scanner.next();

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentUser = new User(username, password);
                loadUserTickets();
                System.out.println("Login successful!");
            } else {
                System.out.println("Invalid credentials.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void logout() {
        if (currentUser != null) {
            currentUser = null;
            System.out.println("Logged out successfully.");
        }
    }

    private static void showAvailableBuses() {
        try (Connection conn = DBConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM buses");

            while (rs.next()) {
                System.out.println("----------------------");
                System.out.println("Bus Number: " + rs.getString("bus_number"));
                System.out.println("Source: " + rs.getString("source"));
                System.out.println("Destination: " + rs.getString("destination"));
                System.out.println("Total Seats: " + rs.getInt("total_seats"));
                System.out.println("Available Seats: " + rs.getInt("available_seats"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void bookTicket() {
        if (currentUser == null) {
            System.out.println("Please login first.");
            return;
        }
        showAvailableBuses();
        System.out.print("Enter Bus Number: ");
        String busNumber = scanner.next();
        System.out.print("Enter Seat Number: ");
        String seatNumber = scanner.next();
        System.out.print("Enter Passenger Name: ");
        String name = scanner.next();

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement checkSeat = conn.prepareStatement(
                    "SELECT * FROM tickets WHERE bus_number = ? AND seat_number = ?");
            checkSeat.setString(1, busNumber);
            checkSeat.setString(2, seatNumber);
            ResultSet rs = checkSeat.executeQuery();
            if (rs.next()) {
                System.out.println("Seat already booked.");
                return;
            }

            String ticketID = UUID.randomUUID().toString().substring(0, 8);
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO tickets (ticket_id, username, bus_number, seat_number, passenger_name) VALUES (?, ?, ?, ?, ?)"
            );
            ps.setString(1, ticketID);
            ps.setString(2, currentUser.username);
            ps.setString(3, busNumber);
            ps.setString(4, seatNumber);
            ps.setString(5, name);
            ps.executeUpdate();

            PreparedStatement updateSeats = conn.prepareStatement(
                    "UPDATE buses SET available_seats = available_seats - 1 WHERE bus_number = ?"
            );
            updateSeats.setString(1, busNumber);
            updateSeats.executeUpdate();

            currentUser.tickets.add(new Ticket(ticketID, busNumber, seatNumber, name));
            System.out.println("Ticket booked successfully! Ticket ID: " + ticketID);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void cancelTicket() {
        if (currentUser == null) {
            System.out.println("Please login first.");
            return;
        }

        System.out.print("Enter Ticket ID to cancel: ");
        String ticketID = scanner.next();

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement getTicket = conn.prepareStatement(
                    "SELECT * FROM tickets WHERE ticket_id = ? AND username = ?");
            getTicket.setString(1, ticketID);
            getTicket.setString(2, currentUser.username);
            ResultSet rs = getTicket.executeQuery();

            if (rs.next()) {
                String busNumber = rs.getString("bus_number");
                String seatNumber = rs.getString("seat_number");

                PreparedStatement deleteTicket = conn.prepareStatement(
                        "DELETE FROM tickets WHERE ticket_id = ?");
                deleteTicket.setString(1, ticketID);
                deleteTicket.executeUpdate();

                PreparedStatement updateSeats = conn.prepareStatement(
                        "UPDATE buses SET available_seats = available_seats + 1 WHERE bus_number = ?");
                updateSeats.setString(1, busNumber);
                updateSeats.executeUpdate();

                currentUser.tickets.removeIf(t -> t.ticketID.equals(ticketID));
                System.out.println("Ticket cancelled successfully.");
            } else {
                System.out.println("Ticket not found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void viewMyTickets() {
        if (currentUser == null) {
            System.out.println("Please login first.");
            return;
        }
        currentUser.displayTickets();
    }

    private static void loadUserTickets() {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM tickets WHERE username = ?");
            ps.setString(1, currentUser.username);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                currentUser.tickets.add(new Ticket(
                        rs.getString("ticket_id"),
                        rs.getString("bus_number"),
                        rs.getString("seat_number"),
                        rs.getString("passenger_name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
