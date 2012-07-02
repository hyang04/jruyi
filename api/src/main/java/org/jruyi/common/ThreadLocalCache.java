/**
 * Copyright 2012 JRuyi.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jruyi.common;

import org.jruyi.common.internal.ThreadLocalCacheProvider;

/**
 * This is the factory class for {@code IThreadLocalCache}.
 * 
 * @see IThreadLocalCache
 */
public final class ThreadLocalCache {

	private static final IFactory m_factory = ThreadLocalCacheProvider
			.getInstance().getFactory();

	/**
	 * A factory class to create instances of {@code IThreadLocalCache}. It is
	 * used to separate the implementation provider from the API module.
	 */
	public interface IFactory {

		/**
		 * Create a thread local cache which is softly referenced and backed by
		 * an array with 6 as the initial capacity.
		 * 
		 * @return a thread local cache
		 */
		public <E> IThreadLocalCache<E> softArrayCache();

		/**
		 * Create a thread local cache which is softly referenced and backed by
		 * an array with the specified initial capacity.
		 * 
		 * @param initialCapacity
		 *            the initial capacity of the backing array
		 * @return a thread local cache
		 */
		public <E> IThreadLocalCache<E> softArrayCache(int initialCapacity);

		/**
		 * Create a thread local cache which is softly reference and backed by a
		 * linked list.
		 * 
		 * @return a thread local cache
		 */
		public <E> IThreadLocalCache<E> softLinkedCache();

		/**
		 * Create a thread local cache which is weakly referenced and backed by
		 * an array with 6 as the initial capacity.
		 * 
		 * @return a thread local cache
		 */
		public <E> IThreadLocalCache<E> weakArrayCache();

		/**
		 * Create a thread local cache which is weakly referenced and backed by
		 * an array with the specified initial capacity.
		 * 
		 * @param initialCapacity
		 *            the initial capacity of the backing array
		 * @return a thread local cache
		 */
		public <E> IThreadLocalCache<E> weakArrayCache(int initialCapacity);

		/**
		 * Create a thread local cache which is weakly referenced and backed by
		 * a linked list.
		 * 
		 * @return a thread local cache
		 */
		public <E> IThreadLocalCache<E> weakLinkedCache();
	}

	private ThreadLocalCache() {
	}

	/**
	 * Create a thread local cache which is softly referenced and backed by an
	 * array with 6 as the initial capacity.
	 * 
	 * @return a thread local cache
	 */
	public static <E> IThreadLocalCache<E> softArrayCache() {
		return m_factory.softArrayCache();
	}

	/**
	 * Create a thread local cache which is softly referenced and backed by an
	 * array with the specified initial capacity.
	 * 
	 * @param initialCapacity
	 *            the initial capacity of the backing array
	 * @return a thread local cache
	 */
	public static <E> IThreadLocalCache<E> softArrayCache(int initialCapacity) {
		return m_factory.softArrayCache(initialCapacity);
	}

	/**
	 * Create a thread local cache which is softly reference and backed by a
	 * linked list.
	 * 
	 * @return a thread local cache
	 */
	public static <E> IThreadLocalCache<E> softLinkedCache() {
		return m_factory.softLinkedCache();
	}

	/**
	 * Create a thread local cache which is weakly referenced and backed by an
	 * array with 6 as the initial capacity.
	 * 
	 * @return a thread local cache
	 */
	public static <E> IThreadLocalCache<E> weakArrayCache() {
		return m_factory.weakArrayCache();
	}

	/**
	 * Create a thread local cache which is weakly referenced and backed by an
	 * array with the specified initial capacity.
	 * 
	 * @param initialCapacity
	 *            the initial capacity of the backing array
	 * @return a thread local cache
	 */
	public static <E> IThreadLocalCache<E> weakArrayCache(int initialCapacity) {
		return m_factory.weakArrayCache(initialCapacity);
	}

	/**
	 * Create a thread local cache which is weakly referenced and backed by a
	 * linked list.
	 * 
	 * @return a thread local cache
	 */
	public static <E> IThreadLocalCache<E> weakLinkedCache() {
		return m_factory.weakLinkedCache();
	}
}
