// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import tools.nsc.Global
import tools.nsc.plugins.Plugin

/** This plugin's class.
  * @param global Compiler to which this plugin is plugged.
  */
class Initialization(val global: Global) extends Plugin {
  final val name = "initialization"

  final val description = "checks for accesses to uninitialized fields"

  final val components = List(new Order(global), new Check(global))
}
