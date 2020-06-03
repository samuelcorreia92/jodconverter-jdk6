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

import org.jodconverter.local.process.PumpStreamHandler;
import org.jodconverter.local.process.StreamPumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wrapper class for a process we want to redirect the output and error stream. */
class VerboseProcess {

  private static final Logger LOGGER = LoggerFactory.getLogger(VerboseProcess.class);

  private final Process process;
  private final PumpStreamHandler streamHandler;

  /**
   * Creates a new wrapper for the given process.
   *
   * @param process The process for which the wrapper is created.
   */
  /* default */ VerboseProcess(final Process process) {
    super();

    if (process == null) {
      throw new NullPointerException("process must not be null");
    }

    this.process = process;

    streamHandler =
        new PumpStreamHandler(
            new StreamPumper(
                process.getInputStream(),
                new StreamPumper.LineConsumer() {
                  public void consume(String line) {
                    LOGGER.info(line);
                  }
                }),
            new StreamPumper(
                process.getErrorStream(),
                new StreamPumper.LineConsumer() {
                  public void consume(String line) {
                    LOGGER.error(line);
                  }
                }));
    streamHandler.start();
  }

  /**
   * Gets the process for this wrapper.
   *
   * @return The process.
   */
  /* default */ Process getProcess() {
    return process;
  }

  /**
   * Gets the exit code for the process.
   *
   * @return The exit code of the process, or null if not terminated yet.
   */
  /* default */ Integer getExitCode() {

    try {
      final int exitValue = process.exitValue();
      streamHandler.stop();
      LOGGER.trace("Process has been terminated with exit value {}", exitValue);
      return exitValue;

    } catch (IllegalThreadStateException ex) {
      LOGGER.trace("Could not get exit value; the process is running");
      return null;
    }
  }
}
