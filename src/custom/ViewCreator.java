package custom;

import custom.utils.MyUtils;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.http.HttpSession;

import custom.entities.RelationView;
import custom.enums.Keywords;

/**
 * This class is the core of the Interceptor. It contains all the visits that
 * the parser does on the query, and changes a table with a view specified by
 * the policy when necessary. This code has been inspired by:
 * http://jsqlparser.sourceforge.net/example.php
 */
public class ViewCreator implements SelectVisitor, FromItemVisitor, ExpressionVisitor, ItemsListVisitor {

	/*
	 * ============================================================================================ 
	 * ATTRIBUTES
	 * ============================================================================================ 
	 */

	private List<String> tables;

	/**
	 * There are special names, that are not table names but are parsed as
	 * tables. These names are collected here and are not included in the tables
	 * - names anymore.
	 */
	private List<String> otherItemNames;

	/*
	 * ============================================================================================ 
	 * PRIVATE METHODS
	 * ============================================================================================ 
	 */

	private void init() {
		otherItemNames = new ArrayList<String>();
		tables = new ArrayList<String>();
	}

	/**
	 * This methods returns <i>True</i> if the policy has to be applied. A
	 * policy has to be applied when the logged user has rights that are <b>LESS
	 * THEN OR EQUAL</b> to the ones specified in the policy. NB: null is the
	 * right of a non logged user.
	 * 
	 * @param rview
	 *            is the relation view to check
	 * @return True in case the policy has to be applied
	 */
	private boolean isLoggedUserInPolicy(RelationView rview) {
		// Logged user Instance
		HttpSession sessionObj = SessionInfo.getSessionValue(new Long(Thread.currentThread().getId()));
		String loggedUserRights = (String) sessionObj.getAttribute("UserRights");
		// User role specified in policy
		String policyRole = rview.getRole();

		if (loggedUserRights == Keywords.NOT_LOGGED_USER.toString() || loggedUserRights.equals(Keywords.LOGGED_OUT_USER.toString())) {
			//System.out.println(" -> ACCEPTED");
			return true;
		}
		if (policyRole != Keywords.NOT_LOGGED_USER.toString() && !policyRole.equals(Keywords.LOGGED_OUT_USER.toString()))
			if (Integer.parseInt(loggedUserRights) <= Integer.parseInt(policyRole)) {
				return true;
			}
		return false;
	}

	/**
	 * This method replaces all the policy keywords with the user session
	 * attributes.
	 * 
	 * @param policyView
	 *            is the string that contains the pulled view
	 * @return the same view with the data coming from the session
	 */
	private String replaceKeywords(String policyView) {
		// Keywords replacement
		String userID = (String) SessionInfo.getSessionValue(new Long(Thread.currentThread().getId())).getAttribute("UserID");
		String userRole = (String) SessionInfo.getSessionValue(new Long(Thread.currentThread().getId())).getAttribute("UserRights");
		policyView = policyView.replaceAll(Keywords.USER_ID_ATTRIBUTE.toString(), ("\'" + userID + "\'"));
		policyView = policyView.replaceAll(Keywords.USER_ROLE_ATTRIBUTE.toString(), ("\'" + userRole + "\'"));
		return policyView;
	}

	/*
	 * ============================================================================================ 
	 * ENTRY CLASS PUBLIC METHODS
	 * ============================================================================================ 
	 */

	/**
	 * Main entry for this Tool class. A list of found tables is returned.
	 *
	 * @param select
	 * @return tables contained in this select query
	 */
	public List<String> getTableList(Select select) {
		init();
		if (select.getWithItemsList() != null) {
			for (WithItem withItem : select.getWithItemsList()) {
				//System.out.println("Calling getTableList-withItem.accept()");
				withItem.accept(this);
			}
		}
		select.getSelectBody().accept(this);
		return tables;
	}

