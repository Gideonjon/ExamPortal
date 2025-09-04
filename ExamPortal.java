import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamPortal extends JFrame {
    private Connection conn;
    private String currentUser;

    public ExamPortal() {
        setTitle("Online Exam Portal");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initDatabase();
        showLoginScreen();
    }

    // ---------------- Database ----------------
    private void initDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:examportal.db");
            Statement stmt = conn.createStatement();

            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, username TEXT UNIQUE, password TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS questions (id INTEGER PRIMARY KEY, question TEXT, option1 TEXT, option2 TEXT, option3 TEXT, option4 TEXT, answer INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS attempts (id INTEGER PRIMARY KEY, username TEXT, score INTEGER)");

            // Add sample questions if empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM questions");
            if (rs.next() && rs.getInt("count") == 0) {
                stmt.execute("INSERT INTO questions (question, option1, option2, option3, option4, answer) VALUES" +
                        "('What is 2 + 2?', '3', '4', '5', '6', 2)," +
                        "('Capital of France?', 'Berlin', 'London', 'Paris', 'Rome', 3)," +
                        "('Java is ...?', 'A fruit', 'A language', 'A car', 'A drink', 2)");
            }

            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------------- Login Screen ----------------
    private void showLoginScreen() {
        getContentPane().removeAll();
        JPanel panel = new JPanel(new GridLayout(4, 2));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        JButton loginBtn = new JButton("Login");
        JButton signupBtn = new JButton("Sign Up");

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(loginBtn);
        panel.add(signupBtn);

        add(panel, BorderLayout.CENTER);

        loginBtn.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());
            if (authenticate(user, pass)) {
                currentUser = user;
                showMainMenu();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials.");
            }
        });

        signupBtn.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());
            if (register(user, pass)) {
                JOptionPane.showMessageDialog(this, "User registered successfully! You can login now.");
            } else {
                JOptionPane.showMessageDialog(this, "Username already exists.");
            }
        });

        revalidate();
        repaint();
    }

    private boolean authenticate(String username, String password) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE username=? AND password=?");
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean register(String username, String password) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    // ---------------- Main Menu ----------------
    private void showMainMenu() {
        getContentPane().removeAll();

        JButton takeExamBtn = new JButton("Take Exam");
        JButton attemptsBtn = new JButton("My Attempts");
        JButton aboutBtn = new JButton("About");
        JButton logoutBtn = new JButton("Logout");

        JPanel panel = new JPanel(new GridLayout(4, 1));
        panel.add(takeExamBtn);
        panel.add(attemptsBtn);
        panel.add(aboutBtn);
        panel.add(logoutBtn);

        add(panel, BorderLayout.CENTER);

        takeExamBtn.addActionListener(e -> showExamScreen());
        attemptsBtn.addActionListener(e -> showAttempts());
        aboutBtn.addActionListener(e -> showAbout());
        logoutBtn.addActionListener(e -> showLoginScreen());

        revalidate();
        repaint();
    }

    // ---------------- Exam Screen ----------------
    private void showExamScreen() {
        getContentPane().removeAll();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        List<JRadioButton[]> optionsList = new ArrayList<>();
        ButtonGroup[] groups;

        List<Integer> answers = new ArrayList<>();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM questions");
            while (rs.next()) {
                panel.add(new JLabel(rs.getString("question")));
                JRadioButton[] options = new JRadioButton[4];
                ButtonGroup group = new ButtonGroup();
                for (int i = 0; i < 4; i++) {
                    options[i] = new JRadioButton(rs.getString("option" + (i + 1)));
                    group.add(options[i]);
                    panel.add(options[i]);
                }
                optionsList.add(options);
                answers.add(rs.getInt("answer"));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JButton submitBtn = new JButton("Submit");
        panel.add(submitBtn);

        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane, BorderLayout.CENTER);

        submitBtn.addActionListener(e -> {
            int score = 0;
            for (int i = 0; i < optionsList.size(); i++) {
                JRadioButton[] options = optionsList.get(i);
                if (options[answers.get(i) - 1].isSelected()) {
                    score++;
                }
            }
            saveAttempt(score);
            JOptionPane.showMessageDialog(this, "Your score: " + score);
            showMainMenu();
        });

        revalidate();
        repaint();
    }

    private void saveAttempt(int score) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO attempts (username, score) VALUES (?, ?)");
            pstmt.setString(1, currentUser);
            pstmt.setInt(2, score);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ---------------- Attempts ----------------
    private void showAttempts() {
        getContentPane().removeAll();
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);

        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM attempts WHERE username=?");
            pstmt.setString(1, currentUser);
            ResultSet rs = pstmt.executeQuery();
            StringBuilder sb = new StringBuilder("Your Attempts:\n");
            while (rs.next()) {
                sb.append("Attempt #").append(rs.getInt("id")).append(": Score = ").append(rs.getInt("score")).append("\n");
            }
            textArea.setText(sb.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        add(new JScrollPane(textArea), BorderLayout.CENTER);
        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> showMainMenu());
        add(backBtn, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    // ---------------- About ----------------
    private void showAbout() {
        getContentPane().removeAll();

        JPanel panel = new JPanel(new BorderLayout());
        JLabel info = new JLabel("<html><center><h2>Online Exam Portal</h2><p>Created by Gideon</p></center></html>", SwingConstants.CENTER);

        // Load your image (place "myphoto.jpg" in the same folder as the .java file)
        ImageIcon icon = new ImageIcon("myphoto.jpg");
        Image scaledImg = icon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        JLabel imageLabel = new JLabel(new ImageIcon(scaledImg));

        panel.add(info, BorderLayout.NORTH);
        panel.add(imageLabel, BorderLayout.CENTER);

        JButton backBtn = new JButton("Back");
        backBtn.addActionListener(e -> showMainMenu());
        panel.add(backBtn, BorderLayout.SOUTH);

        add(panel);

        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ExamPortal().setVisible(true));
    }
}
