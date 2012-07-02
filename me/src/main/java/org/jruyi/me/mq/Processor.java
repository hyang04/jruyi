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

import org.jruyi.common.StrUtil;
import org.jruyi.me.IMessage;
import org.jruyi.me.IProcessor;
import org.osgi.framework.ServiceReference;

final class Processor extends Endpoint {

	private final ServiceReference m_reference;
	private IProcessor m_processor;

	Processor(String id, MessageQueue mq, ServiceReference reference) {
		super(id, mq);
		m_reference = reference;
	}

	@Override
	public void onMessage(IMessage message) {
		processor().process(message);
		send(message);
	}

	private IProcessor processor() {
		IProcessor processor = m_processor;
		if (processor == null) {
			processor = mq().locateProcessor(m_reference);
			if (processor == null)
				throw new RuntimeException(StrUtil.buildString(this,
						" is unavailable"));

			m_processor = processor;
		}

		return processor;
	}
}