	/**
	 * Main entry for this Tool class. A list of found tables is returned.
	 *
	 * @param delete
	 * @return tables contained in this delete query
	 */
	public List<String> getTableList(Delete delete) {
		init();
		tables.add(delete.getTable().getName());
		if (delete.getWhere() != null) {
			delete.getWhere().accept(this);
		}

		return tables;
	}

	/**
	 * Main entry for this Tool class. A list of found tables is returned.
	 *
	 * @param insert
	 * @return tables contained in this insert query
	 */
	public List<String> getTableList(Insert insert) {
		init();
		tables.add(insert.getTable().getName());

		if (insert.getItemsList() != null) {
			insert.getItemsList().accept(this);
		}

		return tables;
	}

	/**
	 * Main entry for this Tool class. A list of found tables is returned.
	 *
	 * @param replace
	 * @return tables contained in this replace query
	 */
	public List<String> getTableList(Replace replace) {
		init();
		tables.add(replace.getTable().getName());
		if (replace.getExpressions() != null) {
			for (Expression expression : replace.getExpressions()) {
				expression.accept(this);
			}
		}
		if (replace.getItemsList() != null) {
			replace.getItemsList().accept(this);
		}

		return tables;
	}

	/**
	 * Main entry for this Tool class. A list of found tables is returned.
	 *
	 * @param update
	 * @return tables contained in this update query
	 */
	public List<String> getTableList(Update update) {
		init();

		Table table = update.getTables().get(0);
		tables.add(table.getName());

		if (update.getExpressions() != null) {
			for (Expression expression : update.getExpressions()) {
				expression.accept(this);
			}
		}

		if (update.getFromItem() != null) {
			update.getFromItem().accept(this);
		}

		if (update.getJoins() != null) {
			for (Join join : update.getJoins()) {
				join.getRightItem().accept(this);
			}
		}

		if (update.getWhere() != null) {
			update.getWhere().accept(this);
		}

		return tables;
	}

	/*
	 * ============================================================================================ 
	 * PUBLIC METHODS
	 * ============================================================================================ 
	 */

	/**
	 * This method takes in input a database table and checks if the policy says
	 * to modify this table for the current user. In the positive case, it
	 * replaces the <i>originalTable</i> with the view specified by the policy;
	 * otherwise it simply returns the <i>originalTable</i>.
	 * 
	 * @param originalTable
	 *            is the input table to check
	 * @return the modified table or the original one
	 */
	public Table createView(Table originalTable) {

		Hashtable<String, ArrayList<RelationView>> readPolicy = SessionInfo.getPolicies();

		if (readPolicy.containsKey(originalTable.getName())) {
			ArrayList<RelationView> rviewList = (ArrayList<RelationView>) readPolicy.get(originalTable.getName());
			//System.out.println("		-->Read policy contains table '" + originalTable);

			/* NB: this method applies the FIRST MATCHING RULE !!! */
			for (RelationView rview : rviewList) {
				/*
				 * If the logged user role is not listed in the policy, don't create the view. 
				 * Example: the policy says that only users with rights = 1
				 * (or lower) can access this table, the logged user has rights = 2,
				 * so the policy cannot be applied
				 */
				if (isLoggedUserInPolicy(rview)) {

					String replacementView = rview.getPolicy();

					// Keywords replacement
					replacementView = replaceKeywords(replacementView);



					if(replacementView.contains("JOIN")){
						for(int i=0;i<rview.getJoinTables().size();i++)
							if(rview.getJoinTables().get(i).tableName.equals(originalTable.getName())){
								String supp="";
								for(int x=0;x<rview.getJoinTables().get(i).columns.length;x++){
									try {
										for (int k = 0; k < rview.getSelectItemsList().size(); k++)
											if (rview.getSelectItemsList().get(k).contains(rview.getJoinTables().get(i).columns[x]) && !rview.getSelectItemsList().get(k).contains("NULL") )
												if(!suppContain(supp,rview.getJoinTables().get(i).columns[x]))
													supp = supp + " " + rview.getJoinTables().get(i).columns[x] + ",";

									} catch (JSQLParserException e) {
									e.printStackTrace();
								}
								}
								supp=supp.substring(0,supp.length()-1);
								replacementView="( SELECT "+supp+" FROM ("+ replacementView+") AS "+originalTable.getName()+" )";
							}
					}else
						replacementView="( " + replacementView + " )";


					Table newTable = new Table(replacementView);

					/* This puts the alias at the end of the view.
					 * Ex: (select * from members) AS members */
					if (originalTable.getAlias() == null)
						newTable.setAlias(new Alias(originalTable.getName()));
					else
						newTable.setAlias(originalTable.getAlias());
					return newTable;
				}
			}
		}

		return originalTable;
	}

