package parser

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

class ClipBoardParsersTest extends ClipBoardParser with FlatSpec with ShouldMatchers {

	private def assertFail[T](input: String)(implicit p: Parser[T]) {
		evaluating(parsing(input)) should produce[IllegalArgumentException]
	}


	"The ExpressionParsers" should "parse simple expressions" in {
		//just declare the parser to test once and mark it implicit
		implicit val parserToTest = expr
		parsing("15") should equal(Number(15))
		parsing("5 + 5") should equal(BinaryOp("+", Number(5), Number(5)))		
		parsing("(5 + 5)") should equal(Parens(BinaryOp("+", Number(5), Number(5))))		
		assertFail("5 +")
		//parsing("5 + 5 + 5") should equal(BinaryOp("+", BinaryOp("+", Number(5), Number(5)), Number(5)))
		parsing("(5 + 5) + 5") should equal(BinaryOp("+", Parens(BinaryOp("+", Number(5), Number(5))), Number(5)))
		assertFail("5 + (5 + 5")
		parsing("(5 + 5) + (5 + 5)") should equal(BinaryOp("+", Parens(BinaryOp("+", Number(5), Number(5))), Parens(BinaryOp("+", Number(5), Number(5)))))
		parsing("(5 + 5) + (5 - 5)") should equal(BinaryOp("+", Parens(BinaryOp("+", Number(5), Number(5))), Parens(BinaryOp("-", Number(5), Number(5)))))
		parsing("\"foobar\"") should equal(StringLiteral("foobar"))
		parsing("\"foobar\" + 5") should equal(BinaryOp("+", StringLiteral("foobar"), Number(5)))
		assertFail("\"foo")
	}
	
	
	"The ExpressionParsers" should "parse function calls" in {
		//just declare the parser to test once and mark it implicit
		implicit val parserToTest = expr
		parsing("@doThings(5 + 5, 5)") should equal(Function("doThings", List(BinaryOp("+", Number(5), Number(5)), Number(5))))
		assertFail("@doThings(5 + 5 5)")
		assertFail("@doThings(5 + 5, )")
		parsing("@doSideEffect()") should equal(Function("doSideEffect", List()))
		assertFail("@doSideEffects")
		parsing("foo.bar.baz") should equal(ClipboardRef(List("foo", "bar", "baz").map{StdClipboardIdent}))
		parsing(".bar.baz") should equal(LocalClipboardRef(List("bar", "baz").map{StdClipboardIdent}))
		assertFail("foo.bar.")
		parsing("@doThings(5 + 5, 5, foo.bar.baz)") should equal(Function("doThings", 
				List(BinaryOp("+", Number(5), Number(5)), Number(5), ClipboardRef(List("foo", "bar", "baz").map{StdClipboardIdent}))))
		parsing("@doThings(5 + 5, 5, .foo.bar.baz)") should equal(Function("doThings", 
				List(BinaryOp("+", Number(5), Number(5)), Number(5), LocalClipboardRef(List("foo", "bar", "baz").map{StdClipboardIdent}))))
		parsing("foo(5).bar") should equal(ClipboardRef(List(ClipboardCollection("foo", Number(5)), StdClipboardIdent("bar"))))
		assertFail("foo(5")
		assertFail("foo 5")
		parsing("foo(5).bar(5 + 3)") should equal(ClipboardRef(List(
				ClipboardCollection("foo", Number(5)), 
				ClipboardCollection("bar", BinaryOp("+", Number(5), Number(3))))))
		parsing("@baz(foo(5).bar(5 + 3))") should equal(Function("baz", List(ClipboardRef(List(
				ClipboardCollection("foo", Number(5)), 
				ClipboardCollection("bar", BinaryOp("+", Number(5), Number(3))))))))
		parsing("@baz(foo(5).bar(5 + 3))") should equal(Function("baz", List(ClipboardRef(List(
				ClipboardCollection("foo", Number(5)), 
				ClipboardCollection("bar", BinaryOp("+", Number(5), Number(3))))))))
		parsing("@lol(\"foobar\", 5)") should equal(Function("lol", List(StringLiteral("foobar"), Number(5))))
	}

	
	"The ExpressionParsers" should "parse clipboard references" in {
		//just declare the parser to test once and mark it implicit
		implicit val parserToTest = expr
		parsing("foo.bar.baz") should equal(ClipboardRef(List("foo", "bar", "baz").map{StdClipboardIdent}))
		parsing(".bar.baz") should equal(LocalClipboardRef(List("bar", "baz").map{StdClipboardIdent}))
		assertFail("foo.bar.")
		parsing("@doThings(5 + 5, 5, foo.bar.baz)") should equal(Function("doThings", 
				List(BinaryOp("+", Number(5), Number(5)), Number(5), ClipboardRef(List("foo", "bar", "baz").map{StdClipboardIdent}))))
		parsing("@doThings(5 + 5, 5, .foo.bar.baz)") should equal(Function("doThings", 
				List(BinaryOp("+", Number(5), Number(5)), Number(5), LocalClipboardRef(List("foo", "bar", "baz").map{StdClipboardIdent}))))
		parsing("foo(5).bar") should equal(ClipboardRef(List(ClipboardCollection("foo", Number(5)), StdClipboardIdent("bar"))))
		assertFail("foo(5")
		assertFail("foo 5")
		parsing("foo(5).bar(5 + 3)") should equal(ClipboardRef(List(
				ClipboardCollection("foo", Number(5)), 
				ClipboardCollection("bar", BinaryOp("+", Number(5), Number(3))))))
		parsing("@baz(foo(5).bar(5 + 3))") should equal(Function("baz", List(ClipboardRef(List(
				ClipboardCollection("foo", Number(5)), 
				ClipboardCollection("bar", BinaryOp("+", Number(5), Number(3))))))))
		parsing("@baz(foo(5).bar(5 + 3))") should equal(Function("baz", List(ClipboardRef(List(
				ClipboardCollection("foo", Number(5)), 
				ClipboardCollection("bar", BinaryOp("+", Number(5), Number(3))))))))
	}
	
	"JavaOut" should "generate java code" in {
		implicit val parserToTest = expr
		JavaOut.buildExpr(parsing("5 + 5")) should equal("5.0 + 5.0")
		JavaOut.buildExpr(parsing("@doThing(5)")) should equal("doThing(5.0)")
		JavaOut.buildExpr(parsing("foo.bar.baz")) should equal("""resolve("foo").resolve("bar").resolve("baz")""")
		JavaOut.buildExpr(parsing("foo(5).bar.baz(5 + 5)")) should equal("""resolve("foo", 5.0).resolve("bar").resolve("baz", 5.0 + 5.0)""")
	}	
}
