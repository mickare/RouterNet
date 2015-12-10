package de.rennschnitzel.backbone.api.network.event;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.api.network.Network;
import de.rennschnitzel.backbone.api.network.message.ObjectMessage;
import lombok.Getter;

public class ObjectMessageInEvent<T> extends MessageInEvent<ObjectMessage> {

  @Getter
  private final Class<T> objectClass;
  @Getter
  private final T object;

  @SuppressWarnings("unchecked")
  public ObjectMessageInEvent(Class<T> objectClass, ObjectMessage msg) throws Exception {
    super(msg);
    Preconditions.checkNotNull(objectClass);
    Preconditions.checkArgument(objectClass.getName().equalsIgnoreCase(msg.getType()));
    this.objectClass = objectClass;
    this.object = (T) Network.FST.getObjectInput(msg.getData().newInput()).readObject(objectClass);
  }

}
