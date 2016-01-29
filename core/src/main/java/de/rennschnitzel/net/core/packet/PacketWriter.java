package de.rennschnitzel.net.core.packet;

import io.netty.util.concurrent.Future;


public interface PacketWriter<F extends Future<?>> extends PacketWriterDefault<F>, PacketWriterPromise<F> {

}
