// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.PluginComponent

package object check {
  /** Component providing compilation phase that reads initialization
    * order from annotations and checks if this order seems to be
    * correct (i.e. there are no obvious reference-before-initialization
    * errors).
    */
  def component(global: Global): PluginComponent = new Check(global)
}
