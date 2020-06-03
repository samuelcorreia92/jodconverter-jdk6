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

import com.google.common.base.Joiner;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.uno.Exception;
import com.sun.star.uno.XComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A collection of utility functions to make Office info easier to get.
 *
 * <p>Inspired by the work of Dr. Andrew Davison from the website <a
 * href="http://fivedots.coe.psu.ac.th/~ad/jlop">Java LibreOffice Programming</a>.
 */
public final class Info { // NOPMD - Disable utility class name rule violation

  private static final Logger LOGGER = LoggerFactory.getLogger(Info.class);

  private static final String NODE_PRODUCT = "/org.openoffice.Setup/Product";
  private static final String NODE_L10N = "/org.openoffice.Setup/L10N";
  // private static final String NODE_OFFICE = "/org.openoffice.Setup/Office";

  private static final String[] NODE_PATHS = {NODE_PRODUCT, NODE_L10N};

  /**
   * Gets whether the specified context is for an OpenOffice installation.
   *
   * @param context The context.
   * @return {@code true} if the specified context is for an OpenOffice installation, {@code false}
   *     otherwise.
   * @throws WrappedUnoException If an UNO exception occurs. The UNO exception will be the cause of
   *     the {@link WrappedUnoException}.
   */
  public static boolean isOpenOffice(final XComponentContext context) {
    return "openoffice".equalsIgnoreCase(getOfficeName(context));
  }

  /**
   * Gets whether the specified context is for a LibreOffice installation.
   *
   * @param context The context.
   * @return {@code true} if the specified context is for an LibreOffice installation, {@code false}
   *     otherwise.
   * @throws WrappedUnoException If an UNO exception occurs. The UNO exception will be the cause of
   *     the {@link WrappedUnoException}.
   */
  public static boolean isLibreOffice(final XComponentContext context) {
    return "libreoffice".equalsIgnoreCase(getOfficeName(context));
  }

  /**
   * Gets the office product name for the given context.
   *
   * @param context The context.
   * @return The office product name, or {@code null} if it could not be retrieved.
   * @throws WrappedUnoException If an UNO exception occurs. The UNO exception will be the cause of
   *     the {@link WrappedUnoException}.
   */
  public static String getOfficeName(final XComponentContext context) {
    return getConfig(context, "ooName");
  }

  /**
   * Gets the office product version (long representation) for the given context, e.g 6.1.0.3
   *
   * @param context The context.
   * @return The office product version, or {@code null} if it could not be retrieved.
   * @throws WrappedUnoException If an UNO exception occurs. The UNO exception will be the cause of
   *     the {@link WrappedUnoException}.
   */
  public static String getOfficeVersionLong(final XComponentContext context) {
    return getConfig(context, "ooSetupVersionAboutBox");
  }

  /**
   * Gets the office product version (short representation) for the given context, e.g 6.1
   *
   * @param context The context.
   * @return The office product version, or {@code null} if it could not be retrieved.
   * @throws WrappedUnoException If an UNO exception occurs. The UNO exception will be the cause of
   *     the {@link WrappedUnoException}.
   */
  public static String getOfficeVersionShort(final XComponentContext context) {
    return getConfig(context, "ooSetupVersion");
  }

  /**
   * Compares two versions strings (ex. 1.6.1).
   *
   * @param version1 The first version to compare.
   * @param version2 The second version to compare.
   * @param length The version length for normalization.
   * @return -1 if version1 &lt; version2, 1 if version1 &gt; version2, 0 if version1 = version2.
   */
  public static int compareVersions(
      final String version1, final String version2, final int length) {

    if (version1 == null && version2 == null) {
      return 0;
    } else if (version1 == null) {
      return -1;
    } else if (version2 == null) {
      return 1;
    }

    final String[] numbers1 = normalizeVersion(version1, length).split("\\.");
    final String[] numbers2 = normalizeVersion(version2, length).split("\\.");

    for (int i = 0; i < numbers1.length; i++) {
      if (Integer.parseInt(numbers1[i]) < Integer.parseInt(numbers2[i])) {
        return -1;
      } else if (Integer.parseInt(numbers1[i]) > Integer.parseInt(numbers2[i])) {
        return 1;
      }
    }

    return 0;
  }

