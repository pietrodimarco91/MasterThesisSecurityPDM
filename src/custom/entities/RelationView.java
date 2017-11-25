package custom.entities;

import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.select.Select;
import custom.enums.Keywords;
import custom.utils.MyUtils;
import custom.utils.QueryFiledsFinder;

/**
 * This class contains a mapping of speciefied views related to the role of the
 * user on which the have to be applied.
 * 
 * @author paolobruzzo
 */
public class RelationView implements Comparable<RelationView> {

	private String role;
	private String policy;
	private String permission;
	private Calendar beginDate;
	private Calendar expirationDate;

	/**
	 * RelatonView Constructor
	 * 
	 * @param role
	 *            is the role of the user to whom the policy has to be applied
	 * @param view
	 *            is the policy query that will be use to build the view
	 * @param permission
	 *            are the permissions on the view
	 * @param beginDate
	 * 			  is the begin date of the view
	 * @param expirationDate
	 *            is the expiration date of the view
	 * @throws ParseException 
	 */
	public RelationView(String role, String view, String permission, String beginDate, String expirationDate) throws ParseException {
		this.role = role;
		this.policy = view;
		this.permission = permission;
		this.beginDate = MyUtils.getDateFromString(beginDate);
		this.expirationDate = MyUtils.getDateFromString(expirationDate);
	}

	public String getRole() {
		return role;
	}

	public String getPolicy() {
		return policy;
	}

	public String getPermission() {
		return permission;
	}
	
	public Calendar getBeginDate(){
		return beginDate;
	}

	public Calendar getExpirationDate() {
		return expirationDate;
	}
	
	public String getBeginDateToString(){
		return MyUtils.getStringFromDate(beginDate);
	}
	
	public String getExpirationDateToString(){
		return MyUtils.getStringFromDate(expirationDate);
	}
	
	/**
	 * Get the select items of this policy
	 * @return the select items of this policy
	 * @throws JSQLParserException
	 */
	public List<String> getSelectItemsList() throws JSQLParserException{
		CCJSqlParserManager parserManager = new CCJSqlParserManager();
		QueryFiledsFinder queryFieldsFinder = new QueryFiledsFinder();
		Select query = (Select) parserManager.parse(new StringReader(this.policy));
		return new ArrayList<String>(queryFieldsFinder.getSelectItemList(query));
	}
	
	/**
	 * Get the where condition of this policy
	 * @return the where condition
	 * @throws JSQLParserException
	 */
	public String getWhereCondition() throws JSQLParserException{
		CCJSqlParserManager parserManager = new CCJSqlParserManager();
		QueryFiledsFinder queryFieldsFinder = new QueryFiledsFinder();
		Select query = (Select) parserManager.parse(new StringReader(this.policy));
		return queryFieldsFinder.getWhereCondition(query);
	}
	
	/**
	 * Get the joins of this policy 
	 * @return the joins
	 * @throws JSQLParserException
	 */
	public List<String> getJoins() throws JSQLParserException{
		CCJSqlParserManager parserManager = new CCJSqlParserManager();
		QueryFiledsFinder queryFieldsFinder = new QueryFiledsFinder();
		Select query = (Select) parserManager.parse(new StringReader(this.policy));
		return new ArrayList<String>(queryFieldsFinder.getJoins(query));
	}
	
	/**
	 * Get the group by columns
	 * @return the group by columns
	 * @throws JSQLParserException
	 */
	public List<String> getGroupByColumns() throws JSQLParserException{
		CCJSqlParserManager parserManager = new CCJSqlParserManager();
		QueryFiledsFinder queryFieldsFinder = new QueryFiledsFinder();
		Select query = (Select) parserManager.parse(new StringReader(this.policy));
		return new ArrayList<String>(queryFieldsFinder.getGroupByColumns(query));
	}

	@Override
	public int compareTo(RelationView thatRView) {
		if (thatRView.getRole().equals(Keywords.LOGGED_OUT_USER.toString()))
			return 1;
		if (this.role.equals(Keywords.LOGGED_OUT_USER.toString()))
			return -1;
		int thatRole = Integer.parseInt(thatRView.getRole());
		int thisRole = Integer.parseInt(this.role);
		return thisRole - thatRole;
	}

	/**
	 * This comparator sorts the Relation Views with respect to their user role
	 * in ascending order of privileges.
	 */
	public static Comparator<RelationView> RelationViewComparator = new Comparator<RelationView>() {
		public int compare(RelationView r1, RelationView r2) {
			return r1.compareTo(r2);
		}
	};

}
