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

package org.jodconverter.local.task;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.XComponent;
import com.sun.star.task.ErrorCodeIOException;
import org.jodconverter.core.document.DocumentFormat;
import org.jodconverter.core.job.SourceDocumentSpecs;
import org.jodconverter.core.job.TargetDocumentSpecs;
import org.jodconverter.core.office.OfficeContext;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.util.AssertUtils;
import org.jodconverter.local.filter.FilterChain;
import org.jodconverter.local.filter.RefreshFilter;
import org.jodconverter.local.office.LocalOfficeContext;
import org.jodconverter.local.office.LocalOfficeUtils;
import org.jodconverter.local.office.utils.Lo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.jodconverter.local.office.LocalOfficeUtils.toUnoProperties;
import static org.jodconverter.local.office.LocalOfficeUtils.toUrl;

/** Represents the default behavior for a local conversion task. */
public class LocalConversionTask extends AbstractLocalOfficeTask {

  private static final String ERROR_MESSAGE_STORE = "Could not store document: ";

  private static final Logger LOGGER = LoggerFactory.getLogger(LocalConversionTask.class);

  private final TargetDocumentSpecs target;
  private final FilterChain filterChain;
  private final Map<String, Object> storeProperties;

  /**
   * Creates a new conversion task from a specified source to a specified target.
   *
   * @param source The source specifications for the conversion.
   * @param target The target specifications for the conversion.
   * @param loadProperties The load properties to be applied when loading the document. These
   *     properties are added after the load properties of the document format specified in the
   *     {@code source} arguments.
   * @param filterChain The filter chain to use with this task.
   * @param storeProperties The store properties to be applied when storing the document. These
   *     properties are added after the store properties of the document format specified in the
   *     {@code target} arguments.
   */
  public LocalConversionTask(
      final SourceDocumentSpecs source,
      final TargetDocumentSpecs target,
      final Map<String, Object> loadProperties,
      final FilterChain filterChain,
      final Map<String, Object> storeProperties) {
    super(source, loadProperties);

    this.target = target;
    if (filterChain == null) {
      this.filterChain = RefreshFilter.CHAIN;
    } else {
      this.filterChain = filterChain.copy();
    }
    this.storeProperties = storeProperties;
  }

  @Override
  public void execute(final OfficeContext context) throws OfficeException {

    LOGGER.info(
        "Executing local conversion task [{} -> {}]...",
        Optional.of(source)
            .transform(
                new Function<SourceDocumentSpecs, DocumentFormat>() {
                  public DocumentFormat apply(SourceDocumentSpecs input) {
                    return input.getFormat();
                  }
                })
            .transform(
                new Function<DocumentFormat, String>() {
                  public String apply(DocumentFormat input) {
                    return input.getExtension();
                  }
                })
            .or("?"),
        Optional.of(target)
            .transform(
                new Function<TargetDocumentSpecs, DocumentFormat>() {
                  public DocumentFormat apply(TargetDocumentSpecs input) {
                    return input.getFormat();
                  }
                })
            .transform(
                new Function<DocumentFormat, String>() {
                  public String apply(DocumentFormat input) {
                    return input.getExtension();
                  }
                })
            .or("?"));
    final LocalOfficeContext localContext = (LocalOfficeContext) context;

    // Obtain a source file that can be loaded by office. If the source
    // is an input stream, then a temporary file will be created from the
    // stream. The temporary file will be deleted once the task is done.
    final File sourceFile = source.getFile();
    try {

      // Get the target file (which is a temporary file if the
      // output target is an output stream).
      final File targetFile = target.getFile();

      XComponent document = null;
      try {
        document = loadDocument(localContext, sourceFile);
        modifyDocument(context, document);
        storeDocument(document, targetFile);

        // onComplete on target will copy the temp file to
        // the OutputStream and then delete the temp file
        // if the output is an OutputStream
        target.onComplete(targetFile);

      } catch (OfficeException officeEx) {
        LOGGER.error("Local conversion failed.", officeEx);
        target.onFailure(targetFile, officeEx);
        throw officeEx;
      } catch (Exception ex) {
        LOGGER.error("Local conversion failed.", ex);
        final OfficeException officeEx = new OfficeException("Local conversion failed", ex);
        target.onFailure(targetFile, officeEx);
        throw officeEx;
      } finally {
        closeDocument(document);
      }

    } finally {

      // Here the source file is no longer required so we can delete
      // any temporary file that has been created if required.
      source.onConsumed(sourceFile);
    }
  }

  // Gets the office properties to apply when the converted
  // document will be saved as the output file.
  private Map<String, Object> getStoreProperties(final XComponent document) throws OfficeException {
    AssertUtils.notNull(target.getFormat(), "Target format must not be null");

    final Map<String, Object> storeProps = new HashMap<String, Object>();
    appendProperties(
        storeProps,
        target.getFormat().getStoreProperties(LocalOfficeUtils.getDocumentFamily(document)));
    appendProperties(storeProps, storeProperties);

    return storeProps;
  }

  // Modifies the document after it has been loaded and before
  // it gets saved in the new format.
  protected void modifyDocument(final OfficeContext context, final XComponent document)
      throws OfficeException {

    filterChain.doFilter(context, document);
  }

  // Stores the converted document as the output file.
  protected void storeDocument(final XComponent document, final File targetFile)
      throws OfficeException {

    final Map<String, Object> storeProps = getStoreProperties(document);

    // FilterName must be specify.
    AssertUtils.isTrue(storeProps.containsKey("FilterName"), "Unsupported conversion");

    try {
      Lo.qi(XStorable.class, document).storeToURL(toUrl(targetFile), toUnoProperties(storeProps));
    } catch (ErrorCodeIOException errorCodeIoEx) {
      throw new OfficeException(
          ERROR_MESSAGE_STORE + targetFile.getName() + "; errorCode: " + errorCodeIoEx.ErrCode,
          errorCodeIoEx);
    } catch (IOException ioEx) {
      throw new OfficeException(ERROR_MESSAGE_STORE + targetFile.getName(), ioEx);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "source="
        + source
        + ", loadProperties="
        + loadProperties
        + ", target="
        + target
        + ", storeProperties="
        + storeProperties
        + '}';
  }
}
