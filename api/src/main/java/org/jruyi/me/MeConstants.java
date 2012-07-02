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
package org.jruyi.me;

/**
 * Defines standard names for Message Engine's constants.
 */
public final class MeConstants {

	/**
	 * Property name of end point ID.
	 */
	public static final String EP_ID = "jruyi.me.endpoint.id";
	/**
	 * Property name of prehandler chain.
	 */
	public static final String EP_PREHANDLERS = "jruyi.me.endpoint.prehandlers";
	/**
	 * Property name of posthandler chain.
	 */
	public static final String EP_POSTHANDLERS = "jruyi.me.endpoint.posthandlers";

	/**
	 * Property name of handler ID.
	 */
	public static final String HANDLER_ID = "jruyi.me.handler.id";

	/**
	 * Message property name of message engine event.
	 */
	public static final String MP_EVENT = "jruyi.me.event";
	/**
	 * Message property name of message engine error.
	 */
	public static final String MP_ERROR = "jruyi.me.error";

	/**
	 * Route not found error.
	 */
	public static final String E_ROUTE_NOT_FOUND = "E_ROUTE_NOT_FOUND";
	/**
	 * Consumer not found error.
	 */
	public static final String E_CONSUMER_NOT_FOUND = "E_CONSUMER_NOT_FOUND";

	private MeConstants() {
	}
}
