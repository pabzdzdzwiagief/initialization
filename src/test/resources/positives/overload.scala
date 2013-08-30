// Overloaded methods are properly resolved

package localhost

class overload {
  m1()
  val v1 = 4

  def m1() {
    println(v1)
  }

  def m1(int: Int) {
    println(int)
  }
}

// overload.scala:10: warning: value v1 is referenced before assignment
//         at localhost.overload.m1(overload.scala:6)
//         at localhost.overload.<init>(overload.scala:5)
//
//     println(v1)
//             ^
