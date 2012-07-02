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
package org.jruyi.me.handler;

import java.util.Dictionary;

import org.jruyi.common.Properties;
import org.jruyi.common.StringBuilder;
import org.jruyi.me.IMessage;
import org.jruyi.me.IPostHandler;
import org.jruyi.me.IPreHandler;
import org.jruyi.me.MeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MsgLog implements IPreHandler, IPostHandler {

	private static final Logger m_logger = LoggerFactory.getLogger(MsgLog.class);

	public static String[] getInterfaces() {
		return new String[]{IPreHandler.class.getName(), IPostHandler.class.getName()};
	}

	public static Dictionary<String, ?> getProperties() {
		Properties properties = new Properties();
		properties.put(MeConstants.HANDLER_ID, "org.jruyi.me.handler.msglog");
		return properties;
	}

	@Override
	public boolean preHandle(IMessage message) {
		String s = null;
		StringBuilder builder = StringBuilder.get();
		try {
			s = builder.append("Endpoint[").append(message.to()).
					append("], dequeue:").append(message).toString();
		} finally {
			builder.close();
		}

		m_logger.info(s);

		return true;
	}

	@Override
	public boolean postHandle(IMessage message) {

		String s = null;
		StringBuilder builder = StringBuilder.get();
		try {
			s = builder.append("Endpoint[").append(message.from()).
					append("], enqueue:").append(message).toString();
		} finally {
			builder.close();
		}

		m_logger.info(s);

		return true;
	}
}
