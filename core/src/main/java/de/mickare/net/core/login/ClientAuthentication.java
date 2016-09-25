package de.mickare.net.core.login;

import com.google.protobuf.ByteString;

public interface ClientAuthentication {
	public ByteString calculateResponse( ByteString challenge );
}