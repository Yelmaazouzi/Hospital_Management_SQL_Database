/**
 * 
 */
package csc480project;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Scanner;

/**
 * @author Yassine El Maazouzi
 *
 */
public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static final PrintStream out = System.out;

    public static void main(String[] args) throws SQLException {
        try (Connection conn = getConnection("jdbc:derby:db/hospitaldb")) {
			populateDummyData(conn);
            displayMenu();
            loop:
            while (true) {
                switch (requestString("Selection (0 to quit, 9 for menu)?")) {
                    case "0": // Quit
                        break loop;

                    case "1": // Reset
                        resetTables(conn);
                        break;

                    case "2": // List patients
                        listPatients(conn);
                        break;

                    case "3": // List doctors
                        listDoctors(conn);
                        break;

                    case "4": // Add patient
                        addPatient(conn);
                        break;

                    case "5": // Add doctor
                        addDoctor(conn);
                        break;

                    case "6": // List appointments
                        listAppointments(conn);
                        break;
                    case "7":
                        listAppointmentsByDoctor(conn);
                        break;
                    case "8":
                        listAppointmentsByPatient(conn);
                        break;
                    case "9":
                        averageAppointmentsPerDoctor(conn);
                        break;
					case "10": 
						deleteAppointment(conn);
						break;
					case "11": 
						deletePatient(conn);
						break;
					case "12":
						deleteDoctor(conn);
                    default:
                        displayMenu();
                        break;
            }
		}

        } catch (SQLException e) {
            e.printStackTrace();
        }
        out.println("done");
    }

    private static Connection getConnection(String url) {
        try {
            Connection conn = DriverManager.getConnection(url);
            return conn;
        } catch (SQLException e) {
            try {
                Connection conn = DriverManager.getConnection(url + ";create=true");
                createTables(conn);
                return conn;

            } catch (SQLException e2) {
                throw new RuntimeException("cannot connect to database", e2);
            }
        }
    }

    private static void displayMenu() {
        out.println("\n");
        out.println("0: Quit");
        out.println("1: Reset Tables");
        out.println("2: List Patients");
        out.println("3: List Doctors");
        out.println("4: Add Patient");
        out.println("5: Add Doctor");
        out.println("6: List Appointments");
        out.println("7: List Appointments by Doctor");
        out.println("8: List Appointments by Patient");
        out.println("9: Calculate Average Appointments per Doctor");
		out.println("10: Delete Appointment");
    	out.println("11: Delete Patient");
		out.println("12: Delete Doctor");
    
    }

    private static String requestString(String prompt) {
        out.print(prompt);
        out.flush();
        return in.nextLine();
    }

    private static Integer requestInt(String prompt) {
		while (true) {
			try {
				out.print(prompt);
				return Integer.parseInt(in.nextLine());
			} catch (NumberFormatException e) {
				out.println("Invalid input. Please enter an integer.");
			}
		}
	}
	

    private static void createTables(Connection conn) {
        // First clean up from previous runs, if any
        dropTables(conn);

        // Now create the schema
        addTables(conn);
    }

	private static void dropTables(Connection conn) {
		doUpdateNoError(conn, "drop table APPOINTMENT", "Table APPOINTMENT dropped.");
		doUpdateNoError(conn, "drop table DOCTOR", "Table DOCTOR dropped.");
		doUpdateNoError(conn, "drop table PATIENTS", "Table PATIENTS dropped.");
	}

	
	
	
	
    private static void resetTables(Connection conn) throws SQLException {
		dropTables(conn);
		addTables(conn);
		populateDummyData(conn);
	}
	

	private static void addTables(Connection conn) {
		// Create patient table if it doesn't exist
		if (!tableExists(conn, "PATIENTS")) {
			String createPatientTable = "CREATE TABLE PATIENTS (" +
					"patient_id INT PRIMARY KEY," +
					"patient_name VARCHAR(255) NOT NULL," +
					"patient_gender VARCHAR(10) NOT NULL," +
					"patient_dob DATE NOT NULL," +
					"patient_address VARCHAR(255) NOT NULL," +
					"patient_phone VARCHAR(20) NOT NULL," +
					"patient_email VARCHAR(255) NOT NULL" +
					")";
			doUpdate(conn, createPatientTable, "Table PATIENTS created.");
		}
	
		// Create doctor table if it doesn't exist
		if (!tableExists(conn, "DOCTOR")) {
			String createDoctorTable = "CREATE TABLE DOCTOR (" +
					"doctor_id INT PRIMARY KEY," +
					"doctor_name VARCHAR(255) NOT NULL," +
					"doctor_gender VARCHAR(10) NOT NULL," +
					"doctor_dob DATE NOT NULL," +
					"doctor_address VARCHAR(255) NOT NULL," +
					"doctor_phone VARCHAR(20) NOT NULL," +
					"doctor_email VARCHAR(255) NOT NULL," +
					"specialization VARCHAR(100) NOT NULL" +
					")";
			doUpdate(conn, createDoctorTable, "Table DOCTOR created.");
		}
	
		// Create appointment table if it doesn't exist
		if (!tableExists(conn, "APPOINTMENT")) {
			String createAppointmentTable = "CREATE TABLE APPOINTMENT (" +
					"appointment_id INT PRIMARY KEY," +
					"appointment_date TIMESTAMP NOT NULL," +
					"patient_id INT NOT NULL," +
					"doctor_id INT NOT NULL," +
					"FOREIGN KEY (patient_id) REFERENCES PATIENTS(patient_id)," +
					"FOREIGN KEY (doctor_id) REFERENCES DOCTOR(doctor_id)" +
					")";
			doUpdate(conn, createAppointmentTable, "Table APPOINTMENT created.");
		}
	}
	
	private static boolean tableExists(Connection conn, String tableName) {
		try {
			DatabaseMetaData metaData = conn.getMetaData();
			ResultSet rs = metaData.getTables(null, null, tableName, null);
			return rs.next();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	

private static void doUpdate(Connection conn, String statement, String message) {
    try (Statement stmt = conn.createStatement()) {
        stmt.executeUpdate(statement);
        System.out.println(message);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

private static void doUpdateNoError(Connection conn, String statement, String message) {
    try (Statement stmt = conn.createStatement()) {
        stmt.executeUpdate(statement);
        System.out.println(message);
    } catch (SQLException e) {
    }
}

private static void listPatients(Connection conn) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
        String sql = "SELECT * FROM patients";
        ResultSet rs = stmt.executeQuery(sql);

        System.out.println("Patients:");
        System.out.println("--------------------------------------------------------");
        System.out.printf("%-10s %-20s %-30s %-15s%n", "Patient ID", "Name", "Address", "Phone");
        System.out.println("--------------------------------------------------------");
        
        while (rs.next()) {
            int patientId = rs.getInt("patient_id");
            String patientName = rs.getString("patient_name");
            String patientAddress = rs.getString("patient_address");
            String patientPhone = rs.getString("patient_phone");
            
            System.out.printf("%-10s %-20s %-30s %-15s%n", patientId, patientName, patientAddress, patientPhone);
        }
        
        System.out.println("--------------------------------------------------------");
    } catch (SQLException e) {
        System.out.println("Error retrieving patients: " + e.getMessage());
    }
}


private static void listDoctors(Connection conn) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
        String sql = "SELECT * FROM doctor";
        ResultSet rs = stmt.executeQuery(sql);

        System.out.println("Doctors:");
        System.out.println("--------------------------------------------------------");
        System.out.printf("%-10s %-20s %-15s %-15s%n", "Doctor ID", "Name", "Specialization", "Phone");
        System.out.println("--------------------------------------------------------");

        while (rs.next()) {
            int doctorId = rs.getInt("doctor_id");
            String doctorName = rs.getString("doctor_name");
            String specialization = rs.getString("specialization");
            String doctorPhone = rs.getString("doctor_phone");

            System.out.printf("%-10s %-20s %-15s %-15s%n", doctorId, doctorName, specialization, doctorPhone);
        }

        System.out.println("--------------------------------------------------------");
    } catch (SQLException e) {
        System.out.println("Error retrieving doctors: " + e.getMessage());
    }
}


