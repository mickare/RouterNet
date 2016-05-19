package de.rennschnitzel.net.router;

import com.google.common.net.HostAndPort;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Settings {
	
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Address {
		@NonNull
		private String host;
		private int port;
		
		public HostAndPort getHostAndPort() {
			return HostAndPort.fromParts( host, port );
		}
	}
	
	@NonNull
	private Address address = new Address( "localhost", 5070 );
	
}
