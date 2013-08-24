// Copyright (c) 2013 Aleksander Bielawski. All rights reserved.
// Use of this source code is governed by a BSD-style license
// that can be found in the LICENSE file.

package com.github.pabzdzdzwiagief.initialization

import io.Source
import java.io.ByteArrayInputStream
import reflect.internal.util.SourceFile
import reflect.internal.util.BatchSourceFile
import reflect.io.AbstractFile
import reflect.io.VirtualFile

/** Creates source files from external resources. */
class SourceLoader extends (String => SourceFile) {
  /** Wraps source code to form acceptable by Scala compiler object.
    * @param sourcePath path to loaded source code.
    * @return SourceFile containing requested source code.
    */
  def apply(sourcePath: String): SourceFile = {
    val file: AbstractFile = new VirtualFile(sourcePath.split('/').last) {
      override val container = this //FIXME workaround needed by nsc's file ranking

      override def input = new ByteArrayInputStream(content.getBytes)

      override val sizeOption = Some(content.getBytes.length)

      private lazy val content = {
        val source = Source.fromURL(getClass.getResource(sourcePath))
        val content = source.mkString
        source.close()
        content
      }
    }
    new BatchSourceFile(file)
  }
}
