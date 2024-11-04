import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

// Custom exception for database operations
class DatabaseException extends Exception {
    public DatabaseException(String message) {
        super(message);
    }
}

// Database connection manager
class DatabaseConnection {
    private Connection connection;

    public DatabaseConnection(String url, String user, String password) throws DatabaseException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Database connected successfully!");
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("MySQL Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            throw new DatabaseException("Failed to connect to the database: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection() throws DatabaseException {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                throw new DatabaseException("Error while closing the connection: " + e.getMessage());
            }
        }
    }
}

// BookManager handles all book-related operations with the database
class BookManager {
    private final Connection connection;
    private static final String ADD_BOOK_QUERY = "INSERT INTO books (id, title, author, publisher, year, available) VALUES (?, ?, ?, ?, ?, TRUE)";
    private static final String VIEW_BOOKS_QUERY = "SELECT b.id, b.title, b.author, b.publisher, b.year, (CASE WHEN ib.book_id IS NOT NULL THEN 'Issued' ELSE 'Available' END) AS status FROM books b LEFT JOIN issued_books ib ON b.id = ib.book_id";
    private static final String ISSUE_BOOK_QUERY = "INSERT INTO issued_books (book_id, student_name, registration_number, issue_date, return_date) VALUES (?, ?, ?, ?, ?)";
    private static final String VIEW_ISSUED_BOOKS_QUERY = "SELECT ib.id, ib.book_id, b.title, ib.student_name, ib.registration_number, ib.issue_date, ib.return_date FROM issued_books ib JOIN books b ON ib.book_id = b.id";
    private static final String RETURN_BOOK_QUERY_UPDATE = "UPDATE books SET available = TRUE WHERE id = ? AND available = FALSE";
    private static final String RETURN_BOOK_QUERY_DELETE = "DELETE FROM issued_books WHERE book_id = ?";
   

    public BookManager(DatabaseConnection db) throws DatabaseException {
        this.connection = db.getConnection();
        if (this.connection == null) {
            throw new DatabaseException("Failed to initialize BookManager: connection is null.");
        }
    }

    public boolean addBook(int id, String title, String author, String publisher, int year) {
        if (!isValidBook(id, title, author, publisher, year)) {
            showErrorDialog("Invalid input, please check the book details.");
            return false;
        }

        try (PreparedStatement ps = connection.prepareStatement(ADD_BOOK_QUERY)) {
            ps.setInt(1, id);
            ps.setString(2, title);
            ps.setString(3, author);
            ps.setString(4, publisher);
            ps.setInt(5, year);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                showInfoDialog("Book added successfully!");
                return true;
            } else {
                showErrorDialog("Failed to add the book.");
                return false;
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            showErrorDialog("Error: Book with ID " + id + " already exists.");
            return false;
        } catch (SQLException e) {
            showErrorDialog("Error while adding book: " + e.getMessage());
            return false;
        }
    }
    private String calculateOverdueDate(String returnDateStr) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date returnDate = dateFormat.parse(returnDateStr);
            long overdueMillis = returnDate.getTime() + (7L * 24 * 60 * 60 * 1000); // 7 days in milliseconds
            Date overdueDate = new Date(overdueMillis);
            return dateFormat.format(overdueDate);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    public boolean deleteBook(int bookId) {
        try {
            String query = "DELETE FROM books WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, bookId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error deleting book: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    

    public DefaultTableModel getBooksTableModel(int bookId) {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Title", "Author", "Publisher", "Year", "Status"}, 0);
        String query = bookId == -1 ? VIEW_BOOKS_QUERY : VIEW_BOOKS_QUERY + " WHERE b.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            if (bookId != -1) {
                ps.setInt(1, bookId);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("publisher"),
                        rs.getInt("year"),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            showErrorDialog("Error while retrieving books: " + e.getMessage());
        }
        return model;
    }
    
    public boolean issueBook(int bookId, String studentName, String registrationNumber) {
        String issueDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 15);
        String returnDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());

