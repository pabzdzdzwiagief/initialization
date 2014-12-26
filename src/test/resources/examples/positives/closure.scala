// *not implemented* Access from a closure

package localhost

class closure {
  ({ () => m1() })()
  val v1 = 4

  def m1() {
    println(v1)
  }
}

// closure.scala:10: warning: value v1 is referenced before assignment
//         at localhost.closure.m1(closure.scala:6)
//         at localhost.closure.<init>(closure.scala:5)
//
//     println(v1)
//             ^
