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

import com.google.anymote.Messages.RemoteMessage;

/**
 * A transport layer interface.
 */
public interface WireAdapter {

  /**
   * Sends a message.
   *
   * @param remoteMessage the message to send.
   */
  public void sendRemoteMessage(RemoteMessage remoteMessage);

  /**
   * Gets the next message and calls the listener.
   * <p>
   * Blocks while waiting for a message.
   *
   * @return {@code true} if a message has been read successfully.
   */
  public boolean getNextRemoteMessage();

  /**
   * Stops receiving messages.
   */
  public void stop();

}
