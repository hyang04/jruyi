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

import org.jruyi.common.IBufferReader;

/**
 * This abstract class provides pass-through implementation of {@link IFilter}.
 */
public abstract class Filter implements IFilter {

	/**
	 * Return the current length of the given {@code in} as the message
	 * boundary.
	 * 
	 * @param session
	 *            the current IO session.
	 * @param in
	 *            the {@link IBufferReader} holding the available data.
	 * @return the message length
	 */
	@Override
	public int tellBoundary(ISession session, IBufferReader in) {
		return in.length();
	}

	/**
	 * Pass through the given {@code msg}.
	 * 
	 * @param session
	 *            the current IO session
	 * @param msg
	 *            the incoming message
	 * @param output
	 *            an {@link IFilterOutput} object used to pass the output to the
	 *            next filter in the filter chain
	 * @return true
	 */
	@Override
	public boolean onMsgArrive(ISession session, Object msg, IFilterOutput output) {
		output.put(msg);
		return true;
	}

	/**
	 * Pass through the given {@code msg}.
	 * 
	 * @param session
	 *            the current IO session
	 * @param msg
	 *            the outgoing message
	 * @param output
	 *            an {@link IFilterOutput} object used to pass the output to the
	 *            previous filter in the filter chain
	 * @return true
	 */
	@Override
	public boolean onMsgDepart(ISession session, Object msg, IFilterOutput output) {
		output.put(msg);
		return true;
	}
}
