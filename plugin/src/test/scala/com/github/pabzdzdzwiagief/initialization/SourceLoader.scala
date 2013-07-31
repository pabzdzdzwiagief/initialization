// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import io.Source
import reflect.internal.util.SourceFile
import reflect.internal.util.BatchSourceFile
import reflect.io.AbstractFile
import reflect.io.VirtualFile

/** Creates source files from external resources. */
class SourceLoader extends (String => SourceFile) {
  /** Wraps source code to form acceptable by Scala compiler object.
    * @param name name identifying requested source code.
    * @return SourceFile containing requested source code
    *         or null if resource with given name does not exist.
    */
  def apply(name: String): SourceFile = {
    val file: AbstractFile = new VirtualFile(name) {
      override val container = this //FIXME workaround needed by nsc's file ranking
    }
    new BatchSourceFile(file, getSourceFileContent(name))
  }

  /** Reads resource content to memory.
    * @param name resource name to read.
    * @return String with full source code from resource.
    */
  private def getSourceFileContent(name: String) = {
    val source = Source.fromURL(getClass.getResource(name))
    val content = source.mkString
    source.close()
    content
  }
}
