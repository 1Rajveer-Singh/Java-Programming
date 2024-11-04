
# 📚 Library Management System

A Java-based **Library Management System** that simplifies book management and enhances user experience through 
features such as book addition, issue tracking, return processing, and activity logging.
Ideal for small libraries or academic institutions.

---

## 🔧 Features

- **Admin Login**: Secure access to manage library features.
- **Admin Logout**:securely dissable the library features.
- **Add,Delete & View Books**: Easily add, view, and manage book records.
- **Issue & Return Books**: Track book issue and return status.
- **Activity Log**: Monitor user activities with timestamps for each action (Self Study, Reading Books, Issued Book, Return Book).
- **Refresh Options**: Instant table refresh options to keep data current.

---

## 🖥️ Screens & Components

1. **Admin Login Dialog**: Restricts access to core functions.
2. **Admin Logout Dialog**: disable to access to core functions.
3. **Activity Tracker**: Logs users' activities within the library.
4. **Add Book Panel**: Add new books with details like ID, title, author, publisher, and year.
5. **Delete Book Panel**:delete books which are not available in library.
6. **View Books Panel**: Display and refresh current book records,include search box.
7. **Issue Book Panel**: Process book issues with details on student info.
8. **View Issued Books Panel**: View currently issued books with refresh option.
9. **Return Book Panel**: Manage book returns with automated status updates.

---

## 🚀 Getting Started

1. **Clone the Repository**
   ```bash
   git clone https://github.com/your-username/library-management-system.git
   cd library-management-system
   ```

2. **Set Up Database**  
   - Create a MySQL database named `library_management`.
   - Use the following schema:

     ```sql
     CREATE DATABASE library_management;
     USE library_management;

     CREATE TABLE activity_log (
      id INT NOT NULL AUTO_INCREMENT,
      registration_no VARCHAR(255) NOT NULL,
      name VARCHAR(255) NOT NULL,
      activity VARCHAR(255) NOT NULL,
      date DATE NOT NULL,
      time TIME NOT NULL,
      PRIMARY KEY (id)
     );


     CREATE TABLE books (
      id INT NOT NULL AUTO_INCREMENT,
      title VARCHAR(100) NOT NULL,
      author VARCHAR(100) NOT NULL,
      publisher VARCHAR(100) NOT NULL,
      year INT NOT NULL,
      available TINYINT(1) DEFAULT 1,
      PRIMARY KEY (id)
     );


     CREATE TABLE issued_books (
      id INT NOT NULL AUTO_INCREMENT,
      book_id INT NOT NULL UNIQUE,
      student_name VARCHAR(100) NOT NULL,
      registration_number VARCHAR(50) NOT NULL,
      issue_date DATE NOT NULL,
      return_date DATE NOT NULL,
      PRIMARY KEY (id)
     );


    
        
     ```

3. **Run the Application**
   - Update the `DatabaseConnection` credentials in `LibraryManagementSystem.java`:
     ```java
     db = new DatabaseConnection("jdbc:mysql://localhost:3306/library_management", "root", "your_password");
     ```

4. **Compile and Run**
   - Use any Java IDE or compile manually:
     ```bash
     javac LibraryManagementSystem.java
     java LibraryManagementSystem
     ```

---
##SCREEN-SHORT:
"C:\Users\rkste\Desktop\PROJECT HOLDING TEAM FOR SCREEN SHORT WORK IS ONN\Screenshot 2024-11-04 130100.png"
"C:\Users\rkste\Desktop\PROJECT HOLDING TEAM FOR SCREEN SHORT WORK IS ONN\Screenshot 2024-11-04 130201.png"
"C:\Users\rkste\Desktop\PROJECT HOLDING TEAM FOR SCREEN SHORT WORK IS ONN\Screenshot 2024-11-04 130522.png"
"C:\Users\rkste\Desktop\PROJECT HOLDING TEAM FOR SCREEN SHORT WORK IS ONN\Screenshot 2024-11-04 130301.png"
"C:\Users\rkste\Desktop\PROJECT HOLDING TEAM FOR SCREEN SHORT WORK IS ONN\Screenshot 2024-11-04 130456.png"
"C:\Users\rkste\Desktop\PROJECT HOLDING TEAM FOR SCREEN SHORT WORK IS ONN\Screenshot 2024-11-04 130618.png"
"C:\Users\rkste\Desktop\PROJECT HOLDING TEAM FOR SCREEN SHORT WORK IS ONN\Screenshot 2024-11-04 130645.png"
"C:\Users\rkste\Desktop\PROJECT HOLDING TEAM FOR SCREEN SHORT WORK IS ONN\Screenshot 2024-11-04 130933.png"


## 🛠️ Requirements

- **Java 8+**
- **MySQL Database**
- **JDBC Driver** (MySQL Connector)

---

## 📂 Project Structure

- **LibraryManagementSystem.java**: Main class and GUI controller.
- **DatabaseConnection.java**: Manages database connection.
- **BookManager.java**: Handles book data operations (add, issue, return).

---

## 📋 Contributing

Contributions are welcome! To contribute:

1. Fork this repository.
2. Create a new branch for your feature.
3. Commit your changes and open a Pull Request.

---

## 📄 License

This project is licensed under the MIT License.

---

Happy Coding!
