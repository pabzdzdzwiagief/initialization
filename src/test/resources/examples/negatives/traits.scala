// Trait member initialized in time

package localhost

class traits extends first with second

trait first {
  val x = 42
}

trait second {
  val x: Int

  println(x)
}

