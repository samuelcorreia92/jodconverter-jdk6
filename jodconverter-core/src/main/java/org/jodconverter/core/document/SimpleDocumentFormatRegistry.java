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

package org.jodconverter.core.document;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.jodconverter.core.util.AssertUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** A SimpleDocumentFormatRegistry contains a collection of document formats supported by office. */
public class SimpleDocumentFormatRegistry implements DocumentFormatRegistry {

  private final Map<String, DocumentFormat> fmtsByExtension = new HashMap<String, DocumentFormat>();
  private final Map<String, DocumentFormat> fmtsByMediaType = new HashMap<String, DocumentFormat>();

  /**
   * Add a new format to the registry.
   *
   * @param documentFormat The format to add.
   */
  public void addFormat(final DocumentFormat documentFormat) {
    for (String extension : documentFormat.getExtensions()) {
      fmtsByExtension.put(extension.toLowerCase(Locale.ROOT), documentFormat);
      fmtsByMediaType.put(documentFormat.getMediaType().toLowerCase(Locale.ROOT), documentFormat);
    }
  }

  @Override
  public DocumentFormat getFormatByExtension(final String extension) {

    AssertUtils.notNull(extension, "extension must not be null");
    return fmtsByExtension.get(extension.toLowerCase(Locale.ROOT));
  }

  @Override
  public DocumentFormat getFormatByMediaType(final String mediaType) {

    AssertUtils.notNull(mediaType, "mediaType must not be null");
    return fmtsByMediaType.get(mediaType.toLowerCase(Locale.ROOT));
  }

  @Override
  public Set<DocumentFormat> getOutputFormats(final DocumentFamily documentFamily) {
    AssertUtils.notNull(documentFamily, "documentFamily must not be null");

    // Use fmtsByMediaType since fmtsByExtension may contain the same
    // DocumentFormat with multiple extensions (e.g: jpg, jpeg).
    return Sets.newHashSet(
        Iterables.filter(
            fmtsByMediaType.values(),
            new Predicate<DocumentFormat>() {
              public boolean apply(DocumentFormat input) {
                return input.getStoreProperties(documentFamily) != null;
              }
            }));
  }
}
