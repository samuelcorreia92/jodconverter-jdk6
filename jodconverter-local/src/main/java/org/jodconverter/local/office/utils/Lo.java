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

package org.jodconverter.local.office.utils;

import com.google.common.base.Optional;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import org.jodconverter.core.util.AssertUtils;

/**
 * A collection of utility functions to make Office easier to use.
 *
 * <p>Inspired by the work of Dr. Andrew Davison from the website <a
 * href="http://fivedots.coe.psu.ac.th/~ad/jlop">Java LibreOffice Programming</a>.
 */
public final class Lo { // NOPMD - Disable utility class name rule violation

  // Document types service names
  // public static final String WRITER_SERVICE = "com.sun.star.text.TextDocument";
  public static final String WRITER_SERVICE = "com.sun.star.text.GenericTextDocument";
  public static final String CALC_SERVICE = "com.sun.star.sheet.SpreadsheetDocument";
  public static final String DRAW_SERVICE = "com.sun.star.drawing.DrawingDocument";
  public static final String IMPRESS_SERVICE = "com.sun.star.presentation.PresentationDocument";

  /**
   * Queries the given UNO object for the given Java class (which must represent a UNO interface
   * type).
   *
   * @param <T> The requested UNO interface type.
   * @param type A Java class representing a UNO interface type; cannot be {@code null}.
   * @param object A reference to any Java object representing (a facet of) a UNO object; cannot be
   *     {@code null}.
   * @return A reference to the requested UNO interface type.
   * @see UnoRuntime#queryInterface(Class, Object)
   */
  public static <T> T qi(final Class<T> type, final Object object) {

    AssertUtils.notNull(type, "type must not be null");
    AssertUtils.notNull(type, "object must not be null");

    final T obj = UnoRuntime.queryInterface(type, object);

    AssertUtils.notNull(
        obj,
        String.format(
            "UNO object of type %s must not be null for object of type %s",
            type.getName(), object.getClass().getName()));

    return obj;
  }

  /**
   * Queries the given UNO object for the given Java class (which must represent a UNO interface
   * type).
   *
   * @param <T> the requested UNO interface type.
   * @param type A Java class representing a UNO interface type; may be {@code null}.
   * @param object A reference to any Java object representing (a facet of) a UNO object; may be
   *     {@code null}.
   * @return A reference to the requested UNO interface type if available, otherwise {@code null}.
   * @see UnoRuntime#queryInterface(Class, Object)
   */
  public static <T> Optional<T> qiOptional(final Class<T> type, final Object object) {
    return Optional.fromNullable(UnoRuntime.queryInterface(type, object));
  }

  /**
   * Gets the XMultiServiceFactory for the given component.
   *
   * @param component The component.
   * @return The service factory.
   */
  public static XMultiServiceFactory getServiceFactory(final XComponent component) {
    return qi(XMultiServiceFactory.class, component);
  }

  /**
   * Create an interface object of the given class from the given named service; uses given
   * XComponent and 'old' XMultiServiceFactory, so a document must have been already loaded/created.
   *
   * @param <T> The requested UNO interface type.
   * @param component The component.
   * @param type A Java class representing a UNO interface type.
   * @param serviceName The service name.
   * @return A reference to the requested UNO interface type if available, otherwise {@code null}.
   * @throws WrappedUnoException If an UNO exception occurs. The UNO exception will be the cause of
   *     the {@link WrappedUnoException}.
   */
  public static <T> T createInstanceMSF(
      final XComponent component, final Class<T> type, final String serviceName) {

    // Create service component using the specified factory.
    // Then uses bridge to obtain proxy to remote interface inside service;
    // implements casting across process boundaries
    return createInstanceMSF(getServiceFactory(component), type, serviceName);
  }

  /**
   * Create an interface object of the given class from the given named service; uses given 'old'
   * XMultiServiceFactory, so a document must have been already loaded/created.
   *
   * @param <T> The requested UNO interface type.
   * @param factory The service factory.
   * @param type A Java class representing a UNO interface type.
   * @param serviceName The service name.
   * @return A reference to the requested UNO interface type if available, otherwise {@code null}.
   * @throws WrappedUnoException If an UNO exception occurs. The UNO exception will be the cause of
   *     the {@link WrappedUnoException}.
   */
  public static <T> T createInstanceMSF(
      final XMultiServiceFactory factory, final Class<T> type, final String serviceName) {

    // Create service component using the specified factory.
    // Then uses bridge to obtain proxy to remote interface inside service;
    // implements casting across process boundaries
    try {
      return qi(type, factory.createInstance(serviceName));
    } catch (Exception ex) {
      throw new WrappedUnoException(ex.getMessage(), ex);
    }
  }

  /**
   * Create an interface object of the given class from the given named service; uses given
   * XComponentContext and 'new' XMultiComponentFactory so only a bridge to office is needed.
   *
   * @param <T> The requested UNO interface type.
   * @param context The component context.
   * @param type A Java class representing a UNO interface type.
   * @param serviceName The service name.
   * @return A reference to the requested UNO interface type if available, otherwise {@code null}.
   * @throws WrappedUnoException If an UNO exception occurs. The UNO exception will be the cause of
   *     the {@link WrappedUnoException}.
   */
  public static <T> T createInstanceMCF(
      final XComponentContext context, final Class<T> type, final String serviceName) {

    // Create service component using the specified component context.
    // Then uses bridge to obtain proxy to remote interface inside service;
    // implements casting across process boundaries
    try {
      return qiOptional(
              type, context.getServiceManager().createInstanceWithContext(serviceName, context))
          .orNull();
    } catch (Exception ex) {
      throw new WrappedUnoException(ex);
    }
  }

  // Suppresses default constructor, ensuring non-instantiability.
  private Lo() {
    throw new AssertionError("Utility class must not be instantiated");
  }
}
