package de.mickare.net.util;

import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class ImmutableCollectors {
	
	public static <T> Collector<T, ImmutableSet.Builder<T>, ImmutableSet<T>> toSet() {
		return Collector.of( ImmutableSet::builder, ( b, v ) -> b.add( v ), ( b1, b2 ) -> b1.addAll( b2.build() ), ( b ) -> b.build(), Characteristics.UNORDERED );
	}
	
	public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> toList() {
		return Collector.of( ImmutableList::builder, ( b, v ) -> b.add( v ), ( b1, b2 ) -> b1.addAll( b2.build() ), ( b ) -> b.build() );
	}
	
	public static <T, K, U> Collector<T, ImmutableMap.Builder<K, U>, ImmutableMap<K, U>> toMap( Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper ) {
		return Collector.of( ImmutableMap::builder, ( b, v ) -> b.put( keyMapper.apply( v ), valueMapper.apply( v ) ), ( b1, b2 ) -> b1.putAll( b2.build() ), ( b ) -> b.build() );
	}
	
}