private static void addPatient(Connection conn) {
    int patientId = requestInt("Enter patient ID: ");
    String patientName = requestString("Enter patient name: ");
    String patientGender = requestString("Enter patient gender: ");
    String patientDOB = requestString("Enter patient date of birth (YYYY-MM-DD): ");
    String patientAddress = requestString("Enter patient address: ");
    String patientPhone = requestString("Enter patient phone number: ");
    String patientEmail = requestString("Enter patient email: ");

    String sql = "INSERT INTO patients (patient_id, patient_name, patient_gender, patient_dob, patient_address, patient_phone, patient_email) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, patientId);
        stmt.setString(2, patientName);
        stmt.setString(3, patientGender);
        stmt.setDate(4, Date.valueOf(patientDOB));
        stmt.setString(5, patientAddress);
        stmt.setString(6, patientPhone);
        stmt.setString(7, patientEmail);

        int rowsInserted = stmt.executeUpdate();
        if (rowsInserted > 0) {
            System.out.println("Patient added successfully.");
        } else {
            System.out.println("Failed to add patient.");
        }
    } catch (SQLException e) {
        System.out.println("Error adding patient: " + e.getMessage());
    }
}



private static void addDoctor(Connection conn) {
    String sql = "INSERT INTO doctor (doctor_id, doctor_name, doctor_gender, doctor_dob, doctor_address, doctor_phone, doctor_email, specialization) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        int id = requestInt("Enter doctor ID: ");
        String name = requestString("Enter doctor name: ");
        String gender = requestString("Enter doctor gender (Male/Female): ");
        String dob = requestString("Enter doctor date of birth (yyyy-mm-dd): ");
        String address = requestString("Enter doctor address: ");
        String phone = requestString("Enter doctor phone: ");
        String email = requestString("Enter doctor email: ");
        String specialization = requestString("Enter doctor specialization: ");

        pstmt.setInt(1, id);
        pstmt.setString(2, name);
        pstmt.setString(3, gender);
        pstmt.setDate(4, Date.valueOf(dob));
        pstmt.setString(5, address);
        pstmt.setString(6, phone);
        pstmt.setString(7, email);
        pstmt.setString(8, specialization);

        pstmt.executeUpdate();
        System.out.println("Doctor added successfully.");
    } catch (SQLException e) {
        System.out.println("Error adding Doctor: " + e.getMessage());
    }
}

