// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization.order

import tools.nsc.Global
import tools.nsc.plugins.PluginComponent
import tools.nsc.transform.Transform

private[this] class Order(val global: Global)
  extends PluginComponent with Transform with AnnotatorComponent {
  import global.{CompilationUnit, Transformer}

  final val phaseName = "initorder"

  override final val runsBefore = List("pickler")

  final val runsAfter = List("typer", "superaccessors")

  protected def newTransformer(unit: CompilationUnit): Transformer = annotator
}
