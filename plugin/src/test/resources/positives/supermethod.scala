// Invocations with `super.` prefix lead to proper method

package localhost

class supermethod extends base {
  m1()
  val v1 = 4

  override def m1() {
    super.m1()
  }
}

abstract class base {
  val v1: Int

  def m1() {
    println(v1)
  }
}

// supermethod.scala:18: warning: value v1 is referenced before assignment
//         at localhost.base.m1(supermethod.scala:10)
//         at localhost.supermethod.m1(supermethod.scala:6)
//         at localhost.supermethod.<init>(supermethod.scala:5)
//
//     println(v1)
//             ^

