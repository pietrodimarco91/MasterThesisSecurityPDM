package custom.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Modulo;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;


/**
 * This class is used to extract interesting parts of a query
 * 
 * @author paolobruzzo
 *
 */
public class QueryFiledsFinder implements SelectVisitor, FromItemVisitor, ExpressionVisitor, ItemsListVisitor {

	/*
	 * ============================================================================================ 
	 * ATTRIBUTES
	 * ============================================================================================ 
	 */

	private List<Table> tables = new ArrayList<Table>();
	private List<String> selectItems = new ArrayList<String>();
	private List<String> joins = new ArrayList<String>();
	private List<String> groupByColumns = new ArrayList<String>();
	private String whereCondition;

	/*
	 * ============================================================================================ 
	 * PRIVATE METHODS
	 * ============================================================================================ 
	 */

	private void clearAll() {
		tables.clear();
		selectItems.clear();
		joins.clear();
		groupByColumns.clear();
		whereCondition = null;
	}

	/*
	 * ============================================================================================ 
	 * PUBLIC METHODS
	 * ============================================================================================ 
	 */

	public List<Table> getTableList(Select select) {
		clearAll();
		select.getSelectBody().accept(this);
		return tables;
	}

	public List<String> getSelectItemList(Select select) {
		clearAll();
		select.getSelectBody().accept(this);
		return selectItems;
	}

	public List<String> getJoins(Select select) {
		clearAll();
		select.getSelectBody().accept(this);
		return joins;
	}

	public String getWhereCondition(Select select) {
		clearAll();
		select.getSelectBody().accept(this);
		return whereCondition;
	}

	public List<String> getGroupByColumns(Select select) {
		clearAll();
		select.getSelectBody().accept(this);
		return groupByColumns;
	}

	/*
	 * ============================================================================================ 
	 * OVERRIDE METHODS
	 * ============================================================================================ 
	 */

	@Override
	public void visit(PlainSelect plainSelect) {
		plainSelect.getFromItem().accept(this);

		if (plainSelect.getJoins() != null) {
			for (Iterator<Join> joinsIt = plainSelect.getJoins().iterator(); joinsIt.hasNext();) {
				Join join = (Join) joinsIt.next();
				join.getRightItem().accept(this);
				joins.add(join.toString());
			}
		}
		if (plainSelect.getWhere() != null) {
			this.whereCondition = plainSelect.getWhere().toString();
			plainSelect.getWhere().accept(this);
		}

		if (plainSelect.getGroupByColumnReferences() != null) {
			for (Expression e : plainSelect.getGroupByColumnReferences()) {
				groupByColumns.add(e.toString());
			}
		}

		for (SelectItem s : plainSelect.getSelectItems()) {
			selectItems.add(s.toString());
		}
	}

	public void visitBinaryExpression(BinaryExpression binaryExpression) {
		binaryExpression.getLeftExpression().accept(this);
		binaryExpression.getRightExpression().accept(this);
	}
	
	@Override
	public void visit(Table table) {
		tables.add(table);
	}

	@Override
	public void visit(SubSelect subSelect) {
		subSelect.getSelectBody().accept(this);
	}

	@Override
	public void visit(WithItem withItem) {
		withItem.getSelectBody().accept(this);
	}

	@Override
	public void visit(Addition addition) {
		visitBinaryExpression(addition);
	}

	@Override
	public void visit(AndExpression andExpression) {
		visitBinaryExpression(andExpression);
	}

	@Override
	public void visit(Between between) {
		between.getLeftExpression().accept(this);
		between.getBetweenExpressionStart().accept(this);
		between.getBetweenExpressionEnd().accept(this);
	}

	@Override
	public void visit(Column tableColumn) {
	}

	@Override
	public void visit(Division division) {
		visitBinaryExpression(division);
	}

	@Override
	public void visit(DoubleValue doubleValue) {
	}

	@Override
	public void visit(EqualsTo equalsTo) {
		visitBinaryExpression(equalsTo);
	}

	@Override
	public void visit(Function function) {
	}

	@Override
	public void visit(GreaterThan greaterThan) {
		visitBinaryExpression(greaterThan);
	}

	@Override
	public void visit(GreaterThanEquals greaterThanEquals) {
		visitBinaryExpression(greaterThanEquals);
	}

	@Override
	public void visit(InExpression inExpression) {
		inExpression.getLeftExpression().accept(this);
		inExpression.getRightItemsList().accept(this);
	}

