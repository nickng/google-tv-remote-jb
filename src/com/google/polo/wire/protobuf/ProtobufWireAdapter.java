/*
 * Copyright (C) 2009 Google Inc.  All rights reserved.
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

package com.google.polo.wire.protobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.polo.exception.BadSecretException;
import com.google.polo.exception.NoConfigurationException;
import com.google.polo.exception.PoloException;
import com.google.polo.exception.ProtocolErrorException;
import com.google.polo.pairing.PairingContext;
import com.google.polo.pairing.PoloUtil;
import com.google.polo.pairing.message.ConfigurationAckMessage;
import com.google.polo.pairing.message.ConfigurationMessage;
import com.google.polo.pairing.message.EncodingOption;
import com.google.polo.pairing.message.OptionsMessage;
import com.google.polo.pairing.message.PairingRequestAckMessage;
import com.google.polo.pairing.message.PairingRequestMessage;
import com.google.polo.pairing.message.PoloMessage;
import com.google.polo.pairing.message.SecretAckMessage;
import com.google.polo.pairing.message.SecretMessage;
import com.google.polo.wire.PoloWireInterface;
import com.google.polo.wire.protobuf.PoloProto.Options.Encoding.EncodingType;
import com.google.polo.wire.protobuf.PoloProto.Options.RoleType;
import com.google.polo.wire.protobuf.PoloProto.OuterMessage;
import com.google.polo.wire.protobuf.PoloProto.OuterMessage.MessageType;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessageLite;

/**
 * Implementation of {@link PoloWireInterface} that uses Protocol Buffers for
 * the data representation.
 * <p>
 * The primary work of this class is to translate Protocol Buffer messages
 * instances (derived from {@link GeneratedMessage} to an internal message
 * instance (derived from {@link PoloMessage}, and vice versa.
 * <p>
 * The reason we are going through all this trouble, and not using protocol
 * buffer objects directly, is that we'd like to limit the scope of protocol
 * buffers to the wire protocol only. Some applications may prefer to use a
 * different wire format, where the requirement of adding the protobuf library
 * could be an impediment.
 */
public class ProtobufWireAdapter implements PoloWireInterface {

	/**
	 * The output coming from the peer.
	 */
	private final InputStream mInputStream;

	/**
	 * The input going to the peer.
	 */
	private final OutputStream mOutputStream;

	/**
	 * Constructor.
	 * 
	 * @param input
	 *            the {@link InputStream} from the peer
	 * @param output
	 *            the {@link OutputStream} to the peer
	 */
	public ProtobufWireAdapter(InputStream input, OutputStream output) {
		mInputStream = input;
		mOutputStream = output;
	}

	/**
	 * Generates a new instance from a {@link PairingContext}.
	 * 
	 * @param context
	 *            the {@link PairingContext}
	 * @return the new instance
	 */
	public static ProtobufWireAdapter fromContext(PairingContext context) {
		return new ProtobufWireAdapter(context.getPeerInputStream(),
				context.getPeerOutputStream());
	}

	/**
	 * Returns the next message sent over the wire, blocking as necessary.
	 */
	public PoloMessage getNextMessage() throws IOException, PoloException {
		return protoToPoloMessage(readNextInnerMessage());
	}

	/**
	 * Returns the next message read over the wire, requiring it to be a certain
	 * type.
	 * 
	 * @param type
	 *            the required message type
	 * @throws IOException
	 *             on error during read
	 * @throws PoloException
	 *             if the wrong message type was read, or on protocol error
	 */
	public PoloMessage getNextMessage(PoloMessage.PoloMessageType type)
			throws IOException, PoloException {
		PoloMessage message = getNextMessage();
		if (message.getType() != type) {
			throw new PoloException("Wrong message type (wanted " + type
					+ ", got " + message.getType() + ")");
		}
		return message;
	}

	/**
	 * Returns the next message seen on the input stream.
	 * 
	 * @return the next OuterMessage read from the wire
	 * @throws IOException
	 *             on error during read
	 */
	private OuterMessage readNextOuterMessage() throws IOException,
			PoloException {
		// Read the preamble (length of payload)
		byte[] preambleBuffer = readBytesBlocking(4);
		int messageLen = (int) PoloUtil.intBigEndianBytesToLong(preambleBuffer);

		// Read the payload (serialized PoloMessage)
		byte[] messageBuffer = readBytesBlocking(messageLen);

		// Decode and return the payload
		OuterMessage message = OuterMessage.parseFrom(messageBuffer);

		OuterMessage.Status status = message.getStatus();
		if (status != OuterMessage.Status.STATUS_OK) {
			throw new ProtocolErrorException();
		}

		return message;
	}