	private boolean suppContain(String supp, String column) {
		return supp.contains(column);
	}

	/**
	 * Query the attributes for the original table, the view will be mapped on them
	 * @param replacementView
	 * @param name
	 */
	private String getOriginalAtt(String replacementView, String name) {
		return "SELECT  GROUP_CONCAT(DISTINCT " +
				"CONCAT('MAX(CASE WHEN b.COLUMN_NAME = ''', " +
				"b.COLUMN_NAME, " +
				"''' THEN a.',b.COLUMN_NAME,' END) AS ', " +
				"b.COLUMN_NAME)" +
				") INTO @sql " +
				"FROM " +
				"("+replacementView+") as a , " +
				"(SELECT `COLUMN_NAME` " +
				"FROM `INFORMATION_SCHEMA`.`COLUMNS` " +
				"WHERE `TABLE_SCHEMA`='bookstore' AND `TABLE_NAME`='"+name+"') as b; " +
				"SET @sql = CONCAT('SELECT   ',@sql,' " +
				"FROM    ("+replacementView+") as a , " +
				"(SELECT `COLUMN_NAME` " +
				"FROM `INFORMATION_SCHEMA`.`COLUMNS` " +
				"WHERE `TABLE_SCHEMA`=''bookstore'' AND `TABLE_NAME`=''"+name+"'') as b " +
				"GROUP BY a.ssn'); " +
				"PREPARE stmt FROM @sql; " +
				"EXECUTE stmt; " +
				"DEALLOCATE PREPARE stmt";
	}

	/*
	 * ============================================================================================ 
	 * OVERIDES VISIT METHODS FOR THE PARSER
	 * ============================================================================================ 
	 */

	@Override
	public void visit(PlainSelect plainSelect) {
		//System.out.println("	*** JSqlParser has visited plainSelect:" + plainSelect);
		plainSelect.getFromItem().accept(this);

		if (plainSelect.getJoins() != null) {
			for (Join join : plainSelect.getJoins()) {
				join.getRightItem().accept(this);

			}
			modifyOn(plainSelect.getJoins().get(plainSelect.getJoins().size()-1));

		}
		if (plainSelect.getWhere() != null) {
			plainSelect.getWhere().accept(this);
		}

	}

	private void modifyOn(Join onExpression) {
		onExpression.getOnExpression().accept(this);



	}

	@Override
	public void visit(Table tableName) {
		//System.out.println("	*** JSqlParser has visited tableName:" + tableName);
		String tableWholeName = tableName.getFullyQualifiedName();
		Table replacedTable = createView(tableName);
		tableName.setName(replacedTable.getName());
		tableName.setAlias(replacedTable.getAlias());
		if (!otherItemNames.contains(tableWholeName.toLowerCase()) && !tables.contains(tableWholeName)) {
			tables.add(tableWholeName);
		}
	}

	@Override
	public void visit(SubSelect subSelect) {
		//System.out.println("	*** JSqlParser has visited subSelect:" + subSelect);
		subSelect.getSelectBody().accept(this);
	}

