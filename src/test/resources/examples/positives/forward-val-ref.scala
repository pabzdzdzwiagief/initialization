// Forward val reference

package localhost

class forward {
  val x = y
  val y = 1
}

// forward-val-ref.scala:6: warning: value y is referenced before assignment
//         at localhost.forward.<init>(forward-val-ref.scala:5)
//
//   val x = y
//           ^