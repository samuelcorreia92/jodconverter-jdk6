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

package org.jodconverter.core.util;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/** Contains files helper functions. */
public final class FileUtils {

  private static final char UNIX_SEPARATOR = '/';
  private static final char WINDOWS_SEPARATOR = '\\';

  //  private static int lastIndexOfSeparator(final String filename) {
  //
  //    final int idx = filename.lastIndexOf(UNIX_SEPARATOR);
  //    if (idx == -1) {
  //      return filename.lastIndexOf(WINDOWS_SEPARATOR);
  //    }
  //    return idx;
  //  }

  private static boolean endsWithSeparator(final String filename) {

    if (filename.length() == 0) {
      return false;
    }
    final char lastChar = filename.charAt(filename.length() - 1);
    return lastChar == UNIX_SEPARATOR || lastChar == WINDOWS_SEPARATOR;
  }

  /**
   * Deletes a file. If file is a directory, delete it and all sub-directories.
   *
   * @param file File or directory to delete, can be {@code null}.
   * @return {@code true} If the file or directory is deleted, {@code false} otherwise. The file or
   *     directory is considered deleted if it does not exist when the function ends, meaning that a
   *     {@code null} input file or a file that does not exist will also returns {@code false}.
   * @throws IOException If an IO error occurs.
   */
  public static boolean delete(final File file) throws IOException {
    if (file == null || !file.exists()) {
      return false;
    }

    org.apache.commons.io.FileUtils.forceDelete(file);
    return true;
  }

  /**
   * Deletes a file, never throwing an exception. If file is a directory, delete it and all
   * sub-directories.
   *
   * @param file File or directory to delete, can be {@code null}.
   * @return {@code true} If the file or directory is deleted, {@code false} otherwise.
   */
  public static boolean deleteQuietly(final File file) {
    try {
      return delete(file);
    } catch (IOException ignored) {
      return false;
    }
  }

  /**
   * Gets the name minus the path and extension from a full filename. The text after the last
   * forward or backslash and before the last dot is returned.
   *
   * @param filename The filename to query, may be {@code null}.
   * @return The name of the file without the path and extension, or an empty string if none exists.
   */
  public static String getBaseName(final String filename) {
    return FilenameUtils.getBaseName(filename);
  }

  /**
   * Gets the file extension from a full filename. The text after the last dot is returned.
   *
   * @param filename The filename to query, may be {@code null}.
   * @return The extension of the file, or an empty string if none exists.
   */
  public static String getExtension(final String filename) {
    return FilenameUtils.getExtension(filename);
  }

  /**
   * Gets the name minus the path from a full filename. The text after the last forward or backslash
   * is returned.
   *
   * @param filename The filename to query, may be {@code null}.
   * @return The name of the file without the path, or an empty string if none exists.
   */
  public static String getName(final String filename) {
    return FilenameUtils.getName(filename);
  }

  /**
   * Copies a file to another path, preserving the last modified date.
   *
   * @param srcFile An existing file to copy, must not be {@code null}.
   * @param destFile The target file, must not be {@code null}.
   * @param options Options specifying how the copy should be done.
   * @throws IOException If an IO error occurs.
   */
  //  TODO FERD
  //  public static void copyFile(final File srcFile, final File destFile, final CopyOption...
  // options)
  //      throws IOException {
  //    AssertUtils.notNull(srcFile, "srcFile must not be null");
  //    AssertUtils.notNull(destFile, "destFile must not be null");
  //
  //    final Path srcPath = srcFile.toPath();
  //
  //    AssertUtils.isTrue(Files.isRegularFile(srcPath), "srcFile must be an existing file");
  //
  //    Files.copy(srcPath, destFile.toPath(), options);
  //    destFile.setLastModified(srcFile.lastModified());
  //  }

  /**
   * Copies a file to a directory, preserving the last modified date.
   *
   * @param srcFile An existing file to copy, must not be {@code null}.
   * @param destDir The directory to place the copy in, must not be {@code null}.
   * @param options Options specifying how the copy should be done.
   * @throws IOException If an IO error occurs.
   */
  //  TODO FERD
  //  public static void copyFileToDirectory(
  //      final File srcFile, final File destDir, final CopyOption... options) throws IOException {
  //    AssertUtils.notNull(srcFile, "srcFile must not be null");
  //    AssertUtils.notNull(destDir, "destDir must not be null");
  //
  //    final Path srcPath = srcFile.toPath();
  //
  //    AssertUtils.isTrue(Files.isRegularFile(srcPath), "srcFile must be an existing file");
  //
  //    final Path destPath = destDir.toPath().resolve(srcFile.getName());
  //    Files.copy(srcPath, destPath, options);
  //    destPath.toFile().setLastModified(srcFile.lastModified());
  //  }

  /**
   * Copies a directory recursively, preserving the files last modified date.
   *
   * @param srcDir An existing directory to copy, must not be {@code null}.
   * @param destDir The target directory, must not be {@code null}.
   * @throws IOException If an IO error occurs.
   */
  public static void copyDirectory(final File srcDir, final File destDir) throws IOException {
    org.apache.commons.io.FileUtils.copyDirectory(srcDir, destDir);
  }

  /**
   * Reads the contents of a file into a String.
   *
   * @param file The file to read, must not be {@code null}.
   * @param encoding The encoding to use, must not be {@code null}.
   * @return the file contents, never {@code null}.
   * @throws IOException If an IO error occurs.
   */
  public static String readFileToString(final File file, final Charset encoding)
      throws IOException {
    AssertUtils.notNull(file, "file must not be null");
    AssertUtils.notNull(encoding, "encoding must not be null");

    return org.apache.commons.io.FileUtils.readFileToString(file, encoding);
  }

  // Suppresses default constructor, ensuring non-instantiability.
  private FileUtils() {
    throw new AssertionError("Utility class must not be instantiated");
  }
}
