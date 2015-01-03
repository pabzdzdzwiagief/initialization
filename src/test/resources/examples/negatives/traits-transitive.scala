// Transitively mixed-in methods do not trigger warnings when called later

package localhost

class traits extends mixin {
  val x = 42

  mixinMethod()
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
