package custom;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import custom.entities.RelationView;
import custom.enums.Keywords;
import custom.utils.MyUtils;
import custom.utils.QueryWizard;

/**
 * The data stored in this class will be accessible for all the session context
 * 
 * @author paolobruzzo
 *
 */
public class SessionInfo {

	/*
	 * ==================================== 
	 * ATTRIBUTES
	 * ====================================
	 */

	// Used to store all the user sessions
	private static Hashtable<Long, HttpSession> globalsessionMap = new Hashtable<Long, HttpSession>();
	// Used to store the policies file associated to the application
	private static Hashtable<String, ArrayList<RelationView>> policies = new Hashtable<String, ArrayList<RelationView>>();
	// Filepath of the policy XML file
	private static String policyFilePath;
	// Time of the next update
	private static long nextUpdate = MyUtils.getMaxTime();
	// Thread that handles the policy update
	private static Thread updateThread;

	/*
	 * ==================================== 
	 * PUBLIC METHODS
	 * ====================================
	 */

	/**
	 * Sets the current user, by mapping the thread id with the session object
	 * 
	 * @param threadId
	 * @param sessionObj
	 */
	public static void setSessionValues(Long threadId, HttpSession sessionObj) {
		globalsessionMap.put(threadId, sessionObj);
		return;
	}

	/**
	 * Returns the current session object
	 * 
	 * @param threadId
	 * @return the http session
	 */
	public static HttpSession getSessionValue(Long threadId) {
		return globalsessionMap.get(threadId);
	}

	/**
	 * Set the filepath of the policies file to read
	 * 
	 * @param filePath
	 */
	public static void setPolicyFilepath(String filePath) {
		policyFilePath = filePath;
	}

	/*
	 * In case the policies hasn't been read in this context, read it from the
	 * file. Otherwise just return the one stored in memory
	 */
	public static Hashtable<String, ArrayList<RelationView>> getPolicies() {
		// TODO: restore the if statement
		if (policies.isEmpty())
			storePolicyFromFile(policyFilePath);
		// In each case
		return policies;
	}

	/*
	 * ==================================== 
	 * PRIVATE METHODS
	 * ====================================
	 */

