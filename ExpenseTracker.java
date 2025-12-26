import java.awt.Component;
import java.awt.EventQueue;
import java.awt.LayoutManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.sqlite.SQLiteDataSource;
import com.formdev.flatlaf.FlatLightLaf;

public class ExpenseTracker {
   private static Connection conn;
   private static SQLiteDataSource ds;
   private JFrame frame;
   private JTable table;
   private JTextField dateField;
   private JTextField descField;
   private JTextField amountField;
   private JTextField nameField;
   
   private int currentAccountId = 0;

   public static void main(String[] args) {
      EventQueue.invokeLater(() -> {
         try {
            ExpenseTracker window = new ExpenseTracker();
            window.frame.setVisible(true);
         } catch (Exception e) {
            e.printStackTrace();
         }
      });
   }

   public ExpenseTracker() {
      this.initDB();
      this.initialize();
   }

   private void initDB() {
      ds = new SQLiteDataSource();
      try {
         ds.setUrl("jdbc:sqlite:ExpensesDB.db");
         conn = ds.getConnection();
         Statement stmt = conn.createStatement();
         stmt.executeUpdate("CREATE TABLE IF NOT EXISTS accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT);");
         stmt.executeUpdate("CREATE TABLE IF NOT EXISTS expenses (id INTEGER PRIMARY KEY AUTOINCREMENT, account_id INTEGER, date TEXT, description TEXT, amount REAL, FOREIGN KEY (account_id) REFERENCES accounts(id));");
         stmt.close();
         conn.close();
      } catch (SQLException e) {
         e.printStackTrace();
         JOptionPane.showMessageDialog((Component)null, "Database initialization failed: " + e.getMessage());
      }
   }

   private void addAccount(String accountName) {
      if (accountName != null && !accountName.trim().isEmpty()) {
         try {
            conn = ds.getConnection();
            String sql = "INSERT INTO accounts (name) VALUES (?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, accountName);
            stmt.executeUpdate();
            stmt.close();
            conn.close();
            JOptionPane.showMessageDialog(this.frame, "Account Added Successfully");
         } catch (SQLException e) {
            JOptionPane.showMessageDialog(this.frame, "Error adding account: " + e.getMessage());
         }
      } else {
         JOptionPane.showMessageDialog(this.frame, "Account name cannot be empty");
      }
   }

