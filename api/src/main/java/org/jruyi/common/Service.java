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

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class provides a default implementation for state transition of a
 * service.
 */
public abstract class Service implements IService {

	private final ReentrantLock m_lock = new ReentrantLock();
	private State m_state = Stopped.INST;

	static abstract class State {

		private final int m_state;

		State(int state) {
			m_state = state;
		}

		public void start(Service service) throws Exception {
		}

		public void start(Service service, int options) throws Exception {
		}

		public void stop(Service service) {
		}

		public void stop(Service service, int options) {
		}

		public final int state() {
			return m_state;
		}
	}

	static final class Started extends State {

		static final State INST = new Started(ACTIVE);

		private Started(int state) {
			super(state);
		}

		@Override
		public void stop(Service service) {
			service.changeState(Stopping.INST);
			service.stopInternal();
			service.changeState(Stopped.INST);
		}

		@Override
		public void stop(Service service, int options) {
			service.changeState(Stopping.INST);
			service.stopInternal(options);
			service.changeState(Stopped.INST);
		}

		@Override
		public String toString() {
			return "Active";
		}
	}

	static final class Stopped extends State {

		static final State INST = new Stopped(STOPPED);

		private Stopped(int state) {
			super(state);
		}

		@Override
		public void start(Service service) throws Exception {
			service.changeState(Starting.INST);
			try {
				service.startInternal();
			} catch (Exception e) {
				service.changeState(Stopped.INST);
				throw e;
			}

			service.changeState(Started.INST);
		}

		@Override
		public void start(Service service, int options) throws Exception {
			service.changeState(Starting.INST);
			try {
				service.startInternal(options);
			} catch (Exception e) {
				service.changeState(Stopped.INST);
				throw e;
			}

			service.changeState(Started.INST);
		}

		@Override
		public String toString() {
			return "Stopped";
		}
	}

	static final class Starting extends State {

		static final State INST = new Starting(STARTING);

		private Starting(int state) {
			super(state);
		}

		@Override
		public String toString() {
			return "Starting";
		}
	}

	static final class Stopping extends State {

		static final State INST = new Stopping(STOPPING);

		private Stopping(int state) {
			super(state);
		}

		@Override
		public String toString() {
			return "Stopping";
		}
	}

	/**
	 * Start this service and the service state changes to {@code STARTED}.
	 * 
	 * @throws Exception
	 *             If this service failed to start.
	 */
	@Override
	public final void start() throws Exception {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.start(this);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Start this service with the specified {@code options} and the service
	 * state changes to {@code STARTED}.
	 * 
	 * @param options start options
	 * @throws Exception
	 *             If this service failed to start.
	 */
	@Override
	public final void start(int options) throws Exception {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.start(this, options);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Stop this service and the state changes to {@code STOPPED}.
	 */
	@Override
	public final void stop() {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.stop(this);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Stop this service with the specified {@code options} and the service
	 * state changes to {@code STOPPED}.
	 * 
	 * @param options stop options
	 */
	@Override
	public final void stop(int options) {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.stop(this, options);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * An empty implementation.
	 */
	@Override
	public void update(Map<String, ?> properties) throws Exception {
	}

	/**
	 * Return the current state of this service.
	 * 
	 * @return the current state
	 */
	@Override
	public final int state() {
		return m_state.state();
	}

	/**
	 * A callback method for a derived class to do some customized work for
	 * starting.
	 * 
	 * @throws Exception
	 *             If any error occurs
	 */
	protected abstract void startInternal() throws Exception;

	/**
	 * A callback method for a derived class to do some customized work for
	 * starting with options.  This default implementation just calls
	 * {@code startInternal()}.
	 *
	 * @param options start options
	 * @throws Exception 
	 *             If any error occurs
	 */
	protected void startInternal(int options) throws Exception {
		startInternal();
	}

	/**
	 * A callback method for a derived class to do some customized work for
	 * stopping.
	 */
	protected abstract void stopInternal();

	/**
	 * A callback method for a derived class to do some customized work for
	 * stopping with options.  This default implementation just calls
	 * {@code stopInternal()}.
	 *
	 * @param options stop options
	 */
	protected void stopInternal(int options) {
		stopInternal();
	}

	final void changeState(State state) {
		m_state = state;
	}
}
