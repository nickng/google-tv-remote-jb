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

import java.util.concurrent.atomic.AtomicInteger;

import com.google.anymote.Key.Action;
import com.google.anymote.Key.Code;
import com.google.anymote.Messages.Data;
import com.google.anymote.Messages.Fling;
import com.google.anymote.Messages.FlingResult;
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
 * Manages messages on the device side.
 */
public final class DeviceMessageAdapter
    implements DeviceAdapter, IMessageListener {

  /**
   * A remote message sender.
   */
  private final WireAdapter mWireAdapter;

  /**
   * A message receiver.
   */
  private final MessageReceiver mMessageReceiver;

  /**
   * Ping counter.
   */
  private final AtomicInteger mPingCounter = new AtomicInteger();

  public DeviceMessageAdapter(MessageReceiver receiver, WireAdapter sender) {
    mWireAdapter = sender;
    mMessageReceiver = receiver;
  }

  public void onMessage(RemoteMessage message) {
    if (message.hasResponseMessage()) {
      Integer sequenceNumber =
          message.hasSequenceNumber() ? message.getSequenceNumber() : null;
      interpretResponse(message.getResponseMessage(), sequenceNumber);
    }
  }

  public WireAdapter getWireAdapter() {
    return mWireAdapter;
  }

  /**
   * Interprets a response message.
   *
   * @param message the received message
   */
  private void interpretResponse(
      ResponseMessage message, Integer sequenceNumber) {
    boolean isEmpty = true;
    if (message.hasDataMessage()) {
      isEmpty = false;
      onData(message.getDataMessage());
    }
    if (message.hasFlingResultMessage()) {
      isEmpty = false;
      onFlingResult(message.getFlingResultMessage(), sequenceNumber);
    }
    if (isEmpty && sequenceNumber != null) {
      onAck();
    }
  }

  private void onAck() {
    mMessageReceiver.onAck();
  }

  private void onData(Data connectionReply) {
    String type = connectionReply.getType();
    String data = connectionReply.getData();
    mMessageReceiver.onData(type, data);
  }

  private void onFlingResult(FlingResult flingResult, Integer sequenceNumber) {
    mMessageReceiver.onFlingResult(flingResult, sequenceNumber);
  }

  public void sendPing() {
    RequestMessage.Builder request = getRequestMessageBuilder();
    sendRequest(request, mPingCounter.incrementAndGet());
  }

  public void sendConnect(ConnectInfo connectInfo) {
    RequestMessage.Builder request = getRequestMessageBuilder();
    request.setConnectMessage(connectInfo.getProto());
    sendRequest(request);
  }

  public void sendData(String type, String data) {
    Data.Builder builder = Data.newBuilder();
    builder.setData(data);
    builder.setType(type);
    RequestMessage.Builder request = getRequestMessageBuilder();
    request.setDataMessage(builder);
    sendRequest(request);
  }

  public void sendKeyEvent(Code keycode, Action action) {
    KeyEvent.Builder builder = KeyEvent.newBuilder();
    builder.setKeycode(keycode);
    builder.setAction(action);
    RequestMessage.Builder request = getRequestMessageBuilder();
    request.setKeyEventMessage(builder);
    sendRequest(request);
  }

  public void sendMouseMove(int xDelta, int yDelta) {
    MouseEvent.Builder builder = MouseEvent.newBuilder();
    builder.setXDelta(xDelta);
    builder.setYDelta(yDelta);
    RequestMessage.Builder request = getRequestMessageBuilder();
    request.setMouseEventMessage(builder);
    sendRequest(request);
  }

  public void sendMouseWheel(int xScroll, int yScroll) {
    MouseWheel.Builder builder = MouseWheel.newBuilder();
    builder.setXScroll(xScroll);
    builder.setYScroll(yScroll);
    RequestMessage.Builder request = getRequestMessageBuilder();
    request.setMouseWheelMessage(builder);
    sendRequest(request);
  }

  public void sendFling(String uri, int sequenceNumber) {
    Fling.Builder builder = Fling.newBuilder();
    builder.setUri(uri);
    RequestMessage.Builder request = getRequestMessageBuilder();
    request.setFlingMessage(builder);
    sendRequest(request, sequenceNumber);
  }

  /**
   * Constructs an empty {@code RequestMessage}.
   *
   * @return a request message ready to be send
   */
  private static RequestMessage.Builder getRequestMessageBuilder() {
    return RequestMessage.newBuilder();
  }

  /**
   * Sends a request message.
   *
   * @param requestBuilder the builder that contains the message to send
   * @param sequenceNumber the sequence number associated with message
   */
  private void sendRequest(
      RequestMessage.Builder requestBuilder, Integer sequenceNumber) {
    RemoteMessage.Builder messageBuilder = RemoteMessage.newBuilder()
        .setRequestMessage(requestBuilder);
    if (sequenceNumber != null) {
      messageBuilder.setSequenceNumber(sequenceNumber);
    }
    mWireAdapter.sendRemoteMessage(messageBuilder.build());
  }

  private void sendRequest(RequestMessage.Builder requestBuilder) {
    sendRequest(requestBuilder, null);
  }

  /**
   * Stops messages reception.
   * <p>
   * The streams will not be closed by function.
   */
  public void stop() {
    mWireAdapter.stop();
  }
}
