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

package org.jodconverter.local.office;

import com.sun.star.beans.PropertyValue;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XServiceInfo;
import org.jodconverter.core.document.DocumentFamily;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.util.AssertUtils;
import org.jodconverter.core.util.OSUtils;
import org.jodconverter.core.util.StringUtils;
import org.jodconverter.local.office.utils.Lo;
import org.jodconverter.local.process.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Provides helper functions for local office. */
public final class LocalOfficeUtils {

  private static final String EXECUTABLE_DEFAULT = "program/soffice.bin";
  private static final String EXECUTABLE_MAC = "program/soffice";
  private static final String EXECUTABLE_MAC_41 = "MacOS/soffice";
  private static final String EXECUTABLE_WINDOWS = "program/soffice.exe";
  private static final Logger LOGGER = LoggerFactory.getLogger(LocalOfficeUtils.class);

  /**
   * This class is required in order to create a default office home only on demand, as explained by
   * the Initialization-on-demand holder idiom:
   * https://www.wikiwand.com/en/Initialization-on-demand_holder_idiom
   */
  private static class DefaultOfficeHomeHolder {

    /* default */ static final File INSTANCE;

    static {
      if (StringUtils.isNotBlank(System.getProperty("office.home"))) {
        INSTANCE = new File(System.getProperty("office.home"));

      } else if (OSUtils.IS_OS_WINDOWS) {

        // Try to find the most recent version of LibreOffice or OpenOffice,
        // starting with the 64-bit version. %ProgramFiles(x86)% on 64-bit
        // machines; %ProgramFiles% on 32-bit ones
        final String programFiles64 = System.getenv("ProgramFiles");
        final String programFiles32 = System.getenv("ProgramFiles(x86)");

        INSTANCE =
            findOfficeHome(
                EXECUTABLE_WINDOWS,
                programFiles64 + File.separator + "LibreOffice",
                programFiles64 + File.separator + "LibreOffice 5",
                programFiles32 + File.separator + "LibreOffice 5",
                programFiles32 + File.separator + "OpenOffice 4",
                programFiles64 + File.separator + "LibreOffice 4",
                programFiles32 + File.separator + "LibreOffice 4",
                programFiles64 + File.separator + "LibreOffice 3",
                programFiles32 + File.separator + "LibreOffice 3",
                programFiles32 + File.separator + "OpenOffice.org 3");

      } else if (OSUtils.IS_OS_MAC) {

        File homeDir =
            findOfficeHome(
                EXECUTABLE_MAC_41,
                "/Applications/LibreOffice.app/Contents",
                "/Applications/OpenOffice.app/Contents",
                "/Applications/OpenOffice.org.app/Contents");

        if (homeDir == null) {
          homeDir =
              findOfficeHome(
                  EXECUTABLE_MAC,
                  "/Applications/LibreOffice.app/Contents",
                  "/Applications/OpenOffice.app/Contents",
                  "/Applications/OpenOffice.org.app/Contents");
        }

        INSTANCE = homeDir;

      } else {

        // UNIX

        // Linux or other *nix variants
        INSTANCE =
            findOfficeHome(
                EXECUTABLE_DEFAULT,
                "/usr/lib64/libreoffice",
                "/usr/lib/libreoffice",
                "/usr/local/lib64/libreoffice",
                "/usr/local/lib/libreoffice",
                "/opt/libreoffice",
                "/usr/lib64/openoffice",
                "/usr/lib64/openoffice.org3",
                "/usr/lib64/openoffice.org",
                "/usr/lib/openoffice",
                "/usr/lib/openoffice.org3",
                "/usr/lib/openoffice.org",
                "/opt/openoffice4",
                "/opt/openoffice.org3");
      }

      LOGGER.debug("Default office home set to {}", INSTANCE);
    }

    private static File findOfficeHome(final String executablePath, final String... homePaths) {
      for (String homePath : homePaths) {
        File homeDir = new File(homePath);
        if (new File(homeDir, executablePath).isFile()) {
          return homeDir;
        }
      }
      return null;
    }
  }