	/**
	 * Reads the next inner message from the wire, decoding and handling the
	 * outer message in the process.
	 * 
	 * @return a protocol buffer message
	 * @throws IOException
	 *             on error during read
	 * @throws PoloException
	 *             on protocol error
	 */
	private GeneratedMessageLite readNextInnerMessage() throws IOException,
			PoloException {
		OuterMessage message = readNextOuterMessage();
		MessageType type = message.getType();

		ByteString payload = message.getPayload();

		if (type == MessageType.MESSAGE_TYPE_OPTIONS) {
			return PoloProto.Options.parseFrom(payload);
		} else if (type == MessageType.MESSAGE_TYPE_PAIRING_REQUEST) {
			return PoloProto.PairingRequest.parseFrom(payload);
		} else if (type == MessageType.MESSAGE_TYPE_PAIRING_REQUEST_ACK) {
			return PoloProto.PairingRequestAck.parseFrom(payload);
		} else if (type == MessageType.MESSAGE_TYPE_CONFIGURATION) {
			return PoloProto.Configuration.parseFrom(payload);
		} else if (type == MessageType.MESSAGE_TYPE_CONFIGURATION_ACK) {
			return PoloProto.ConfigurationAck.parseFrom(payload);
		} else if (type == MessageType.MESSAGE_TYPE_SECRET) {
			return PoloProto.Secret.parseFrom(payload);
		} else if (type == MessageType.MESSAGE_TYPE_SECRET_ACK) {
			return PoloProto.SecretAck.parseFrom(payload);
		}

		throw new IOException("Could not unparse message");
	}

	/**
	 * Convenience method to read a fixed number of bytes from the client
	 * InputStream, blocking if necessary.
	 * 
	 * @param numBytes
	 *            the number of bytes to read
	 * @return the bytes read
	 * @throws IOException
	 *             on error during read
	 */
	private byte[] readBytesBlocking(int numBytes) throws IOException {
		byte[] buf = new byte[numBytes];
		int bytesRead = 0;

		// For an SSLSocket, read() can frequently return zero bytes,
		// or fewer bytes than desired, due to SSL unwrapping and other
		// non-application-data events.
		while (bytesRead < numBytes) {
			int inc = mInputStream.read(buf, bytesRead, numBytes - bytesRead);
			if (inc < 0) {
				throw new IOException("Stream closed while reading.");
			}
			bytesRead += inc;
		}
		return buf;
	}

	/**
	 * Wraps an outer message in an inner message.
	 * 
	 * @param message
	 *            the {@link GeneratedMessageLite} to wrap
	 * @throws PoloException
	 *             if the message was not well formed
	 */
	private OuterMessage wrapInnerMessage(GeneratedMessageLite message)
			throws PoloException {
		MessageType type;
		if (message instanceof PoloProto.Options) {
			type = MessageType.MESSAGE_TYPE_OPTIONS;
		} else if (message instanceof PoloProto.PairingRequest) {
			type = MessageType.MESSAGE_TYPE_PAIRING_REQUEST;
		} else if (message instanceof PoloProto.PairingRequestAck) {
			type = MessageType.MESSAGE_TYPE_PAIRING_REQUEST_ACK;
		} else if (message instanceof PoloProto.Configuration) {
			type = MessageType.MESSAGE_TYPE_CONFIGURATION;
		} else if (message instanceof PoloProto.ConfigurationAck) {
			type = MessageType.MESSAGE_TYPE_CONFIGURATION_ACK;
		} else if (message instanceof PoloProto.Secret) {
			type = MessageType.MESSAGE_TYPE_SECRET;
		} else if (message instanceof PoloProto.SecretAck) {
			type = MessageType.MESSAGE_TYPE_SECRET_ACK;
		} else {
			throw new PoloException("Bad inner message type.");
		}

		// compose outer message
		OuterMessage.Builder builder = OuterMessage.newBuilder();
		builder.setStatus(OuterMessage.Status.STATUS_OK);
		builder.setProtocolVersion(1);
		builder.setType(type);
		builder.setPayload(message.toByteString());

		return builder.build();
	}

	/**
	 * Writes an {@link OuterMessage} to the wire.
	 * 
	 * @param message
	 *            the message
	 * @throws IOException
	 *             on error during write
	 */
	private void writeMessage(OuterMessage message) throws IOException {
		byte[] messageBytes = message.toByteArray();
		int messageLength = messageBytes.length;

		mOutputStream.write(PoloUtil.intToBigEndianIntBytes(messageLength));
		mOutputStream.write(messageBytes);
	}

