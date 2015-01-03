// Mixed-in method invocations are being traced

package localhost

class traits extends mixin {
  mixinMethod()

  val x = 42
}

trait mixin {
  val x: Int

  def mixinMethod() {
    println(x)
  }
}

// traits-method-mixed-in.scala:15: warning: value x is referenced before assignment
//         at localhost.mixin$class.mixinMethod(traits-method-mixed-in.scala:5)
//         at localhost.traits.mixinMethod(traits-method-mixed-in.scala:6)
//         at localhost.traits.<init>(traits-method-mixed-in.scala:5)
//
//     println(x)
//             ^

