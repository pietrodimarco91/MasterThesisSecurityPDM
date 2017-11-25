package custom.parser;

public class Edge {
  private Node smallerNode;
  private Node biggerNode;
  private int weight;

  public Edge(Node smaller, Node bigger, int weight)
  {
    this.smallerNode = smaller;
    this.biggerNode = bigger;
    this.weight = weight;
  }
  
  /* getters */
  public Node getSmaller() { return smallerNode; }
  public Node getBigger() { return biggerNode; }
  public int getWeight() { return weight; }

  public static void main(String[] argv)
  {
    System.out.println("Argh");
  }
}
