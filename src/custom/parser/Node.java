package custom.parser;

import java.util.ArrayList;

public class Node {
	private String name;
	private String type;              /* make this an enum or some such? */
	private int minConst;
	private int maxConst;
	private ArrayList<Pair> minCol;
	// private ArrayList<Pair> maxCol;
	private ArrayList<Edge> greaterAdj; /* outgoing edges for nodes that are greater than current node */
	private ArrayList<Edge> lesserAdj;  /* incoming edges for nodes that are less than current node */
	private String sqlClause;
	private String caseStmt;
	private boolean fixedMin;
	// private boolean fixedMax;

	/* Constructor */
	public Node(String name, String type)
	{
		this.name = name;
		this.type = type;
		this.greaterAdj = new ArrayList<Edge>();
		this.lesserAdj = new ArrayList<Edge>();
		this.minConst = Integer.MIN_VALUE;
		this.maxConst = Integer.MAX_VALUE;
		this.minCol = new ArrayList<Pair>();
		this.sqlClause = String.valueOf(Integer.MIN_VALUE);
		this.caseStmt = "";
		// this.maxCol = new ArrayList<Pair>();
		this.fixedMin = false;
		// this.fixedMax = false;
		if (type.equals("constant")) {
			this.minConst = this.maxConst = Integer.parseInt(name);
			this.fixedMin = true;
			// this.fixedMax  = true;
		}
	}

	/* add an edge relationship is 0 if to smaller node
	 * 1 if to larger */
	public void addEdge(Edge e, int relationship)
	{
		if (relationship == 0)
			lesserAdj.add(e);
		else if (relationship == 1)
			greaterAdj.add(e);
	}

	/* Getters */
	public String getName() 				{ return name; }
	public String getType() 				{ return type; }
	public int getMinConst() 				{ return minConst; }
	public int getMaxConst() 				{ return maxConst; }
	public String getSqlClause() 			{ return sqlClause; }
	public String getCaseStmt() 			{ return caseStmt; }
	public ArrayList<Pair> getMinCol() 		{ return minCol; }
	public ArrayList<Edge> getOutEdges() 	{ return greaterAdj; }

	/* Setters */
	public void setFixedMin(boolean val) { this.fixedMin = val; }
	// public void setFixedMax(boolean val) { this.fixedMax = val; }

	/* THE ALGORITHM */
	public void fixMin()
	{
		Node n;
		ArrayList<Edge> adjacents = this.lesserAdj;
		// System.out.println("Fixing min for: ");
		// System.out.println(this.name);

		// base case no incoming edges
		if (adjacents.size() == 0) {
			this.fixedMin = true;
			return;
		}
		for (Edge e : adjacents) {
			// get adjacent node
			n = e.getSmaller();
			// System.out.format("Checking adjacent node: %s\n", n.getName());
			// get the edge weight
			int edgeWeight = e.getWeight();
			// if node not already fixed, recursively fix it
			if (!n.fixedMin)
				n.fixMin();		
			// TODO:  how hacky is this, will it work?
			if (this.fixedMin)
				return;
			// for values in n.minCol check if value minCol
			// if so set to max of two vals else add to it
			// set minConst to max of it and n.minConst
			// but only do this for predecessors that are where nodes
			if (n.getType().equals("where")) {
				for (Pair p : n.minCol) {
					boolean match = false;
					for (Pair q : this.minCol) {
						if (p.getNode().equals(q.getNode())) {
							q.setVal(Math.max(q.getVal(), p.getVal() + edgeWeight));
							match = true;
							break;
						}
					}
					if (match == false) {
						this.minCol.add(new Pair(p.getNode(), p.getVal() + edgeWeight));
					}
				}
			}
			if (!n.getType().equals("select"))
				// now do constants
				if (minConst < n.minConst + edgeWeight)
					minConst = n.minConst + edgeWeight;
			// and if not a constant need to add to minCol
			if (n.getType().equals("select")) {
				this.minCol.add(new Pair(n, edgeWeight));
			}
		}


		int count = 0;
		sqlClause = String.valueOf(minConst); 
		System.out.println("Parent node: " + this.getName());
		for (Pair q : this.minCol) {
			++count;
			System.out.println("Now node: " + q.getNode().getName());
			sqlClause += ", " + q.getNode().getName() + " + " + String.valueOf(q.getVal());
			// and its part of caseSTMT if both the pair node
			// and the current node are both select nodes
			if (this.type.equals("select") && q.getNode().getType().equals("select")) {
				if (this.caseStmt.length() > 0)
					this.caseStmt += " AND ";
				System.out.println("Adding to caseStmt from: " + this.name + " for node : " + q.getNode().getName());
				this.caseStmt += this.name + " >= " + q.getNode().getName() + " + " + String.valueOf(q.getVal());
			}
		}
		if (count > 0)
			sqlClause = "GREATEST( " + sqlClause + " )";
		this.fixedMin = true;
	}

	/* MAIN */
	public static void main(String[] argv)
	{
		Node aNode = new Node("Billy", "Sunday");
		Node anIntNode = new Node("123", "Monday");
		System.out.println(aNode.getName());
		System.out.println(aNode.getMinConst());
		System.out.println(anIntNode.getName());
		System.out.println(anIntNode.getMinConst());
	}
}
