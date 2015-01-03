// Checking initialization bug made in base class

package localhost

class superconstructor extends base {
  val v1 = 4
}

abstract class base {
  val v1: Int
  m1()

  def m1() {
    println(v1)
  }
}

// superconstructor.scala:14: warning: value v1 is referenced before assignment
//         at localhost.base.m1(superconstructor.scala:11)
//         at localhost.base.<init>(superconstructor.scala:5)
//         at localhost.superconstructor.<init>(superconstructor.scala:5)
//
//     println(v1)
//             ^

