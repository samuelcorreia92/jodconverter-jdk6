/*
 * Copyright 2004 - 2012 Mirko Nasato and contributors
 *           2016 - 2020 Simon Braconnier and contributors
 *
 * This file is part of JODConverter - Java OpenDocument Converter.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jodconverter.core.job;

import org.jodconverter.core.office.TemporaryFileMaker;
import org.jodconverter.core.util.AssertUtils;
import org.jodconverter.core.util.FileUtils;
import org.jodconverter.core.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Target document specifications for from an input stream. */
public class TargetDocumentSpecsFromOutputStream extends AbstractTargetDocumentSpecs
    implements TargetDocumentSpecs {

  private final OutputStream outputStream;
  private final boolean closeStream;
  private final TemporaryFileMaker fileMaker;

  /**
   * Creates specs for the specified output stream.
   *
   * @param outputStream The target output stream.
   * @param fileMaker Temporary file maker.
   * @param closeStream If we close the stream on completion.
   */
  public TargetDocumentSpecsFromOutputStream(
      final OutputStream outputStream,
      final TemporaryFileMaker fileMaker,
      final boolean closeStream) {
    super(fileMaker.makeTemporaryFile());

    AssertUtils.notNull(outputStream, "outputStream must not be null");
    AssertUtils.notNull(fileMaker, "fileMaker must not be null");
    this.outputStream = outputStream;
    this.closeStream = closeStream;
    this.fileMaker = fileMaker;
  }

  @Override
  public File getFile() {
    return getFormat() == null
        ? super.getFile()
        : fileMaker.makeTemporaryFile(getFormat().getExtension());
  }

  @Override
  public void onComplete(final File tempFile) {

    // Copy the content of the tempFile, which is the result
    // of the conversion, to the outputStream
    try {
      IOUtils.copy(new FileInputStream(tempFile), outputStream);
      if (closeStream) {
        outputStream.close();
      }

    } catch (IOException ex) {
      throw new DocumentSpecsIOException("Could not write file '" + tempFile + "' to stream", ex);
    } finally {

      // Ensure the created tempFile is deleted
      FileUtils.deleteQuietly(tempFile);
    }
  }

  @Override
  public void onFailure(final File tempFile, final Exception exception) {

    // Ensure the created tempFile is deleted
    FileUtils.deleteQuietly(tempFile);
  }
}
