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

package com.google.anymote.server;

import com.google.anymote.Key.Action;
import com.google.anymote.Key.Code;
import com.google.anymote.common.ConnectInfo;

/**
 * An interface that gets notified when a message is received on the server.
 */
public interface RequestReceiver {

  /**
   * Called when a key event is received on the server.
   *
   * @param keycode the received linux keycode
   * @param action the action on the key (up/down)
   */
  public void onKeyEvent(Code keycode, Action action);

  /**
   * Called when a mouse movement is received.
   *
   * @param xDelta relative movement of the mouse along the x-axis
   * @param yDelta relative movement of the mouse along the y-axis
   */
  public void onMouseEvent(int xDelta, int yDelta);

  /**
   * Called when a mouse wheel event is received.
   *
   * @param xScroll the scrolling along the x-axis
   * @param yScroll the scrolling along the x-axis
   */
  public void onMouseWheel(int xScroll, int yScroll);

  /**
   * A general way to send data to the server.
   * <p>
   * The interpretation of this is up to the server.
   *
   * @param data the data represented as a string
   */
  public void onData(String type, String data);

  /**
   * A message sent upon the connection of a device.
   *
   * @param connectInfo the connection information sent by a remote device
   */
  public void onConnect(ConnectInfo connectInfo);

  /**
   * Called when viewing the URI is requested.
   *
   * @param uri URI to be opened
   * @return {@code true} if succeeded
   */
  public boolean onFling(String uri);
}