private static void listAppointments(Connection conn) {
    String sql = "SELECT appointment.appointment_id, appointment_date, patient_name, doctor_name FROM appointment JOIN patient ON appointment.patient_id = patient.patient_id JOIN doctor ON appointment.doctor_id = doctor.doctor_id";
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        System.out.println("Appointments:");
        System.out.println("--------------------------------------------------------");
        System.out.printf("%-15s %-20s %-20s %-20s%n", "Appointment ID", "Appointment Date", "Patient Name", "Doctor Name");
        System.out.println("--------------------------------------------------------");

        while (rs.next()) {
            int appointmentId = rs.getInt("appointment_id");
            Timestamp appointmentDate = rs.getTimestamp("appointment_date");
            String patientName = rs.getString("patient_name");
            String doctorName = rs.getString("doctor_name");

            System.out.printf("%-15s %-20s %-20s %-20s%n", appointmentId, appointmentDate, patientName, doctorName);
        }

        System.out.println("--------------------------------------------------------");
    } catch (SQLException e) {
        System.out.println("Error retrieving appointments: " + e.getMessage());
    }
}

private static void listAppointmentsByDoctor(Connection conn) {
    int doctorId = requestInt("Enter doctor ID: ");
    String sql = "SELECT appointment.appointment_id, appointment_date, patient_name, doctor_name " +
            "FROM appointment " +
            "JOIN patient ON appointment.patient_id = patient.patient_id " +
            "JOIN doctor ON appointment.doctor_id = doctor.doctor_id " +
            "WHERE doctor.doctor_id = ?";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, doctorId);
        ResultSet rs = pstmt.executeQuery();
        System.out.println("List of Appointments for Doctor ID: " + doctorId);
        System.out.println("+--------------+---------------------+----------------------+----------------------+");
        System.out.println("| Appointment  | Appointment         | Patient              | Doctor               |");
        System.out.println("| ID           | Date                | Name                 | Name                 |");
        System.out.println("+--------------+---------------------+----------------------+----------------------+");
        while (rs.next()) {
            System.out.format("| %-12s | %-19s | %-20s | %-20s |%n",
                    rs.getInt("appointment_id"), rs.getTimestamp("appointment_date"),
                    rs.getString("patient_name"), rs.getString("doctor_name"));
        }
        System.out.println("+--------------+---------------------+----------------------+----------------------+");
    } catch (SQLException e) {
        System.out.println("Error listing appointments: " + e.getMessage());
    }
}

