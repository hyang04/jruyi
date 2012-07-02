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
package org.jruyi.io;

/**
 * Defines standard names for IO constants.
 */
public final class IoConstants {

	/**
	 * The service property name of filter ID.
	 */
	public static final String FILTER_ID = "jruyi.io.filter.id";
	/**
	 * The service property name of SSLContextInfo ID.
	 */
	public static final String SSLCI_ID = "jruyi.io.sslci.id";

	/**
	 * The message property name of session event.
	 */
	public static final String MP_SESSION_EVENT = "jruyi.io.sessionEvent";
	/**
	 * The message property name of session action.
	 */
	public static final String MP_SESSION_ACTION = "jruyi.io.sessionAction";
	/**
	 * The message property name of passive session.
	 */
	public static final String MP_PASSIVE_SESSION = "jruyi.io.passiveSession";
	/**
	 * The message property name of active session.
	 */
	public static final String MP_ACTIVE_SESSION = "jruyi.io.activeSession";

	private IoConstants() {
	}
}