        try (PreparedStatement ps = connection.prepareStatement(ISSUE_BOOK_QUERY)) {
            ps.setInt(1, bookId);
            ps.setString(2, studentName);
            ps.setString(3, registrationNumber);
            ps.setString(4, issueDate);
            ps.setString(5, returnDate);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                showInfoDialog("Book issued successfully to " + studentName);
                return true;
            } else {
                showErrorDialog("Book cannot be issued (either does not exist or is already issued).");
                return false;
            }
        } catch (SQLException e) {
            showErrorDialog("Error while issuing book: " + e.getMessage());
            return false;
        }
    }

    public DefaultTableModel getIssuedBooksTableModel() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Book ID", "Title", "Student Name", "Reg No", "Issue Date", "Return Date", "Overdue Date"}, 0);
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(VIEW_ISSUED_BOOKS_QUERY)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                int bookId = rs.getInt("book_id");
                String title = rs.getString("title");
                String studentName = rs.getString("student_name");
                String registrationNumber = rs.getString("registration_number");
                String issueDate = rs.getString("issue_date");
                String returnDate = rs.getString("return_date");
    
                // Calculate overdue date
                String overdueDate = calculateOverdueDate(returnDate);
    
                model.addRow(new Object[]{
                    id,
                    bookId,
                    title,
                    studentName,
                    registrationNumber,
                    issueDate,
                    returnDate,
                    overdueDate // Add the calculated overdue date here
                });
            }
        } catch (SQLException e) {
            showErrorDialog("Error while retrieving issued books: " + e.getMessage());
        }
        return model;
    }
    
        

    public boolean returnBook(int bookId) {
        try (PreparedStatement psDelete = connection.prepareStatement(RETURN_BOOK_QUERY_DELETE);
             PreparedStatement psUpdate = connection.prepareStatement(RETURN_BOOK_QUERY_UPDATE)) {

            psDelete.setInt(1, bookId);
            int rowsAffected = psDelete.executeUpdate();
            if (rowsAffected > 0) {
                psUpdate.setInt(1, bookId);
                psUpdate.executeUpdate();
                showInfoDialog("Book returned successfully!");
                return true;
            } else {
                showErrorDialog("Book cannot be returned (either does not exist or was not issued).");
                return false;
            }
        } catch (SQLException e) {
            showErrorDialog("Error while returning book: " + e.getMessage());
            return false;
        }
    }

    private boolean isValidBook(int id, String title, String author, String publisher, int year) {
        return title != null && !title.isEmpty() && author != null && !author.isEmpty() &&
               publisher != null && !publisher.isEmpty() && year > 0 && id > 0;
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfoDialog(String message) {
        JOptionPane.showMessageDialog(null, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }
}
class IssuedBooksTableRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        cell.setForeground(Color.BLACK);  // Default color

        // Get the overdue date from the table model
        String overdueDateStr = (String) table.getValueAt(row, 6);  // Assuming Overdue Date is in column index 6
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Date overdueDate = dateFormat.parse(overdueDateStr);
            Date currentDate = new Date();
            
            // If the current date is past the overdue date, show it in red
            if (currentDate.after(overdueDate)) {
                cell.setForeground(Color.RED);
            }
        } catch (Exception e) {
            e.printStackTrace();  // Handle parsing exceptions
        }

        return cell;
    }
}

// Main application class to display the GUI
public class LibraryManagement extends JFrame {
    private DatabaseConnection db;
    private BookManager bookManager;
    private JTable booksTable;
    private JTable issuedBooksTable;
    private DefaultTableModel booksTableModel;
    private DefaultTableModel issuedBooksTableModel;
    private JTextField searchField;
    private DefaultTableModel activityLogTableModel;
    private JTabbedPane tabbedPane;
    private String adminUsername = "ADMIN-NAME";//add admin user name for login.
    private String adminPassword = "ADMIN-PASSWORD";//add admin user password for login.

