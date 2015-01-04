// Access from method of a transitive inner class

package localhost

class inner {
  val i = new innerClass
  val m = new i.mostInnerClass

  m.innerMethod()

  val v1 = 4

  def m1() {
    println(v1)
  }

  class innerClass {
    class mostInnerClass {
      def innerMethod() {
        m1()
      }
    }
  }
}

// inner-method-nested.scala:14: warning: value v1 is referenced before assignment
//         at localhost.inner.m1(inner-method-nested.scala:20)
//         at localhost.inner$innerClass$mostInnerClass.innerMethod(inner-method-nested.scala:9)
//         at localhost.inner.<init>(inner-method-nested.scala:5)
//
//     println(v1)
//             ^
