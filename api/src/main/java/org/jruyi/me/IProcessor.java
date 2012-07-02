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
 * Service for processing messages.
 * <p>
 * A processor is an end point unable to produce new messages but only to
 * process and forward messages. The processed messages will be put back to the
 * message queue for routing.
 */
public interface IProcessor {

	/**
	 * Process the specified {@code message}.
	 * 
	 * @param message
	 *            the message to be processed
	 */
	public void process(IMessage message);
}