  /**
   * Find the best process manager that will be used to retrieve a process PID and to kill a process
   * by PID.
   *
   * @return The best process manager according to the current OS.
   */
  public static ProcessManager findBestProcessManager() {

    if (OSUtils.IS_OS_MAC) {
      return MacProcessManager.getDefault();
    } else if (OSUtils.IS_OS_FREE_BSD) {
      return FreeBSDProcessManager.getDefault();
    } else if (OSUtils.IS_OS_UNIX) {
      return UnixProcessManager.getDefault();
    } else if (OSUtils.IS_OS_WINDOWS) {
      final WindowsProcessManager windowsProcessManager = WindowsProcessManager.getDefault();
      return windowsProcessManager.isUsable()
          ? windowsProcessManager
          : PureJavaProcessManager.getDefault();
    } else {
      // NOTE: UnixProcessManager can't be trusted to work on Solaris
      // because of the 80-char limit on ps output there
      return PureJavaProcessManager.getDefault();
    }
  }

  /**
   * Builds an array of {@link OfficeUrl} from an array of port numbers and an array of pipe names.
   *
   * @param portNumbers The port numbers from which office URLs will be created, may be null.
   * @param pipeNames The pipe names from which office URLs will be created, may be null.
   * @return an array of office URL. If both arguments are null, then an array is returned with a
   *     single office URL, using the default port number 2002.
   */
  public static List<OfficeUrl> buildOfficeUrls(
      final List<Integer> portNumbers, final List<String> pipeNames) {

    // Assign default value if no pipe names or port numbers have been specified.
    if ((portNumbers == null || portNumbers.isEmpty())
        && (pipeNames == null || pipeNames.isEmpty())) {
      return Collections.singletonList(new OfficeUrl(2002));
    }

    // Build the office URL list and return it
    final List<OfficeUrl> officeUrls = new ArrayList<OfficeUrl>();
    if (portNumbers != null) {
      for (Integer portNumber : portNumbers) {
        officeUrls.add(new OfficeUrl(portNumber));
      }
    }
    if (pipeNames != null) {
      for (String pipeName : pipeNames) {
        officeUrls.add(new OfficeUrl(pipeName));
      }
    }
    return officeUrls;
  }

  /**
   * Gets the default office home directory, which is auto-detected.
   *
   * @return A {@code File} instance that is the directory where lives the first detected office
   *     installation.
   */
  public static File getDefaultOfficeHome() {
    return DefaultOfficeHomeHolder.INSTANCE;
  }

  /**
   * Gets the {@link org.jodconverter.core.document.DocumentFamily} of the specified document.
   *
   * @param document The document whose family will be returned.
   * @return The {@link org.jodconverter.core.document.DocumentFamily} for the specified document.
   * @throws org.jodconverter.core.office.OfficeException If the document family cannot be
   *     retrieved.
   */
  public static DocumentFamily getDocumentFamily(final XComponent document) throws OfficeException {

    AssertUtils.notNull(document, "document must not be null");

    final XServiceInfo serviceInfo = Lo.qi(XServiceInfo.class, document);
    if (serviceInfo.supportsService("com.sun.star.text.GenericTextDocument")) {
      // NOTE: a GenericTextDocument is either a TextDocument, a WebDocument, or a GlobalDocument
      // but this further distinction doesn't seem to matter for conversions
      return DocumentFamily.TEXT;
    } else if (serviceInfo.supportsService("com.sun.star.sheet.SpreadsheetDocument")) {
      return DocumentFamily.SPREADSHEET;
    } else if (serviceInfo.supportsService("com.sun.star.presentation.PresentationDocument")) {
      return DocumentFamily.PRESENTATION;
    } else if (serviceInfo.supportsService("com.sun.star.drawing.DrawingDocument")) {
      return DocumentFamily.DRAWING;
    }

    throw new OfficeException("Document of unknown family: " + serviceInfo.getImplementationName());
  }

