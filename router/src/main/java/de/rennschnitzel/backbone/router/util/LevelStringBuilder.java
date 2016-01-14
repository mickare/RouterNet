package de.rennschnitzel.backbone.router.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;

public abstract class LevelStringBuilder {

  public static LevelStringBuilder newBuilder() {
    return new MainStringBuilder();
  }

  public static LevelStringBuilder newBuilder(String childIndent) {
    return new MainStringBuilder(childIndent);
  }

  public static LevelStringBuilder newBuilder(String childIndent, String mainIndent) {
    return new MainStringBuilder(childIndent, mainIndent);
  }

  private static interface Child {
    List<String> getLeafs(String indent);

    boolean hasStringBuilder();

    StringBuilder getStringBuilder();
  }

  private static class TextChild implements Child {
    private final StringBuilder builder = new StringBuilder();

    @Override
    public List<String> getLeafs(String indent) {
      String[] lines = builder.toString().split("\n");
      for (int i = 0; i < lines.length; i++) {
        lines[i] = indent + lines[i];
      }
      return Arrays.asList(lines);
    }

    @Override
    public boolean hasStringBuilder() {
      return true;
    }

    @Override
    public StringBuilder getStringBuilder() {
      return builder;
    }

  }

  private static class MainStringBuilder extends BasicStringBuilder {

    private final String childIndent;

    private MainStringBuilder() {
      this(" ");
    }

    private MainStringBuilder(String childIndent) {
      this(childIndent, "");
    }

    private MainStringBuilder(String childIndent, String mainIndent) {
      super(mainIndent);
      this.childIndent = childIndent;
    }

    @Override
    public LevelStringBuilder down() {
      BasicStringBuilder child = new BasicStringBuilder(this.childIndent);
      this.addChild(child);
      return child;
    }

    @Override
    public LevelStringBuilder down(String indent) {
      BasicStringBuilder child = new BasicStringBuilder(indent);
      this.addChild(child);
      return child;
    }
  }

  private static class BasicStringBuilder extends LevelStringBuilder implements Child {

    private LinkedList<Child> childs = new LinkedList<Child>();

    private BasicStringBuilder() {
      this(" ");
    }

    private BasicStringBuilder(String indent) {
      super(indent != null ? indent : "");
    }

    @Override
    public List<String> getLeafs(String indent) {
      String leafIndent = indent + this.getIndent();
      List<String> leafs = Lists.newLinkedList();
      for (Child c : childs) {
        leafs.addAll(c.getLeafs(leafIndent));
      }
      return leafs;
    }

    protected void addChild(Child child) {
      this.childs.add(child);
    }

    @Override
    public boolean hasStringBuilder() {
      return false;
    }

    @Override
    public StringBuilder getStringBuilder() {
      return null;
    }

    private StringBuilder getLevelStringBuilder() {
      Child child;
      if (this.childs.isEmpty()) {
        child = new TextChild();
        this.childs.add(child);
      } else {
        child = this.childs.getLast();
        if (!child.hasStringBuilder()) {
          child = new TextChild();
          this.childs.add(child);
        }
      }
      return child.getStringBuilder();
    }

    @Override
    public LevelStringBuilder freshLine() {
      StringBuilder sb = this.getLevelStringBuilder();
      if (sb.length() == 0) {
        return this;
      }
      int pos = sb.lastIndexOf("\n");
      if (pos < sb.length() - 1) {
        sb.append('\n');
      }
      return this;
    }

    @Override
    public LevelStringBuilder append(String v) {
      this.getLevelStringBuilder().append(v);
      return this;
    }

    @Override
    public LevelStringBuilder append(boolean v) {
      this.getLevelStringBuilder().append(v);
      return this;
    }

    @Override
    public LevelStringBuilder append(int v) {
      this.getLevelStringBuilder().append(v);
      return this;
    }

    @Override
    public LevelStringBuilder append(float v) {
      this.getLevelStringBuilder().append(v);
      return this;
    }

    @Override
    public LevelStringBuilder append(double v) {
      this.getLevelStringBuilder().append(v);
      return this;
    }

    @Override
    public LevelStringBuilder append(long v) {
      this.getLevelStringBuilder().append(v);
      return this;
    }

    @Override
    public LevelStringBuilder append(Object v) {
      this.getLevelStringBuilder().append(v);
      return this;
    }

    @Override
    public LevelStringBuilder append(CharSequence v) {
      this.getLevelStringBuilder().append(v);
      return this;
    }

    @Override
    public LevelStringBuilder append(char v) {
      this.getLevelStringBuilder().append(v);
      return this;
    }

    @Override
    public LevelStringBuilder append(char[] v) {
      this.getLevelStringBuilder().append(v);
      return this;
    }

    @Override
    public LevelStringBuilder append(StringBuffer v) {
      this.getLevelStringBuilder().append(v);
      return this;
    }

    @Override
    public LevelStringBuilder down() {
      BasicStringBuilder child = new BasicStringBuilder(this.getIndent());
      this.childs.add(child);
      return child;
    }

    @Override
    public LevelStringBuilder down(String indent) {
      BasicStringBuilder child = new BasicStringBuilder(indent);
      this.childs.add(child);
      return child;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (String leaf : this.getLeafs(this.getIndent())) {
        if (first) {
          first = false;
        } else {
          sb.append('\n');
        }
        sb.append(leaf);
      }
      return sb.toString();
    }

  }

  // ********************************************************************** //
  // Basic class
  // ********************************************************************** //

  private final String indent;

  public LevelStringBuilder(String indent) {
    this.indent = indent;
  }

  public abstract LevelStringBuilder append(String v);

  public abstract LevelStringBuilder append(boolean v);

  public abstract LevelStringBuilder append(int v);

  public abstract LevelStringBuilder append(float v);

  public abstract LevelStringBuilder append(double v);

  public abstract LevelStringBuilder append(long v);

  public abstract LevelStringBuilder append(Object v);

  public abstract LevelStringBuilder append(CharSequence v);

  public abstract LevelStringBuilder append(char v);

  public abstract LevelStringBuilder append(char[] v);

  public abstract LevelStringBuilder append(StringBuffer v);

  public abstract LevelStringBuilder down();

  public abstract LevelStringBuilder down(String indent);

  public LevelStringBuilder newLine() {
    return this.append('\n');
  }

  public abstract LevelStringBuilder freshLine();

  public String getIndent() {
    return indent;
  }

}
