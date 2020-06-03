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

package org.jodconverter.remote.office;

import org.jodconverter.core.office.AbstractOfficeManagerPool;
import org.jodconverter.core.office.InstalledOfficeManagerHolder;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.core.office.OfficeUtils;
import org.jodconverter.core.util.AssertUtils;
import org.jodconverter.remote.ssl.SslConfig;

import java.io.File;
import java.util.ArrayList;

/**
 * {@link org.jodconverter.core.office.OfficeManager} pool implementation that does not depend on an
 * office installation to process conversion taks.
 */
public final class RemoteOfficeManager extends AbstractOfficeManagerPool {

  /**
   * Creates a new builder instance.
   *
   * @return A new builder instance.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new {@link RemoteOfficeManager} with default configuration.
   *
   * @param urlConnection The URL to the LibreOfficeOnline server.
   * @return A {@link RemoteOfficeManager} with default configuration.
   */
  public static RemoteOfficeManager make(final String urlConnection) {
    return builder().urlConnection(urlConnection).build();
  }

  /**
   * Creates a new {@link RemoteOfficeManager} with default configuration. The created manager will
   * then be the unique instance of the {@link
   * org.jodconverter.core.office.InstalledOfficeManagerHolder} class. Note that if the {@code
   * InstalledOfficeManagerHolder} class already holds an {@code OfficeManager} instance, the owner
   * of this existing manager is responsible to stopped it.
   *
   * @param urlConnection The URL to the LibreOfficeOnline server.
   * @return A {@link RemoteOfficeManager} with default configuration.
   */
  public static RemoteOfficeManager install(final String urlConnection) {
    return builder().urlConnection(urlConnection).install().build();
  }

  private RemoteOfficeManager(
      final File workingDir,
      final String urlConnection,
      final Integer poolSize,
      final SslConfig sslConfig,
      final Long connectTimeout,
      final Long socketTimeout,
      final Long taskExecutionTimeout,
      final Long taskQueueTimeout) {
    super(workingDir, poolSize, taskQueueTimeout);

    int poolSizeSafe = poolSize == null ? DEFAULT_POOL_SIZE : poolSize;
    ArrayList<OfficeManager> officeManagers = new ArrayList<OfficeManager>(poolSizeSafe);
    for (int i = 0; i < poolSizeSafe; i++) {
      officeManagers.add(
          new RemoteOfficeManagerPoolEntry(
              urlConnection, sslConfig, connectTimeout, socketTimeout, taskExecutionTimeout));
    }
    setEntries(officeManagers);
  }

  /**
   * A builder for constructing a {@link RemoteOfficeManager}.
   *
   * @see RemoteOfficeManager
   */
  public static final class Builder extends AbstractOfficeManagerPoolBuilder<Builder> {

    // The maximum size of the pool.
    private static final int MAX_POOL_SIZE = 1000;

    private Integer poolSize;
    private String urlConnection;
    private SslConfig sslConfig;
    private Long connectTimeout;
    private Long socketTimeout;

    // Private constructor so only RemoteOfficeManager can initialize an instance of this builder.
    private Builder() {
      super();
    }

    @Override
    public RemoteOfficeManager build() {

      AssertUtils.notBlank(urlConnection, "urlConnection must not be null nor blank");

      // Assign default values for properties that are not set yet.
      if (workingDir == null) {
        workingDir = OfficeUtils.getDefaultWorkingDir();
      }

      final RemoteOfficeManager manager =
          new RemoteOfficeManager(
              workingDir,
              urlConnection,
              poolSize,
              sslConfig,
              connectTimeout,
              socketTimeout,
              taskExecutionTimeout,
              taskQueueTimeout);
      if (install) {
        InstalledOfficeManagerHolder.setInstance(manager);
      }
      return manager;
    }

    /**
     * Specifies the pool size of the manager.
     *
     * @param poolSize The pool size.
     * @return This builder instance.
     */
    public Builder poolSize(final Integer poolSize) {

      if (poolSize != null) {
        AssertUtils.isTrue(
            poolSize >= 0 && poolSize <= MAX_POOL_SIZE,
            String.format("poolSize %s must be between %d and %d", poolSize, 1, MAX_POOL_SIZE));
      }
      this.poolSize = poolSize;
      return this;
    }

    /**
     * Specifies the URL connection of the manager.
     *
     * @param urlConnection The URL connection.
     * @return This builder instance.
     */
    public Builder urlConnection(final String urlConnection) {

      this.urlConnection = urlConnection;
      return this;
    }

    /**
     * Specifies the SSL configuration to secure communication with LibreOffice Online.
     *
     * @param sslConfig The SSL configuration.
     * @return This builder instance.
     */
    public Builder sslConfig(final SslConfig sslConfig) {

      this.sslConfig = sslConfig;
      return this;
    }

    /**
     * The timeout in milliseconds until a connection is established. A timeout value of zero is
     * interpreted as an infinite timeout. A negative value is interpreted as undefined (system
     * default).
     *
     * <p>&nbsp; <b><i>Default</i></b>: 60000 (1 minute)
     *
     * @param connectTimeout The connect timeout, in milliseconds.
     * @return This builder instance.
     */
    public Builder connectTimeout(final Long connectTimeout) {

      if (connectTimeout != null) {
        AssertUtils.isTrue(
            connectTimeout >= 0,
            String.format("connectTimeout %s must greater than or equal to 0", connectTimeout));
      }
      this.connectTimeout = connectTimeout;
      return this;
    }

    /**
     * Specifies the socket timeout ({@code SO_TIMEOUT}) in milliseconds, which is the timeout for
     * waiting for data or, put differently, a maximum period inactivity between two consecutive
     * data packets). A timeout value of zero is interpreted as an infinite timeout. A negative
     * value is interpreted as undefined (system default).
     *
     * <p>&nbsp; <b><i>Default</i></b>: 120000 (2 minutes)
     *
     * @param socketTimeout The socket timeout, in milliseconds.
     * @return This builder instance.
     */
    public Builder socketTimeout(final Long socketTimeout) {

      if (socketTimeout != null) {
        AssertUtils.isTrue(
            socketTimeout >= 0,
            String.format("socketTimeout %s must greater than or equal to 0", socketTimeout));
      }
      this.socketTimeout = socketTimeout;
      return this;
    }
  }
}
