// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization.check

import tools.nsc.Global
import tools.nsc.Phase
import tools.nsc.plugins.PluginComponent

private[this] class Check(val global: Global)
  extends PluginComponent with AnnotationCheckerComponent {
  final val phaseName = "initcheck"

  final val runsAfter = List("initseq", "refchecks")

  final def newPhase(prev: Phase): Phase = checker(prev)
}
