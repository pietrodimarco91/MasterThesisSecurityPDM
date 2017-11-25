package custom.parser;

public class Atom {
  private String lhs;
  private String rhs;
  private String comparison;
  private int difference; // the value when all constants moved to rhs

  /* Constructor */
  public Atom(String lhs, String comparison, String rhs, int difference)
  {
    this.lhs = lhs;
    this.rhs = rhs;
    this.comparison = comparison;
    this.difference = difference;
  }

  /* getters */
  public String getLhs() { return lhs; }
  public String getRhs() { return rhs; }
  public String getComparison() { return comparison; }
  public int getDifference() { return difference; }
}
