// Uninitialized trait member

package localhost

class traits extends second with first

trait first {
  val x = 42
}

trait second {
  val x: Int

  println(x)
}

// traits.scala:14: warning: value x is referenced before assignment
//         at localhost.second$class./*second$class*/$init$(traits.scala:11)
//         at localhost.traits.<init>(traits.scala:5)
//
//     println(x)
//             ^

