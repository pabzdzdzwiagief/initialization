// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent

package object order {
  /** Component providing compilation phase that annotates certain
    * class members to leave information about how initialization
    * sequence looks like.
    */
  def component(global: Global): PluginComponent = new Order(global)
}
