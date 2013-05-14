package com.squarespace.template;

import static com.squarespace.template.Operator.LOGICAL_AND;
import static com.squarespace.template.SyntaxErrorType.DEAD_CODE_BLOCK;
import static com.squarespace.template.SyntaxErrorType.EOF_IN_BLOCK;
import static com.squarespace.template.SyntaxErrorType.NOT_ALLOWED_AT_ROOT;
import static com.squarespace.template.SyntaxErrorType.NOT_ALLOWED_IN_BLOCK;
import static com.squarespace.template.plugins.CorePredicates.PLURAL;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

import com.squarespace.template.Instructions.RootInst;
import com.squarespace.template.plugins.CorePredicates;


/**
 * This test case ensures that valid sequences are accepted and executed by the
 * CodeBuilder, producing the expected result, and invalid sequences are rejected
 * and raise the expected error.
 */
public class CodeValidityTest extends UnitTestBase {

  @Test
  public void testBasic() throws CodeException  {
    RootInst root = builder().text("foo").text("bar").eof().code();
    assertContext(execute("{}", root), "foobar");
  }

  @Test
  public void testIfExpression() throws CodeException {
    CodeMaker mk = maker();
    
    // AND TESTS
    
    CodeBuilder builder = builder().ifexpn(mk.strlist("a", "b"), mk.oplist(Operator.LOGICAL_AND));
    RootInst root = builder.text("A").or().text("B").end().eof().code();
    
    assertContext(execute("{\"a\": 1, \"b\": 3.14159}", root), "A");
    assertContext(execute("{\"a\": true, \"b\": \"Bill\"}", root), "A");
    
    assertContext(execute("{\"a\": 1}", root), "B");
    assertContext(execute("{\"a\": true, \"b\": false}", root), "B");
    assertContext(execute("{}", root), "B");
    
    // OR TESTS

    builder = builder().ifexpn(mk.strlist("a", "b", "c"), mk.oplist(Operator.LOGICAL_OR, Operator.LOGICAL_OR));
    root = builder.text("A").or().text("B").end().eof().code();
    
    assertContext(execute("{\"a\": true}", root), "A");
    assertContext(execute("{\"b\": \"Bill\"}", root), "A");
    assertContext(execute("{\"c\": 3.14159}", root), "A");
    assertContext(execute("{\"a\": false, \"b\": 0, \"c\": \"Fred\"}", root), "A");
    assertContext(execute("{\"a\": {}, \"b\": {\"c\": 1}}", root), "A");

    assertContext(execute("{}", root), "B");
    assertContext(execute("{\"a\": \"\"}", root), "B");
    assertContext(execute("{\"a\": null}", root), "B");
    assertContext(execute("{\"c\": false}", root), "B");
    assertContext(execute("{\"c\": false, \"b\": 0}", root), "B");
  }

  @Test
  public void testPredicateInvalid() throws CodeException {
    CodeMaker mk = maker();
    assertInvalid(DEAD_CODE_BLOCK, mk.predicate(PLURAL), mk.text("A"), mk.or(), mk.text("B"), mk.or());
    assertInvalid(EOF_IN_BLOCK, mk.predicate(PLURAL), mk.eof());
  }
  
  @Test
  public void testRepeat() throws CodeException {
    String jsonData = "{\"foo\": [0, 0, 0]}";
    RootInst root = builder().repeated("foo").text("1").var("@").alternatesWith().text("-").end().eof().code();
    assertContext(execute(jsonData, root), "10-10-10");
    
    root = builder().repeated("bar").text("1").end().eof().code();
    assertContext(execute("{}", root), "");
  }
  
  @Test
  public void testRepeatOr() throws CodeException {
    String jsonData = "{\"a\": [0, 0, 0]}";
    RootInst root1 = builder().repeated("a").var("@").alternatesWith().text("-").or().text("X").end().eof().code();
    assertContext(execute(jsonData, root1), "0-0-0");

    jsonData = "{\"b\": [0, 0, 0]}";
    assertContext(execute(jsonData, root1), "X");
  }
  
  @Test
  public void testRepeatIndex() throws CodeException {
    String jsonData = "{\"foo\": [\"A\", \"B\", \"C\"]}";
    CodeBuilder cb = builder();
    cb.repeated("foo").var("@").var("@index").alternatesWith().text(".").end().eof();
    RootInst root = cb.code();
    assertContext(execute(jsonData, root), "A0.B1.C2");
  }

