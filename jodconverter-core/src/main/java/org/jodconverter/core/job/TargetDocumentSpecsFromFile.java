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

import org.jodconverter.core.util.FileUtils;

import java.io.File;

/** Target document specifications for from a file. */
public class TargetDocumentSpecsFromFile extends AbstractTargetDocumentSpecs
    implements TargetDocumentSpecs {

  /**
   * Creates specs for the specified file.
   *
   * @param file The target file.
   */
  public TargetDocumentSpecsFromFile(final File file) {
    super(file);
  }

  @Override
  public void onComplete(File file) {}

  @Override
  public void onFailure(final File file, final Exception exception) {
    // Ensure the created file is deleted
    FileUtils.deleteQuietly(file);
  }
}