  /**
   * Normalizes a version string so that it has 'length' number of version numbers separated by '.'
   */
  private static String normalizeVersion(final String version, final int length) {

    final List<String> numbers = new ArrayList<String>(Arrays.asList(version.split("\\.")));
    while (numbers.size() < length) {
      numbers.add("0");
    }

    return Joiner.on('.').join(numbers);
  }

  /**
   * Gets the configuration value of the specified property.
   *
   * @param context The main context.
   * @param propName The property name of the property value to get.
   * @return The property value, or null if not found.
   * @throws WrappedUnoException If an UNO exception occurs. The UNO exception will be the cause of
   *     the {@link WrappedUnoException}.
   */
  public static String getConfig(final XComponentContext context, final String propName) {

    for (final String nodePath : NODE_PATHS) {
      final Object info = getConfig(context, nodePath, propName);
      if (info != null) {
        return (String) info;
      }
    }
    return null;
  }

  /**
   * Gets the configuration value of the specified property for the specified path.
   *
   * @param context The main context.
   * @param nodePath The path for which the properties are get.
   * @param propName The property name of the property value to get.
   * @return The property value, or null if not found.
   * @throws WrappedUnoException If an UNO exception occurs. The UNO exception will be the cause of
   *     the {@link WrappedUnoException}.
   */
  public static Object getConfig(
      final XComponentContext context, final String nodePath, final String propName) {
    final XPropertySet set = getConfigProperties(context, nodePath);
    if (set == null) {
      return null;
    }
    return Props.getProperty(set, propName);
  }

  /**
   * Gets the configuration provider for the given context.
   *
   * @param context The main context.
   * @return The {@link XMultiServiceFactory} service, or null if not available.
   */
  public static XMultiServiceFactory getConfigProvider(final XComponentContext context) {
    return Lo.createInstanceMCF(
        context, XMultiServiceFactory.class, "com.sun.star.configuration.ConfigurationProvider");
  }

  private static Object getConfigAccess(
      final XComponentContext context, final String serviceSpecifier, final String nodePath) {

    final XMultiServiceFactory provider = getConfigProvider(context);
    if (provider == null) {
      LOGGER.debug("Could not create configuration provider");
      return null;
    }

    // Specifies the location of the view root in the configuration.
    try {
      return provider.createInstanceWithArguments(
          serviceSpecifier, Props.makeProperties("nodepath", nodePath));
    } catch (Exception ex) {
      LOGGER.debug("Could not access config for: " + nodePath, ex);
    }

    return null;
  }

  /**
   * Gets the read-only configuration access for the specified path.
   *
   * @param context The main context.
   * @param nodePath The path for which the configuration access is get.
   * @return The read-only configuration access service, or null if not available.
   */
  public static Object getConfigAccess(final XComponentContext context, final String nodePath) {
    return getConfigAccess(context, "com.sun.star.configuration.ConfigurationAccess", nodePath);
  }

  /**
   * Gets the updatable configuration access for the specified path.
   *
   * @param context The main context.
   * @param nodePath The path for which the configuration access is get.
   * @return The updatable configuration access service, or null if not available.
   */
  public static Object getConfigUpdateAccess(
      final XComponentContext context, final String nodePath) {
    return getConfigAccess(
        context, "com.sun.star.configuration.ConfigurationUpdateAccess", nodePath);
  }

  /**
   * Gets the configuration properties for the specified path.
   *
   * @param context The main context.
   * @param nodePath The path for which the properties are get.
   * @return A {@link XPropertySet} containing the configuration properties for the specified path,
   *     or null if not found.
   */
  public static XPropertySet getConfigProperties(
      final XComponentContext context, final String nodePath) {

    final Object configAccess = getConfigAccess(context, nodePath);
    if (configAccess == null) {
      LOGGER.debug("Could not create configuration access service");
      return null;
    }

    return Lo.qi(XPropertySet.class, configAccess);
  }

  /**
   * Gets whether the given document is of the given document type.
   *
   * @param document The document.
   * @param documentType The document type to check.
   * @return {@code true} if the document is of the specified type, {@code true} otherwise.
   */
  public static boolean isDocumentType(final XComponent document, final String documentType) {
    return Lo.qi(XServiceInfo.class, document).supportsService(documentType);
  }

  // Suppresses default constructor, ensuring non-instantiability.
  private Info() {
    throw new AssertionError("Utility class must not be instantiated");
  }
}
