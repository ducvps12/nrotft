import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDB {
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/nrotft?useUnicode=true&characterEncoding=UTF-8", "root", "");
            Statement stmt = conn.createStatement();
            
            System.out.println("--- CT Goku SSJ ---");
            ResultSet rs = stmt.executeQuery("SELECT * FROM item_template WHERE name LIKE '%Goku SSJ%'");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + " - " + rs.getString("name") + " (Head: " + rs.getInt("head") + ")");
            }
            
            System.out.println("--- HeadAvatar ---");
            ResultSet rs2 = stmt.executeQuery("SELECT * FROM head_avatar WHERE head_id IN (1454, 57, 101) LIMIT 10");
            while (rs2.next()) {
                System.out.println("Head: " + rs2.getInt("head_id") + " -> Avatar: " + rs2.getInt("avatar_id"));
            }
            
            System.out.println("--- Part Goku ---");
            ResultSet rs3 = stmt.executeQuery("SELECT * FROM part WHERE name LIKE '%Goku SSJ%'");
            while (rs3.next()) {
                System.out.println("ID: " + rs3.getInt("id") + " - " + rs3.getString("name"));
            }
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
