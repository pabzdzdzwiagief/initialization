// Calling inherited methods from subclass constructor

package localhost

class inheritance extends base {
  m1()
  val v1 = 4
}

abstract class base {
  val v1: Int

  def m1() {
    println(v1)
  }
}

// inheritance.scala:14: warning: value v1 is referenced before assignment
//         at localhost.base.m1(inheritance.scala:6)
//         at localhost.inheritance.<init>(inheritance.scala:5)
//
//     println(v1)
//             ^

