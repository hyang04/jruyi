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

import java.lang.ref.WeakReference;

/**
 * A node contains 2 references to point to previous node and next node
 * respectively, and a reference to hold a data element. It is used to form a
 * doubly linked list.
 * 
 * <p>
 * This class has a thread local cache mechanism for node instances.
 * 
 * @param <E>
 *            the type of the element
 */
public final class BiListNode<E> implements ICloseable {

	BiListNode<E> m_next;
	BiListNode<E> m_previous;
	E m_e;

	static final class Stack {

		private static final ThreadLocal<WeakReference<Stack>> m_cache;
		private final BiListNode<?> m_head = new BiListNode<Object>();

		static {
			m_cache = new ThreadLocal<WeakReference<Stack>>() {

				@Override
				protected WeakReference<Stack> initialValue() {
					return new WeakReference<Stack>(null);
				}
			};
		}

		static Stack get() {
			Stack t = m_cache.get().get();
			if (t == null) {
				t = new Stack();
				m_cache.set(new WeakReference<Stack>(t));
			}
			return t;
		}

		boolean isEmpty() {
			return m_head.m_next == null;
		}

		<E> BiListNode<E> pop() {
			@SuppressWarnings("unchecked")
			final BiListNode<E> head = (BiListNode<E>) m_head;
			final BiListNode<E> next = head.m_next;
			head.m_next = next.m_next;
			next.m_next = null;
			return next;
		}

		<E> void push(BiListNode<E> node) {
			@SuppressWarnings("unchecked")
			final BiListNode<E> head = (BiListNode<E>) m_head;
			node.m_next = head.m_next;
			head.m_next = node;
			node.m_previous = null;
			node.m_e = null;
		}
	}

	private BiListNode() {
	}

	/**
	 * Return a {@code BiListNode} instance fetched from the current thread's
	 * local cache if the cache is not empty. Otherwise a new instance will be
	 * created and returned.
	 * 
	 * @return an instance of {@code BiListNode}
	 */
	@SuppressWarnings("unchecked")
	public static <E> BiListNode<E> create() {
		Stack stack = Stack.get();
		return (BiListNode<E>) (stack.isEmpty() ? new BiListNode<Object>()
				: stack.pop());
	}

	/**
	 * Return the data element.
	 * 
	 * @return the data element
	 */
	public E get() {
		return m_e;
	}

	/**
	 * Return the next node.
	 * 
	 * @return the next node
	 */
	public BiListNode<E> next() {
		return m_next;
	}

	/**
	 * Set the next node to the specified {@code node}.
	 * 
	 * @param node
	 *            the next node to be pointed to
	 */
	public void next(BiListNode<E> node) {
		m_next = node;
	}

	/**
	 * Return the previous node.
	 * 
	 * @return the previous node
	 */
	public BiListNode<E> previous() {
		return m_previous;
	}

	/**
	 * Set the previous node to the specified {@code node}.
	 * 
	 * @param node
	 *            the previous node to be pointed to
	 */
	public void previous(BiListNode<E> node) {
		m_previous = node;
	}

	/**
	 * Set the data element to the specified {@code e}.
	 * 
	 * @param e
	 *            the data element to be held
	 */
	public void set(E e) {
		m_e = e;
	}

	/**
	 * Recycle this object to the current thread's local cache.
	 */
	@Override
	public void close() {
		Stack stack = Stack.get();
		stack.push(this);
	}
}
