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
 * A node contains a reference to point to the next node and a reference to hold
 * the element data. It is used to form a singly linked list.
 * 
 * <p>
 * This class has a thread local cache mechanism for node instances.
 * 
 * @param <E>
 *            the type of the element
 */
public final class ListNode<E> implements ICloseable {

	E m_e;
	ListNode<E> m_next;

	static final class Stack {

		private static final ThreadLocal<WeakReference<Stack>> m_cache;
		private final ListNode<?> m_head = new ListNode<Object>();

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

		<E> ListNode<E> pop() {
			@SuppressWarnings("unchecked")
			final ListNode<E> head = (ListNode<E>) m_head;
			final ListNode<E> next = head.m_next;
			head.m_next = next.m_next;
			next.m_next = null;
			return next;
		}

		<E> void push(ListNode<E> node) {
			@SuppressWarnings("unchecked")
			final ListNode<E> head = (ListNode<E>) m_head;
			node.m_next = head.m_next;
			head.m_next = node;
			node.m_e = null;
		}
	}

	/**
	 * Return a {@code ListNode} instance fetched from the current thread's
	 * local cache if the cache is not empty. Otherwise a new instance will be
	 * created and returned.
	 * 
	 * @return an instance of {@code ListNode}
	 */
	@SuppressWarnings("unchecked")
	public static <E> ListNode<E> create() {
		Stack cache = Stack.get();
		return (ListNode<E>) (cache.isEmpty() ? new ListNode<Object>() : cache
				.pop());
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
	 * Set the data element to the specified {@code e}.
	 * 
	 * @param e
	 *            the data element to be held
	 */
	public void set(E e) {
		m_e = e;
	}

	/**
	 * Return the next node.
	 * 
	 * @return the next node
	 */
	public ListNode<E> next() {
		return m_next;
	}

	/**
	 * Set the next node to the specified {@code node}.
	 * 
	 * @param node
	 *            the next node to be pointed to
	 */
	public void next(ListNode<E> node) {
		m_next = node;
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
