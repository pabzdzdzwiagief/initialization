// Overridden methods are properly handled

package localhost

class overrides extends base {
  override def m1() {
    println(v2)
  }
}

abstract class base {
  val v1 = 4
  m1()
  val v2 = 2

  def m1() {
    println(v1)
  }
}

// overrides.scala:7: warning: value v2 is referenced before assignment
//         at localhost.overrides.m1(overrides.scala:13)
//         at localhost.base.<init>(overrides.scala:11)
//         at localhost.overrides.<init>(overrides.scala:5)
//
//     println(v2)
//             ^

