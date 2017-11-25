package custom.utils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import custom.entities.RelationView;
import custom.enums.Keywords;
import custom.parser.Parser;

/**
 * This class is the core of the queries modification.
 * 
 * @author paolobruzzo
 * modified by George Sullivan
 * (rewrote mergeSimpleRelations
 */
public class QueryWizard {

	/*
	 * ============================================================================================ 
	 * ATTRIBUTES and CONSTRUCTOR
	 * ============================================================================================ 
	 */

	ArrayList<RelationView> policyList;
	ArrayList<String> columns;
	String tableName;

	/**
	 * Construct an object that will modify all the queries of a given table.
	 * 
	 * @param policyList
	 *            is the list of policies
	 * @param tableName
	 *            is the name of the table that has policies
	 * @param columns
	 *            are the columns associated to the table
	 */
	public QueryWizard(List<RelationView> policyList, String tableName, List<String> columns) {
		/* Sort it and add it to the policies. It's important to sort them
		 * in the proper way, since the first matching rule is the one that will 
		 * be taken. */
		this.policyList = new ArrayList<RelationView>(policyList);
		Collections.sort(this.policyList, RelationView.RelationViewComparator);

		this.tableName = tableName;
		this.columns = new ArrayList<String>(columns);
	}

	/*
	 * ============================================================================================ 
	 * PUBLIC METHODS
	 * ============================================================================================ 
	 */

	/**
	 * @return The list of modified relations
	 * @throws JSQLParserException
	 *             when an input policy cannot be parsed
	 * @throws ParseException
	 */
	public ArrayList<RelationView> getModifiedRelations() throws JSQLParserException, ParseException {
		ArrayList<RelationView> modifiedRelations = new ArrayList<RelationView>();
		ArrayList<RelationView> tempRelations = new ArrayList<RelationView>();

		/* Takes the sublists of relations of the same role, and pass them to
		 * the merging function */
		tempRelations.add(policyList.get(0));
		for (int i = 1; i < policyList.size(); i++) {
			if (policyList.get(i).getRole().equals(policyList.get(i - 1).getRole()))
				tempRelations.add(policyList.get(i));
			else {
				modifiedRelations.add(mergeAllRelations(tempRelations));
				tempRelations.clear();
				tempRelations.add(policyList.get(i));
			}
		}
		modifiedRelations.add(mergeAllRelations(tempRelations));

		return modifiedRelations;
	}

	/*
	 * ============================================================================================ 
	 * PRIVATE METHODS
	 * ============================================================================================ 
	 */

	/**
	 * This method will handle the UNION of the merged individual queries with
	 * the aggregate one.
	 * 
	 * @param sameRolePolicyList
	 *            contains all the policies of the current table.
	 * @return the merged Relation View
	 * @throws JSQLParserException
	 * @throws ParseException
	 */
	private RelationView mergeAllRelations(List<RelationView> sameRolePolicyList) throws JSQLParserException, ParseException {
		List<RelationView> simpleRelationList = new ArrayList<RelationView>();
		// Only 1 in the current version
		List<RelationView> aggregateRelationList = new ArrayList<RelationView>();
		for (RelationView r : sameRolePolicyList) {
			if (r.getPolicy().toUpperCase().contains("SUM(") || r.getPolicy().toUpperCase().contains("AVG(")
					|| r.getPolicy().toUpperCase().contains("COUNT(") || r.getPolicy().toUpperCase().contains("MIN(")
					|| r.getPolicy().toUpperCase().contains("MAX("))
				aggregateRelationList.add(r);
			else
				simpleRelationList.add(r);
		}

		// Merge simple rules (if any)
		RelationView mergedSimpleView = null;
		if (simpleRelationList.size() > 0){
			mergedSimpleView = mergeSimpleRelations(simpleRelationList);
		}


		// Build aggregate rule (if any)
		RelationView mergedAggregateView = null;
		if (aggregateRelationList.size() > 0)
			if (simpleRelationList.size() > 0)
				mergedAggregateView = buildAggregateQuery(aggregateRelationList, mergedSimpleView);
			else
				mergedAggregateView = buildAggregateQuery(aggregateRelationList, null);

		// Merge everything together
		String result = null;
		if (mergedSimpleView != null && mergedAggregateView != null)
			result = mergedSimpleView.getPolicy() + " UNION ALL " + mergedAggregateView.getPolicy();
		else if (mergedSimpleView != null)
			result = mergedSimpleView.getPolicy();
		else if (mergedAggregateView != null)
			result = mergedAggregateView.getPolicy();

		return new RelationView(sameRolePolicyList.get(0).getRole(), result, null, MyUtils.getOldestBeginDateToString(sameRolePolicyList),
				MyUtils.getOldestExpirationDateToString(sameRolePolicyList));
	}

