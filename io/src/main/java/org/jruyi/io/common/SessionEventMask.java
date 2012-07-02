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
package org.jruyi.io.common;

import org.jruyi.io.SessionEvent;


public final class SessionEventMask {

	private static final int M_OPENED = 0x0001;
	private static final int M_CLOSED = 0x0002;
	private static final int M_CONN_TIMEDOUT = 0x0004;
	private static final int M_READ_TIMEDOUT = 0x0008;
	private static final int M_CONN_ERROR = 0x0010;
	private static final int M_RW_ERROR = 0x0020;
	private static final String[] EVENT_NAMES = {
		SessionEvent.OPENED.name(),
		SessionEvent.CLOSED.name(),
		SessionEvent.CONN_TIMEDOUT.name(),
		SessionEvent.READ_TIMEDOUT.name(),
		SessionEvent.CONN_ERROR.name(),
		SessionEvent.RW_ERROR.name(),
	};
	private static final int[] EVENT_VALS = {
		M_OPENED,
		M_CLOSED,
		M_CONN_TIMEDOUT,
		M_READ_TIMEDOUT,
		M_CONN_ERROR,
		M_RW_ERROR,
	};
	private final int m_mask;

	public SessionEventMask(String[] sessionEvents) {
		int mask = 0;
		if (sessionEvents != null) {
			for (String event : sessionEvents) {
				for (int i = 0; i < EVENT_NAMES.length; ++i)
					if (EVENT_NAMES[i].equals(event))
						mask |= EVENT_VALS[i];
			}
		}
		m_mask = mask;
	}

	public boolean notifyOpened() {
		return (m_mask & M_OPENED) != 0;
	}

	public boolean notifyClosed() {
		return (m_mask & M_CLOSED) != 0;
	}

	public boolean notifyConnTimedout() {
		return (m_mask & M_CONN_TIMEDOUT) != 0;
	}

	public boolean notifyReadTimedout() {
		return (m_mask & M_READ_TIMEDOUT) != 0;
	}

	public boolean notifyConnError() {
		return (m_mask & M_CONN_ERROR) != 0;
	}

	public boolean notifyRwError() {
		return (m_mask & M_RW_ERROR) != 0;
	}
}
