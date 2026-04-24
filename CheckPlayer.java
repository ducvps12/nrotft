import java.sql.*;

public class CheckPlayer {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DriverManager.getConnection(
            "jdbc:mysql://103.157.204.182:3306/nrotft", "root", "Nro@2026!");
        
        // Check account
        System.out.println("=== ACCOUNT ===");
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM account WHERE id = 49");
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        while (rs.next()) {
            for (int i = 1; i <= cols; i++) {
                System.out.println(meta.getColumnName(i) + ": " + rs.getString(i));
            }
        }
        rs.close();
        
        // Check player
        System.out.println("\n=== PLAYER ===");
        rs = st.executeQuery("SELECT * FROM player WHERE account_id = 49");
        meta = rs.getMetaData();
        cols = meta.getColumnCount();
        while (rs.next()) {
            for (int i = 1; i <= cols; i++) {
                System.out.println(meta.getColumnName(i) + ": " + rs.getString(i));
            }
        }
        rs.close();
        
        conn.close();
    }
}