	@Override
	public void visit(WithItem withItem) {
		//System.out.println("	*** JSqlParser has visited withItem:" + withItem);
		otherItemNames.add(withItem.getName().toLowerCase());
		withItem.getSelectBody().accept(this);
	}

	@Override
	public void visit(Addition addition) {
		//System.out.println("	*** JSqlParser has visited addition:" + addition);
		visitBinaryExpression(addition);
	}

	@Override
	public void visit(AndExpression andExpression) {
		//System.out.println("	*** JSqlParser has visited andExpression:" + andExpression);
		visitBinaryExpression(andExpression);
	}

	@Override
	public void visit(Between between) {
		//System.out.println("	*** JSqlParser has visited between:" + between);
		between.getLeftExpression().accept(this);
		between.getBetweenExpressionStart().accept(this);
		between.getBetweenExpressionEnd().accept(this);
	}

	@Override
	public void visit(Column tableColumn) {
		//System.out.println("	*** JSqlParser has visited tableColumn:" + tableColumn);
	}

	@Override
	public void visit(Division division) {
		//System.out.println("	*** JSqlParser has visited division:" + division);
		visitBinaryExpression(division);
	}

	@Override
	public void visit(DoubleValue doubleValue) {
		//System.out.println("	*** JSqlParser has visited doubleValue:" + doubleValue);
	}

	@Override
	public void visit(EqualsTo equalsTo) {
		//System.out.println("	*** JSqlParser has visited equalsTo:" + equalsTo);
		visitBinaryExpression(equalsTo);
	}

	@Override
	public void visit(Function function) {
		//System.out.println("	*** JSqlParser has visited function:" + function);
	}

	@Override
	public void visit(GreaterThan greaterThan) {
		//System.out.println("	*** JSqlParser has visited graterThen:" + greaterThan);
		visitBinaryExpression(greaterThan);
	}

	@Override
	public void visit(GreaterThanEquals greaterThanEquals) {
		//System.out.println("	*** JSqlParser has visited greaterThanEquals: " + greaterThanEquals);
		visitBinaryExpression(greaterThanEquals);
	}

	@Override
	public void visit(InExpression inExpression) {
		//System.out.println("	*** JSqlParser has visited inExpression: " + inExpression);
		inExpression.getLeftExpression().accept(this);
		inExpression.getRightItemsList().accept(this);
	}

	@Override
	public void visit(SignedExpression signedExpression) {
		//System.out.println("	*** JSqlParser has visited signedExpression: " + signedExpression);
		signedExpression.getExpression().accept(this);
	}

	@Override
	public void visit(IsNullExpression isNullExpression) {
		//System.out.println("	*** JSqlParser has visited isNullExpression: " + isNullExpression);
	}

	@Override
	public void visit(JdbcParameter jdbcParameter) {
		//System.out.println("	*** JSqlParser has visited jdbcParameter: " + jdbcParameter);
	}

	@Override
	public void visit(LikeExpression likeExpression) {
		//System.out.println("	*** JSqlParser has visited likeExpression: " + likeExpression);
		visitBinaryExpression(likeExpression);
	}

	@Override
	public void visit(ExistsExpression existsExpression) {
		//System.out.println("	*** JSqlParser has visited existsExpression: " + existsExpression);
		existsExpression.getRightExpression().accept(this);
	}

	@Override
	public void visit(LongValue longValue) {
		//System.out.println("	*** JSqlParser has visited longValue: " + longValue);
	}

	@Override
	public void visit(HexValue hexValue) {

	}


	@Override
	public void visit(MinorThan minorThan) {
		//System.out.println("	*** JSqlParser has visited minorThan: " + minorThan);
		visitBinaryExpression(minorThan);
	}

	@Override
	public void visit(MinorThanEquals minorThanEquals) {
		//System.out.println("	*** JSqlParser has visited minorThanEquals: " + minorThanEquals);
		visitBinaryExpression(minorThanEquals);
	}

