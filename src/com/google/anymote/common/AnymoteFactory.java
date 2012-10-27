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

import java.io.InputStream;
import java.io.OutputStream;

import com.google.anymote.device.DeviceAdapter;
import com.google.anymote.device.DeviceMessageAdapter;
import com.google.anymote.device.MessageReceiver;
import com.google.anymote.server.RequestReceiver;
import com.google.anymote.server.ServerAdapter;
import com.google.anymote.server.ServerMessageAdapter;

/**
 * Creates the classes needed to use the protocol. Instantiate one of those to
 * use the protocol.
 */
public final class AnymoteFactory {

  // Utility class
  private AnymoteFactory() {
    throw new IllegalStateException("Should not instantiate");
  }

  /**
   * Initializes the server side of the remote protocol.
   * <p>
   * Asynchronous calls when requests are received will be made to the receiver.
   *
   * @param receiver the receiver of the events on the server side
   * @param input the stream where the events are received
   * @param output the stream where events can be sent
   * @param errorListener the error listener for the protocol
   * @return a server adapter that will receive messages from the device
   */
  public static ServerAdapter getServerAdapter(RequestReceiver receiver,
      InputStream input, OutputStream output, ErrorListener errorListener) {
    RemoteWireAdapter remoteWireAdapter =
        new RemoteWireAdapter(input, output, errorListener);
    ServerMessageAdapter serverMessageAdapter =
        new ServerMessageAdapter(receiver, remoteWireAdapter);
    remoteWireAdapter.setMessageListener(serverMessageAdapter);
    remoteWireAdapter.startReceivingThread();
    return serverMessageAdapter;
  }

  /**
   * Initializes the device side of the remote protocol.
   * <p>
   * Asynchronous calls when message are received will be made to the receiver.
   *
   * @param receiver the receiver of the events on the server side
   * @param input the stream where the events are received
   * @param output the stream where events can be sent
   * @param errorListener the error listener for the protocol
   * @return a device adapter that will receive messages from the server
   */
  public static DeviceAdapter getDeviceAdapter(MessageReceiver receiver,
      InputStream input, OutputStream output, ErrorListener errorListener) {
    RemoteWireAdapter remoteWireAdapter =
        new RemoteWireAdapter(input, output, errorListener);
    DeviceMessageAdapter deviceMessageAdapter =
        new DeviceMessageAdapter(receiver, remoteWireAdapter);
    remoteWireAdapter.setMessageListener(deviceMessageAdapter);
    remoteWireAdapter.startReceivingThread();
    return deviceMessageAdapter;
  }
  /**
   * Initializes the server side of the remote protocol.
   * <p>
   * Doesn't start the receiving thread, for faster and synchronous testing.
   *
   * @param receiver the receiver of the events on the server side
   * @param input    the stream where the events are received
   * @param output   the stream where events can be sent
   * @hide
   */
  public static ServerAdapter getServerAdapterNoThread(
      RequestReceiver receiver, InputStream input, OutputStream output) {
    RemoteWireAdapter remoteWireAdapter = new RemoteWireAdapter(input, output, null);
    ServerMessageAdapter serverMessageAdapter = new ServerMessageAdapter(
        receiver, remoteWireAdapter);
    remoteWireAdapter.setMessageListener(serverMessageAdapter);
    return serverMessageAdapter;
  }

  /**
   * Initializes the device side of the remote protocol.
   * <p>
   * Doesn't start the receiving thread, for faster and synchronous testing.
   *
   * @param receiver the receiver of the events on the server side
   * @param input    the stream where the events are received
   * @param output   the stream where events can be sent
   * @hide
   */
  public static DeviceAdapter getDeviceAdapterNoThread(
      MessageReceiver receiver, InputStream input, OutputStream output) {
    RemoteWireAdapter remoteWireAdapter = new RemoteWireAdapter(input, output, null);
    DeviceMessageAdapter deviceMessageAdapter = new DeviceMessageAdapter(
        receiver, remoteWireAdapter);
    remoteWireAdapter.setMessageListener(deviceMessageAdapter);
    return deviceMessageAdapter;
  }
}
