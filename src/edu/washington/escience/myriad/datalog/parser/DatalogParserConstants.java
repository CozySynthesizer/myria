/* Generated By:JavaCC: Do not edit this line. DatalogParserConstants.java */
package edu.washington.escience.myriad.datalog.parser;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface DatalogParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int WS = 4;
  /** RegularExpression Id. */
  int COMMENT = 5;
  /** RegularExpression Id. */
  int BACKEND = 6;
  /** RegularExpression Id. */
  int ANSWER = 7;
  /** RegularExpression Id. */
  int INTEGER_TYPE = 8;
  /** RegularExpression Id. */
  int LONG_TYPE = 9;
  /** RegularExpression Id. */
  int STRING_TYPE = 10;
  /** RegularExpression Id. */
  int UNKNOWN_TYPE = 11;
  /** RegularExpression Id. */
  int COUNT = 12;
  /** RegularExpression Id. */
  int MAX = 13;
  /** RegularExpression Id. */
  int MIN = 14;
  /** RegularExpression Id. */
  int FUN = 15;
  /** RegularExpression Id. */
  int A2Z = 16;
  /** RegularExpression Id. */
  int A2ZNUM = 17;
  /** RegularExpression Id. */
  int VARNAME = 18;
  /** RegularExpression Id. */
  int DIGITS = 19;
  /** RegularExpression Id. */
  int LONG = 20;
  /** RegularExpression Id. */
  int INTEGER = 21;
  /** RegularExpression Id. */
  int PREDICATE = 22;
  /** RegularExpression Id. */
  int VAR = 23;
  /** RegularExpression Id. */
  int SINGLE_STRING = 24;
  /** RegularExpression Id. */
  int DOUBLE_STRING = 25;
  /** RegularExpression Id. */
  int RULEDEF = 26;
  /** RegularExpression Id. */
  int LPAREN = 27;
  /** RegularExpression Id. */
  int RPAREN = 28;
  /** RegularExpression Id. */
  int LBRACE = 29;
  /** RegularExpression Id. */
  int RBRACE = 30;
  /** RegularExpression Id. */
  int NIL = 31;
  /** RegularExpression Id. */
  int COMMA = 32;
  /** RegularExpression Id. */
  int DOT = 33;
  /** RegularExpression Id. */
  int EQ = 34;
  /** RegularExpression Id. */
  int NE = 35;

  /** Lexical state. */
  int DEFAULT = 0;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\n\"",
    "<WS>",
    "<COMMENT>",
    "\"backend\"",
    "\"ans\"",
    "\"int\"",
    "\"long\"",
    "\"string\"",
    "\"unknown\"",
    "\"COUNT\"",
    "\"MAX\"",
    "\"MIN\"",
    "\"FUN\"",
    "<A2Z>",
    "<A2ZNUM>",
    "<VARNAME>",
    "<DIGITS>",
    "<LONG>",
    "<INTEGER>",
    "<PREDICATE>",
    "<VAR>",
    "<SINGLE_STRING>",
    "<DOUBLE_STRING>",
    "\":-\"",
    "\"(\"",
    "\")\"",
    "\"[\"",
    "\"]\"",
    "<NIL>",
    "\",\"",
    "\".\"",
    "\"=\"",
    "\"!=\"",
  };

}
