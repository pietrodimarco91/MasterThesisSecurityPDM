package custom.parser;

public class Pair {
  private Node n;
  private int val;

  /* Constructor */
  public Pair(Node n, int val) {
    this.n = n;
    this.val = val;
  }

  /* getters */
  public Node getNode() { return n; }
  public int getVal() { return val; }

  /* setters */
  public void setVal(int newVal) { this.val = newVal; }
  public void incrementVal(int inc) { this.val += inc; }
}
