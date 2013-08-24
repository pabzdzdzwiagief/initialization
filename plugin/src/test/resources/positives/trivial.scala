// Trivial "val x = x"

package localhost

class trivial {
  val x: Int = {
    identity(x)
  }
}

// trivial.scala:7: warning: value x is referenced before assignment
//         at localhost.trivial.<init>(trivial.scala:5)
//
//     identity(x)
//              ^

