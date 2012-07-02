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
package org.jruyi.io.tcpclient;

import java.lang.reflect.Method;
import java.util.Map;

import org.jruyi.common.ICloseable;
import org.jruyi.common.StrUtil;
import org.jruyi.io.channel.IChannel;
import org.jruyi.me.IMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShortConn extends TcpClient {

	private static final Logger m_logger = LoggerFactory.getLogger(ShortConn.class);
	private static final Method[] EMTPY_MANDATORY_PROPS = new Method[0];
	private TcpClientConf m_conf;

	@Override
	public void onMessage(IMessage message) {
		Object data = message.attachment();
		if (data == null) {
			m_logger.warn(StrUtil.buildString(this,
					" consumes a null message: ", message));

			message.close();
			return;
		}

		connect(message);
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		super.onChannelOpened(channel);

		IMessage message = (IMessage) channel.attachment();
		channel.write(message.detach(), false);
	}

	@Override
	public void onMessageSent(IChannel channel, Object data) {
		int timeout = m_conf.readTimeout();
		if (timeout > 0)
			channel.scheduleReadTimeout(timeout);
		else if (timeout == 0)
			channel.close();
	}

	@Override
	public void onMessageReceived(IChannel channel, Object data) {
		if (channel.cancelTimeout()) {
			IMessage message = (IMessage) channel.detach();
			channel.close();

			if (message != null) {
				message.attach(data);
				enqueue(message);
			}
		} else if (data instanceof ICloseable)
			// if false, channel has timed out.
			((ICloseable) data).close();
	}

	@Override
	public void onChannelIdleTimedOut(IChannel channel) {
		super.onChannelIdleTimedOut(channel);
		channel.close();
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		super.onChannelConnectTimedOut(channel);
		channel.close();
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		super.onChannelReadTimedOut(channel);
		channel.close();
	}

	@Override
	public void startInternal() {
		m_logger.info(StrUtil.buildString("Starting ", this, "..."));

		super.startInternal();

		m_logger.info(StrUtil.buildString(this, " started"));
	}

	@Override
	public void stopInternal() {
		m_logger.info(StrUtil.buildString("Stopping ", this, "..."));

		super.stopInternal();

		m_logger.info(StrUtil.buildString(this, " stopped"));
	}

	@Override
	String getFactoryPid() {
		return "org.jruyi.io.tcpclient.shortconn";
	}

	@Override
	TcpClientConf configuration() {
		return m_conf;
	}

	@Override
	Method[] getMandatoryPropsAccessors() {
		return EMTPY_MANDATORY_PROPS;
	}

	@Override
	TcpClientConf updateConf(Map<String, ?> props) {
		TcpClientConf conf = m_conf;
		if (props == null)
			m_conf = null;
		else {
			TcpClientConf newConf = new TcpClientConf();
			newConf.initialize(props);
			m_conf = newConf;
		}

		return conf;
	}
}