	@Override
	public void visit(Multiplication multiplication) {
		//System.out.println("	*** JSqlParser has visited multiplication: " + multiplication);
		visitBinaryExpression(multiplication);
	}

	@Override
	public void visit(NotEqualsTo notEqualsTo) {
		//System.out.println("	*** JSqlParser has visited notEqualsTo: " + notEqualsTo);
		visitBinaryExpression(notEqualsTo);
	}

	@Override
	public void visit(NullValue nullValue) {
		//System.out.println("	*** JSqlParser has visited nullValue: " + nullValue);
	}

	@Override
	public void visit(OrExpression orExpression) {
		//System.out.println("	*** JSqlParser has visited orExpression: " + orExpression);
		visitBinaryExpression(orExpression);
	}

	@Override
	public void visit(Parenthesis parenthesis) {
		//System.out.println("	*** JSqlParser has visited parenthesis: " + parenthesis);
		parenthesis.getExpression().accept(this);
	}

	@Override
	public void visit(StringValue stringValue) {
		//System.out.println("	*** JSqlParser has visited stringValue: " + stringValue);
	}

	@Override
	public void visit(Subtraction subtraction) {
		//System.out.println("	*** JSqlParser has visited subtraction: " + subtraction);
		visitBinaryExpression(subtraction);
	}

	public void visitBinaryExpression(BinaryExpression binaryExpression) {
		//System.out.println("	*** JSqlParser has visited binaryExpression: " + binaryExpression);
		binaryExpression.getLeftExpression().accept(this);
		binaryExpression.getRightExpression().accept(this);
	}

	@Override
	public void visit(ExpressionList expressionList) {
		//System.out.println("	*** JSqlParser has visited expressionList: " + expressionList);
		for (Expression expression : expressionList.getExpressions()) {
			expression.accept(this);
		}
	}

	@Override
	public void visit(DateValue dateValue) {
		//System.out.println("	*** JSqlParser has visited dateValue: " + dateValue);
	}

	@Override
	public void visit(TimestampValue timestampValue) {
		//System.out.println("	*** JSqlParser has visited timestampValue: " + timestampValue);
	}

	@Override
	public void visit(TimeValue timeValue) {
		//System.out.println("	*** JSqlParser has visited timeValue: " + timeValue);
	}

	@Override
	public void visit(CaseExpression caseExpression) {
		//System.out.println("	*** JSqlParser has visited caseExpression: " + caseExpression);
	}

	@Override
	public void visit(WhenClause whenClause) {
		//System.out.println("	*** JSqlParser has visited whenClause: " + whenClause);
	}

	@Override
	public void visit(AllComparisonExpression allComparisonExpression) {
		//System.out.println("	*** JSqlParser has visited allComparisonExpression: " + allComparisonExpression);
		allComparisonExpression.getSubSelect().getSelectBody().accept(this);
	}

	@Override
	public void visit(AnyComparisonExpression anyComparisonExpression) {
		//System.out.println("	*** JSqlParser has visited anyComparisonExpression: " + anyComparisonExpression);
		anyComparisonExpression.getSubSelect().getSelectBody().accept(this);
	}

	@Override
	public void visit(SubJoin subjoin) {
		//System.out.println("	*** JSqlParser has visited subjoin: " + subjoin);
		subjoin.getLeft().accept(this);
		subjoin.getJoin().getRightItem().accept(this);
	}

	@Override
	public void visit(Concat concat) {
		//System.out.println("	*** JSqlParser has visited concat: " + concat);
		visitBinaryExpression(concat);
	}

	@Override
	public void visit(Matches matches) {
		//System.out.println("	*** JSqlParser has visited matches: " + matches);
		visitBinaryExpression(matches);
	}

	@Override
	public void visit(BitwiseAnd bitwiseAnd) {
		//System.out.println("	*** JSqlParser has visited bitwiseAnd: " + bitwiseAnd);
		visitBinaryExpression(bitwiseAnd);
	}