  /**
   * Gets the office executable within an office installation.
   *
   * @param officeHome The root (home) directory of the office installation.
   * @return A instance of the executable file.
   */
  public static File getOfficeExecutable(final File officeHome) {

    // Mac
    if (OSUtils.IS_OS_MAC) {
      // Starting with LibreOffice 4.1 the location of the executable has changed on Mac.
      // It's now in program/soffice. Handle both cases!
      File executableFile = new File(officeHome, EXECUTABLE_MAC_41);
      if (!executableFile.isFile()) {
        executableFile = new File(officeHome, EXECUTABLE_MAC);
      }
      return executableFile;
    }

    // Windows
    if (OSUtils.IS_OS_WINDOWS) {
      return new File(officeHome, EXECUTABLE_WINDOWS);
    }

    // Everything else
    return new File(officeHome, EXECUTABLE_DEFAULT);
  }

  /**
   * Creates a {@code PropertyValue} with the specified name and value.
   *
   * @param name The property name.
   * @param value The property value.
   * @return The created {@code PropertyValue}.
   */
  public static PropertyValue property(final String name, final Object value) {

    final PropertyValue prop = new PropertyValue();
    prop.Name = name;
    prop.Value = value;
    return prop;
  }

  /**
   * Converts a regular java map to an array of {@code PropertyValue}, usable as arguments with UNO
   * interface types.
   *
   * @param properties The map to convert.
   * @return An array of {@code PropertyValue}.
   */
  public static PropertyValue[] toUnoProperties(final Map<String, Object> properties) {

    final List<PropertyValue> propertyValues = new ArrayList<PropertyValue>(properties.size());
    for (final Map.Entry<String, Object> entry : properties.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Map) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> subProperties = (Map<String, Object>) value;
        value = toUnoProperties(subProperties);
      }
      propertyValues.add(property(entry.getKey(), value));
    }
    return propertyValues.toArray(new PropertyValue[0]);
  }

  /**
   * Constructs an URL from the specified file as expected by office.
   *
   * @param file The file for which an URL will be constructed.
   * @return A valid office URL.
   */
  public static String toUrl(final File file) {

    final String path = file.toURI().getRawPath();
    final String url = path.startsWith("//") ? "file:" + path : "file://" + path;
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  /**
   * Validates that the specified File instance is a valid office home directory.
   *
   * @param officeHome The home to validate.
   * @exception IllegalStateException If the specified directory if not a valid office home
   *     directory.
   */
  public static void validateOfficeHome(final File officeHome) {

    if (!officeHome.isDirectory()) {
      throw new IllegalStateException(
          "officeHome doesn't exist or is not a directory: " + officeHome);
    }

    if (!getOfficeExecutable(officeHome).isFile()) {
      throw new IllegalStateException(
          "Invalid officeHome: it doesn't contain soffice.bin: " + officeHome);
    }
  }

  /**
   * Validates that the specified File instance is a valid office template profile directory.
   *
   * @param templateProfileDir The directory to validate.
   * @exception IllegalStateException If the specified directory if not a valid office template
   *     profile directory.
   */
  public static void validateOfficeTemplateProfileDirectory(final File templateProfileDir) {

    // Template profile directory is not required.
    if (templateProfileDir == null || new File(templateProfileDir, "user").isDirectory()) {
      return;
    }

    throw new IllegalStateException(
        "templateProfileDir doesn't appear to contain a user profile: " + templateProfileDir);
  }

  /**
   * Validates that the specified File instance is a valid office working directory.
   *
   * @param workingDir The directory to validate.
   * @exception IllegalStateException If the specified directory if not a valid office working
   *     directory.
   */
  public static void validateOfficeWorkingDirectory(final File workingDir) {

    if (!workingDir.isDirectory()) {
      throw new IllegalStateException(
          "workingDir doesn't exist or is not a directory: " + workingDir);
    }

    if (!workingDir.canWrite()) {
      throw new IllegalStateException("workingDir '" + workingDir + "' cannot be written to");
    }
  }

  // Suppresses default constructor, ensuring non-instantiability.
  private LocalOfficeUtils() {
    throw new AssertionError("Utility class must not be instantiated");
  }
}
