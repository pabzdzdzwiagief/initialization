// Default parameter values are also checked

package localhost

class default {
  m1()
  val v1 = 4

  def m1(int: Int = v1) {
    println(int)
  }
}

// default.scala:9: warning: value v1 is referenced before assignment
//         at localhost.default.m1$default$1(default.scala:6)
//         at localhost.default.<init>(default.scala:5)
//
//   def m1(int: Int = v1) {
//                     ^
