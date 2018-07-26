package custom.parser;

// import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
  private String whereClause;
  private List<String> selectList;
  // private String fromClause;
  private Atom[] whereAtoms;
  private Graph whereGraph;
 

  /* Constructor */
  public Parser(String where, List<String> selectList)
  {
    this.whereClause = where;
    this.selectList = selectList;
  }

  /* Parse String into Atoms */
  public int atomize()
  {
    String lhs, rhs, comparison;
    String[] atom, lAtom, rAtom;
    String[] clauses = whereClause.split(" AND ");
    int numAtoms = clauses.length;
    // initialize the whereAtoms
    whereAtoms = new Atom[numAtoms];
    for (int i = 0; i < numAtoms; ++i) {
      int difference = 0;
      boolean flip = false;
      // test different comparison operators
      if (clauses[i].contains("<=")) {
        comparison = "<=";
      } else if (clauses[i].contains(">=")) {
        comparison = ">=";
        flip = true;
      } else if (clauses[i].contains("<")) {
        comparison = "<";
        difference += 1;
      } else if (clauses[i].contains(">")) {
        comparison = ">";
        difference += 1;
        flip = true;
      } else if (clauses[i].contains("=")) {
        comparison = "=";
      } else {
        System.out.println("Bad Atom: aborting");
        return -1;
      }
      atom = clauses[i].split(comparison);
      if (flip) {
        lhs = atom[1];
        rhs = atom[0];
      } else {
        lhs = atom[0];
        rhs = atom[1];
      }
      lAtom = lhs.trim().split(" ");
      rAtom = rhs.trim().split(" ");
      if (lAtom.length == 3) {
        if (lAtom[1].equals("+")) {
          difference += Integer.parseInt(lAtom[2]);
          lhs = lAtom[0].trim();
        } else if (lAtom[1].equals("-")) {
          difference -= Integer.parseInt(lAtom[2]);
          lhs = lAtom[0].trim();
        }
      } else if (lAtom.length == 1) {
        lhs = lhs.trim();
      } else {
        System.out.println("Bad Atom: aborting");
        return -1;
      }
      if (rAtom.length == 3) {
        if (rAtom[1].equals("+")) {
          difference -= Integer.parseInt(rAtom[2]);
          rhs = rAtom[0].trim();
        } else if (rAtom[1].equals("-")) {
          difference += Integer.parseInt(rAtom[2]);
          rhs = rAtom[0].trim();
        }
      } else if (rAtom.length == 1) {
        rhs = rhs.trim();
      } else {
        System.out.println("Bad Atom: aborting");
        return -1;
      }
      whereAtoms[i] = new Atom(lhs, comparison, rhs, difference);
    }
    return 0;
  }

  /* build graph */
  public int buildGraph()
  {
    this.whereGraph = new Graph();
    for (Atom a : whereAtoms) {
      String type;
      String lhsStr = a.getLhs();
      String rhsStr = a.getRhs();
      Node lhs = this.whereGraph.getNode(lhsStr);
      Node rhs = this.whereGraph.getNode(rhsStr);
      // check if lhs already a node
      // MAKE SQL STATEMENT INTO CLASS AND MAKE THIS A METHOD OF IT?
      if (lhs == null) {
        // get type
        type = "";
        for (String s : this.selectList) {
          if (s.equals(lhsStr)) {
            type = "select";
            break;
          }
        }
        if (type.equals("")) {
          try {
            Integer.parseInt(lhsStr);
            type = "constant";
          }
          catch (NumberFormatException e) {
            type = "where";
          }
        }
        // create node
        lhs = this.whereGraph.addNode(lhsStr, type);
      }

      if (rhs == null) {
        // get type
        type = "";
        for (String s : this.selectList) {
          if (s.equals(rhsStr)) {
            type = "select";
            break;
          }
        }
        if (type.equals("")) {
          try {
            Integer.parseInt(rhsStr);
            type = "constant";
          }
          catch (NumberFormatException e) {
            type = "where";
          }
        }
        // create node
        rhs = this.whereGraph.addNode(rhsStr, type);
      }
      // create edge
      Edge e = null;
      Edge f = null;
      String compStr = a.getComparison();
      int difference = a.getDifference();
      /*if (compStr.equals("<")) {
        e = new Edge(lhs, rhs, 1);
      } else if (compStr.equals("<=")) {
        e = new Edge(lhs, rhs, 0);
      } else */
      if (compStr.equals("=")) {
        e = new Edge(lhs, rhs, difference);
        f = new Edge(rhs, lhs, difference);
      } else {
        e = new Edge(lhs, rhs, difference);
      }
      // add edges to nodes
      lhs.addEdge(e, 1);
      rhs.addEdge(e, 0);
      if (compStr.equals("=")) {
        lhs.addEdge(f, 0);
        rhs.addEdge(f, 1);
      }
    } // end for loop of atoms
    return 0;
  }

  /* do the algorithm, i.e. fix the nodes */
  public void fix() { this.whereGraph.fixNodes(); }

  /* construct from clause */
  /*public List<String> buildFrom()
  {
	List<String> whereList = new ArrayList<String>();
    boolean first = true;
    fromClause = "SELECT";
    for (Node n : this.whereGraph.getNodes()) {
      System.out.println("Node: " + n.getName() + " is of " + n.getType() + " type");
      if (n.getType().equals("where")) {
    	whereList.add(n.getName());
        if (!first)   // if not the first one, need comma
          fromClause += ",";
        else                  // else, no comma, but no longer first
          first = false;
        fromClause += " CASE WHEN " + whereClause + " THEN GREATEST(";
        fromClause += String.valueOf(n.getMinConst());
        for (Pair p : n.getMinCol())
          fromClause += ", " + p.getColumn() + " + " + String.valueOf(p.getVal());
        System.out.println("Column name: " + n.getName());
        fromClause += ") END AS " + n.getName();  //Closing paren for greatest
      }
    }
    //for (int i = 1; i < selectList.length; ++i)
    //  fromClause += ", CASE WHEN " + whereClause + " THEN " + selectList[i] + " END AS " + selectList[i];
    return whereList;
  } */

  /* getters */
  public String getFrom(String s) { return this.whereGraph.getNode(s).getSqlClause(); }
  public String getCaseStmt() { return this.whereGraph.getCaseStmt(); }
  
  public void printSelect() 
  {
    for (String s: selectList)
      System.out.println(s);
  }
  
  public boolean isWhereNode(String s)
  {
	  Node n = this.whereGraph.getNode(s);
	  if (n != null && n.getType().equals("where"))
		  return true;
	  return false;
  }
  public static void main(String[] argv)
  {
    // String columns[] = {"item_id", "first_name", "last_name", "A", "B", "C", "D", "E", "F", "G"};
    List<String> select = Arrays.asList("first_name", "last_name", "A");  // in select
    Parser clause = new Parser("200 < B + 5 AND A < B AND C < B", select);
    System.out.println("============Parse Test===============");
    clause.printSelect();
    System.out.println("=========atomize========");
    clause.atomize();
    clause.buildGraph();
    clause.fix();
    // clause.buildFrom();
    System.out.println("=========end============");
    /*
    int num_atoms = 2;
    Atom[] atoms = new Atom[num_atoms];
    atoms[0] = new Atom("10", "<", "salary");
    atoms[1] = new Atom("bonus", "<", "salary");
    // add more atoms
    // Build graph
    Graph atomGraph = new Graph();
    // for each atom check if lhs/rhs are already in graph
    for (Node n : atomGraph.getNodes()) {
      System.out.println(n.getName());
      System.out.println(n.getMinConst());
    }
    System.out.println("Fixing NODES w00t!");
    atomGraph.fixNodes();
    for (Node n : atomGraph.getNodes()) {
      System.out.println(n.getName());
      System.out.println(n.getMinConst());
    }
    */
  }
}
