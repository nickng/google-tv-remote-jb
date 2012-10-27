/*
 * Copyright (C) 2010 Google Inc.  All rights reserved.
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

package com.google.anymote.common;

import com.google.anymote.Messages.Connect;

/**
 * A wrapper around a connection message sent from the device to the server upon
 * a connection.
 */
public final class ConnectInfo {

  /**
   * Remote device name.
   */
  private final String mDeviceName;

  /**
   * Device version number.
   */
  private final int mVersion;

  /**
   * Default number if the version is not set.
   */
  public static final int NOT_SET = -1;

  public ConnectInfo(String deviceName) {
    mDeviceName = deviceName;
    mVersion = NOT_SET;
  }

  public ConnectInfo(String deviceName, int version) {
    mDeviceName = deviceName;
    mVersion = version;
  }

  /**
   * Returns the name of the device that initiated the connection.
   */
  public String getDeviceName() {
    return mDeviceName;
  }

  /**
   * Returns {@code true} if the connect info has a version id.
   */
  public boolean hasVersionNumber() {
    return mVersion != NOT_SET;
  }

  /**
   * Returns the version of the device that initiated the connection or
   * {@link #NOT_SET} if unknown.
   */
  public int getVersionNumber() {
    return mVersion;
  }

  /**
   * Get a {@link ConnectInfo} object from a {@link Connect} protocol buffer.
   *
   * @param  message the protocol buffer to read
   * @return the corresponding info object
   */
  public static ConnectInfo parseFromProto(Connect message) {
    String deviceName = message.getDeviceName();
    ConnectInfo info;
    if (message.hasVersion()) {
      info = new ConnectInfo(deviceName, message.getVersion());
    } else {
      info = new ConnectInfo(deviceName);
    }
    return info;
  }

  /**
   * Creates a {@link Connect} protocol buffer from a {@link ConnectInfo}.
   *
   * @return the corresponding protocol buffer
   */
  public Connect getProto() {
    Connect.Builder builder = Connect.newBuilder();
    builder.setDeviceName(mDeviceName);
    if (mVersion != -1) {
      builder.setVersion(mVersion);
    }
    return builder.build();
  }

  @Override
  public String toString() {
      return "DeviceInfo: "  + mDeviceName + "[" + mVersion + "]";
  }
}