	private static void storePolicyFromFile(String filepath) {
		try {

			// Read the file
			File fXmlFile = new File(filepath);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			// Parse and normalize the file
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			// Get the root element
			Element rootElement = doc.getDocumentElement();
			System.out.println("	+++++++++++++ Read policies from File ++++++++++++++++");
			System.out.println("		Filepath: " + filepath);
			System.out.println("		Root element :" + rootElement.getNodeName());

			// Get all the tables mentioned in the file
			NodeList tables = rootElement.getElementsByTagName(Keywords.TABLE.toString());

			// For each table found
			for (int i = 0; i < tables.getLength(); i++) {

				// Store the current table
				Element table = (Element) tables.item(i);
				String tableName = table.getAttribute(Keywords.TABLE_NAME.toString());

				String[] parts = tableName.split(" JOIN ");
				System.out.println("		- Table Name: " + tableName);

				for (int k=0;k<parts.length;k++) {
					// Get the columns and place them into a List
					String columnsString = table.getElementsByTagName(Keywords.TABLE_COLUMNS.toString()).item(0).getTextContent();
					List<String> columns = new ArrayList<String>(Arrays.asList(columnsString.split(Keywords.VALUES_SEPARATOR.toString())));

					//Pietro implement JOIN
					ArrayList<JoinTable> joinTables = new ArrayList<JoinTable>();
					for (int j = 0; j < table.getElementsByTagName(Keywords.TABLE_COLUMNS_JOIN.toString()).getLength(); j++) {
						joinTables.add(new JoinTable(table.getElementsByTagName(Keywords.TABLE_COLUMNS_JOIN.toString()).item(j).getAttributes().getNamedItem("table").getTextContent(), table.getElementsByTagName(Keywords.TABLE_COLUMNS_JOIN.toString()).item(j).getTextContent().split(Keywords.VALUES_SEPARATOR.toString())));
					}

					// Get the list of all the table policies, and prepare the
					// arrayList to store them
					NodeList relationViews = table.getElementsByTagName(Keywords.RELATION_VIEW.toString());
					ArrayList<RelationView> policyList = new ArrayList<RelationView>();

					// For each policy of this table
					for (int j = 0; j < relationViews.getLength(); j++) {

						// Store the current policy
						Element rView = (Element) relationViews.item(j);

						// TODO: add permissions

						// Get dates
						String beginDate = rView.getElementsByTagName(Keywords.BEGIN_DATE.toString()).item(0).getTextContent();
						String endDate = rView.getElementsByTagName(Keywords.EXPIRATION_DATE.toString()).item(0).getTextContent();

						// Update the time of the next update
						nextUpdate = updateNextUpdate(beginDate, endDate);

						// Store the policy only if it is active in this moment
						if (policyIsActive(beginDate, endDate)) {
							// Fill the relation view with user role and policy
							String userRole = rView.getElementsByTagName(Keywords.USER_ROLE.toString()).item(0).getTextContent();
							String policyRule = rView.getElementsByTagName(Keywords.POLICY.toString()).item(0).getTextContent();

							policyList.add(new RelationView(userRole, policyRule, null, joinTables, beginDate, endDate));
							System.out.println("			- From " + beginDate + " To " + endDate + " - Role " + userRole + " - Policy: " + policyRule + "\n Tables:" + joinTables);
						}
					}

					QueryWizard queryWizard = new QueryWizard(policyList, tableName, columns,String.valueOf(k));
					policies.put(parts[k], queryWizard.getModifiedRelations());
				}
			}

			printStoredPolicies(policies);
			System.out.println("	++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			
			// Set thread that handles the update of the policy rules
			startPolicyUpdateTimer(updateThread, nextUpdate);
			nextUpdate = MyUtils.getMaxTime();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Clears the policies to re-launch the update
	private static void startPolicyUpdateTimer(Thread updateThread, final long nextUpdate) {
		updateThread = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(nextUpdate);
					policies.clear();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		updateThread.start();
	}

	private static boolean policyIsActive(String begin, String end) throws DOMException, ParseException {
		// Dates
		Calendar beginDate = MyUtils.getDateFromString(begin);
		Calendar now = MyUtils.getCurrentDateTime();
		Calendar endDate = MyUtils.getDateFromString(end);

		// Check validity
		if (now.after(beginDate) && now.before(endDate))
			return true;

		return false;
	}

	// Computes the time in which the next policy update needs to be done
	private static long updateNextUpdate(String begin, String end) throws ParseException {
		Calendar beginDate = MyUtils.getDateFromString(begin);
		Calendar now = MyUtils.getCurrentDateTime();
		Calendar endDate = MyUtils.getDateFromString(end);

		if (beginDate.after(now) && (beginDate.getTimeInMillis() - now.getTimeInMillis() < nextUpdate))
			nextUpdate = beginDate.getTimeInMillis() - now.getTimeInMillis();

		if (endDate.after(now) && (endDate.getTimeInMillis() - now.getTimeInMillis() < nextUpdate))
			nextUpdate = endDate.getTimeInMillis() - now.getTimeInMillis();

		return nextUpdate;

	}

	private static void printStoredPolicies(Hashtable<String, ArrayList<RelationView>> policies) {
		System.out.println("		STORED policies:");
		for (String key : policies.keySet()) {
			System.out.println("		-Table: " + key);
			for (RelationView r : policies.get(key)) {
				System.out.println("			- From " + r.getBeginDateToString() + " To " + r.getExpirationDateToString() + " - Role "+ r.getRole() + " - Policy: " + r.getPolicy());
			}
		}
	}

}
