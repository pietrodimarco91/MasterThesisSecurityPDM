package custom;

import com.mysql.jdbc.ResultSetInternalMethods;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

import javax.servlet.http.HttpSession;
import java.io.StringReader;
import java.sql.*;

public class OracleInterceptor {

    Connection con;
    Statement stmt;

    public OracleInterceptor(String sSQL) {
        try{
//step1 load the driver class
            Class.forName("oracle.jdbc.driver.OracleDriver");

//step2 create  the connection object
            con=DriverManager.getConnection(
                    "jdbc:oracle:thin:@localhost:1521:xe","system","oracle");



        }catch(Exception e){ System.out.println(e);}
    }
    /*
     * ====================================
     * ATTRIBUTES
     * ====================================
     */

    private String pauseFilter = "";

    /*
     * ====================================
     * PRIVATE METHODS
     * ====================================
     */

    /**
     * This method checks if the running query is the login one
     *
     * @return True if the current query is the login one
     */
    private boolean isFilterPaused() {
        HttpSession sessionObj = SessionInfo.getSessionValue(new Long(Thread.currentThread().getId()));
        pauseFilter = (String) sessionObj.getAttribute("PauseFilter");
        if (pauseFilter == null || pauseFilter.equals("") || pauseFilter.equals("no"))
            return false;
        return true;
    }

    /*
     * The JSP applications usually replace ' with '' . Here we see if an
     * attacker tried to bypass this simple protection. In such a case, go back
     * to the original ' , and let our Interceptor do the job of preventing the attack.
     */
    private String editEscapingCharactersInQuery(String stringSQL) {
        final String SQL_ESCAPING = "\''";
        final String SQL_ESCAPING_REGEX = "\\''";
        if (stringSQL.contains(SQL_ESCAPING))
            return stringSQL.replace(SQL_ESCAPING_REGEX, "'");
        // else
        return stringSQL;
    }

    /*
     * ====================================
     * PUBLIC METHODS
     * ====================================
     */

    public ResultSet preProcess(String stringSQL) {

        //step3 create the statement object
        try {
            stmt = con.createStatement();


            stringSQL = editEscapingCharactersInQuery(stringSQL);

            if (isFilterPaused())
                return null;

            CCJSqlParserManager pm = new CCJSqlParserManager();
            net.sf.jsqlparser.statement.Statement statement;

            try {
                statement = pm.parse(new StringReader(stringSQL));
                ViewCreator viewCreator = new ViewCreator();

                if (statement instanceof Select) {
                    Select selectStatement = (Select) statement;

                    viewCreator.getTableList(selectStatement);

                    System.out.println("-------------------------------------------------------------");
                    System.out.println("Original Query: " + stringSQL);
                    System.out.println("Modified Query: " + selectStatement.toString());
                    ResultSet result = stmt.executeQuery(selectStatement.toString());
                    return result;
                } else if (statement instanceof Insert) {
                } else if (statement instanceof Update) {
                } else if (statement instanceof Delete) {
                }
            } catch (JSQLParserException e) {
                //System.out.println("Wrong SQL query string : " + stringSQL);
            } catch (SQLException e) {
                e.printStackTrace();
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void close(ResultSet result) {
        try {
            stmt.executeQuery("ALTER SYSTEM FLUSH BUFFER_CACHE");
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
