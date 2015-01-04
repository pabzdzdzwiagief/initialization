// Access from constructor of a transitive inner class

package localhost

class inner {
  val i = new innerClass

  val v1 = 4

  def m1() {
    println(v1)
  }

  class innerClass {
    val m = new mostInnerClass

    class mostInnerClass {
      m1()
    }
  }
}

// inner-nested.scala:11: warning: value v1 is referenced before assignment
//         at localhost.inner.m1(inner-nested.scala:18)
//         at localhost.inner$innerClass$mostInnerClass.<init>(inner-nested.scala:15)
//         at localhost.inner$innerClass.<init>(inner-nested.scala:6)
//         at localhost.inner.<init>(inner-nested.scala:5)
//
//     println(v1)
//             ^