	/**
	 * Writes a new message to the wire.
	 */
	public void sendMessage(PoloMessage message) throws IOException,
			PoloException {
		GeneratedMessageLite pb = poloMessageToProto(message);
		OuterMessage outerMessage = wrapInnerMessage(pb);
		writeMessage(outerMessage);
	}

	/**
	 * Sends a new error message to the wire.
	 */
	public void sendErrorMessage(Exception e) throws IOException {
		OuterMessage.Builder builder = OuterMessage.newBuilder();
		builder.setProtocolVersion(1);

		if (e instanceof NoConfigurationException) {
			builder.setStatus(OuterMessage.Status.STATUS_BAD_CONFIGURATION);
		} else if (e instanceof BadSecretException) {
			builder.setStatus(OuterMessage.Status.STATUS_BAD_SECRET);
		} else {
			builder.setStatus(OuterMessage.Status.STATUS_ERROR);
		}

		OuterMessage message = builder.build();
		writeMessage(message);
	}

	/**
	 * Converts an internal message to the corresponding protocol buffer
	 * message.
	 * 
	 * @param poloMessage
	 *            the internal message
	 * @return a new {@link GeneratedMessage} instance
	 */
	private GeneratedMessageLite poloMessageToProto(PoloMessage poloMessage) {
		if (poloMessage instanceof PairingRequestMessage) {
			return toProto((PairingRequestMessage) poloMessage);
		} else if (poloMessage instanceof PairingRequestAckMessage) {
			return toProto((PairingRequestAckMessage) poloMessage);
		} else if (poloMessage instanceof OptionsMessage) {
			return toProto((OptionsMessage) poloMessage);
		} else if (poloMessage instanceof ConfigurationMessage) {
			return toProto((ConfigurationMessage) poloMessage);
		} else if (poloMessage instanceof ConfigurationAckMessage) {
			return toProto((ConfigurationAckMessage) poloMessage);
		} else if (poloMessage instanceof SecretMessage) {
			return toProto((SecretMessage) poloMessage);
		} else if (poloMessage instanceof SecretAckMessage) {
			return toProto((SecretAckMessage) poloMessage);
		}
		return null;
	}

	/**
	 * Converts a {@link PairingRequestMessage} to a
	 * {@link PoloProto.PairingRequest}.
	 */
	private PoloProto.PairingRequest toProto(PairingRequestMessage poloMessage) {
		PoloProto.PairingRequest.Builder builder = PoloProto.PairingRequest
				.newBuilder();
		builder.setServiceName(poloMessage.getServiceName());
		if (poloMessage.hasClientName()) {
			builder.setClientName(poloMessage.getClientName());
		}
		return builder.build();
	}

	/**
	 * Converts a {@link PairingRequestAckMessage} to a
	 * {@link PoloProto.PairingRequestAck}.
	 */
	private PoloProto.PairingRequestAck toProto(
			PairingRequestAckMessage poloMessage) {
		PoloProto.PairingRequestAck.Builder builder = PoloProto.PairingRequestAck
				.newBuilder();
		if (poloMessage.hasServerName()) {
			builder.setServerName(poloMessage.getServerName());
		}
		return builder.build();
	}

	/**
	 * Converts a {@link OptionsMessage} to a {@link PoloProto.Options}.
	 */
	@SuppressWarnings("incomplete-switch")
	private PoloProto.Options toProto(OptionsMessage poloMessage) {
		PoloProto.Options.Builder builder = PoloProto.Options.newBuilder();

		switch (poloMessage.getProtocolRolePreference()) {
		case DISPLAY_DEVICE:
			builder.setPreferredRole(PoloProto.Options.RoleType.ROLE_TYPE_INPUT);
			break;
		case INPUT_DEVICE:
			builder.setPreferredRole(PoloProto.Options.RoleType.ROLE_TYPE_OUTPUT);
			break;
		}

		for (EncodingOption enc : poloMessage.getOutputEncodingSet()) {
			builder.addOutputEncodings(toProto(enc));
		}

		for (EncodingOption enc : poloMessage.getInputEncodingSet()) {
			builder.addInputEncodings(toProto(enc));
		}

		return builder.build();
	}

	/**
	 * Converts a {@link ConfigurationMessage} to a
	 * {@link PoloProto.Configuration}.
	 */
	private PoloProto.Configuration toProto(ConfigurationMessage poloMessage) {
		PoloProto.Configuration.Builder builder = PoloProto.Configuration
				.newBuilder();
		builder.setEncoding(toProto(poloMessage.getEncoding()));
		builder.setClientRole(toProto(poloMessage.getClientRole()));
		return builder.build();
	}

