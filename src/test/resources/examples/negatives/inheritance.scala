// Calling inherited methods from subclass constructor

package localhost

class inheritance extends base {
  val v1 = 4
  m1()
}

abstract class base {
  val v1: Int

  def m1() {
    println(v1)
  }
}

