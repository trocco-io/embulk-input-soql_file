package org.embulk.input.soql;

/**
 * SoqlUtils
 */
public class SoqlUtils
{
  private static final int GUESS_LIMIT = 30;

  private SoqlUtils() {}

  public static String soqlForGuess(String soql)
  {
    if (soql.toUpperCase().contains("LIMIT")) {
      return String.format("%s LIMIT %d", soql, GUESS_LIMIT);
    }
    return soql;
  }
}
