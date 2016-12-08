package de.mickare.routernet.client;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;

import lombok.Data;
import lombok.NoArgsConstructor;

public @Data @NoArgsConstructor class HomeSettings {
	private UUID id = UUID.randomUUID();
	private Set<String> namespaces = Sets.newHashSet();
	private String name = null;
}
