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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.jruyi.common.IBufferFactory;
import org.jruyi.common.Service;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IFilter;
import org.jruyi.io.IoConstants;
import org.jruyi.io.SessionEvent;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.IChannelAdmin;
import org.jruyi.io.channel.IChannelService;
import org.jruyi.io.filter.IFilterManager;
import org.jruyi.io.tcp.TcpChannel;
import org.jruyi.io.tcp.TcpChannelConf;
import org.jruyi.me.IConsumer;
import org.jruyi.me.IEndpoint;
import org.jruyi.me.IMessage;
import org.jruyi.me.IProducer;
import org.jruyi.me.MeConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TcpClient extends Service implements IEndpoint,
		IConsumer, IChannelService {

	private static final Logger m_logger = LoggerFactory
			.getLogger(TcpClient.class);
	private String m_caption;
	private IProducer m_producer;
	private IChannelAdmin m_ca;
	private IFilterManager m_fm;
	private IBufferFactory m_bf;
	private IFilter[] m_filters;
	private boolean m_closed = true;
	private ConcurrentHashMap<Object, IChannel> m_channels;
	private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

	@Override
	public final Object getConfiguration() {
		return configuration();
	}

	@Override
	public IChannelAdmin getChannelAdmin() {
		return m_ca;
	}

	@Override
	public final IBufferFactory getBufferFactory() {
		return m_bf;
	}

	@Override
	public int readThreshold() {
		return configuration().readThreshold();
	}

	@Override
	public final IFilter[] getFilterChain() {
		return m_filters;
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		m_logger.debug("{}: CLOSED", channel);

		ConcurrentHashMap<Object, IChannel> channels = m_channels;
		if (channels != null)
			channels.remove(channel.id());
	}

	@Override
	public void onChannelException(IChannel channel, Throwable t) {
		try {
			IMessage message = (IMessage) channel.detach();
			if (message != null) {
				if (message.attachment() != null) {
					m_logger.error(StrUtil.buildString(this,
							" got an error on connecting: ", message), t);
					if (configuration().sessionEventMask().notifyConnError()) {
						fireSessionEvent(message, SessionEvent.CONN_ERROR);
						return;
					}
				} else {
					m_logger.error(StrUtil.buildString(this,
							" got an error on sending/recving: ", message), t);
					if (configuration().sessionEventMask().notifyRwError()) {
						fireSessionEvent(message, SessionEvent.RW_ERROR);
						return;
					}
				}

				message.close();
			}
		} finally {
			channel.close();
		}
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		m_logger.debug("{}: OPENED", channel);

		Object id = channel.id();
		final ReadLock readLock = m_lock.readLock();
		if (!readLock.tryLock()) {
			channel.close();
			return;
		}
		try {
			if (m_closed) {
				channel.close();
				return;
			}

			m_channels.put(id, channel);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void onChannelIdleTimedOut(IChannel channel) {
		m_logger.debug("{}: IDLE_TIMEOUT", channel);
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		IMessage message = (IMessage) channel.detach();
		if (message != null) {
			m_logger.warn(StrUtil.buildString(channel, ": CONNECT_TIMEOUT, ",
					message));

			if (configuration().sessionEventMask().notifyConnTimedout())
				fireSessionEvent(message, SessionEvent.CONN_TIMEDOUT);
			else
				message.close();
		}
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		IMessage message = (IMessage) channel.detach();
		if (message != null) {
			m_logger.warn(StrUtil.buildString(channel, ": READ_TIMEOUT, ",
					message));

			if (configuration().sessionEventMask().notifyConnTimedout())
				fireSessionEvent(message, SessionEvent.READ_TIMEDOUT);
			else
				message.close();
		}
	}

	@Override
	public final IConsumer consumer() {
		return this;
	}

	@Override
	public final String toString() {
		return m_caption;
	}

	@Override
	public final void producer(IProducer producer) {
		m_producer = producer;
	}

	@Override
	public void update(Map<String, ?> properties) throws Exception {
		TcpClientConf oldConf = updateConf(properties);
		TcpClientConf newConf = configuration();
		updateFilters(oldConf, newConf);

		if ((state() & (STOPPED | STOPPING)) != 0)
			return;

		if (oldConf.isMandatoryChanged(newConf, getMandatoryPropsAccessors())) {
			stop();
			start();
		}
	}

	@Override
	protected void startInternal() {
		m_closed = false;
		m_channels = new ConcurrentHashMap<Object, IChannel>(32);
	}

	@Override
	protected void stopInternal() {
		final WriteLock writeLock = m_lock.writeLock();
		writeLock.lock();
		try {
			m_closed = true;
		} finally {
			writeLock.unlock();
		}

		Collection<IChannel> channels = m_channels.values();
		m_channels = null;

		for (IChannel channel : channels)
			channel.close();
	}

	protected void setChannelAdmin(IChannelAdmin cm) {
		m_ca = cm;
	}

	protected void unsetChannelAdmin(IChannelAdmin cm) {
		m_ca = null;
	}

	protected void setFilterManager(IFilterManager fm) {
		m_fm = fm;
	}

	protected void unsetFilterManager(IFilterManager fm) {
		m_fm = null;
	}

	protected void setBufferFactory(IBufferFactory bf) {
		m_bf = bf;
	}

	protected void unsetBufferFactory(IBufferFactory bf) {
		if (m_bf == bf)
			m_bf = bf;
	}

	protected void activate(ComponentContext context, Map<String, ?> properties)
			throws Exception {
		String id = (String) properties.get(MeConstants.EP_ID);

		m_caption = StrUtil.buildString("TcpClient[", id, "]");
		updateFilters(updateConf(properties), configuration());

		start();
	}

	protected void deactivate() {
		stop();

		updateFilters(updateConf(null), null);
	}

	abstract String getFactoryPid();

	abstract TcpClientConf updateConf(Map<String, ?> props);

	abstract TcpClientConf configuration();

	Method[] getMandatoryPropsAccessors() {
		return TcpClientConf.getMandatoryPropsAccessors();
	}

	final void connect() {
		new TcpChannel(this).connect(configuration().connectTimeout());
	}

	final void connect(Object attachment) {
		TcpChannel channel = new TcpChannel(this);
		channel.attach(attachment);
		channel.connect(configuration().connectTimeout());
	}

	final void enqueue(IMessage message) {
		m_producer.send(message);
	}

	final IProducer producer() {
		return m_producer;
	}

	private void updateFilters(TcpChannelConf oldConf, TcpChannelConf newConf) {
		String[] newNames = newConf == null ? StrUtil.getEmptyStringArray()
				: newConf.filters();
		String[] oldNames = StrUtil.getEmptyStringArray();
		IFilterManager fm = m_fm;
		if (oldConf == null)
			m_filters = fm.getFilters(oldNames);
		else
			oldNames = oldConf.filters();

		if (Arrays.equals(newNames, oldNames))
			return;

		m_filters = fm.getFilters(newNames);
		fm.ungetFilters(oldNames);
	}

	private void fireSessionEvent(IMessage message, SessionEvent event) {
		IProducer producer = m_producer;
		message.putProperty(IoConstants.MP_SESSION_EVENT, event);
		producer.send(message);
	}

}
