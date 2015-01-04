// Access from method of an inner class

package localhost

class inner {
  val i = new innerClass

  i.innerMethod()

  val v1 = 4

  def m1() {
    println(v1)
  }

  class innerClass {
    def innerMethod() {
      m1()
    }
  }
}

// inner-method.scala:13: warning: value v1 is referenced before assignment
//         at localhost.inner.m1(inner-method.scala:18)
//         at localhost.inner$innerClass.innerMethod(inner-method.scala:8)
//         at localhost.inner.<init>(inner-method.scala:5)
//
//     println(v1)
//             ^