  @Test
  public void testSection() throws CodeException {
    RootInst root = builder().section("foo").var("bar").or().text("B").end().eof().code();
    assertContext(execute("{\"foo\": 1}", root), "");
    assertContext(execute("{}", root), "B");
    assertContext(execute("{\"foo\": {\"bar\": 1}}", root), "1");
  }

  @Test
  public void testPluralSingular() throws CodeException {
    CodeBuilder cb = builder().section("@").predicate(CorePredicates.PLURAL).text("A");
    cb.or(CorePredicates.SINGULAR).text("B");
    cb.or().text("C").end(); // end or
    cb.end(); // section
    
    RootInst root = cb.eof().code();
    assertEquals(repr(root), "{.section @}{.plural?}A{.or singular?}B{.or}C{.end}{.end}");
    
    assertContext(execute("174", root), "A");
    assertContext(execute("174.35", root), "A");
    assertContext(execute("1", root), "B");
    assertContext(execute("1.0", root), "B");
    assertContext(execute("0", root), "C");
  }

  @Test
  public void testVariables() throws CodeException {
    RootInst root = builder().var("foo").var("bar").eof().code();
    assertContext(execute("{\"foo\": 1, \"bar\": 2}", root), "12");
  }

  @Test
  public void testInvalid() {
    try {
      builder().or(CorePredicates.SINGULAR);
      fail("Invalid syntax passed as OK");
    } catch (CodeSyntaxException e) {
      assertEquals(e.getError().getType(), NOT_ALLOWED_AT_ROOT);
    }
  }
  
  @Test
  public void testChaining() throws CodeException {
    CodeBuilder cb = new CodeBuilder();
    cb.section("@").var("@").end().eof();
    
    assertContext(execute("1", cb.code()), "1");
  }
  
  @Test
  public void testDeadCode() {
    CodeMaker mk = maker();
    assertInvalid(DEAD_CODE_BLOCK, mk.predicate(PLURAL), mk.text("A"), mk.or(), mk.text("B"), mk.or());
    assertInvalid(DEAD_CODE_BLOCK, mk.repeated("@"), mk.text("A"), mk.alternates(), mk.or(), mk.or());
  }
  
  /**
   * Feed EOF to the machine at an unexpected time.
   */
  @Test
  public void testEOFInBlock() {
    CodeMaker mk = maker();
    assertInvalid(EOF_IN_BLOCK, mk.section("@"), mk.eof());
    assertInvalid(EOF_IN_BLOCK, mk.section("@"), mk.section("a"), mk.eof());
    assertInvalid(EOF_IN_BLOCK, mk.repeated("@"), mk.eof());
    assertInvalid(EOF_IN_BLOCK, mk.repeated("@"), mk.repeated("b"), mk.eof());
    assertInvalid(EOF_IN_BLOCK, mk.ifexpn(mk.strlist("a","b"), mk.oplist(LOGICAL_AND)), mk.eof());
    assertInvalid(EOF_IN_BLOCK, mk.ifpred(PLURAL), mk.or(), mk.eof());
    assertInvalid(EOF_IN_BLOCK, mk.predicate(PLURAL), mk.eof());
    assertInvalid(EOF_IN_BLOCK, mk.section("@"), mk.or(), mk.eof());
    assertInvalid(EOF_IN_BLOCK, mk.section("a"), mk.or(PLURAL), mk.eof());
    assertInvalid(EOF_IN_BLOCK, mk.repeated("@"), mk.alternates(), mk.eof());
  }
  
  @Test
  public void testUnexpectedInstructions() {
    CodeMaker mk = maker();
    assertInvalid(NOT_ALLOWED_AT_ROOT, mk.or());
    assertInvalid(NOT_ALLOWED_AT_ROOT, mk.alternates());
    assertInvalid(NOT_ALLOWED_IN_BLOCK, mk.repeated("@"), mk.or(), mk.alternates());
    assertInvalid(NOT_ALLOWED_IN_BLOCK, mk.section("a"), mk.or(), mk.alternates());
    assertInvalid(NOT_ALLOWED_IN_BLOCK, mk.predicate(PLURAL), mk.alternates());
  }
  
  private void assertInvalid(SyntaxErrorType type, Instruction... instructions) {
    try {
      CodeBuilder cb = builder();
      cb.accept(instructions);
      cb.code();
      fail(type + " should have raised a syntax exception");
    } catch (CodeSyntaxException e) {
      // Exception means success.
      assertEquals(e.getError().getType(), type);
    }
  }
  
}
