package com.github.pabzdzdzwiagief.initialization.plugin

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin

import com.github.pabzdzdzwiagief.initialization.check
import com.github.pabzdzdzwiagief.initialization.order

/** This plugin's class.
  * @param global Compiler to which this plugin is plugged.
  */
class Initialization(val global: Global) extends Plugin {
  final val name = "initialization"

  final val description = "checks for accesses to uninitialized fields"

  final val components = List(order.phase(global), check.phase(global))
}
