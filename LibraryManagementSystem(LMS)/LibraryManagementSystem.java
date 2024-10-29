import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LibraryManagementSystem extends JFrame {
    private DatabaseConnection db;
    private BookManager bookManager;
    private JTable booksTable;
    private JTable issuedBooksTable;
    private DefaultTableModel booksTableModel;
    private DefaultTableModel issuedBooksTableModel;
    private DefaultTableModel activityLogTableModel;
    private JTabbedPane tabbedPane;
    private String adminUsername = "rajveer";
    private String adminPassword = "rks123";

    public LibraryManagementSystem() {
        // Initialize database connection and book manager
        try {
            db = new DatabaseConnection("jdbc:mysql://localhost:3306/library_management", "root", "rks@2552");
            bookManager = new BookManager(db);
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Initialization Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Set up the main frame
        setTitle("Library Management System");
        setSize(1000, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create tabs for various functionalities
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Activity Tracker", createActivityPanel());

        // Initially disable all other tabs except "Activity Tracker"
        tabbedPane.addTab("Add Book", createAddBookPanel());
        tabbedPane.addTab("View Books", createViewBooksPanel());
        tabbedPane.addTab("Issue Book", createIssueBookPanel());
        tabbedPane.addTab("View Issued Books", createViewIssuedBooksPanel());
        tabbedPane.addTab("Return Book", createReturnBookPanel());
        disableTabsExceptActivityTracker();

        // Add Admin and Logout Buttons on Top Right
        JPanel topPanel = new JPanel(new BorderLayout());
        JButton adminButton = new JButton("Admin");
        JButton logoutButton = new JButton("Logout");

        // Set admin login action
        adminButton.addActionListener(e -> {
            showAdminLoginDialog();
           
        });

        // Set logout action
        logoutButton.addActionListener(e -> {
            disableTabsExceptActivityTracker();
            logActivity("Logout", adminUsername);
            JOptionPane.showMessageDialog(this, "Logged out successfully.");
        });

        // Add buttons to the top panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(adminButton);
        buttonPanel.add(logoutButton);

        topPanel.add(buttonPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        setVisible(true);
    }

    private void disableTabsExceptActivityTracker() {
        for (int i = 1; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setEnabledAt(i, false);
        }
    }

    private void enableAllTabs() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setEnabledAt(i, true);
        }
    }

    private JPanel createAddBookPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 2));
        JLabel idLabel = new JLabel("Book ID:");
        JTextField idField = new JTextField();
        JLabel titleLabel = new JLabel("Title:");
        JTextField titleField = new JTextField();
        JLabel authorLabel = new JLabel("Author:");
        JTextField authorField = new JTextField();
        JLabel publisherLabel = new JLabel("Publisher:");
        JTextField publisherField = new JTextField();
        JLabel yearLabel = new JLabel("Year:");
        JTextField yearField = new JTextField();

        JButton addButton = new JButton("Add Book");
        addButton.addActionListener(e -> {
            try {
                int id = Integer.parseInt(idField.getText());
                String title = titleField.getText();
                String author = authorField.getText();
                String publisher = publisherField.getText();
                int year = Integer.parseInt(yearField.getText());
                if (bookManager.addBook(id, title, author, publisher, year)) {
                    refreshBooksTable();
                    clearFields(idField, titleField, authorField, publisherField, yearField);
                    logActivity("Add Book", "Book ID: " + id + ", Title: " + title);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numeric values for ID and Year.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(idLabel);
        panel.add(idField);
        panel.add(titleLabel);
        panel.add(titleField);
        panel.add(authorLabel);
        panel.add(authorField);
        panel.add(publisherLabel);
        panel.add(publisherField);
        panel.add(yearLabel);
        panel.add(yearField);
        panel.add(new JLabel()); // Empty cell
        panel.add(addButton);

        return panel;
    }

    private void clearFields(JTextField... fields) {
        for (JTextField field : fields) {
            field.setText("");
        }
    }

    private void refreshBooksTable() {
        booksTableModel.setRowCount(0); // Clear existing data
        for (int i = 0; i < bookManager.getBooksTableModel().getRowCount(); i++) {
            booksTableModel.addRow(new Object[]{
                    bookManager.getBooksTableModel().getValueAt(i, 0),
                    bookManager.getBooksTableModel().getValueAt(i, 1),
                    bookManager.getBooksTableModel().getValueAt(i, 2),
                    bookManager.getBooksTableModel().getValueAt(i, 3),
                    bookManager.getBooksTableModel().getValueAt(i, 4),
                    bookManager.getBooksTableModel().getValueAt(i,5)

            });
        }
    }

    private JPanel createViewBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        booksTableModel = bookManager.getBooksTableModel();
        booksTable = new JTable(booksTableModel);
        JScrollPane scrollPane = new JScrollPane(booksTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshBooksTable());
        panel.add(refreshButton, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createViewIssuedBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        issuedBooksTableModel = bookManager.getIssuedBooksTableModel();
        issuedBooksTable = new JTable(issuedBooksTableModel);
        JScrollPane scrollPane = new JScrollPane(issuedBooksTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshIssuedBooksTable());
        panel.add(refreshButton, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createIssueBookPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2));
        JLabel bookIdLabel = new JLabel("Book ID:");
        JTextField bookIdField = new JTextField();
        JLabel studentNameLabel = new JLabel("Student Name:");
        JTextField studentNameField = new JTextField();
        JLabel registrationNumberLabel = new JLabel("Reg. Number:");
        JTextField registrationNumberField = new JTextField();

        JButton issueButton = new JButton("Issue Book");
        issueButton.addActionListener(e -> {
            int bookId = Integer.parseInt(bookIdField.getText());
            String studentName = studentNameField.getText();
            String registrationNumber = registrationNumberField.getText();
            if (bookManager.issueBook(bookId, studentName, registrationNumber)) {
                refreshBooksTable();
                refreshIssuedBooksTable();
                logActivity("Issue Book", "Book ID: " + bookId + ", Issued to: " + studentName);
            }
        });

        panel.add(bookIdLabel);
        panel.add(bookIdField);
        panel.add(studentNameLabel);
        panel.add(studentNameField);
        panel.add(registrationNumberLabel);
        panel.add(registrationNumberField);
        panel.add(new JLabel()); // Empty cell
        panel.add(issueButton);

        return panel;
    }

    private void refreshIssuedBooksTable() {
        issuedBooksTableModel.setRowCount(0);
        DefaultTableModel newModel = bookManager.getIssuedBooksTableModel();
        for (int i = 0; i < newModel.getRowCount(); i++) {
            issuedBooksTableModel.addRow(new Object[] {
                    newModel.getValueAt(i, 0),
                    newModel.getValueAt(i, 1),
                    newModel.getValueAt(i, 2),
                    newModel.getValueAt(i, 3),
                    newModel.getValueAt(i, 4),
                    newModel.getValueAt(i, 5),
                    newModel.getValueAt(i,6)
            });
        }
    }

    private JPanel createReturnBookPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JLabel bookIdLabel = new JLabel("Book ID:");
        JTextField bookIdField = new JTextField();

        JButton returnButton = new JButton("Return Book");
        returnButton.addActionListener(e -> {
            try {
                int bookId = Integer.parseInt(bookIdField.getText());
                if (bookManager.returnBook(bookId)) {
                    refreshBooksTable();
                    refreshIssuedBooksTable();
                    logActivity("Return Book", "Book ID: " + bookId);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid book ID format.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(bookIdLabel);
        panel.add(bookIdField);
        panel.add(new JLabel());
        panel.add(returnButton);

        return panel;
    }

    private JPanel createActivityPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(4, 2));
        JLabel regLabel = new JLabel("Registration No:");
        JTextField regField = new JTextField();
        JLabel nameLabel = new JLabel("Name:");
        JTextField nameField = new JTextField();
        JLabel activityLabel = new JLabel("Activity:");
        JComboBox<String> activityBox = new JComboBox<>(new String[]{"Self Study", "Reading books","issued book","Return book"});
        JButton enterButton = new JButton("Enter");

        formPanel.add(regLabel);
        formPanel.add(regField);
        formPanel.add(nameLabel);
        formPanel.add(nameField);
        formPanel.add(activityLabel);
        formPanel.add(activityBox);
        formPanel.add(new JLabel()); // Placeholder
        formPanel.add(enterButton);

        activityLogTableModel = new DefaultTableModel(new String[]{"Registration No", "Name", "Activity", "Date", "Time"}, 0);
        JTable activityLogTable = new JTable(activityLogTableModel);
        JScrollPane tableScrollPane = new JScrollPane(activityLogTable);
        loadActivityLog();
        enterButton.addActionListener(e -> {
            String registrationNumber = regField.getText();
            String name = nameField.getText();
            String activity = (String) activityBox.getSelectedItem();

            if (registrationNumber.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter both Registration No and Name.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } else {
                logActivity(registrationNumber, name, activity);
                clearFields(regField, nameField);
            }
        });
        activityLogTableModel.addTableModelListener(e -> {
            if (e.getColumn() == 5) { // Delete column
                int row = e.getFirstRow();
                String regNo = (String) activityLogTableModel.getValueAt(row, 0);
                deleteActivityLogEntry(regNo);
            }
        });

        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        return panel;}
        private void loadActivityLog() {
            // Clear the table model before loading new data to avoid duplicates
            activityLogTableModel.setRowCount(0);
        
            // Query to select all records from activity_log
            String query = "SELECT * FROM activity_log";
        
            try (PreparedStatement pstmt = db.getConnection().prepareStatement(query)) {
                ResultSet rs = pstmt.executeQuery();
        
                // Iterate over the ResultSet and add each row to the table model
                while (rs.next()) {
                    String registrationNo = rs.getString("registration_no");
                    String name = rs.getString("name");
                    String activity = rs.getString("activity");
                    String date = rs.getString("date");
                    String time = rs.getString("time");
        
                    // Add a "Delete" button in the last column for each row
                    activityLogTableModel.addRow(new Object[]{registrationNo, name, activity, date, time, "Delete"});
                }
        
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error loading activity log: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        private void logActivity(String registrationNumber, String name, String activity) {
            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            try (PreparedStatement pstmt = db.getConnection().prepareStatement(
                    "INSERT INTO activity_log (registration_no, name, activity, date, time) VALUES (?, ?, ?, ?, ?)")) {
                pstmt.setString(1, registrationNumber);
                pstmt.setString(2, name);
                pstmt.setString(3, activity);
                pstmt.setString(4, date);
                pstmt.setString(5, time);
                pstmt.executeUpdate();
                activityLogTableModel.addRow(new Object[]{registrationNumber, name, activity, date, time});
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error logging activity: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        } private void deleteActivityLogEntry(String regNo) {
            try (PreparedStatement pstmt = db.getConnection().prepareStatement("DELETE FROM activity_log WHERE registration_no = ?")) {
                pstmt.setString(1, regNo);
                pstmt.executeUpdate();
                // Refresh the table
                activityLogTableModel.setRowCount(0);
                loadActivityLog(); // Reload the data after deletion
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting record: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
            

    private void showAdminLoginDialog() {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        Object[] message = {
            "Username:", usernameField,
            "Password:", passwordField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Admin Login", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (username.equals(adminUsername) && password.equals(adminPassword)) {
                JOptionPane.showMessageDialog(this, "Login successful!");
                enableAllTabs();
                logActivity("Admin Login", adminUsername);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.");
            }
        }
    }

    private void logActivity(String action, String adminUsername) {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        activityLogTableModel.addRow(new Object[]{"Manager",adminUsername, action, date,time});
    }

    public static void main(String[] args) {
        new LibraryManagementSystem();
    }
}
