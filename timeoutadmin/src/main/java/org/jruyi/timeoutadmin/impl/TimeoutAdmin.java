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
package org.jruyi.timeoutadmin.impl;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.BiListNode;
import org.jruyi.timeoutadmin.ITimeoutAdmin;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.jruyi.workshop.IWorker;

public final class TimeoutAdmin implements Runnable, ITimeoutAdmin {

	// 30 minutes.
	private static final int DEFAULT_SCALE = 30 * 60;
	private static final int DEFAULT_LOCK_NUM = 32;
	// the maximum timeout in a single schedule
	private int m_scale;
	// minus - 1 from which MUST be a power of 2 and greater than m_scale.
	private int m_capacityMask;
	// minus - 1 from which is a power of 2 and not more than 32.
	private int m_lockNumMask;
	// The hand that points to the current timeout sublist, nodes of witch are
	// between m_dial[m_hand] and m_dial[m_hand + 1].
	private int m_hand;
	private BiListNode<TimeoutEvent>[] m_dial;
	private LinkedList<TimeoutEvent> m_list;
	private ReentrantLock[] m_locks;
	private IWorker m_worker;
	private Thread m_thread;

	@Override
	public ITimeoutNotifier createNotifier(Object subject) {
		return new TimeoutNotifier(subject, this);
	}

	@Override
	public void run() {
		Thread thread = Thread.currentThread();
		long nextExecutionTime = System.currentTimeMillis() + 1000L;
		try {
			long waitTime = 0L;
			while (!thread.isInterrupted()) {
				while ((waitTime = nextExecutionTime
						- System.currentTimeMillis()) < 0) {
					spin();
					nextExecutionTime += 1000L;
				}

				if (waitTime > 0) {
					synchronized (this) {
						wait(waitTime);
					}
				}

				nextExecutionTime = System.currentTimeMillis() + 1000L;

				spin();
			}
		} catch (InterruptedException e) {
		}
	}

	protected void setWorker(IWorker worker) {
		m_worker = worker;
	}

	protected void unsetWorker(IWorker worker) {
		if (m_worker == worker)
			m_worker = null;
	}

	protected void activate(Map<String, ?> properties) {
		String v = (String) properties.get("scale");
		int scale = DEFAULT_SCALE;
		if (v == null || (scale = Integer.parseInt(v)) < 5)
			scale = DEFAULT_SCALE;

		int capacity = 1;
		for (int i = scale + 10; i != 0; i >>= 1)
			capacity <<= 1;

		int lockNum = capacity < DEFAULT_LOCK_NUM ? capacity : DEFAULT_LOCK_NUM;

		m_scale = scale;
		m_capacityMask = capacity - 1;
		m_lockNumMask = lockNum - 1;

		LinkedList<TimeoutEvent> list = new LinkedList<TimeoutEvent>();
		// one more as a tail node for conveniently iterating the timeout
		// sublist
		@SuppressWarnings("unchecked")
		BiListNode<TimeoutEvent>[] dial = (BiListNode<TimeoutEvent>[]) new BiListNode<?>[capacity + 1];
		for (int i = 0; i < capacity + 1; ++i)
			// create sentinel nodes
			dial[i] = list.addLast(null);

		ReentrantLock[] locks = new ReentrantLock[lockNum];
		for (int i = 0; i < lockNum; ++i)
			locks[i] = new ReentrantLock();

		m_hand = 0;
		m_list = list;
		m_dial = dial;
		m_locks = locks;

		m_thread = new Thread(this, "TimeoutAdmin");
		m_thread.start();
	}

	protected void deactivate() {
		m_thread.interrupt();
		try {
			m_thread.join();
		} catch (InterruptedException e) {
		}
		m_list = null;
		m_thread = null;
		m_dial = null;
		m_list = null;
		m_locks = null;
	}

	void schedule(TimeoutNotifier notifier, int timeout) {
		TimeoutEvent event = TimeoutEvent.get(notifier, timeout);
		int scale = m_scale;
		if (timeout > scale) {
			event.setTimeLeft(timeout - scale);
			timeout = scale;
		} else
			event.setTimeLeft(0);

		int index = getEffectiveIndex(m_hand + timeout);
		event.setIndex(index);
		notifier.setNode(m_list.syncInsertAfter(m_dial[index], event,
				getLock(index)));
	}

	void reschedule(TimeoutNotifier notifier, int timeout) {
		BiListNode<TimeoutEvent> node = notifier.getNode();
		TimeoutEvent event = node.get();
		final ReentrantLock lock1 = getLock(event.getIndex());

		// dest sublist
		int scale = m_scale;
		if (timeout > scale) {
			event.setTimeLeft(timeout - scale);
			timeout = scale;
		} else
			event.setTimeLeft(0);

		int index = getEffectiveIndex(m_hand + timeout);
		event.setIndex(index);
		BiListNode<TimeoutEvent> destHead = m_dial[index];
		final ReentrantLock lock2 = getLock(index);

		m_list.syncMoveAfter(destHead, node, lock1, lock2);
	}

	void cancel(TimeoutNotifier notifier) {
		BiListNode<TimeoutEvent> node = notifier.getNode();
		notifier.clearNode();
		TimeoutEvent event = node.get();
		final ReentrantLock lock = getLock(event.getIndex());
		event = m_list.syncRemove(node, lock);

		// release the timeout event
		event.release();
	}

	void fireTimeout(TimeoutNotifier notifier) {
		BiListNode<TimeoutEvent> node = notifier.getNode();
		notifier.clearNode();
		final ReentrantLock lock = getLock(m_hand);
		TimeoutEvent event = m_list.syncRemove(node, lock);

		m_worker.run(event);
	}

	void scheduleNextRound(TimeoutNotifier notifier) {
		BiListNode<TimeoutEvent> node = notifier.getNode();
		TimeoutEvent event = node.get();
		int scale = m_scale;
		int n = event.getTimeLeft();
		// need reschedule
		if (n > scale) {
			event.setTimeLeft(n - scale);
			n = scale;
		} else
			event.setTimeLeft(0);

		int hand = m_hand;
		n = getEffectiveIndex(hand + n);
		event.setIndex(n);

		final ReentrantLock lock1 = getLock(hand);
		final ReentrantLock lock2 = getLock(n);
		// keep using the same list node
		m_list.syncMoveAfter(m_dial[n], node, lock1, lock2);
	}

	private int getEffectiveIndex(int index) {
		return index & (m_capacityMask);
	}

	private ReentrantLock getLock(int index) {
		return m_locks[index & m_lockNumMask];
	}

	private void spin() {
		int hand = m_hand;
		BiListNode<TimeoutEvent>[] dial = m_dial;
		BiListNode<TimeoutEvent> begin = dial[hand];
		BiListNode<TimeoutEvent> end = dial[hand + 1];
		BiListNode<TimeoutEvent> node = null;

		while ((node = begin.next()) != end) {
			TimeoutNotifier notifier = null;
			TimeoutEvent event = node.get();

			// If this node has been cancelled, just skip.
			// Otherwise, go ahead.
			if (event != null && (notifier = event.getNotifier()) != null)
				// Passing "hand" for checking the notifier is still in this
				// same timeout sublist. Otherwise it may be cancelled or
				// rescheduled, and needs to be skipped.
				notifier.onTimeout(hand);
		}

		// spin
		m_hand = getEffectiveIndex(hand + 1);
	}
}
