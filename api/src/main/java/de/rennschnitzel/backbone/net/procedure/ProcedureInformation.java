package de.rennschnitzel.backbone.net.procedure;

import java.util.Objects;

import com.google.common.base.Preconditions;

import de.rennschnitzel.backbone.net.protocol.NetworkProtocol.ProcedureDescription;
import lombok.Getter;

public class ProcedureInformation implements Comparable<ProcedureInformation> {

  @Getter
  private final String name, argumentType, resultType;

  public ProcedureInformation(final ProcedureDescription msg) throws IllegalArgumentException, NullPointerException {
    this(msg.getName(), msg.getArgument(), msg.getResult());
  }


  public ProcedureInformation(final String name, final Class<?> argument, final Class<?> result) {
    this(name, argument.getName(), result.getName());
  }

  public ProcedureInformation(final String name, final String argumentType, final String resultType)
      throws IllegalArgumentException, NullPointerException {
    Preconditions.checkArgument(!name.isEmpty());
    Preconditions.checkArgument(!argumentType.isEmpty());
    Preconditions.checkArgument(!resultType.isEmpty());
    this.name = name.toLowerCase();
    this.argumentType = argumentType.toLowerCase();
    this.resultType = resultType.toLowerCase();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, argumentType, resultType);
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this)
      return true;
    if (!(o instanceof ProcedureInformation))
      return false;
    final ProcedureInformation oi = (ProcedureInformation) o;
    if (!name.equals(oi.name) || !argumentType.equals(oi.argumentType) || !resultType.equals(oi.resultType))
      return false;
    return true;
  }

  @Override
  public int compareTo(ProcedureInformation o) {
    if (this == o) {
      return 0;
    }
    int n = String.CASE_INSENSITIVE_ORDER.compare(name, o.name);
    if (n != 0) {
      return n;
    }
    int arg = String.CASE_INSENSITIVE_ORDER.compare(argumentType, o.argumentType);
    if (arg != 0) {
      return arg;
    }
    return String.CASE_INSENSITIVE_ORDER.compare(resultType, o.resultType);
  }

}
