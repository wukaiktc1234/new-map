import java.sql.*;

public class CheckTemplates {
    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection(
            "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;IFEXISTS=TRUE", "sa", "");
        Statement st = conn.createStatement();

        System.out.println("=== Sprint 2 notification templates ===");
        ResultSet rs = st.executeQuery(
            "SELECT template_code, title, status FROM msg_template " +
            "WHERE template_code LIKE 'recruitment.%' OR template_code LIKE 'offer.%' " +
            "ORDER BY template_code");
        int count = 0;
        while (rs.next()) {
            count++;
            System.out.println("  " + rs.getString(1) + " | " + rs.getString(2) + " | status=" + rs.getInt(3));
        }
        System.out.println("TOTAL Sprint2 templates: " + count);

        System.out.println();
        System.out.println("=== Sprint 2 tables check ===");
        String[] tables = {"recruitment_quotas", "recruitment_feedback", "job_offers"};
        for (String t : tables) {
            try {
                ResultSet trs = st.executeQuery("SELECT COUNT(*) FROM " + t);
                trs.next();
                System.out.println("  Table " + t + " exists, rows=" + trs.getInt(1));
                trs.close();
            } catch (SQLException e) {
                System.out.println("  Table " + t + " MISSING: " + e.getMessage());
            }
        }

        System.out.println();
        System.out.println("=== interviews table Sprint2 columns ===");
        ResultSet crs = st.executeQuery(
            "SELECT column_name FROM information_schema.columns " +
            "WHERE table_name='INTERVIEWS' AND column_name IN ('STORE_INTERVIEWER_ID','STORE_INTERVIEWER_NAME','STORE_EVALUATION','STORE_EVALUATION_TIME','STORE_EVALUATION_SCORE')");
        int colCount = 0;
        while (crs.next()) { colCount++; System.out.println("  " + crs.getString(1)); }
        System.out.println("  Sprint2 interview columns: " + colCount + "/5");
        crs.close();

        System.out.println();
        System.out.println("=== recruitment_requirements table Sprint2 columns ===");
        ResultSet qcrs = st.executeQuery(
            "SELECT column_name FROM information_schema.columns " +
            "WHERE table_name='RECRUITMENT_REQUIREMENTS' AND column_name='QUOTA_ID'");
        boolean hasQuotaId = qcrs.next();
        System.out.println("  quota_id column exists: " + hasQuotaId);
        qcrs.close();

        rs.close();
        st.close();
        conn.close();
    }
}
