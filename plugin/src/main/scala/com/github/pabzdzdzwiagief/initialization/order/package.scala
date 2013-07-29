package com.github.pabzdzdzwiagief.initialization

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent

package object order {
  /** Compilation phase that annotates certain class members to leave
    * information about how initialization sequence looks like.
    */
  def phase(global: Global): PluginComponent = ???
}
