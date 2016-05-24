package de.mickare.metricweb.metric;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingList;

public class EvictingLinkedList<E> extends ForwardingList<E> {

  private final int maxSize;
  private final LinkedList<E> delegate = new LinkedList<>();

  public EvictingLinkedList(int maxSize) {
    Preconditions.checkArgument(maxSize > 0);
    this.maxSize = maxSize;
  }

  @Override
  protected List<E> delegate() {
    return delegate;
  }

  private void shrink() {
    while (this.delegate.size() > maxSize) {
      this.delegate.removeFirst();
    }
  }

  @Override
  public boolean add(E element) {
    boolean result = super.add(element);
    shrink();
    return result;
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    boolean result = super.addAll(c);
    shrink();
    return result;
  }

  @Override
  public void add(int index, E element) {
    super.add(index, element);
    shrink();
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> elements) {
    boolean result = super.addAll(index, elements);
    shrink();
    return result;
  }



}
