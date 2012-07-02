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
package org.jruyi.me.mq;

import org.jruyi.common.IServiceHolder;
import org.jruyi.me.IMessage;
import org.jruyi.me.IPreHandler;

final class PreHandlerDelegator implements IPreHandler {

	private final IServiceHolder<IPreHandler> m_holder;

	PreHandlerDelegator(IServiceHolder<IPreHandler> holder) {
		m_holder = holder;
	}

	@Override
	public boolean preHandle(IMessage message) {
		return m_holder.getService().preHandle(message);
	}
}
