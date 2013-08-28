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

// simple.scala:10: warning: value v1 is referenced before assignment
//         at localhost.simple.m1(simple.scala:6)
//         at localhost.simple.<init>(simple.scala:5)
//
//     println(v1)
//             ^

