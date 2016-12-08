package de.mickare.routernet.core.packet;

import io.netty.util.concurrent.Future;

public interface PacketWriter<F extends Future<?>> extends PacketWriterDefault<F>, PacketWriterPromise<F> {
	
}
