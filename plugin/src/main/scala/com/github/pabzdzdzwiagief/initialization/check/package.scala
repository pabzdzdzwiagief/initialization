package com.github.pabzdzdzwiagief.initialization

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent

package object check {
  /** Compilation phase that reads initialization order from annotations
    * and checks if this order seems to be correct (i.e. there are no
    * obvious reference-before-initialization errors).
    */
  def phase(global: Global): PluginComponent = ???
}
