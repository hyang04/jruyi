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

import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.BiListNode;
import org.jruyi.timeoutadmin.ITimeoutListener;
import org.jruyi.timeoutadmin.ITimeoutNotifier;

final class TimeoutNotifier implements ITimeoutNotifier {

	private final Object m_subject;
	private final TimeoutAdmin m_admin;
	private BiListNode<TimeoutEvent> m_node;
	private ITimeoutListener m_listener;
	private IState m_state = Unscheduled.getInstance();
	private final ReentrantLock m_lock;

	TimeoutNotifier(Object subject, TimeoutAdmin admin) {
		m_subject = subject;
		m_admin = admin;
		m_lock = new ReentrantLock();
	}

	interface IState {

		public boolean schedule(TimeoutNotifier notifier, int timeout);

		public boolean cancel(TimeoutNotifier notifier);

		public boolean reset(TimeoutNotifier notifier);

		public void close(TimeoutNotifier notifier);

		public int state();
	}

	static final class Scheduled implements IState {

		private static final IState m_inst = new Scheduled();

		public static IState getInstance() {
			return m_inst;
		}

		@Override
		public boolean schedule(TimeoutNotifier notifier, int timeout) {
			notifier.getTimeoutAdmin().reschedule(notifier, timeout);
			return true;
		}

		@Override
		public boolean cancel(TimeoutNotifier notifier) {
			notifier.getTimeoutAdmin().cancel(notifier);
			notifier.changeState(Unscheduled.getInstance());
			return true;
		}

		@Override
		public boolean reset(TimeoutNotifier notifier) {
			return false;
		}

		@Override
		public void close(TimeoutNotifier notifier) {
			notifier.getTimeoutAdmin().cancel(notifier);
			notifier.changeState(Closed.getInstance());
		}

		@Override
		public int state() {
			return SCHEDULED;
		}
	}

	static final class Unscheduled implements IState {

		private static final IState m_inst = new Unscheduled();

		public static IState getInstance() {
			return m_inst;
		}

		@Override
		public boolean schedule(TimeoutNotifier notifier, int timeout) {
			notifier.getTimeoutAdmin().schedule(notifier, timeout);
			notifier.changeState(Scheduled.getInstance());
			return true;
		}

		@Override
		public boolean cancel(TimeoutNotifier notifier) {
			return true;
		}

		@Override
		public boolean reset(TimeoutNotifier notifier) {
			return false;
		}

		@Override
		public void close(TimeoutNotifier notifier) {
			notifier.changeState(Closed.getInstance());
		}

		@Override
		public int state() {
			return UNSCHEDULED;
		}
	}

	static final class TimedOut implements IState {

		private static final IState m_inst = new TimedOut();

		public static IState getInstance() {
			return m_inst;
		}

		@Override
		public boolean schedule(TimeoutNotifier notifier, int timeout) {
			return false;
		}

		@Override
		public boolean cancel(TimeoutNotifier notifier) {
			return false;
		}

		@Override
		public boolean reset(TimeoutNotifier notifier) {
			notifier.changeState(Unscheduled.getInstance());
			return true;
		}

		@Override
		public void close(TimeoutNotifier notifier) {
			notifier.changeState(Closed.getInstance());
		}

		@Override
		public int state() {
			return TIMEDOUT;
		}
	}

	static final class Closed implements IState {

		private static final IState m_inst = new Closed();

		public static IState getInstance() {
			return m_inst;
		}

		@Override
		public boolean cancel(TimeoutNotifier notifier) {
			return false;
		}

		@Override
		public boolean schedule(TimeoutNotifier notifier, int timeout) {
			return false;
		}

		@Override
		public boolean reset(TimeoutNotifier notifier) {
			return false;
		}

		@Override
		public void close(TimeoutNotifier notifier) {
		}

		@Override
		public int state() {
			return CLOSED;
		}
	}

	@Override
	public Object getSubject() {
		return m_subject;
	}

	@Override
	public int state() {
		return m_state.state();
	}
	
	@Override
	public boolean schedule(int timeout) {
		if (timeout < 1)
			throw new IllegalArgumentException();

		final ReentrantLock lock = m_lock;
		if (!lock.tryLock()) // fail-fast
			return false;

		try {
			return m_state.schedule(this, timeout);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean cancel() {
		final ReentrantLock lock = m_lock;
		if (!lock.tryLock()) // fail-fast
			return false;

		try {
			return m_state.cancel(this);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean reset() {
		final ReentrantLock lock = m_lock;
		if (!lock.tryLock()) // fail-fast
			return false;

		try {
			return m_state.reset(this);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.close(this);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void setListener(ITimeoutListener listener) {
		m_listener = listener;
	}

	void onTimeout(int hand) {
		final ReentrantLock lock = m_lock;
		// If the lock cannot be acquired, which means this notifier is being
		// cancelled or rescheduled or closed, just skip.
		if (!lock.tryLock())
			return;

		try {
			// If this notifier is not in the same timeout sublist,
			// which means it has been cancelled or rescheduled,
			// then skip.
			TimeoutEvent event = m_node.get();
			if (event == null || hand != event.getIndex())
				return;

			if (event.getTimeLeft() < 1) {
				changeState(TimedOut.getInstance());
				m_admin.fireTimeout(this);
			} else
				m_admin.scheduleNextRound(this);
		} finally {
			lock.unlock();
		}
	}

	ITimeoutListener getListener() {
		return m_listener;
	}

	// Set when scheduled
	void setNode(BiListNode<TimeoutEvent> node) {
		m_node = node;
	}

	// Cleared when cancelled or timeout
	void clearNode() {
		m_node = null;
	}

	TimeoutAdmin getTimeoutAdmin() {
		return m_admin;
	}

	BiListNode<TimeoutEvent> getNode() {
		return m_node;
	}

	void changeState(IState state) {
		m_state = state;
	}
}
