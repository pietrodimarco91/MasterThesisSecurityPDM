package custom.parser;

import java.util.ArrayList;

public class Graph {
  private ArrayList<Node> nodes;
  private String caseStmt;

  /* Constructor */
  public Graph()
  {
    this.nodes = new ArrayList<Node>();
    this.caseStmt = "";
  }

  /* getters */
  public ArrayList<Node> getNodes() { return nodes; }
  public String getCaseStmt() { return caseStmt; }
  /* add a new node and return that node */
  public Node addNode(String name, String type)
  {
    Node newNode = new Node(name, type);
    this.nodes.add(newNode);
    return newNode;
  }

  /* add new edge */
  public void addEdge(Node lesser, Node greater, int weight)
  {
    Edge newEdge = new Edge(lesser, greater, weight);
    lesser.addEdge(newEdge, 1);
    greater.addEdge(newEdge, 0);
  }

  /* get a node */
  public Node getNode(String name)
  {
    for (Node n : nodes)
      if (n.getName().equals(name))
        return n;
    return null;
  }

  /* THE ALGO */
  public void fixNodes()
  {
    for (Node n : nodes)
      n.fixMin();
    for (Node n : nodes) {
    	if (n.getType().equals("select")) {
    		if (caseStmt.length() > 0  && n.getCaseStmt().length() > 0)
    			caseStmt += " AND ";
    		caseStmt += n.getCaseStmt();
    	}
    	System.out.println("Name: " + n.getName() + " Type: " + n.getType());
    	for (Edge e :n.getOutEdges())
    		System.out.println("Destination: " + e.getBigger().getName() + " Weight: " + String.valueOf(e.getWeight()));
    	System.out.println("Case Statement: " + n.getCaseStmt());
    	System.out.println("SQL Query: " + n.getSqlClause());
    }
  }

  public static void main(String[] argv)
  {
  }
}
