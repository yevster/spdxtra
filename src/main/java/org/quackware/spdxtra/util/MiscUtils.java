package org.quackware.spdxtra.util;

import java.util.Iterator;
import java.util.function.Function;

public class MiscUtils {
	/**
	 * Creates an iterable from an existing iterator and a function that transforms values of that iterator
	 * @param iterator
	 * @param func
	 * @return
	 */
	public static <O, T>Iterable<O> fromIteratorConsumer(final Iterator<T> iterator, final Function<T, O> func){
		return new Iterable<O>() {
			@Override
			public Iterator<O> iterator() {
				return new Iterator<O>() {
					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}
					
					@Override
					public O next() {
						return func.apply(iterator.next());
					}
				};
			}
		};
	}

}
