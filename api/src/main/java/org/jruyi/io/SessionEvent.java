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
 * Defines session events.
 */
public enum SessionEvent {

	/**
	 * Indicate that session is opened.
	 */
	OPENED,
	/**
	 * Indicate that session is closed.
	 */
	CLOSED,
	/**
	 * Indicate that connect operation timed out.
	 */
	CONN_TIMEDOUT,
	/**
	 * Indicate that read operation timed out.
	 */
	READ_TIMEDOUT,
	/**
	 * Indicate an IO error on connecting.
	 */
	CONN_ERROR,
	/**
	 * Indicate an IO error on reading or writing.
	 */
	RW_ERROR;
}
