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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.anymote.Messages.RemoteMessage;

/**
 * Transport layer implementation of the Ip Remote Protocol
 */
public final class RemoteWireAdapter implements WireAdapter {

  /**
   * Interface for a request message receiver.
   */
  public interface IMessageListener {
    /**
     * Called when a request message is received.
     *
     *  @param message a received message
     */
    public void onMessage(RemoteMessage message);
  }

  private final InputStream mInputStream;

  private final OutputStream mOutputStream;

  private Thread mReceiverThread;

  private final ErrorListener mErrorListener;

  private IMessageListener mListener;

  public RemoteWireAdapter(InputStream inputStream, OutputStream outputStream,
      ErrorListener errorListener) {
    mInputStream = inputStream;
    mOutputStream = outputStream;
    mErrorListener = errorListener;
  }

  /**
   * Notifies the error listener that an I/O error occurred within the protocol.
   *
   * @param error       error message
   * @param throwable   exception caught
   */
  private void onIoError(String error, Throwable throwable) {
    if (mErrorListener != null) {
      mErrorListener.onIoError(error, throwable);
    }
  }

  public void setMessageListener(IMessageListener listener) {
    mListener = listener;
  }

  public boolean getNextRemoteMessage() {
      RemoteMessage mess;
      try {
        synchronized (mInputStream) {
          mess = RemoteMessage.parseDelimitedFrom(mInputStream);
        }
      } catch (IOException e) {
        onIoError("Cannot read message", e);
        return false;
      }
      interpretMessage(mess);
      return true;
  }

  /**
   * Interpret a received message.
   *
   * @param message the received message
   */
  private void interpretMessage(RemoteMessage message) {
    if (mListener != null) {
      mListener.onMessage(message);
    }
  }

  public void sendRemoteMessage(RemoteMessage remoteMessage) {
    try {
      synchronized (mOutputStream) {
        remoteMessage.writeDelimitedTo(mOutputStream);
      }
    } catch (IOException e) {
      onIoError("Cannot send message", e);
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Doesn't close the streams.
   */
  public void stop() {
    stopReceiverThread();
  }

  /**
   * Starts the receiving thread.
   */
  void startReceivingThread() {
    if (mReceiverThread != null) {
      stopReceiverThread();
    }
    mReceiverThread = new Thread(new Runnable() {
      public void run() {
        boolean available = true;
        while (available) {
          available = getNextRemoteMessage();
        }
        stop();
      }
    });
    mReceiverThread.start();
  }

  /**
   * Stops the receiving thread.
   */
  private void stopReceiverThread() {
    if (mReceiverThread != null) {
      try {
        mReceiverThread.join(250);
      } catch (InterruptedException e) {
        // Nothing: expected.
      }
    }
    mReceiverThread = null;
  }
}