	/**
	 * Converts a {@link EncodingOption} to a {@link PoloProto.Options.Encoding}
	 * .
	 */
	private PoloProto.Options.Encoding toProto(EncodingOption enc) {
		PoloProto.Options.Encoding.Builder builder = PoloProto.Options.Encoding
				.newBuilder();

		PoloProto.Options.Encoding.EncodingType type;

		switch (enc.getType()) {
		case ENCODING_ALPHANUMERIC:
			type = EncodingType.ENCODING_TYPE_ALPHANUMERIC;
			break;
		case ENCODING_HEXADECIMAL:
			type = EncodingType.ENCODING_TYPE_HEXADECIMAL;
			break;
		case ENCODING_NUMERIC:
			type = EncodingType.ENCODING_TYPE_NUMERIC;
			break;
		case ENCODING_QRCODE:
			type = EncodingType.ENCODING_TYPE_QRCODE;
			break;
		default:
			type = EncodingType.ENCODING_TYPE_UNKNOWN;
			break;
		}

		builder.setType(type);
		builder.setSymbolLength(enc.getSymbolLength());
		return builder.build();
	}

	/**
	 * Converts a {@link OptionsMessage.ProtocolRole} to a
	 * {@link PoloProto.Options.RoleType}.
	 */
	private PoloProto.Options.RoleType toProto(OptionsMessage.ProtocolRole role) {
		switch (role) {
		case DISPLAY_DEVICE:
			return RoleType.ROLE_TYPE_OUTPUT;
		case INPUT_DEVICE:
			return RoleType.ROLE_TYPE_INPUT;
		default:
			return RoleType.ROLE_TYPE_UNKNOWN;
		}
	}

	/**
	 * Converts a {@link ConfigurationAckMessage} to a
	 * {@link PoloProto.ConfigurationAck}.
	 */
	private PoloProto.ConfigurationAck toProto(
			ConfigurationAckMessage poloMessage) {
		PoloProto.ConfigurationAck.Builder builder = PoloProto.ConfigurationAck
				.newBuilder();
		return builder.build();
	}

	/**
	 * Converts a {@link SecretMessage} to a {@link PoloProto.Secret}.
	 */
	private PoloProto.Secret toProto(SecretMessage poloMessage) {
		PoloProto.Secret.Builder builder = PoloProto.Secret.newBuilder();
		builder.setSecret(ByteString.copyFrom(poloMessage.getSecret()));
		return builder.build();
	}

	/**
	 * Converts a {@link SecretAckMessage} to a {@link PoloProto.SecretAck}.
	 */
	private PoloProto.SecretAck toProto(SecretAckMessage poloMessage) {
		PoloProto.SecretAck.Builder builder = PoloProto.SecretAck.newBuilder();
		builder.setSecret(ByteString.copyFrom(poloMessage.getSecret()));
		return builder.build();
	}

	//
	// polo -> protocol buffer routines
	//

	/**
	 * Converts a protocol buffer message to the corresponding internal message.
	 * 
	 * @param generatedMessageLite
	 *            the protobuf message to convert
	 * @return the new {@link PoloMessage}
	 */
	private PoloMessage protoToPoloMessage(
			GeneratedMessageLite generatedMessageLite) {
		if (generatedMessageLite instanceof PoloProto.PairingRequest) {
			return fromProto((PoloProto.PairingRequest) generatedMessageLite);
		} else if (generatedMessageLite instanceof PoloProto.PairingRequestAck) {
			return fromProto((PoloProto.PairingRequestAck) generatedMessageLite);
		} else if (generatedMessageLite instanceof PoloProto.Options) {
			return fromProto((PoloProto.Options) generatedMessageLite);
		} else if (generatedMessageLite instanceof PoloProto.Configuration) {
			return fromProto((PoloProto.Configuration) generatedMessageLite);
		} else if (generatedMessageLite instanceof PoloProto.ConfigurationAck) {
			return fromProto((PoloProto.ConfigurationAck) generatedMessageLite);
		} else if (generatedMessageLite instanceof PoloProto.Secret) {
			return fromProto((PoloProto.Secret) generatedMessageLite);
		} else if (generatedMessageLite instanceof PoloProto.SecretAck) {
			return fromProto((PoloProto.SecretAck) generatedMessageLite);
		}
		return null;
	}

