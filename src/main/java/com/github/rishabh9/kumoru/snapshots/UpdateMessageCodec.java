package com.github.rishabh9.kumoru.snapshots;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class UpdateMessageCodec implements MessageCodec<UpdateMessage, UpdateMessage> {
  @Override
  public void encodeToWire(final Buffer buffer, final UpdateMessage updateMessage) {
    try {
      final byte[] bytes = encodeToBytes(updateMessage);
      buffer.appendInt(bytes.length);
      buffer.appendBytes(bytes);
    } catch (IOException e) {
      log.error("Unable to encode message", e);
    }
  }

  @Override
  public UpdateMessage decodeFromWire(final int pos, final Buffer buffer) {
    try {
      final int length = buffer.getInt(pos);
      final int _pos = pos + 4;
      return decodeFromBytes(buffer.getBytes(_pos, _pos + length));
    } catch (IOException | ClassNotFoundException e) {
      log.error("Unable to decode message", e);
    }
    return null;
  }

  @Override
  public UpdateMessage transform(final UpdateMessage updateMessage) {
    return updateMessage;
  }

  @Override
  public String name() {
    return "null";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }

  private byte[] encodeToBytes(final UpdateMessage updateMessage) throws IOException {
    final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    final ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
    objStream.writeObject(updateMessage);
    return byteStream.toByteArray();
  }

  private UpdateMessage decodeFromBytes(final byte[] bytes)
      throws IOException, ClassNotFoundException {
    final ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
    final ObjectInputStream objStream = new ObjectInputStream(byteStream);

    return (UpdateMessage) objStream.readObject();
  }
}