	@Override
	public void visit(BitwiseOr bitwiseOr) {
		//System.out.println("	*** JSqlParser has visited bitwiseOr: " + bitwiseOr);
		visitBinaryExpression(bitwiseOr);
	}

	@Override
	public void visit(BitwiseXor bitwiseXor) {
		//System.out.println("	*** JSqlParser has visited bitwiseXor: " + bitwiseXor);
		visitBinaryExpression(bitwiseXor);
	}

	@Override
	public void visit(CastExpression cast) {
		//System.out.println("	*** JSqlParser has visited cast: " + cast);
		cast.getLeftExpression().accept(this);
	}

	@Override
	public void visit(Modulo modulo) {
		//System.out.println("	*** JSqlParser has visited modulo: " + modulo);
		visitBinaryExpression(modulo);
	}

	@Override
	public void visit(AnalyticExpression analytic) {
		//System.out.println("	*** JSqlParser has visited analytic: " + analytic);
	}

	@Override
	public void visit(WithinGroupExpression withinGroupExpression) {

	}

	@Override
	public void visit(SetOperationList list) {
		//System.out.println("	*** JSqlParser has visited list: " + list);
		for (SelectBody plainSelect : list.getSelects()) {
			visit((PlainSelect) plainSelect);
		}
	}

	@Override
	public void visit(ExtractExpression eexpr) {
		//System.out.println("	*** JSqlParser has visited eexpr: " + eexpr);
	}

	@Override
	public void visit(LateralSubSelect lateralSubSelect) {
		//System.out.println("	*** JSqlParser has visited lateralSubSelect: " + lateralSubSelect);
		lateralSubSelect.getSubSelect().getSelectBody().accept(this);
	}

	@Override
	public void visit(MultiExpressionList multiExprList) {
		//System.out.println("	*** JSqlParser has visited multiExprList: " + multiExprList);
		for (ExpressionList exprList : multiExprList.getExprList()) {
			exprList.accept(this);
		}
	}

	@Override
	public void visit(ValuesList valuesList) {
		//System.out.println("	*** JSqlParser has visited valuesList: " + valuesList);
	}

	@Override
	public void visit(TableFunction tableFunction) {

	}


	@Override
	public void visit(IntervalExpression iexpr) {
		//System.out.println("	*** JSqlParser has visited iexpr: " + iexpr);
	}

	@Override
	public void visit(JdbcNamedParameter jdbcNamedParameter) {
		//System.out.println("	*** JSqlParser has visited jdbcNamedParameter: " + jdbcNamedParameter);
	}

	@Override
	public void visit(OracleHierarchicalExpression oexpr) {
		//System.out.println("	*** JSqlParser has visited oexpr: " + oexpr);
	}

	@Override
	public void visit(RegExpMatchOperator rexpr) {
		//System.out.println("	*** JSqlParser has visited rexpr: " + rexpr);
		visitBinaryExpression(rexpr);
	}

	@Override
	public void visit(JsonExpression jsonExpression) {

	}

	@Override
	public void visit(JsonOperator jsonOperator) {

	}

	@Override
	public void visit(RegExpMySQLOperator regExpMySQLOperator) {

	}

	@Override
	public void visit(UserVariable userVariable) {

	}

	@Override
	public void visit(NumericBind numericBind) {

	}

	@Override
	public void visit(KeepExpression keepExpression) {

	}

	@Override
	public void visit(MySQLGroupConcat mySQLGroupConcat) {

	}

	@Override
	public void visit(RowConstructor rowConstructor) {

	}

	@Override
	public void visit(OracleHint oracleHint) {

	}

	@Override
	public void visit(TimeKeyExpression timeKeyExpression) {

	}

	@Override
	public void visit(DateTimeLiteralExpression dateTimeLiteralExpression) {

	}

	@Override
	public void visit(NotExpression notExpression) {

	}


}