private static void listAppointmentsByPatient(Connection conn) {
    int patientId = requestInt("Enter patient ID: ");
    String sql = "SELECT appointment.appointment_id, appointment_date, patient_name, doctor_name " +
            "FROM appointment " +
            "JOIN patient ON appointment.patient_id = patient.patient_id " +
            "JOIN doctor ON appointment.doctor_id = doctor.doctor_id " +
            "WHERE patient.patient_id = ?";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, patientId);
        ResultSet rs = pstmt.executeQuery();
        System.out.println("List of Appointments for Patient ID: " + patientId);
        System.out.println("+--------------+---------------------+----------------------+----------------------+");
        System.out.println("| Appointment  | Appointment         | Patient              | Doctor               |");
        System.out.println("| ID           | Date                | Name                 | Name                 |");
        System.out.println("+--------------+---------------------+----------------------+----------------------+");
        while (rs.next()) {
            System.out.format("| %-12s | %-19s | %-20s | %-20s |%n",
                    rs.getInt("appointment_id"), rs.getTimestamp("appointment_date"),
                    rs.getString("patient_name"), rs.getString("doctor_name"));
        }
        System.out.println("+--------------+---------------------+----------------------+----------------------+");
    } catch (SQLException e) {
        System.out.println("Error listing appointments: " + e.getMessage());
    }
}

private static void averageAppointmentsPerDoctor(Connection conn) {
    String sql = "SELECT doctor.doctor_id, doctor_name, COUNT(appointment_id) as appointment_count " +
            "FROM doctor " +
            "LEFT JOIN appointment ON doctor.doctor_id = appointment.doctor_id " +
            "GROUP BY doctor.doctor_id, doctor_name";

    int doctorCount = 0;
    int totalAppointments = 0;

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        
		ResultSet rs = pstmt.executeQuery();
        	System.out.println("Calculating Average Appointments per Doctor:");
			System.out.println("+--------------+----------------------+");
			System.out.println("| Doctor | Average Appointments |");
			System.out.println("| Name | per Doctor |");
			System.out.println("+--------------+----------------------+");
		while (rs.next()) {
			int appointmentCount = rs.getInt("appointment_count");
			System.out.format("| %-12s | %-20s |%n",
			rs.getString("doctor_name"), appointmentCount);
			totalAppointments += appointmentCount;
			doctorCount++;
		}
		System.out.println("+--------------+----------------------+");
		double averageAppointments = doctorCount > 0 ? (double) totalAppointments / doctorCount : 0;
		System.out.println("Average appointments per doctor: " + averageAppointments);
		
	} catch (SQLException e) {
			e.printStackTrace();
		}
}
private static void deleteDoctor(Connection conn) {
    int doctorId = requestInt("Enter doctor ID: ");
    String sql = "DELETE FROM doctor WHERE doctor_id = ?";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, doctorId);
        int rowsAffected = pstmt.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("Doctor with ID " + doctorId + " deleted successfully.");
        } else {
            System.out.println("Doctor with ID " + doctorId + " not found.");
        }
    } catch (SQLException e) {
        System.out.println("Error deleting doctor: " + e.getMessage());
    }
}

