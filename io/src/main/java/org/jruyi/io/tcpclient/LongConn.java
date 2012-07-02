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

import java.util.Map;

import org.jruyi.common.ICloseable;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IoConstants;
import org.jruyi.io.SessionAction;
import org.jruyi.io.SessionEvent;
import org.jruyi.io.channel.IChannel;
import org.jruyi.me.IMessage;
import org.jruyi.me.IProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LongConn extends TcpClient {

	private static final Logger m_logger = LoggerFactory
			.getLogger(LongConn.class);
	private TcpClientConf m_conf;

	@Override
	public void onMessage(IMessage message) {
		Object action = message.removeProperty(IoConstants.MP_SESSION_ACTION);
		if (action != null) {
			if (action == SessionAction.OPEN) {
				connect(message);
			} else {
				if (action == SessionAction.CLOSE) {
					IChannel channel = getChannel(message);
					if (channel != null)
						channel.close();
				} else
					m_logger.warn(StrUtil.buildString(this,
							", Unknown Session Action: ", action));

				message.close();
			}

			return;
		}

		IChannel channel = getChannel(message);
		if (channel == null) {
			m_logger.warn(StrUtil.buildString(this, ", Session["
					+ IoConstants.MP_ACTIVE_SESSION + "] Not Found: ", message));
			return;
		}

		channel.write(message.detach(), false);
		message.close();
	}

	@Override
	public void onMessageSent(IChannel channel, Object data) {
		int timeout = m_conf.readTimeout();
		if (timeout > 0)
			channel.scheduleReadTimeout(timeout);
		else if (timeout == 0)
			onChannelReadTimedOut(channel);
	}

	@Override
	public void onMessageReceived(IChannel channel, Object data) {
		if (channel.cancelTimeout())
			enqueue(channel, data);
		else if (data instanceof ICloseable)
			// channel has timed out
			((ICloseable) data).close();
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		super.onChannelOpened(channel);

		fireSessionEvent(channel, SessionEvent.OPENED,
				(IMessage) channel.detach());
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		super.onChannelClosed(channel);

		fireSessionEvent(channel, SessionEvent.CLOSED);
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		super.onChannelConnectTimedOut(channel);

		fireSessionEvent(channel, SessionEvent.CONN_TIMEDOUT);
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		super.onChannelReadTimedOut(channel);

		fireSessionEvent(channel, SessionEvent.READ_TIMEDOUT);
	}

	@Override
	public void onChannelException(IChannel channel, Throwable t) {
		IMessage message = (IMessage) channel.detach();
		if (message != null) {
			if (channel != getChannel(message)) {
				m_logger.error(StrUtil.buildString(this,
						" got an error on connecting: ", message), t);
				if (configuration().sessionEventMask().notifyConnError()) {
					fireSessionEvent(null, SessionEvent.CONN_ERROR, message);
					return;
				}
			} else {
				m_logger.error(StrUtil.buildString(this,
						" got an error on sending/recving: ", message), t);
				if (configuration().sessionEventMask().notifyRwError()) {
					fireSessionEvent(null, SessionEvent.RW_ERROR, message);
					return;
				}
			}

			message.close();
		}
	}

	@Override
	public void startInternal() {
		m_logger.info(StrUtil.buildString("Starting ", this, "..."));

		super.startInternal();

		m_logger.info(StrUtil.buildString(this, " started"));
	}

	@Override
	public final void stopInternal() {
		m_logger.info(StrUtil.buildString("Stopping ", this, "..."));

		super.stopInternal();

		m_logger.info(StrUtil.buildString(this, " stopped"));
	}

	@Override
	TcpClientConf configuration() {
		return m_conf;
	}

	@Override
	String getFactoryPid() {
		return "org.jruyi.io.tcpclient.longconn";
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

	private void enqueue(IChannel channel, Object data) {
		IProducer producer = producer();
		IMessage message = producer.createMessage();
		message.putProperty(IoConstants.MP_ACTIVE_SESSION, channel);
		message.attach(data);

		producer.send(message);
	}

	private IChannel getChannel(IMessage message) {
		return (IChannel) message.getProperty(IoConstants.MP_ACTIVE_SESSION);
	}

	private void fireSessionEvent(IChannel channel, SessionEvent event) {
		IProducer producer = producer();
		IMessage message = producer.createMessage();
		message.putProperty(IoConstants.MP_SESSION_EVENT, event);
		message.putProperty(IoConstants.MP_ACTIVE_SESSION, channel);

		producer.send(message);
	}

	private void fireSessionEvent(IChannel channel, SessionEvent event,
			IMessage message) {
		message.putProperty(IoConstants.MP_SESSION_EVENT, event);
		if (channel != null)
			message.putProperty(IoConstants.MP_ACTIVE_SESSION, channel);

		producer().send(message);
	}
}
