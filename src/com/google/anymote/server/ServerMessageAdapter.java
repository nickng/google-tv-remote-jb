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
import com.google.anymote.Messages.Connect;
import com.google.anymote.Messages.Data;
import com.google.anymote.Messages.Fling;
import com.google.anymote.Messages.FlingResult;
import com.google.anymote.Messages.FlingResult.Result;
import com.google.anymote.Messages.KeyEvent;
import com.google.anymote.Messages.MouseEvent;
import com.google.anymote.Messages.MouseWheel;
import com.google.anymote.Messages.RemoteMessage;
import com.google.anymote.Messages.RequestMessage;
import com.google.anymote.Messages.ResponseMessage;
import com.google.anymote.common.ConnectInfo;
import com.google.anymote.common.RemoteWireAdapter.IMessageListener;
import com.google.anymote.common.WireAdapter;

/**
 * Manages messages on the server side.
 */
public final class ServerMessageAdapter
    implements ServerAdapter, IMessageListener {

  /**
   * The listener of the received events.
   */
  private final RequestReceiver mCommandReceiver;

  /**
   * The remote message sender.
   */
  private final WireAdapter mWireAdapter;

  /**
   * A class that describe a policy to deal with acknowledgments.
   */
  public static class AckPolicy {

    /**
     * Called when a sequence number is received.
     */
    public void onReceivedSequence(int sequenceNumber) {
      // Default implementation does nothing.
    }
  }

  public ServerMessageAdapter(
      RequestReceiver receiver, WireAdapter wireAdapter) {
    mCommandReceiver = receiver;
    mWireAdapter = wireAdapter;
  }

  public WireAdapter getWireAdapter() {
    return mWireAdapter;
  }

  public void onMessage(RemoteMessage message) {
    if (message.hasRequestMessage()) {
      interpretRequest(message.getRequestMessage(),
          message.hasSequenceNumber() ? message.getSequenceNumber() : null);
    }
  }

  /**
   * Interprets a request message.
   *
   * @param message the request message
   */
  private void interpretRequest(RequestMessage message, Integer sequenceNumber) {
    ResponseMessage.Builder builder = ResponseMessage.newBuilder();
    boolean reply = sequenceNumber != null;

    if (message.hasKeyEventMessage()) {
      reply = false;
      onKeyEvent(message.getKeyEventMessage());
    }
    if (message.hasMouseEventMessage()) {
      reply = false;
      onMouseEvent(message.getMouseEventMessage());
    }
    if (message.hasMouseWheelMessage()) {
      reply = false;
      onMouseWheel(message.getMouseWheelMessage());
    }
    if (message.hasDataMessage()) {
      reply = false;
      onData(message.getDataMessage());
    }
    if (message.hasConnectMessage()) {
      reply = false;
      onConnect(message.getConnectMessage());
    }
    if (message.hasFlingMessage()) {
      reply = true;
      builder.setFlingResultMessage(
          onFling(message.getFlingMessage(), sequenceNumber));
    }

    if (reply) {
      sendResponse(builder, sequenceNumber);
    }
  }

  private void onKeyEvent(KeyEvent message) {
    Code keycode = message.getKeycode();
    Action action = message.getAction();
    mCommandReceiver.onKeyEvent(keycode, action);
  }

  private void onMouseEvent(MouseEvent message) {
    int xDelta = message.getXDelta();
    int yDelta = message.getYDelta();
    mCommandReceiver.onMouseEvent(xDelta, yDelta);
  }

  private void onMouseWheel(MouseWheel message) {
    int xScrollAmt = message.getXScroll();
    int yScrollAmt = message.getYScroll();
    mCommandReceiver.onMouseWheel(xScrollAmt, yScrollAmt);
  }

  private void onData(Data message) {
    mCommandReceiver.onData(message.getType(), message.getData());
  }

  private FlingResult.Builder onFling(Fling message, Integer sequenceNumber) {
    boolean success = mCommandReceiver.onFling(message.getUri());
    return FlingResult.newBuilder().setResult(
        success ? Result.SUCCESS : Result.FAILURE);
  }

  private void onConnect(Connect message) {
    mCommandReceiver.onConnect(ConnectInfo.parseFromProto(message));
  }

  public void sendData(String type, String data) {
    Data.Builder replyMessageBuilder = Data.newBuilder();
    replyMessageBuilder.setType(type);
    replyMessageBuilder.setData(data);
    ResponseMessage.Builder responseBuilder = ResponseMessage.newBuilder();
    responseBuilder.setDataMessage(replyMessageBuilder);
    sendResponse(responseBuilder, null);
  }

  /**
   * Sends a response message.
   *
   * @param responseMessage the message to send
   * @param sequenceNumber  optional sequence number associated with response
   */
  private void sendResponse(
      ResponseMessage.Builder responseMessage, Integer sequenceNumber) {
    RemoteMessage.Builder message =
        RemoteMessage.newBuilder().setResponseMessage(responseMessage);
    if (sequenceNumber != null) {
      message.setSequenceNumber(sequenceNumber);
    }
    mWireAdapter.sendRemoteMessage(message.build());
  }

}