   public void loadData(DefaultTableModel model, int accountId) throws SQLException {
      model.setRowCount(0);
      conn = ds.getConnection();
      String sql = "SELECT date, description, amount FROM expenses WHERE account_id = ?";
      PreparedStatement ps = conn.prepareStatement(sql);
      ps.setInt(1, accountId);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
         model.addRow(new Object[]{rs.getString("date"), rs.getString("description"), rs.getDouble("amount")});
      }
      rs.close();
      ps.close();
      conn.close();
   }

   private String[] getAccountDetails(int accountId) {
      double totalExpense = 0.0;
      String accountName = null;
      try {
         conn = ds.getConnection();
         PreparedStatement ps1 = conn.prepareStatement("SELECT name FROM accounts WHERE id = ?");
         ps1.setInt(1, accountId);
         ResultSet rs1 = ps1.executeQuery();
         if (rs1.next()) {
            accountName = rs1.getString("name");
            PreparedStatement ps2 = conn.prepareStatement("SELECT SUM(amount) AS total FROM expenses WHERE account_id = ?");
            ps2.setInt(1, accountId);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
               totalExpense = rs2.getDouble("total");
            }
            rs2.close();
            ps2.close();
         }
         rs1.close();
         ps1.close();
         conn.close();
      } catch (SQLException e) {
         JOptionPane.showMessageDialog(this.frame, "Error retrieving account details: " + e.getMessage());
      }
      return new String[]{accountName, String.valueOf(totalExpense)};
   }

   private void addExpense(int accountId, String date, String description, double amount) {
      if (accountId == 0) {
         JOptionPane.showMessageDialog(this.frame, "Please select an account first.");
      } else {
         try {
            conn = ds.getConnection();
            String sql = "INSERT INTO expenses (account_id, date, description, amount) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, accountId);
            stmt.setString(2, date);
            stmt.setString(3, description);
            stmt.setDouble(4, amount);
            stmt.executeUpdate();
            stmt.close();
            conn.close();
            JOptionPane.showMessageDialog(this.frame, "Expense added successfully to Account ID: " + accountId);
         } catch (SQLException e) {
            JOptionPane.showMessageDialog(this.frame, "Error adding expense: " + e.getMessage());
         }
      }
   }

   public void updateCombox(JComboBox<String> cbx) throws SQLException {
      cbx.removeAllItems();
      conn = ds.getConnection();
      String sql = "SELECT * FROM accounts;";
      PreparedStatement ps = conn.prepareStatement(sql);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
         cbx.addItem(rs.getInt("id") + "|" + rs.getString("name"));
      }
      rs.close();
      ps.close();
      conn.close();
   }

   private void initialize() {
      this.frame = new JFrame();
      this.frame.setBounds(100, 100, 600, 400);
      this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.frame.getContentPane().setLayout(null);
      this.frame.setTitle("Expense Tracker by DataFlair");

      JPanel topPanel = new JPanel();
      topPanel.setBounds(0, 0, 590, 58);
      this.frame.getContentPane().add(topPanel);
      topPanel.setLayout(null);

      JLabel lblSelectAc = new JLabel("Select A/C:");
      lblSelectAc.setBounds(0, 0, 75, 15);
      topPanel.add(lblSelectAc);

      JComboBox<String> accBox = new JComboBox<>();
      accBox.setBounds(86, 0, 130, 24);
      topPanel.add(accBox);

      JLabel lblName = new JLabel("Name:");
      lblName.setBounds(10, 37, 70, 15);
      topPanel.add(lblName);

      this.nameField = new JTextField();
      this.nameField.setBounds(86, 27, 130, 30);
      topPanel.add(this.nameField);

      JButton btnAddAc = new JButton("Add A/C");
      btnAddAc.setBounds(223, 27, 117, 30);
      btnAddAc.addActionListener(e -> {
         this.addAccount(this.nameField.getText());
         try { this.updateCombox(accBox); } catch (SQLException ex) { ex.printStackTrace(); }
      });
      topPanel.add(btnAddAc);

      JButton btnSelect = new JButton("Select");
      btnSelect.setBounds(223, 0, 117, 25);
      topPanel.add(btnSelect);

      JScrollPane scrollPane = new JScrollPane();
      scrollPane.setBounds(0, 60, 590, 211);
      this.frame.getContentPane().add(scrollPane);

      this.table = new JTable();
      this.table.setModel(new DefaultTableModel(new Object[0][], new String[]{"Date", "Description", "Amount"}));
      scrollPane.setViewportView(this.table);

      JPanel bottomPanel = new JPanel();
      bottomPanel.setBounds(0, 270, 600, 90);
      this.frame.getContentPane().add(bottomPanel);
      bottomPanel.setLayout(null);

      JLabel lblDate = new JLabel("Date:");
      lblDate.setBounds(0, 5, 50, 15);
      bottomPanel.add(lblDate);

      this.dateField = new JTextField();
      this.dateField.setBounds(50, 5, 114, 30);
      bottomPanel.add(this.dateField);

      JLabel lblDesc = new JLabel("Description:");
      lblDesc.setBounds(180, 5, 90, 15);
      bottomPanel.add(lblDesc);

      this.descField = new JTextField();
      this.descField.setBounds(270, 5, 114, 30);
      bottomPanel.add(this.descField);

      JLabel lblAmount = new JLabel("Amount:");
      lblAmount.setBounds(390, 5, 70, 15);
      bottomPanel.add(lblAmount);

      this.amountField = new JTextField();
      this.amountField.setBounds(456, 5, 114, 30);
      bottomPanel.add(this.amountField);

      JButton btnAddExpense = new JButton("Add");
      btnAddExpense.setBounds(239, 39, 117, 25);
      btnAddExpense.addActionListener(e -> {
         try {
            this.addExpense(this.currentAccountId, this.dateField.getText(), this.descField.getText(), Double.parseDouble(this.amountField.getText()));
            this.loadData((DefaultTableModel)this.table.getModel(), this.currentAccountId);
         } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this.frame, "Error adding expense: " + ex.getMessage());
         }
      });
      bottomPanel.add(btnAddExpense);

      JLabel lblTotalExpense = new JLabel("Total Expense : ");
      lblTotalExpense.setBounds(22, 75, 120, 15);
      bottomPanel.add(lblTotalExpense);

      JLabel lblTotalAmount = new JLabel("--");
      lblTotalAmount.setBounds(143, 75, 70, 15);
      bottomPanel.add(lblTotalAmount);

      JLabel lblCurrAcc = new JLabel("Current Acc Name:");
      lblCurrAcc.setBounds(270, 76, 130, 15);
      bottomPanel.add(lblCurrAcc);

      JLabel lblAccountName = new JLabel("Account Name");
      lblAccountName.setBounds(412, 76, 130, 15);
      bottomPanel.add(lblAccountName);

      btnSelect.addActionListener(e -> {
         try {
            String selected = (String) accBox.getSelectedItem();
            if (selected == null) return;
            String idStr = selected.substring(0, selected.indexOf('|'));
            this.currentAccountId = Integer.parseInt(idStr);
            this.loadData((DefaultTableModel)this.table.getModel(), this.currentAccountId);
            String[] details = this.getAccountDetails(this.currentAccountId);
            lblTotalAmount.setText(details[1]);
            lblAccountName.setText(details[0]);
         } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this.frame, "Error selecting account: " + ex.getMessage());
         }
      });

      try { this.updateCombox(accBox); } catch (SQLException ex) { ex.printStackTrace(); }
   }
}
