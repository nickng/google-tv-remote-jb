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

package com.google.anymote.device;

import com.google.anymote.Key.Action;
import com.google.anymote.Key.Code;
import com.google.anymote.common.ConnectInfo;

/**
 * Device client that send the messages and manages the connection.
 */
public interface DeviceAdapter {

  /**
   * Sends "ping" message, that should receive ack.
   */
  public void sendPing();

  /**
   * Sends a key event.
   *
   * @param keycode the linux keycode of the event
   * @param action the action for the event
   */
  public void sendKeyEvent(Code keycode, Action action);

  /**
   * Sends a relative mouse movement.
   *
   * @param xDelta movement along the x-axis (horizontal)
   * @param yDelta movement along the y-axis (vertical)
   */
  public void sendMouseMove(int xDelta, int yDelta);

  /**
   * Sends a wheel event.
   *
   * @param xScroll the scroll amount along the x-axis (horizontal)
   * @param yScroll the scroll amount along the y-axis (vertical)
   */
  public void sendMouseWheel(int xScroll, int yScroll);

  /**
   * Generic way to send data to the server.
   *
   * @param type the way the data will be interpreted
   * @param data the data to send
   */
  public void sendData(String type, String data);

  /**
   * Sends a connection message.
   *
   * @param connectInfo information about the device
   */
  public void sendConnect(ConnectInfo connectInfo);

  /**
   * Sends fling event.
   */
  public void sendFling(String uri, int sequenceNumber);

  /**
   * Closes the connection with the server.
   */
  public void stop();
}
