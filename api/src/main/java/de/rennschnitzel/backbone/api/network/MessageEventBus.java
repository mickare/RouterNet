package de.rennschnitzel.backbone.api.network;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;

import de.rennschnitzel.backbone.api.network.event.ByteMessageInEvent;
import de.rennschnitzel.backbone.api.network.event.ObjectMessageInEvent;
import de.rennschnitzel.backbone.api.network.message.ByteMessage;
import de.rennschnitzel.backbone.api.network.message.ObjectMessage;
import de.rennschnitzel.backbone.api.network.procedure.MultiProcedureCall;
import de.rennschnitzel.backbone.api.network.procedure.Procedure;
import de.rennschnitzel.backbone.api.network.procedure.ProcedureCall;
import de.rennschnitzel.backbone.api.network.procedure.ProcedureCallResult;
import de.rennschnitzel.backbone.api.network.procedure.ProcedureInformation;
import de.rennschnitzel.backbone.api.network.procedure.RegisteredProcedure;
import de.rennschnitzel.backbone.api.network.procedure.SingleProcedureCall;
import de.rennschnitzel.backbone.util.concurrent.CloseableLock;
import de.rennschnitzel.backbone.util.concurrent.CloseableReentrantReadWriteLock;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MessageEventBus {

  private final static long MAX_CALL_TIMEOUT = 10000; // 10 seconds

  private final CloseableReentrantReadWriteLock lock = new CloseableReentrantReadWriteLock();

  // key - byte listener
  private final Multimap<String, Listener<ByteMessageInEvent>> listenersByteMessage = HashMultimap.create();
  // type - object listener
  private final BiMap<Class<?>, RegisteredObjectListeners<?>> listenersObject = HashBiMap.create();

  private final BiMap<ProcedureInformation, RegisteredProcedure<?, ?>> registeredProcedures = HashBiMap.create();

  private final Cache<Integer, ProcedureCall<?, ?>> openCalls = CacheBuilder.newBuilder()//
      .expireAfterWrite(MAX_CALL_TIMEOUT, TimeUnit.MILLISECONDS)//
      .removalListener(new RemovalListener<Integer, ProcedureCall<?, ?>>() {
        @Override
        public void onRemoval(RemovalNotification<Integer, ProcedureCall<?, ?>> notify) {
          notify.getValue().checkTimeout();
        }
      })//
      .build();

  public void registerByteMessageListener(String key, Listener<ByteMessageInEvent> listener) {
    Preconditions.checkArgument(!key.isEmpty());
    Preconditions.checkNotNull(listener);
    try (CloseableLock l = lock.writeLock().open()) {
      this.listenersByteMessage.put(key.toLowerCase(), listener);
    }
  }

  public <T> void registerObjectListener(Class<T> objectClass, Listener<ObjectMessageInEvent<T>> listener) {
    Preconditions.checkNotNull(objectClass);
    Preconditions.checkNotNull(listener);
    try (CloseableLock l = lock.writeLock().open()) {
      @SuppressWarnings("unchecked")
      RegisteredObjectListeners<T> col = (RegisteredObjectListeners<T>) this.listenersObject.computeIfAbsent(objectClass,
          (k) -> new RegisteredObjectListeners<T>(objectClass));
      col.listeners.add(listener);
    }
  }

  public <T, R> void registerProcedure(String name, Function<T, R> function) {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(function);
    Preconditions.checkArgument(!name.isEmpty());
    RegisteredProcedure<T, R> proc = new RegisteredProcedure<>(name, function);
    try (CloseableLock l = lock.writeLock().open()) {
      registeredProcedures.put(proc.getInfo(), proc);
    }
  }

  public RegisteredProcedure<?, ?> getRegisteredProcedure(ProcedureInformation info) {
    try (CloseableLock l = lock.readLock().open()) {
      return this.registeredProcedures.get(info);
    }
  }

  public <T, R> ProcedureCallResult<T, R> callProcedure(Server server, Procedure<T, R> procedure, T argument) {
    Preconditions.checkNotNull(server);
    Preconditions.checkNotNull(procedure);
    final SingleProcedureCall<T, R> call = new SingleProcedureCall<>(server, procedure, argument, MAX_CALL_TIMEOUT);
    try (CloseableLock l = lock.readLock().open()) {
      if (!call.isDone()) {
        openCalls.put(call.getId(), call);
        Network.getInstance().sendCall(call);
      }
    } catch (Exception e) {
      call.setException(e);
    }
    return call.getResult();
  }


  public <T, R> Map<UUID, ? extends ListenableFuture<R>> callProcedure(Collection<? extends Server> servers, Procedure<T, R> procedure,
      T argument) {
    Preconditions.checkNotNull(servers);
    Preconditions.checkArgument(!servers.isEmpty());
    Preconditions.checkNotNull(procedure);
    final MultiProcedureCall<T, R> call = new MultiProcedureCall<>(servers, procedure, argument, MAX_CALL_TIMEOUT);
    try (CloseableLock l = lock.readLock().open()) {
      if (!call.isDone()) {
        openCalls.put(call.getId(), call);
        Network.getInstance().sendCall(call);
      }
    } catch (Exception e) {
      call.setException(e);
    }
    return call.getResult();
  }

  @RequiredArgsConstructor
  private static class RegisteredObjectListeners<T> {
    @Getter
    private final Class<T> objectClass;
    private final Set<Listener<ObjectMessageInEvent<T>>> listeners = Sets.newConcurrentHashSet();

    @SuppressWarnings("unchecked")
    private void callUnsecure(ObjectMessageInEvent<?> event) throws ClassCastException {
      if (!objectClass.isAssignableFrom(event.getObjectClass())) {
        throw new ClassCastException("Can't cast from " + event.getObjectClass().getName() + " to " + objectClass);
      }
      this.call((ObjectMessageInEvent<T>) event);
    }

    public void call(ObjectMessageInEvent<T> event) {
      for (Listener<ObjectMessageInEvent<T>> l : listeners) {
        try {
          l.on(event);
        } catch (Exception e) {
          Logger.getGlobal().log(Level.SEVERE, "Listener error!\n" + e.getMessage(), e);
        }
      }
    }
  }

  @FunctionalInterface
  public static interface Listener<E> {
    void on(E event);
  }

  public void callListeners(ByteMessage msg) {
    final ByteMessageInEvent event = new ByteMessageInEvent(msg);
    this.listenersByteMessage.get(msg.getKey()).forEach(l -> l.on(event));
  }

  public void callListeners(ObjectMessage msg) throws Exception {
    final ObjectMessageInEvent<?> event = new ObjectMessageInEvent<>(msg.getClass(), msg);
    this.listenersObject.get(msg.getType()).callUnsecure(event);
  }

}
