import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import org.sqlite.SQLiteDataSource;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class ExpenseTracker {
    private static Connection conn;
    private static SQLiteDataSource ds;
    private JFrame frame;
    private JTable table;
    private JTextField dateField, descField, amountField, nameField;
    private int currentAccountId = 0;
    private JLabel lblTotalValue;
    private JComboBox<String> accBox;
    private boolean darkMode = false;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                new ExpenseTracker().frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public ExpenseTracker() {
        loadThemePreference();
        setLookAndFeel();
        initDB();
        initialize();
    }

    // === THEME HANDLING ===
    private void loadThemePreference() {
        try (BufferedReader br = new BufferedReader(new FileReader("theme.txt"))) {
            darkMode = "dark".equalsIgnoreCase(br.readLine());
        } catch (IOException ignored) {}
    }

    private void saveThemePreference() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("theme.txt"))) {
            bw.write(darkMode ? "dark" : "light");
        } catch (IOException ignored) {}
    }

    private void setLookAndFeel() {
        try {
            if (darkMode) {
                UIManager.setLookAndFeel(new FlatDarculaLaf());
            } else {
                try {
                    UIManager.setLookAndFeel("com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme");
                } catch (Exception e) {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === DATABASE INITIALIZATION ===
    private void initDB() {
        ds = new SQLiteDataSource();
        try {
            ds.setUrl("jdbc:sqlite:ExpensesDB.db");
            conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS expenses (id INTEGER PRIMARY KEY AUTOINCREMENT, account_id INTEGER, date TEXT, description TEXT, amount REAL, FOREIGN KEY (account_id) REFERENCES accounts(id))");
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database initialization failed: " + e.getMessage());
        }
    }

    // === MAIN INTERFACE ===
    private void initialize() {
        frame = new JFrame("💼 Expense Tracker Pro");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 650);
        frame.setLocationRelativeTo(null);

        JPanel background = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                if (darkMode) {
                    g2.setPaint(new GradientPaint(0, 0, new Color(30, 30, 35), getWidth(), getHeight(), new Color(50, 50, 55)));
                } else {
                    g2.setPaint(new GradientPaint(0, 0, new Color(245, 247, 255), getWidth(), getHeight(), new Color(225, 230, 245)));
                }
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        background.setBorder(new EmptyBorder(15, 15, 15, 15));
        frame.setContentPane(background);

        // === HEADER BAR ===
        JPanel header = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = darkMode ? new Color(60, 70, 90) : new Color(70, 110, 250);
                Color c2 = darkMode ? new Color(40, 45, 60) : new Color(140, 70, 255);
                GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), getHeight(), c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            }
        };
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);
        JLabel logo = new JLabel(new ImageIcon("logo.png"));
        JLabel title = new JLabel("Expense Tracker Pro");
        title.setFont(new Font("Segoe UI Semibold", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        titlePanel.add(logo);
        titlePanel.add(title);

        JButton themeToggle = new JButton(darkMode ? "☀ Light Mode" : "🌙 Dark Mode");
        themeToggle.setFocusPainted(false);
        themeToggle.setForeground(Color.WHITE);
        themeToggle.setBackground(new Color(255, 255, 255, 40));
        themeToggle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        themeToggle.setBorder(new RoundedBorder(10));
        themeToggle.setPreferredSize(new Dimension(140, 35));
        themeToggle.addActionListener(e -> toggleTheme());

        header.add(titlePanel, BorderLayout.WEST);
        header.add(themeToggle, BorderLayout.EAST);
        background.add(header, BorderLayout.NORTH);

        // === SIDEBAR ===
        JPanel sidebar = new JPanel(new GridBagLayout());
        sidebar.setOpaque(false);
        sidebar.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                "Accounts", TitledBorder.LEADING, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)
        ));
        sidebar.setPreferredSize(new Dimension(260, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        accBox = new JComboBox<>();
        nameField = new JTextField();
        JButton btnAddAcc = new JButton("➕ Add Account");
        JButton btnDeleteAcc = new JButton("🗑 Delete Account");
        JButton btnSelectAcc = new JButton("✅ Select");
        JButton btnReceipt = new JButton("🧾 Generate Receipt");

        int y = 0;
        gbc.gridx = 0; gbc.gridy = y++; sidebar.add(new JLabel("Select Account:"), gbc);
        gbc.gridy = y++; sidebar.add(accBox, gbc);
        gbc.gridy = y++; sidebar.add(new JLabel("New Account:"), gbc);
        gbc.gridy = y++; sidebar.add(nameField, gbc);
        gbc.gridy = y++; sidebar.add(btnAddAcc, gbc);
        gbc.gridy = y++; sidebar.add(btnDeleteAcc, gbc);
        gbc.gridy = y++; sidebar.add(btnSelectAcc, gbc);
        gbc.gridy = y++; sidebar.add(btnReceipt, gbc);

        // === MAIN TABLE AREA ===
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                "Expenses", TitledBorder.LEADING, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 14)
        ));

        table = new JTable(new DefaultTableModel(new Object[]{"Date", "Description", "Amount"}, 0));
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // === BOTTOM BAR ===
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        dateField = new JTextField(10);
        descField = new JTextField(15);
        amountField = new JTextField(10);
        JButton btnAddExp = new JButton("💰 Add Expense");
        JButton btnDeleteExp = new JButton("🗑 Delete Selected");
        lblTotalValue = new JLabel("0.00");
        lblTotalValue.setFont(new Font("Segoe UI", Font.BOLD, 16));

        c.gridx = 0; bottomPanel.add(new JLabel("Date:"), c);
        c.gridx = 1; bottomPanel.add(dateField, c);
        c.gridx = 2; bottomPanel.add(new JLabel("Description:"), c);
        c.gridx = 3; bottomPanel.add(descField, c);
        c.gridx = 4; bottomPanel.add(new JLabel("Amount:"), c);
        c.gridx = 5; bottomPanel.add(amountField, c);
        c.gridx = 6; bottomPanel.add(btnAddExp, c);
        c.gridx = 7; bottomPanel.add(btnDeleteExp, c);
        c.gridx = 8; bottomPanel.add(new JLabel("Total:"), c);
        c.gridx = 9; bottomPanel.add(lblTotalValue, c);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        background.add(sidebar, BorderLayout.WEST);
        background.add(mainPanel, BorderLayout.CENTER);

        // === ACTIONS ===
        btnAddAcc.addActionListener(e -> { addAccount(nameField.getText()); refreshAccounts(); nameField.setText(""); });
        btnDeleteAcc.addActionListener(e -> deleteAccountAction());
        btnSelectAcc.addActionListener(e -> selectAccount());
        btnAddExp.addActionListener(e -> addExpenseAction());
        btnDeleteExp.addActionListener(e -> deleteSelectedExpense());
        btnReceipt.addActionListener(e -> generateReceipt());

        refreshAccounts();
    }

    // === UTILITY CLASSES ===
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        RoundedBorder(int radius) { this.radius = radius; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 100));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
        }
    }

    // === LOGIC ===
    private void toggleTheme() {
        darkMode = !darkMode;
        saveThemePreference();
        frame.dispose();
        SwingUtilities.invokeLater(() -> new ExpenseTracker().frame.setVisible(true));
    }

    private void refreshAccounts() {
        try {
            accBox.removeAllItems();
            conn = ds.getConnection();
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM accounts");
            while (rs.next()) accBox.addItem(rs.getInt("id") + "|" + rs.getString("name"));
            conn.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        }
    }

    private void selectAccount() {
        String selected = (String) accBox.getSelectedItem();
        if (selected == null) return;
        currentAccountId = Integer.parseInt(selected.split("\\|")[0]);
        refreshExpenses();
    }

    private void addAccount(String name) {
        if (name.isBlank()) {
            JOptionPane.showMessageDialog(frame, "Account name cannot be empty.");
            return;
        }
        try {
            conn = ds.getConnection();
            PreparedStatement ps = conn.prepareStatement("INSERT INTO accounts (name) VALUES (?)");
            ps.setString(1, name);
            ps.executeUpdate();
            conn.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        }
    }

    private void deleteAccountAction() {
        String selected = (String) accBox.getSelectedItem();
        if (selected == null) return;
        int id = Integer.parseInt(selected.split("\\|")[0]);
        int confirm = JOptionPane.showConfirmDialog(frame, "Delete this account and all expenses?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) deleteAccount(id);
    }

    private void deleteAccount(int id) {
        try {
            conn = ds.getConnection();
            conn.createStatement().executeUpdate("DELETE FROM expenses WHERE account_id=" + id);
            conn.createStatement().executeUpdate("DELETE FROM accounts WHERE id=" + id);
            conn.close();
            ((DefaultTableModel) table.getModel()).setRowCount(0);
            lblTotalValue.setText("0.00");
            currentAccountId = 0;
            refreshAccounts();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        }
    }

    private void addExpenseAction() {
        try {
            addExpense(currentAccountId, dateField.getText(), descField.getText(), Double.parseDouble(amountField.getText()));
            dateField.setText(""); descField.setText(""); amountField.setText("");
            refreshExpenses();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
        }
    }

    private void addExpense(int accId, String date, String desc, double amount) throws SQLException {
        if (accId == 0) throw new SQLException("No account selected");
        conn = ds.getConnection();
        PreparedStatement ps = conn.prepareStatement("INSERT INTO expenses (account_id, date, description, amount) VALUES (?, ?, ?, ?)");
        ps.setInt(1, accId);
        ps.setString(2, date);
        ps.setString(3, desc);
        ps.setDouble(4, amount);
        ps.executeUpdate();
        conn.close();
    }

    private void refreshExpenses() {
        try {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);
            double total = 0;
            conn = ds.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT date, description, amount FROM expenses WHERE account_id=?");
            ps.setInt(1, currentAccountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{rs.getString(1), rs.getString(2), rs.getDouble(3)});
                total += rs.getDouble(3);
            }
            lblTotalValue.setText(String.format("%.2f", total));
            conn.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        }
    }

    private void deleteSelectedExpense() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(frame, "Select an expense to delete.");
            return;
        }
        String date = (String) table.getValueAt(row, 0);
        String desc = (String) table.getValueAt(row, 1);
        double amt = (double) table.getValueAt(row, 2);
        try {
            conn = ds.getConnection();
            PreparedStatement ps = conn.prepareStatement("DELETE FROM expenses WHERE account_id=? AND date=? AND description=? AND amount=?");
            ps.setInt(1, currentAccountId);
            ps.setString(2, date);
            ps.setString(3, desc);
            ps.setDouble(4, amt);
            ps.executeUpdate();
            conn.close();
            refreshExpenses();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
        }
    }

    // === GENERATE RECEIPT FEATURE ===
    private void generateReceipt() {
        if (currentAccountId == 0) {
            JOptionPane.showMessageDialog(frame, "Please select an account first!");
            return;
        }
        try {
            conn = ds.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT name FROM accounts WHERE id=?");
            ps.setInt(1, currentAccountId);
            ResultSet accRs = ps.executeQuery();
            String accName = accRs.next() ? accRs.getString("name") : "Unknown";

            PreparedStatement ps2 = conn.prepareStatement("SELECT date, description, amount FROM expenses WHERE account_id=?");
            ps2.setInt(1, currentAccountId);
            ResultSet rs = ps2.executeQuery();

            StringBuilder receipt = new StringBuilder();
            receipt.append("🧾 EXPENSE RECEIPT\n\nAccount: ").append(accName).append("\n\n");
            receipt.append(String.format("%-12s %-25s %s\n", "Date", "Description", "Amount"));
            receipt.append("-----------------------------------------------------\n");

            double total = 0;
            while (rs.next()) {
                receipt.append(String.format("%-12s %-25s %.2f\n",
                        rs.getString("date"), rs.getString("description"), rs.getDouble("amount")));
                total += rs.getDouble("amount");
            }
            receipt.append("-----------------------------------------------------\n");
            receipt.append(String.format("Total: %.2f", total));

            JOptionPane.showMessageDialog(frame, new JScrollPane(new JTextArea(receipt.toString())), 
                    "Account Receipt", JOptionPane.INFORMATION_MESSAGE);
            conn.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error generating receipt: " + e.getMessage());
        }
    }
}
