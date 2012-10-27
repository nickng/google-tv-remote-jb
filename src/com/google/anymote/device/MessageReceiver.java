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

import com.google.anymote.Messages.FlingResult;

/**
 * An interface to listen to the received messages on the remote.
 */
public interface MessageReceiver {
  /**
   * Called when a acknowledgment is received.
   */
  public void onAck();

  /**
   * Called when a response message is received.
   *
   * @param type the type of data sent in a reply
   * @param data the data sent as a reply
   */
  public void onData(String type, String data);

  /**
   * Called when a fling result is received.
   *
   * @param flingResult the received fling result
   * @param sequenceNumber sequence number or {@code null} if not present
   */
  public void onFlingResult(FlingResult flingResult, Integer sequenceNumber);
}
