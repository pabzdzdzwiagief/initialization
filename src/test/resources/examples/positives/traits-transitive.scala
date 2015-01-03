// Transitively mixed-in method invocations are being traced

package localhost

class traits extends mixin {
  mixinMethod()

  val x = 42
}

trait mixin extends mixinOfAMixin {
  def mixinMethod() {
    mixinOfAMixinMethod()
  }
}

trait mixinOfAMixin {
  val x: Int

  def mixinOfAMixinMethod() {
    println(x)
  }
}

// traits-transitive.scala:21: warning: value x is referenced before assignment
//         at localhost.mixinOfAMixin$class.mixinOfAMixinMethod(traits-transitive.scala:5)
//         at localhost.traits.mixinOfAMixinMethod(traits-transitive.scala:13)
//         at localhost.mixin$class.mixinMethod(traits-transitive.scala:5)
//         at localhost.traits.mixinMethod(traits-transitive.scala:6)
//         at localhost.traits.<init>(traits-transitive.scala:5)
//
//     println(x)
//             ^
