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
package org.jruyi.timeoutadmin;

/**
 * A {@code ITimeoutNotifier} is used to schedule a timeout notification of the
 * interested <i>subject</i>. It has 4 states in all. They are Unscheduled,
 * Scheduled, TimedOut and Closed. The state transitions are listed below.
 * 
 * <pre>
 * [Unscheduled] --(schedule)-> [Scheduled]
 * [Scheduled] --(cancel)-> [Unscheduled]
 * [Scheduled] --(timeout)-> [TimedOut]
 * [TimedOut] --(reset)-> [Unscheduled]
 * [Scheduled, Unscheduled, TimedOut] --(close)-> [Closed]
 * </pre>
 * 
 * When {@code ITimeoutNotifier} is created, the initial state is Unscheduled.
 * Except close event, when 2 more events come concurrently, only one will be
 * taken. All the others will fail fast. As to which one will be taken, it
 * depends on whose thread acquires the lock.
 * 
 * <p>
 * When {@code ITimeoutNotifier} goes into state TimedOut, <i>schedule</i>/
 * <i>cancel</i> will not work until it gets <i>reset</i>.
 * 
 * <p>
 * After {@code ITimeoutNotifier} closes, it will not work anymore.
 */
public interface ITimeoutNotifier {

	/**
	 * An {@code int} value representing Unscheduled state.
	 */
	public static final int UNSCHEDULED = 0x01;
	/**
	 * An {@code int} value representing Scheduled state.
	 */
	public static final int SCHEDULED = 0x02;
	/**
	 * An {@code int} value representing Timedout state.
	 */
	public static final int TIMEDOUT = 0x04;
	/**
	 * An {@code int} value representing Closed state.
	 */
	public static final int CLOSED = 0x08;

	/**
	 * Return the subject this notifier concerns.
	 * 
	 * @return the subject
	 */
	public Object getSubject();

	/**
	 * Return the current state of this notifier.
	 * 
	 * @return the current state of this notifier
	 */
	public int state();

	/**
	 * Schedule a notification to be sent out in {@code timeout} seconds. The
	 * previous schedule will be dropped.
	 * 
	 * @param timeout
	 *            time in seconds in which the notifier will be sent
	 * @return false if this notifier timed out or is closed, otherwise true
	 * @throws IllegalArgumentException
	 *             if {@code timeout} is not positive
	 */
	public boolean schedule(int timeout);

	/**
	 * Cancel the notifier.
	 * 
	 * @return false if either timeout or closed, otherwise true
	 */
	public boolean cancel();

	/**
	 * Reset this notifier to be able to be scheduled again if it timed out.
	 * 
	 * @return true if this notifier timed out, otherwise false
	 */
	public boolean reset();

	/**
	 * Close this notifier.
	 */
	public void close();

	/**
	 * Set the listener that is interested in the notification.
	 * 
	 * @param listener
	 *            the notification receiver
	 */
	public void setListener(ITimeoutListener listener);
}
