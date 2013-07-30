// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization.check

import tools.nsc.Phase
import tools.nsc.plugins.PluginComponent

private[this] trait AnnotationCheckerComponent extends PluginComponent {
  import global.CompilationUnit

  def checker(prev: Phase): Phase = new CheckerPhase(prev)

  private class CheckerPhase(prev: Phase) extends StdPhase(prev) {
    def apply(unit: CompilationUnit) = {}
  }
}
