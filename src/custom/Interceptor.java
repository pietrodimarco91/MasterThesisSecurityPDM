package custom;

import java.io.StringReader;
import java.sql.SQLException;
import java.util.Properties;

import javax.servlet.http.HttpSession;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.ResultSetInternalMethods;
import com.mysql.jdbc.Statement;

/**
 * This is the main class of the all Interceptor, the query editing process
 * starts here.
 * @author paolobruzzo
 *
 */
public class Interceptor implements com.mysql.jdbc.StatementInterceptorV2 {

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

	@Override
	public void init(Connection cnctn, Properties prprts) throws SQLException {
	}
	
	@Override
	public ResultSetInternalMethods preProcess(String stringSQL, Statement stmnt, Connection cnctn) throws SQLException {
		
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
				return (ResultSetInternalMethods) cnctn.createStatement().executeQuery(selectStatement.toString());
			} else
			if (statement instanceof Insert) {
			} else
			if (statement instanceof Update) {
			} else
			if (statement instanceof Delete) {
			}
		} catch (JSQLParserException e) {
			//System.out.println("Wrong SQL query string : " + stringSQL);
		}

		return null;
	}

	@Override
	public boolean executeTopLevelOnly() {
		return true;
	}

	@Override
	public void destroy() {
	}

	@Override
	public ResultSetInternalMethods postProcess(String stringSQL, Statement stmnt, ResultSetInternalMethods rsim, Connection cnctn, int i,
			boolean bln, boolean bln1, SQLException sqle) throws SQLException {
		return null;
	}

}