	/*
	 * ============================================================================================ 
	 * PRIVATE METHODS: SIMPLE
	 * ============================================================================================ 
	 */

	/**
	 * Method that merges a list of policies on individual attributes (no
	 * aggregates) into a unique one.
	 * 
	 * @param sameRolePolicyList
	 *            is the list of simple format policies to be merged
	 * @return A new relation view in which:
	 *         <ul>
	 *         <li>The <b>role</b> is unchanged</li>
	 *         <li>The <b>policy</b> is a merge of the policies in input</li>
	 *         <li>The <b>begin date</b> is the oldest date</li>
	 *         <li>The <b>expiration date</b> is the date most in the future</li>
	 *         <li>The <b>permission</b> is null</li>
	 *         </ul>
	 * @throws JSQLParserException
	 *             when an input policy cannot be parsed
	 * @throws ParseException
	 *             when something goes wrong in instantiating the new relation
	 *             view. This should never occur.
	 */
	private RelationView mergeSimpleRelations(List<RelationView> sameRolePolicyList) throws JSQLParserException, ParseException {
		int numColumns = columns.size();
		String[] results = new String[numColumns];
		String whereNot ="";

		// For each policy, build the graph
		boolean firstView = true;
		String result = "";
		for (RelationView rview : sameRolePolicyList) {
			for (int i = 0; i < numColumns; ++i)
				results[i] = "";
			if (!firstView) {
				result += " UNION ";
			}
			result += "SELECT ";
			boolean firstClause = true;
			List<String> select = rview.getSelectItemsList();
			String where = rview.getWhereCondition();		// in disjunctive normal form
			String[] conjunctions = where.split(" OR ");  	// get each of the conjunctions separately 
			for (String s: conjunctions) {
				Parser clause = new Parser(s, select);
				clause.atomize();
				clause.buildGraph();
				// before fixing assume that we have checked for negative cycles
				// using Bellman Ford
				// and reduced strongly connected components to single points
				// so result is DAG
				clause.fix();
				String caseStmt = clause.getCaseStmt();
				if (caseStmt.equals(""))
					caseStmt = "TRUE";
				for (int i = 0; i < numColumns; ++i) {
					String col = columns.get(i);

					if (firstClause)
						results[i] += " CASE";
					// else
					//	results[i] += ", ";
					if (clause.isWhereNode(col)) {
						results[i] += " WHEN " + caseStmt + " THEN " + clause.getFrom(col);
					} else if (contain(rview.getSelectItemsList(),col)) {
						System.out.println("#Current column is in select: " + col);
						results[i] += " WHEN " + caseStmt + " THEN " + col;
					} else {
						results[i] += " WHEN " + caseStmt + " THEN ";
						results[i] += "NULL";
					}
				}
				firstClause = false;
				firstView = false;
			}
			for (int i = 0; i < numColumns; ++i)
				results[i] += " END AS " + columns.get(i);
			result += String.join(", ", results);
			// result = result.substring(0, result.length() - 1); // Remove last comma
			result += " FROM " + tableName;
			// Add the joins
			List<String> joins = rview.getJoins();
			for (String join : joins)
				result += " " + join;
			result += " WHERE (" + where + whereNot + ")";
			whereNot += " AND NOT (" + where + ")";
			firstView = false;
		}
		/*
		// Add the where clauses
		boolean isFirstElement = true;
		for (RelationView rview : sameRolePolicyList) {
			String where = rview.getWhereCondition();
			if (isFirstElement) {
				isFirstElement = false;
				result += " WHERE (" + where + ")";
			} else {
				result += " OR (" + where + ")";
			}
		}
		*/

		return new RelationView(sameRolePolicyList.get(0).getRole(), result, null, MyUtils.getOldestBeginDateToString(sameRolePolicyList),
				MyUtils.getOldestExpirationDateToString(sameRolePolicyList));
	}

