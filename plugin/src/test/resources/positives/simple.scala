// Simple example from README

package localhost

class simple {
  m1()
  val v1 = 4

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