	@Override
	public void visit(SignedExpression signedExpression) {
		signedExpression.getExpression().accept(this);
	}

	@Override
	public void visit(IsNullExpression isNullExpression) {
	}

	@Override
	public void visit(JdbcParameter jdbcParameter) {
	}

	@Override
	public void visit(LikeExpression likeExpression) {
		visitBinaryExpression(likeExpression);
	}

	@Override
	public void visit(ExistsExpression existsExpression) {
		existsExpression.getRightExpression().accept(this);
	}

	@Override
	public void visit(LongValue longValue) {
	}

	@Override
	public void visit(HexValue hexValue) {

	}


	@Override
	public void visit(MinorThan minorThan) {
		visitBinaryExpression(minorThan);
	}

	@Override
	public void visit(MinorThanEquals minorThanEquals) {
		visitBinaryExpression(minorThanEquals);
	}

	@Override
	public void visit(Multiplication multiplication) {
		visitBinaryExpression(multiplication);
	}

	@Override
	public void visit(NotEqualsTo notEqualsTo) {
		visitBinaryExpression(notEqualsTo);
	}

	@Override
	public void visit(NullValue nullValue) {
	}

	@Override
	public void visit(OrExpression orExpression) {
		visitBinaryExpression(orExpression);
	}

	@Override
	public void visit(Parenthesis parenthesis) {
		parenthesis.getExpression().accept(this);
	}

	@Override
	public void visit(StringValue stringValue) {
	}

	@Override
	public void visit(Subtraction subtraction) {
		visitBinaryExpression(subtraction);
	}

	@Override
	public void visit(ExpressionList expressionList) {
		for (Expression expression : expressionList.getExpressions()) {
			expression.accept(this);
		}
	}

	@Override
	public void visit(DateValue dateValue) {
	}

	@Override
	public void visit(TimestampValue timestampValue) {
	}

	@Override
	public void visit(TimeValue timeValue) {
	}

	@Override
	public void visit(CaseExpression caseExpression) {
	}

	@Override
	public void visit(WhenClause whenClause) {
	}

	@Override
	public void visit(AllComparisonExpression allComparisonExpression) {
		allComparisonExpression.getSubSelect().getSelectBody().accept(this);
	}

	@Override
	public void visit(AnyComparisonExpression anyComparisonExpression) {
		anyComparisonExpression.getSubSelect().getSelectBody().accept(this);
	}

	@Override
	public void visit(SubJoin subjoin) {
		subjoin.getLeft().accept(this);
		subjoin.getJoin().getRightItem().accept(this);
	}

	@Override
	public void visit(Concat concat) {
		visitBinaryExpression(concat);
	}

	@Override
	public void visit(Matches matches) {
		visitBinaryExpression(matches);
	}

	@Override
	public void visit(BitwiseAnd bitwiseAnd) {
		visitBinaryExpression(bitwiseAnd);
	}

	@Override
	public void visit(BitwiseOr bitwiseOr) {
		visitBinaryExpression(bitwiseOr);
	}

	@Override
	public void visit(BitwiseXor bitwiseXor) {
		visitBinaryExpression(bitwiseXor);
	}

	@Override
	public void visit(CastExpression cast) {
		cast.getLeftExpression().accept(this);
	}

	@Override
	public void visit(Modulo modulo) {
		visitBinaryExpression(modulo);
	}

	@Override
	public void visit(AnalyticExpression analytic) {
	}

	@Override
	public void visit(WithinGroupExpression withinGroupExpression) {

	}

	@Override
	public void visit(SetOperationList list) {
		for (SelectBody plainSelect : list.getSelects()) {
			visit((PlainSelect) plainSelect);
		}
	}

	@Override
	public void visit(ExtractExpression eexpr) {
	}

	@Override
	public void visit(LateralSubSelect lateralSubSelect) {
		lateralSubSelect.getSubSelect().getSelectBody().accept(this);
	}

	@Override
	public void visit(MultiExpressionList multiExprList) {
		for (ExpressionList exprList : multiExprList.getExprList()) {
			exprList.accept(this);
		}
	}

	@Override
	public void visit(ValuesList valuesList) {
	}

	@Override
	public void visit(TableFunction tableFunction) {

	}

	@Override
	public void visit(IntervalExpression iexpr) {
	}

	@Override
	public void visit(JdbcNamedParameter jdbcNamedParameter) {
	}

	@Override
	public void visit(OracleHierarchicalExpression oexpr) {
	}

	@Override
	public void visit(RegExpMatchOperator rexpr) {
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
