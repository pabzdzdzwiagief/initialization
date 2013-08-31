// *ignore* Access from an inner class

package localhost

class inner {
  val i = new innerClass
  val v1 = 4

  def m1() {
    println(v1)
  }

  class innerClass {
    m1()
  }
}

// inner.scala:10: warning: value v1 is referenced before assignment
//         at localhost.inner.m1(inner.scala:14)
//         at localhost.inner$innerClass.<init>(inner.scala:6)
//         at localhost.inner.<init>(inner.scala:5)
//
//     println(v1)
//             ^
