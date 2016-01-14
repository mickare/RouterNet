package de.rennschnitzel.backbone.router.gson;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.UUID;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JsonTools {
	
	// Using Android's base64 libraries. This can be replaced with any base64 library.
	public static class ByteArrayToBase64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
		
		private final Base64.Decoder decoder = Base64.getDecoder();
		private final Base64.Encoder encoder = Base64.getEncoder();
		
		public byte[] deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context )
				throws JsonParseException {
			return decoder.decode( json.getAsString() );
		}
		
		public JsonElement serialize( byte[] src, Type typeOfSrc, JsonSerializationContext context ) {
			return new JsonPrimitive( encoder.encodeToString( src ) );
		}
	}
	
	public static class UUIDTypeAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {
				
		public UUID deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context )
				throws JsonParseException {
			return UUID.fromString(  json.getAsString() );
		}
		
		public JsonElement serialize( UUID src, Type typeOfSrc, JsonSerializationContext context ) {
			return new JsonPrimitive( src.toString() );
		}
	}
	
}
