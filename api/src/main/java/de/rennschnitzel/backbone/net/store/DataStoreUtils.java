package de.rennschnitzel.backbone.net.store;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Function;
import com.google.protobuf.ByteString;

public class DataStoreUtils {

  private DataStoreUtils() {
    // TODO Auto-generated constructor stub
  }

  public static final Function<List<ByteString>, Optional<ByteString>> TRANSFORM_POP_TO_SINGLE =
      new Function<List<ByteString>, Optional<ByteString>>() {
        @Override
        public Optional<ByteString> apply(List<ByteString> result) {
          if (result == null || result.isEmpty()) {
            return Optional.empty();
          }
          return Optional.of(result.get(0));
        }
      };

}