	private boolean contain(List<String> selectItemsList, String col) {
		for(int i=0;i<selectItemsList.size();i++) {
			System.out.println(selectItemsList.get(i)+":"+col);
			if (selectItemsList.get(i).equals(col)){
				return true;
			}
		}
		return false;
	}

	/*
	 * ============================================================================================ 
	 * PRIVATE METHODS : AGGREGATES
	 * ============================================================================================ 
	 */

	/**
	 * This method handles the transformation of the aggregate query, and calls
	 * different methods based on what kind of aggregate has been specified.
	 * 
	 * @param sameRolePolicyList
	 *            contains the list of aggregates query. In this version it will
	 *            contain only 0 or 1 query at most.
	 * @param mergedSimpleView
	 *            is the merged view of individual relations.
	 * @return the transformed aggregate query in a Relation View format where:
	 *         <ul>
	 *         <li>The <b>role</b> is unchanged</li>
	 *         <li>The <b>policy</b> is the transformation of the policy in
	 *         input</li>
	 *         <li>The <b>begin date</b> is unchanged</li>
	 *         <li>The <b>expiration date</b> is unchanged</li>
	 *         <li>The <b>permission</b> is null</li>
	 *         </ul>
	 * @throws JSQLParserException
	 * @throws ParseException
	 */
	private RelationView buildAggregateQuery(List<RelationView> sameRolePolicyList, RelationView mergedSimpleView)
			throws JSQLParserException, ParseException {
		// At the moment we allow only 1 aggregate function
		RelationView aggrView = sameRolePolicyList.get(0);
		RelationView transformedView = null;

		// Split the aggregate function from the names of the selection items
		List<String> selectItems = aggrView.getSelectItemsList();
		String aggregate = selectItems.get(0).substring(0, selectItems.get(0).indexOf('(')).toUpperCase();

		// Different query merge solutions for each aggregate
		if (aggregate.equals(Keywords.SUM.toString())) {
			transformedView = buildAggregateQueryStandard(Keywords.SUM.toString(), aggrView, mergedSimpleView);
		} else if (aggregate.equals(Keywords.MIN.toString())) {
			transformedView = buildAggregateQueryStandard(Keywords.MIN.toString(), aggrView, mergedSimpleView);
		} else if (aggregate.equals(Keywords.MAX.toString())) {
			transformedView = buildAggregateQueryStandard(Keywords.MAX.toString(), aggrView, mergedSimpleView);
		} else if (aggregate.equals(Keywords.COUNT.toString())) {
			transformedView = buildAggregateQueryStandard(Keywords.COUNT.toString(), aggrView, mergedSimpleView);
		} else if (aggregate.equals(Keywords.AVG.toString())) {
			if (mergedSimpleView == null)
				transformedView = buildAggregateQueryStandard(Keywords.AVG.toString(), aggrView, mergedSimpleView);
			else if (aggrView.getGroupByColumns().size() > 0 )
				transformedView = buildAvgGroupByQuery(Keywords.AVG.toString(), aggrView, mergedSimpleView);
			else
				transformedView = buildAvgQuery(Keywords.AVG.toString(), aggrView, mergedSimpleView);
		}

		return transformedView;
	}

