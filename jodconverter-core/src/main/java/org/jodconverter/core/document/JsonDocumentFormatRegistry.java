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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jodconverter.core.document.DocumentFormat.Builder;
import org.jodconverter.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

/**
 * A JsonDocumentFormatRegistry contains a collection of {@code DocumentFormat} supported by office
 * that has been loaded loaded from a JSON source.
 */
public class JsonDocumentFormatRegistry extends SimpleDocumentFormatRegistry {

  /**
   * Creates a JsonDocumentFormatRegistry from the given InputStream.
   *
   * @param source The InputStream (JSON format) containing the DocumentFormat collection.
   * @return The created JsonDocumentFormatRegistry.
   * @throws IOException If an I/O error occurs.
   */
  public static JsonDocumentFormatRegistry create(final InputStream source) throws IOException {

    return create(IOUtils.toString(source, Charset.forName("UTF-8")));
  }

  /**
   * Creates a JsonDocumentFormatRegistry from the given InputStream.
   *
   * @param source The InputStream (JSON format) containing the DocumentFormat collection.
   * @param customProperties Custom properties applied when loading or storing documents.
   * @return The created JsonDocumentFormatRegistry.
   * @throws IOException If an I/O error occurs.
   */
  public static JsonDocumentFormatRegistry create(
      final InputStream source, final Map<String, DocumentFormatProperties> customProperties)
      throws IOException {

    return create(IOUtils.toString(source, Charset.forName("UTF-8")), customProperties);
  }

  /**
   * Creates a JsonDocumentFormatRegistry from the given source.
   *
   * @param source The string (JSON format) containing the DocumentFormat collection.
   * @return The created JsonDocumentFormatRegistry.
   */
  public static JsonDocumentFormatRegistry create(final String source) {

    final JsonDocumentFormatRegistry registry = new JsonDocumentFormatRegistry();
    registry.readJsonArray(source, null);
    return registry;
  }

  /**
   * Creates a JsonDocumentFormatRegistry from the given source.
   *
   * @param source The string (JSON format) containing the DocumentFormat collection.
   * @param customProperties Custom properties applied when loading or storing documents.
   * @return The created JsonDocumentFormatRegistry.
   */
  public static JsonDocumentFormatRegistry create(
      final String source, final Map<String, DocumentFormatProperties> customProperties) {

    final JsonDocumentFormatRegistry registry = new JsonDocumentFormatRegistry();
    registry.readJsonArray(source, customProperties);
    return registry;
  }

  /** Creates a new instance of the class. */
  protected JsonDocumentFormatRegistry() {
    super();
  }

  // Fill the registry from the given JSON source
  private void readJsonArray(
      final String source, final Map<String, DocumentFormatProperties> customProperties) {

    final Gson gson = new Gson();

    // Deserialization
    final Type collectionType = new TypeToken<Collection<DocumentFormat>>() {}.getType();
    final Collection<DocumentFormat> formats = gson.fromJson(source, collectionType);

    // Fill the registry with loaded formats. Note that we have to use
    // the constructor in order top create read only formats.

    Iterable<DocumentFormat> documentFormats =
        Iterables.transform(
            formats,
            new Function<DocumentFormat, DocumentFormat>() {
              @Override
              public DocumentFormat apply(DocumentFormat fmt) {
                if (customProperties == null || !customProperties.containsKey(fmt.getExtension())) {
                  return DocumentFormat.unmodifiableCopy(fmt);
                }
                final DocumentFormatProperties props = customProperties.get(fmt.getExtension());
                final Builder builder = DocumentFormat.builder().from(fmt).unmodifiable(true);
                // Add custom load/store properties.
                for (Map.Entry<String, Object> entry : props.getLoad().entrySet()) {
                  builder.loadProperty(entry.getKey(), entry.getValue());
                }

                for (Map.Entry<DocumentFamily, Map<String, Object>> entry :
                    props.getStore().entrySet()) {
                  for (Map.Entry<String, Object> entry2 : entry.getValue().entrySet()) {
                    builder.storeProperty(entry.getKey(), entry2.getKey(), entry2.getValue());
                  }
                }

                // Build the format.
                return builder.build();
              }
            });

    for (DocumentFormat documentFormat : documentFormats) {
      addFormat(documentFormat);
    }
  }
}