private static void deleteAppointment(Connection conn) {
    int appointmentId = requestInt("Enter appointment ID: ");
    String sql = "DELETE FROM appointment WHERE appointment_id = ?";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, appointmentId);
        int rowsAffected = pstmt.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("Appointment with ID " + appointmentId + " deleted successfully.");
        } else {
            System.out.println("Appointment with ID " + appointmentId + " not found.");
        }
    } catch (SQLException e) {
        System.out.println("Error deleting appointment: " + e.getMessage());
    }
}

private static void deletePatient(Connection conn) {
    int patientId = requestInt("Enter patient ID: ");
    String sql = "DELETE FROM patient WHERE patient_id = ?";

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, patientId);
        int rowsAffected = pstmt.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("Patient with ID " + patientId + " deleted successfully.");
        } else {
            System.out.println("Patient with ID " + patientId + " not found.");
        }
    } catch (SQLException e) {
        System.out.println("Error deleting patient: " + e.getMessage());
    }
}

private static void populateDummyData(Connection conn) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
        // Check if data already exists in patients table
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM patients");
        rs.next();
        int patientCount = rs.getInt(1);
        if (patientCount > 0) {
            System.out.println("Patients table already populated.");
        } else {
            // Populate patients table
            stmt.executeUpdate("INSERT INTO patients (patient_id, patient_name, patient_gender, patient_dob, patient_address, patient_phone, patient_email) VALUES "
                    + "(1, 'John Doe', 'Male', '1990-01-01', '123 Main St', '555-555-5555', 'john.doe@example.com'),"
                    + "(2, 'Jane Smith', 'Female', '1985-05-15', '456 Elm St', '555-555-1234', 'jane.smith@example.com'),"
                    + "(3, 'Robert Johnson', 'Male', '1978-09-20', '789 Oak Ave', '555-555-7890', 'robert.johnson@example.com')");
            System.out.println("Patients table populated successfully.");
        }

        // Check if data already exists in doctor table
        rs = stmt.executeQuery("SELECT COUNT(*) FROM doctor");
        rs.next();
        int doctorCount = rs.getInt(1);
        if (doctorCount > 0) {
            System.out.println("Doctor table already populated.");
        } else {
            // Populate doctor table
            stmt.executeUpdate("INSERT INTO doctor (doctor_id, doctor_name, doctor_gender, doctor_dob, doctor_address, doctor_phone, doctor_email, specialization) VALUES "
                    + "(1, 'Dr. Anderson', 'Male', '1975-06-12', '100 Maple St', '555-555-1111', 'dr.anderson@example.com', 'Cardiology'),"
                    + "(2, 'Dr. Wilson', 'Female', '1980-03-25', '200 Elm St', '555-555-2222', 'dr.wilson@example.com', 'Dermatology'),"
                    + "(3, 'Dr. Garcia', 'Male', '1972-11-08', '300 Oak Ave', '555-555-3333', 'dr.garcia@example.com', 'Pediatrics')");
            System.out.println("Doctor table populated successfully.");
        }

        // Check if data already exists in appointment table
        rs = stmt.executeQuery("SELECT COUNT(*) FROM appointment");
        rs.next();
        int appointmentCount = rs.getInt(1);
        if (appointmentCount > 0) {
            System.out.println("Appointment table already populated.");
        } else {
            // Populate appointment table
            stmt.executeUpdate("INSERT INTO appointment (appointment_id, appointment_date, patient_id, doctor_id) VALUES "
                    + "(1, '2023-06-01 10:00:00', 1, 1),"
                    + "(2, '2023-06-02 15:30:00', 2, 2),"
                    + "(3, '2023-06-03 09:45:00', 3, 3)");
            System.out.println("Appointment table populated successfully.");
        }
    } catch (SQLException e) {
        System.out.println("Error populating dummy data: " + e.getMessage());
    }
}



}






