package classloader.test

import classloader.SpecializingClassloader
import org.junit.{ Test => JUnitTest }
import java.net.URL
import classloader.target.TargetFactory

object Test {
  // Functions to test, need to be put in the object, else will take $outer as a parameter
  val fun1 = () => System.out.println("hi")
  val fun2 = () => TargetFactory.newTarget_J(5L, 1).print
  val classloader = new SpecializingClassloader(Thread.currentThread().getContextClassLoader())
}

class Test extends Base with ClassloaderSupport {
  import Test._

  @JUnitTest def testSimpleDirect(): Unit =
    assertEqualString(collectSystemOut(fun1()),"hi\n")

  @JUnitTest def testSimpleClassloader(): Unit =
    assertEqualString(collectSystemOut(withClassloader(classloader)(fun1)), "hi\n")

  @JUnitTest def testFactoryDirect(): Unit =
    assertEqualString(collectSystemOut(fun2()), "print(5, 1) by classloader.target.Target_J\n")

  @JUnitTest def testFactoryClassLoader(): Unit =
    assertEqualString(collectSystemOut(withClassloader(classloader)(fun2)), "print(5, 1) by classloader.target.Target_1\n")
}