	/**
	 * This method handles AVG only in case it does not contain the GROUP BY
	 * clause, and the individual query selection is not null.
	 * 
	 * @param aggr
	 *            is the aggregate to handle
	 * @param aggrView
	 *            is the view to transform
	 * @param mergedSimpleView
	 *            is the merged view of individual relations
	 * @return the transformed aggregate query in a Relation View format where:
	 *         <ul>
	 *         <li>The <b>role</b> is unchanged</li>
	 *         <li>The <b>policy</b> is the transformation of the policy in
	 *         input</li>
	 *         <li>The <b>begin date</b> is unchanged</li>
	 *         <li>The <b>expiration date</b> is unchanged</li>
	 *         <li>The <b>permission</b> is null</li>
	 *         </ul>
	 * @throws JSQLParserException
	 * @throws ParseException
	 */
	private RelationView buildAvgQuery(String aggr, RelationView aggrView, RelationView mergedSimpleView) throws JSQLParserException,
			ParseException {
		// Get the select items and the where clause of the aggrView
		List<String> selectItems = aggrView.getSelectItemsList();
		String where = aggrView.getWhereCondition();

		ArrayList<String> pureSelectItems = new ArrayList<String>();
		for (String s : selectItems)
			pureSelectItems.add(s.substring(s.indexOf('(') + 1, s.indexOf(')')));

		String result = "SELECT";

		// Build the select statement
		for (int i = 1; i <= columns.size(); i++) {
			if (pureSelectItems.contains(columns.get(i - 1))) {
				result += " ((ifnull(P" + (i * 2 - 1) + ",0)+ifnull(P" + (i * 2) + ",0))+(ifnull(N" + (i * 2) + ",0)*ifnull(P" + (i * 2 - 1) + ",0))"
						+ "-(ifnull(N" + (i * 2 - 1) + ",0)*ifnull(P" + (i * 2) + ",0)))"
						+ "/(ifnull(N" + (i * 2 - 1) + ",0)+ifnull(N" + (i * 2) + ",0)) AS " + columns.get(i - 1) + ",";
			} else
				result += " NULL AS " + columns.get(i - 1) + ",";
		}

		result = result.substring(0, result.length() - 1); // Remove last comma

		// Build the from statement
		result += " FROM";

		for (int i = 1; i <= columns.size(); i++) {
			if (pureSelectItems.contains(columns.get(i - 1))) {
				// T1
				result += " (SELECT COUNT(" + columns.get(i - 1) + ") AS N" + (i * 2 - 1) + ", SUM(" + columns.get(i - 1) + ") AS P"
						+ (i * 2 - 1) + " FROM " + tableName;
				
				if (mergedSimpleView.getJoins().size() > 0)
					for (String join : mergedSimpleView.getJoins())
						result += " " + join;

				String mergedSimpleViewWhere = mergedSimpleView.getWhereCondition();
				if (mergedSimpleViewWhere == null)
					mergedSimpleViewWhere = "TRUE";

				if (where == null)
					where = "TRUE";
				result += " WHERE " + where;
				
				// negation of the simple merge
				if (mergedSimpleViewWhere != null)
					result += " AND NOT " + mergedSimpleViewWhere;
				
				result += ") AS T" + (i * 2 - 1);
				result += " JOIN";

				// T2
				result += " (SELECT COUNT(" + columns.get(i - 1) + ") AS N" + (i * 2) + ", SUM(" + columns.get(i - 1) + ") AS P" + (i * 2)
						+ " FROM " + tableName;

				if (mergedSimpleView.getJoins().size() > 0)
					for (String join : mergedSimpleView.getJoins())
						result += " " + join;

				// negation of the simple merge
				if (mergedSimpleViewWhere != null)
					result += " WHERE " + mergedSimpleViewWhere;

				result += ") AS T" + (i * 2);
				result += " JOIN";
			}
		}

		result = result.substring(0, result.length() - " JOIN".length()); // Remove
																			// last
																			// join
		return new RelationView(aggrView.getRole(), result, null, aggrView.getBeginDateToString(), aggrView.getExpirationDateToString());
	}
	
	
	/**
	 * This method handles AVG only in case it contains the GROUP BY
	 * clause, and the individual query selection is not null.
	 * 
	 * @param aggr
	 *            is the aggregate to handle
	 * @param aggrView
	 *            is the view to transform
	 * @param mergedSimpleView
	 *            is the merged view of individual relations
	 * @return the transformed aggregate query in a Relation View format where:
	 *         <ul>
	 *         <li>The <b>role</b> is unchanged</li>
	 *         <li>The <b>policy</b> is the transformation of the policy in
	 *         input</li>
	 *         <li>The <b>begin date</b> is unchanged</li>
	 *         <li>The <b>expiration date</b> is unchanged</li>
	 *         <li>The <b>permission</b> is null</li>
	 *         </ul>
	 * @throws JSQLParserException
	 * @throws ParseException
	 */
	private RelationView buildAvgGroupByQuery(String aggr, RelationView aggrView, RelationView mergedSimpleView) throws JSQLParserException,
			ParseException {
		// Get the select items and the where clause of the aggrView
		List<String> selectItems = aggrView.getSelectItemsList();
		String where = aggrView.getWhereCondition();

		ArrayList<String> pureSelectItems = new ArrayList<String>();
		for (String s : selectItems)
			pureSelectItems.add(s.substring(s.indexOf('(') + 1, s.indexOf(')')));

		String result = "SELECT";

		// Build the select statement
		for (int i = 1; i <= columns.size(); i++) {
			if (pureSelectItems.contains(columns.get(i - 1))) {
				result += " ((ifnull(P" + (i * 2 - 1) + ",0)+ifnull(P" + (i * 2) + ",0))+(ifnull(N" + (i * 2 - 1) + ",0)*ifnull(P" + (i * 2) + ",0))-(ifnull(N" + (i * 2) + ",0)*ifnull(P"
						+ (i * 2 - 1) + ",0)))/(ifnull(N" + (i * 2 - 1) + ",0)+ifnull(N" + (i * 2) + ",0)) AS " + columns.get(i - 1) + ",";
			} else 
			if(aggrView.getGroupByColumns().contains(columns.get(i - 1))){
				boolean found = false;
				for (int j = 1; j <= columns.size() && !found; j++)
					if (pureSelectItems.contains(columns.get(j - 1))){ 
						result += " T"+(j * 2) + "." + columns.get(i - 1) + " AS "+ columns.get(i - 1) + ",";
						found = true;
					}
			}
			else
				result += " NULL AS " + columns.get(i - 1) + ",";
		}

		result = result.substring(0, result.length() - 1); // Remove last comma

		// Build the from statement
		result += " FROM";

		for (int i = 1; i <= columns.size(); i++) {
			if (pureSelectItems.contains(columns.get(i - 1))) {
				
				String groupByString = "";
				for(int j=0 ; j < aggrView.getGroupByColumns().size() ; j++)
					groupByString += aggrView.getGroupByColumns().get(j)+", ";
				groupByString = groupByString.substring(0, groupByString.length() - ", ".length()); 
				
				// T1
				result += " (SELECT "+groupByString+", COUNT(" + columns.get(i - 1) + ") AS N" + (i * 2 - 1) + ", SUM(" + columns.get(i - 1) + ") AS P"
						+ (i * 2 - 1) + " FROM " + tableName;
				
				if (mergedSimpleView.getJoins().size() > 0)
					for (String join : mergedSimpleView.getJoins())
						result += " " + join;

				String mergedSimpleViewWhere = mergedSimpleView.getWhereCondition();
				if (mergedSimpleViewWhere == null)
					mergedSimpleViewWhere = "TRUE";

				result += " WHERE " + where;
				
				// negation of the simple merge
				if (mergedSimpleViewWhere != null)
					result += " AND " + mergedSimpleViewWhere;
				
				result += " GROUP BY "+groupByString+") AS T" + (i * 2 - 1);
				result += " RIGHT OUTER JOIN";

				// T2
				result += " (SELECT "+groupByString+", COUNT(" + columns.get(i - 1) + ") AS N" + (i * 2) + ", SUM(" + columns.get(i - 1) + ") AS P" + (i * 2)
						+ " FROM " + tableName;

				if (mergedSimpleView.getJoins().size() > 0)
					for (String join : mergedSimpleView.getJoins())
						result += " " + join;

				if (where == null)
					where = "TRUE";
				result += " WHERE " + where;

				// negation of the simple merge
				if (mergedSimpleViewWhere != null)
					result += " AND NOT " + mergedSimpleViewWhere;

				result += " GROUP BY "+groupByString+" ) AS T" + (i * 2);
				result += " ON";
				for(String gb : aggrView.getGroupByColumns())
					result += " T"+(i * 2)+"."+gb+"=T"+(i * 2 -1)+"."+gb+" AND";
				
				result = result.substring(0, result.length() - " AND".length());
				result += " RIGHT OUTER JOIN";
			}
		}

		result = result.substring(0, result.length() - " RIGHT OUTER JOIN".length()); // Remove
																			// last
																			// join
		return new RelationView(aggrView.getRole(), result, null, aggrView.getBeginDateToString(), aggrView.getExpirationDateToString());
	}

	
	/**
	 * This method handles COUNT, MIN, MAX, SUM, and the AVG only in case the individual tuples are null.
	 * 
	 * @param aggr
	 *            is the aggregate to handle
	 * @param aggrView
	 *            is the view to transform
	 * @param mergedSimpleView
	 *            is the merged view of individual relations
	 * @return the transformed aggregate query in a Relation View format where:
	 *         <ul>
	 *         <li>The <b>role</b> is unchanged</li>
	 *         <li>The <b>policy</b> is the transformation of the policy in
	 *         input</li>
	 *         <li>The <b>begin date</b> is unchanged</li>
	 *         <li>The <b>expiration date</b> is unchanged</li>
	 *         <li>The <b>permission</b> is null</li>
	 *         </ul>
	 * @throws JSQLParserException
	 * @throws ParseException
	 */
	private RelationView buildAggregateQueryStandard(String aggr, RelationView aggrView, RelationView mergedSimpleView)
			throws JSQLParserException, ParseException {
		List<String> selectItems = aggrView.getSelectItemsList();
		String where = aggrView.getWhereCondition();

		ArrayList<String> pureSelectItems = new ArrayList<String>();
		for (String s : selectItems)
			pureSelectItems.add(s.substring(s.indexOf('(') + 1, s.indexOf(')')));
		
		String result = "SELECT";

		// For each column specified in the policy file related to the current
		// table ...
		for (String column : columns) {
			if (!aggr.equals(Keywords.COUNT.toString()) && pureSelectItems.contains(column))
				result += " " + aggr + "(" + column + ") AS " + column + ",";
			else if (aggrView.getGroupByColumns().contains(column))
				result += " " + column + ",";
			else
				result += " NULL AS " + column + ",";
		}

		result = result.substring(0, result.length() - 1); // Remove last comma
		result += " FROM " + tableName;
		
		if(mergedSimpleView != null){
			if (mergedSimpleView.getJoins().size() > 0)
				for (String join : mergedSimpleView.getJoins())
					result += " " + join;
		}
		
		if (where == null)
			where = "TRUE";
		result += " WHERE " + where;
	
		if(mergedSimpleView != null){
			String mergedSimpleViewWhere = mergedSimpleView.getWhereCondition();
			// negation of the simple merge
			if (mergedSimpleViewWhere != null)
				result += " AND NOT " + mergedSimpleViewWhere;
		}

		boolean isFirstElement = true;
		for (String g : aggrView.getGroupByColumns()) {
			if (isFirstElement) {
				result += " GROUP BY";
				isFirstElement = false;
			}
			result += " " + g + ",";
		}
		if (!isFirstElement)
			result = result.substring(0, result.length() - 1); // Remove last
																// comma

		return new RelationView(aggrView.getRole(), result, null, aggrView.getBeginDateToString(), aggrView.getExpirationDateToString());
	}
}