	/**
	 * Converts a {@link PoloProto.PairingRequest} to a
	 * {@link PairingRequestMessage}.
	 */
	private PairingRequestMessage fromProto(
			PoloProto.PairingRequest protoMessage) {
		return new PairingRequestMessage(protoMessage.getServiceName(),
				protoMessage.hasClientName() ? protoMessage.getClientName()
						: null);
	}

	/**
	 * Converts a {@link PoloProto.PairingRequestAck} to a
	 * {@link PairingRequestAckMessage}.
	 */
	private PairingRequestAckMessage fromProto(
			PoloProto.PairingRequestAck protoMessage) {
		return new PairingRequestAckMessage(
				protoMessage.hasServerName() ? protoMessage.getServerName()
						: null);
	}

	/**
	 * Converts a {@link PoloProto.Options} to a {@link OptionsMessage}.
	 */
	@SuppressWarnings("incomplete-switch")
	private OptionsMessage fromProto(PoloProto.Options protoMessage) {
		OptionsMessage optionsMessage = new OptionsMessage();

		System.out.println("Read options: " + protoMessage);

		switch (protoMessage.getPreferredRole()) {
		case ROLE_TYPE_INPUT:
			optionsMessage
					.setProtocolRolePreference(OptionsMessage.ProtocolRole.INPUT_DEVICE);
			break;
		case ROLE_TYPE_OUTPUT:
			optionsMessage
					.setProtocolRolePreference(OptionsMessage.ProtocolRole.DISPLAY_DEVICE);
			break;
		}

		for (PoloProto.Options.Encoding e : protoMessage
				.getInputEncodingsList()) {
			optionsMessage.addInputEncoding(fromProto(e));
		}

		for (PoloProto.Options.Encoding e : protoMessage
				.getOutputEncodingsList()) {
			optionsMessage.addOutputEncoding(fromProto(e));
		}

		return optionsMessage;
	}

	/**
	 * Converts a {@link PoloProto.Configuration} to a
	 * {@link ConfigurationMessage}.
	 */
	@SuppressWarnings("incomplete-switch")
	private ConfigurationMessage fromProto(PoloProto.Configuration protoMessage) {
		EncodingOption enc = fromProto(protoMessage.getEncoding());
		OptionsMessage.ProtocolRole role = OptionsMessage.ProtocolRole.UNKNOWN;

		switch (protoMessage.getClientRole()) {
		case ROLE_TYPE_INPUT:
			role = OptionsMessage.ProtocolRole.INPUT_DEVICE;
			break;
		case ROLE_TYPE_OUTPUT:
			role = OptionsMessage.ProtocolRole.DISPLAY_DEVICE;
			break;
		}

		return new ConfigurationMessage(enc, role);
	}

	/**
	 * Converts a {@link PoloProto.ConfigurationAck} to a
	 * {@link ConfigurationAckMessage}.
	 */
	private ConfigurationAckMessage fromProto(
			PoloProto.ConfigurationAck protoMessage) {
		return new ConfigurationAckMessage();
	}

	/**
	 * Converts a {@link PoloProto.Secret} to a {@link SecretMessage}.
	 */
	private SecretMessage fromProto(PoloProto.Secret protoMessage) {
		return new SecretMessage(protoMessage.getSecret().toByteArray());
	}

	/**
	 * Converts a {@link PoloProto.SecretAck} to a {@link SecretAckMessage}.
	 */
	private SecretAckMessage fromProto(PoloProto.SecretAck protoMessage) {
		return new SecretAckMessage(protoMessage.getSecret().toByteArray());
	}

	/**
	 * Converts a {@link PoloProto.Options.Encoding} to a {@link EncodingOption}
	 * .
	 */
	private EncodingOption fromProto(PoloProto.Options.Encoding enc) {
		EncodingOption.EncodingType type;

		switch (enc.getType()) {
		case ENCODING_TYPE_ALPHANUMERIC:
			type = EncodingOption.EncodingType.ENCODING_ALPHANUMERIC;
			break;
		case ENCODING_TYPE_HEXADECIMAL:
			type = EncodingOption.EncodingType.ENCODING_HEXADECIMAL;
			break;
		case ENCODING_TYPE_NUMERIC:
			type = EncodingOption.EncodingType.ENCODING_NUMERIC;
			break;
		case ENCODING_TYPE_QRCODE:
			type = EncodingOption.EncodingType.ENCODING_QRCODE;
			break;
		default:
			type = EncodingOption.EncodingType.ENCODING_UNKNOWN;
		}

		return new EncodingOption(type, enc.getSymbolLength());

	}

}
