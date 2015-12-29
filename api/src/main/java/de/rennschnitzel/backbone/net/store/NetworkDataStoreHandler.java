package de.rennschnitzel.backbone.net.store;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import de.rennschnitzel.backbone.net.Connection;
import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreRequestMessage;
import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreRequestType;
import de.rennschnitzel.backbone.net.protocol.DataStoreProtocol.DataStoreResponseMessage;
import de.rennschnitzel.backbone.net.protocol.TransportProtocol.Packet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NetworkDataStoreHandler {

  @NonNull
  private final Logger logger;

  @NonNull
  @Getter
  private final BaseDataStore dataStore; 


  @RequiredArgsConstructor
  private static class Response {

    @Getter
    private final int id;
    @NonNull
    @Getter
    private final EntryKey key;
    @NonNull
    @Getter
    private final DataStoreRequestType type;
    @Getter
    private final boolean success;
    @NonNull
    @Getter
    private final ImmutableList<ByteString> data;

    public Response(int id, EntryKey key, DataStoreRequestType type) {
      this(id, key, type, false, ImmutableList.of());
    }

    public Response(int id, EntryKey key, DataStoreRequestType type, List<byte[]> data) {
      this(id, key, type, true, transform(data));
    }

    public Response(DataStoreRequestMessage req, EntryKey key, List<byte[]> data) {
      this(req.getId(), key, req.getType(), data);
    }

    public Response(DataStoreRequestMessage req, EntryKey key, byte[] data) {
      this(req.getId(), key, req.getType(), true, ImmutableList.of(ByteString.copyFrom(data)));
    }

    private static ImmutableList<ByteString> transform(List<byte[]> list) {
      ImmutableList.Builder<ByteString> b = ImmutableList.builder();
      list.forEach(e -> b.add(ByteString.copyFrom(e)));
      return b.build();
    }



    public DataStoreResponseMessage toProtocol() {
      DataStoreResponseMessage.Builder b = DataStoreResponseMessage.newBuilder();
      b.setId(id);
      b.setKey(key.toProtocol());
      b.setType(type);
      b.setSuccess(success);
      b.addAllData(data);
      return b.build();
    }

  }


  private Response failed(Response res) {
    return new Response(res.getId(), res.getKey(), res.getType());
  }

  private Response failed(DataStoreRequestMessage req) {
    return new Response(req.getId(), EntryKey.from(req.getKey()), req.getType());
  }

  private void send(Connection con, Response response) {

    try {
      con.send(Packet.newBuilder().setDataStoreResponse(response.toProtocol()));
    } catch (Exception ex) {
      try {
        logger.log(Level.SEVERE, "DataStore: Failed to send response\n" + ex.getMessage(), ex);
        con.send(Packet.newBuilder().setDataStoreResponse(failed(response).toProtocol()));
      } catch (Exception ex2) {
        logger.log(Level.SEVERE, "DataStore: Failed to send fail-response\n" + ex2.getMessage(), ex2);
      }
    }
  }

  private List<byte[]> transform(List<ByteString> list) {
    return Lists.transform(list, ByteString::toByteArray);
  }

  public void handle(Connection con, DataStoreRequestMessage req) {
    Preconditions.checkNotNull(con);
    Preconditions.checkNotNull(req);

    EntryKey key = EntryKey.from(req.getKey());

    try {
      switch (req.getType()) {
        case GET:
          send(con, new Response(req, key, dataStore.get(key)));
          break;
        case GET_INDEX:
          int index0 = req.getDataList().get(0).asReadOnlyByteBuffer().getInt();
          send(con, new Response(req, key, dataStore.get(key, index0)));
          break;
        case SET:
          dataStore.set(key, transform(req.getDataList()));
          send(con, new Response(req, key, Collections.emptyList()));
          break;
        case ADD:
          dataStore.add(key, transform(req.getDataList()));
          send(con, new Response(req, key, Collections.emptyList()));
          break;
        case CLEAR:
          dataStore.clear(key);
          send(con, new Response(req, key, Collections.emptyList()));
          break;
        case REMOVE:
          int removed = dataStore.remove(key, transform(req.getDataList()));         
          send(con, new Response(req, key, ByteBuffer.allocate(4).putInt(removed).array()));
          break;
        case REMOVE_INDEX:
          int index1 = req.getDataList().get(0).asReadOnlyByteBuffer().getInt();
          send(con, new Response(req, key, dataStore.remove(key, index1)));
          break;
        case PUSH:
          dataStore.push(key, transform(req.getDataList()));
          send(con, new Response(req, key, Collections.emptyList()));
          break;
        case POP:
          int amount = req.getDataList().get(0).asReadOnlyByteBuffer().getInt();
          send(con, new Response(req, key, dataStore.pop(key, amount)));
          break;
        default:
          throw new UnsupportedOperationException("Undefined data store request \"" + req.getType().toString() + "\"!");
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "DataStore: Failed to handle data request\n" + ex.getMessage(), ex);
      send(con, failed(req));
    }

  }

}