    public LibraryManagement() {
        try {
            //DATABASE=library_management , USER=root(for default user) , PASSWORD=Enter you database password which you set.
            db = new DatabaseConnection("jdbc:mysql://localhost:3306/library_management", "root", "ENTER-DATABASE-PASSWORD");
            bookManager = new BookManager(db);
        } catch (DatabaseException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Initialization Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setTitle("Library Management System");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        //Attendance panel
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Activity Tracker", createActivityPanel());

       //Initially disable all other tabs except "Activity Tracker"
        tabbedPane.addTab("Add Book", createAddBookPanel());
        tabbedPane.addTab("View Books", createViewBooksPanel());
        tabbedPane.addTab("Issue Book", createIssueBookPanel());
        tabbedPane.addTab("View Issued Books", createViewIssuedBooksPanel());
        tabbedPane.addTab("Return Book", createReturnBookPanel());
        tabbedPane.addTab("Delete Book", createDeleteBookPanel());
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
        JTextField idField = new JTextField(10);
        JTextField titleField = new JTextField(10);
        JTextField authorField = new JTextField(10);
        JTextField publisherField = new JTextField(10);
        JTextField yearField = new JTextField(10);
        

        panel.add(new JLabel("Book ID:"));
        panel.add(idField);
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Author:"));
        panel.add(authorField);
        panel.add(new JLabel("Publisher:"));
        panel.add(publisherField);
        panel.add(new JLabel("Year:"));
        panel.add(yearField);

        JButton addButton = new JButton("Add Book");
        addButton.addActionListener(e -> {
            try {
                int id = Integer.parseInt(idField.getText());
                String title = titleField.getText();
                String author = authorField.getText();
                String publisher = publisherField.getText();
                int year = Integer.parseInt(yearField.getText());

                if (bookManager.addBook(id, title, author, publisher, year)) {
                    booksTableModel.setRowCount(0); // Refresh the table after adding a book
                    refreshBooksTable(-1);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Please enter valid details for the book.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(new JLabel());
        panel.add(addButton);
        return panel;
    }
    private void clearFields(JTextField... fields) {
        for (JTextField field : fields) {
            field.setText("");
        }
    }
    private JPanel createDeleteBookPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JLabel bookIdLabel = new JLabel("Book ID:");
        JTextField bookIdField = new JTextField();
    
        JButton deleteButton = new JButton("Delete Book");
        deleteButton.addActionListener(e -> {
            try {
                int bookId = Integer.parseInt(bookIdField.getText());
                int confirm = JOptionPane.showConfirmDialog(
                    this, 
                    "Are you sure you want to delete the book with ID: " + bookId + "?",
                    "Confirm Deletion", 
                    JOptionPane.YES_NO_OPTION
                );
    
                if (confirm == JOptionPane.YES_OPTION) {
                    if (bookManager.deleteBook(bookId)) {
                        refreshBooksTable(-1);
                        refreshIssuedBooksTable();
                        bookIdField.setText("");  // Clear the text field after deletion
                        
                        JOptionPane.showMessageDialog(this, "Book deleted successfully.");
                    } else {
                        JOptionPane.showMessageDialog(this, "Book ID not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid numeric Book ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    
        panel.add(bookIdLabel);
        panel.add(bookIdField);
        panel.add(new JLabel()); // Empty cell for layout spacing
        panel.add(deleteButton);
    
        return panel;
    }
    
    private String calculateOverdueDate(String returnDateStr) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    try {
        Date returnDate = dateFormat.parse(returnDateStr);
        // Add 7 days to the return date for the overdue date
        long overdueMillis = returnDate.getTime() + (7L * 24 * 60 * 60 * 1000);  // 7 days in milliseconds
        Date overdueDate = new Date(overdueMillis);
        return dateFormat.format(overdueDate);
    } catch (Exception e) {
        e.printStackTrace();
        return "";
    }
}

    
    private JPanel createViewBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel searchLabel = new JLabel("Search by Book ID:");
        searchField = new JTextField(10);
        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");

        searchButton.addActionListener(e -> {
            try {
                int bookId = Integer.parseInt(searchField.getText());
                refreshBooksTable(bookId);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Please enter a valid book ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        refreshButton.addActionListener(e -> refreshBooksTable(-1));

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);

        booksTableModel = new DefaultTableModel(new String[]{"ID", "Title", "Author", "Publisher", "Year", "Status"}, 0);
        booksTable = new JTable(booksTableModel);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(booksTable), BorderLayout.CENTER);
        refreshBooksTable(-1);

        return panel;
    }

    private JPanel createIssueBookPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2));
        JTextField bookIdField = new JTextField(10);
        JTextField studentNameField = new JTextField(10);
        JTextField registrationNumberField = new JTextField(10);

        panel.add(new JLabel("Book ID:"));
        panel.add(bookIdField);
        panel.add(new JLabel("Student Name:"));
        panel.add(studentNameField);
        panel.add(new JLabel("Registration Number:"));
        panel.add(registrationNumberField);

        JButton issueButton = new JButton("Issue Book");
        issueButton.addActionListener(e -> {
            try {
                int bookId = Integer.parseInt(bookIdField.getText());
                String studentName = studentNameField.getText();
                String registrationNumber = registrationNumberField.getText();
                if (bookManager.issueBook(bookId, studentName, registrationNumber)) {
                    refreshBooksTable(-1);
                    refreshIssuedBooksTable();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Please enter valid details for issuing the book.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.add(new JLabel());
        panel.add(issueButton);
        return panel;
    }
    
    private JPanel createViewIssuedBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        issuedBooksTableModel = new DefaultTableModel(new String[]{"ID", "Book ID", "Title", "Student Name", "Reg No", "Issue Date", "Return Date","Overdue Date"}, 0);
        issuedBooksTable = new JTable(issuedBooksTableModel);
        issuedBooksTable.setDefaultRenderer(Object.class, new IssuedBooksTableRenderer());
        panel.add(new JScrollPane(issuedBooksTable), BorderLayout.CENTER);
        refreshIssuedBooksTable();
        return panel;
    }

    private JPanel createReturnBookPanel() {
        JPanel panel = new JPanel(new GridLayout(2,2));
        JButton returnButton = new JButton("Return Book");
        JTextField bookIdField = new JTextField();
        JLabel bookIdLabel=new JLabel("Book ID:");
        
        
        
        panel.add(bookIdLabel);
        panel.add(bookIdField);
        panel.add(new JLabel());
        panel.add(returnButton);
        
        
        

        returnButton.addActionListener(e -> {
            try {
                int bookId = Integer.parseInt(bookIdField.getText());
                if (bookManager.returnBook(bookId)) {
                    refreshBooksTable(-1);
                    refreshIssuedBooksTable();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Please enter a valid book ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

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



    private void refreshBooksTable(int bookId) {
        booksTable.setModel(bookManager.getBooksTableModel(bookId));
    }

    private void refreshIssuedBooksTable() {
        issuedBooksTable.setModel(bookManager.getIssuedBooksTableModel());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LibraryManagement::new);
    }
}
