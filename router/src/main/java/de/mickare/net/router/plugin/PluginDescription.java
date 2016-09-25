package de.mickare.net.router.plugin;

import java.io.File;
import java.util.List;

import com.google.common.collect.ImmutableList;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PluginDescription {

  private String name;
  private String main;
  private String version;
  private String author;
  private List<String> depends = ImmutableList.of();
  private List<String> softDepends = ImmutableList.of();
  private @Setter @NonNull File file = null;
  private String description = null;

}
