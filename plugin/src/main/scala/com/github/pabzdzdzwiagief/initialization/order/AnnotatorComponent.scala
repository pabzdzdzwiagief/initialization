// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization.order

import tools.nsc.plugins.PluginComponent

private[this] trait AnnotatorComponent extends PluginComponent {
  import global.{Transformer, noopTransformer}

  val annotator: Transformer = noopTransformer
}
