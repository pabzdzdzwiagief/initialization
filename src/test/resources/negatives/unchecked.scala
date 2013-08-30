// Simple example from README, but with @unchecked annotation

package localhost

class unchecked {
  (m1(): @scala.unchecked)
  val v1 = 4

  def m1() {
    println(v1)
  }
}

