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
 * Service for creating notifiers.
 */
public interface ITimeoutAdmin {

	/**
	 * Create a notifier with the specified {@code subject}.
	 * 
	 * @param subject
	 *            the subject of the timeout event sent by this notifier
	 * @return a notifier object
	 */
	public ITimeoutNotifier createNotifier(Object subject);
}
