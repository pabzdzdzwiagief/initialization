// Initialization bug prevented by use of lazy val and early definition

package localhost

class superconstructor extends {
  val v2 = 2
} with base {
  lazy val v1 = 4
}

abstract class base {
  val v1: Int
  val v2: Int
  m1()

  def m1() {
    println(v1, v2)
  }
}

