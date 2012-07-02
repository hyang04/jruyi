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
 * Service for handling messages leaving the message queue.
 */
public interface IPreHandler {

	/**
	 * Handle the specified {@code message}.
	 * <p>
	 * If this method returns false, then the specified {@code message} will be
	 * dropped immediately.
	 * 
	 * @param message
	 *            the message leaving the message queue
	 * @return true if no errors, otherwise false
	 */
	public boolean preHandle(IMessage message);
}
