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
package org.jruyi.io.udpserver;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.jruyi.common.IBufferFactory;
import org.jruyi.common.ICloseable;
import org.jruyi.common.Service;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IFilter;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.IChannelAdmin;
import org.jruyi.io.channel.IChannelService;
import org.jruyi.io.filter.IFilterManager;
import org.jruyi.me.IConsumer;
import org.jruyi.me.IEndpoint;
import org.jruyi.me.IMessage;
import org.jruyi.me.IProducer;
import org.jruyi.me.MeConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UdpServer extends Service implements IChannelService,
		IConsumer, IEndpoint {

	private static final Logger m_logger = LoggerFactory.getLogger(UdpServer.class);
	private String m_caption;
	private Configuration m_conf;
	private IProducer m_producer;
	private DatagramChannel m_datagramChannel;
	private IChannelAdmin m_ca;
	private IFilterManager m_fm;
	private IBufferFactory m_bf;
	private IFilter[] m_filters;
	private boolean m_closed;
	private ConcurrentHashMap<Object, IChannel> m_channels;
	private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

	@Override
	public Object getConfiguration() {
		return m_conf;
	}

	@Override
	public IChannelAdmin getChannelAdmin() {
		return m_ca;
	}

	@Override
	public IBufferFactory getBufferFactory() {
		return m_bf;
	}

	@Override
	public int readThreshold() {
		return 0;
	}

	@Override
	public IFilter[] getFilterChain() {
		return m_filters;
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		m_logger.debug("{}: CLOSED", channel);

		m_channels.remove(channel.remoteAddress());
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onChannelException(IChannel channel, Throwable t) {
		channel.close();
		m_logger.error(StrUtil.buildString(channel, " got an error"), t);
	}

	@Override
	public void onChannelIdleTimedOut(IChannel channel) {
		m_logger.debug("{}: IDLE_TIMEOUT", channel);

		channel.close();
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		m_logger.debug("{}: OPENED", channel);

		Object key = channel.remoteAddress();
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

			m_channels.put(key, channel);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onMessageReceived(IChannel channel, Object data) {
		// failed to reschedule, channel timed out
		if (!scheduleIdleTimeout(channel))
			return;

		IProducer producer = m_producer;
		IMessage message = producer.createMessage();
		Object id = channel.remoteAddress();
		message.deposit(this, id);

		message.attach(data);

		producer.send(message);
	}

	@Override
	public void onMessageSent(IChannel channel, Object data) {
	}

	@Override
	public void onMessage(IMessage message) {
		try {
			Object data = message.detach();
			IChannel channel = getChannel(message);
			if (channel == null) {
				m_logger.warn(StrUtil.buildString(this,
						" failed to send(channel closed): ", message));

				if (data instanceof ICloseable)
					((ICloseable) data).close();

				return;
			}

			channel.write(data, false);
		} finally {
			message.close();
		}
	}

	@Override
	public IConsumer consumer() {
		return this;
	}

	@Override
	public void producer(IProducer producer) {
		m_producer = producer;
	}

	@Override
	public String toString() {
		return m_caption;
	}

	@Override
	public void update(Map<String, ?> properties) throws Exception {

		Configuration newConf = new Configuration();
		newConf.initialize(properties);

		Configuration oldConf = m_conf;
		updateConf(newConf);

		if ((state() & (STOPPED | STOPPING)) != 0)
			return;

		if (oldConf.isMandatoryChanged(newConf,
				Configuration.getMandatoryPropsAccessors())) {
			stop();
			start();
		}
	}

	@Override
	protected void startInternal() throws Exception {
		m_logger.info(StrUtil.buildString("Starting ", this, "..."));

		m_closed = false;

		Configuration conf = m_conf;
		InetAddress bindAddr = null;
		String host = conf.bindAddr();
		if (host != null)
			bindAddr = InetAddress.getByName(host);

		m_channels = new ConcurrentHashMap<Object, IChannel>(
				conf.initCapacityOfChannelMap());

		SocketAddress localAddr = null;
		DatagramChannel datagramChannel = DatagramChannel.open();
		try {
			DatagramSocket socket = datagramChannel.socket();
			initSocket(socket, conf);
			localAddr = new InetSocketAddress(bindAddr, conf.port());
			socket.bind(localAddr);
			datagramChannel.configureBlocking(false);
		} catch (Exception e) {
			try {
				datagramChannel.close();
			} catch (Exception e2) {
			}
			m_datagramChannel = null;
			m_channels = null;
			throw e;
		}

		m_datagramChannel = datagramChannel;
		m_ca.onRegisterRequired(new UdpServerChannel(this, datagramChannel,
				localAddr));

		m_logger.info(StrUtil.buildString(this, " started: ", conf.port()));
	}

	@Override
	protected void stopInternal() {
		m_logger.info(StrUtil.buildString("Stopping ", this, "..."));

		try {
			m_datagramChannel.close();
		} catch (Exception e) {
			m_logger.error(StrUtil.buildString(this,
					" failed to close DatagramChannel"), e);
		}

		final WriteLock writeLock = m_lock.writeLock();
		writeLock.lock();
		try {
			m_closed = true;
		} finally {
			writeLock.unlock();
		}

		Collection<IChannel> channels = m_channels.values();
		for (IChannel channel : channels)
			channel.close();

		m_channels = null;

		m_datagramChannel = null;

		m_logger.info(StrUtil.buildString(this, " stopped"));
	}

	protected void setChannelAdmin(IChannelAdmin ca) {
		m_ca = ca;
	}

	protected void unsetChannelAdmin(IChannelAdmin ca) {
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
			m_bf = null;
	}

	protected void activate(ComponentContext context, Map<String, ?> properties)
			throws Exception {
		String id = (String) properties.get(MeConstants.EP_ID);
		m_caption = StrUtil.buildString("UdpServer[", id, "]");
		Configuration conf = new Configuration();
		conf.initialize(properties);
		updateConf(conf);

		start();
	}

	protected void deactivate() {
		stop();

		updateConf(null);
	}

	IChannel getChannel(SocketAddress key) {
		return m_channels.get(key);
	}

	private static void initSocket(DatagramSocket socket, Configuration conf)
			throws SocketException {

		socket.setReuseAddress(true);

		Integer recvBufSize = conf.recvBufSize();
		if (recvBufSize != null)
			socket.setReceiveBufferSize(recvBufSize);
	}

	private void updateConf(Configuration newConf) {
		String[] newNames = newConf == null ? StrUtil.getEmptyStringArray()
				: newConf.filters();
		String[] oldNames = StrUtil.getEmptyStringArray();
		IFilterManager fm = m_fm;
		if (m_conf == null)
			m_filters = fm.getFilters(oldNames);
		else
			oldNames = m_conf.filters();

		m_conf = newConf;

		if (Arrays.equals(newNames, oldNames))
			return;

		m_filters = fm.getFilters(newNames);
		fm.ungetFilters(oldNames);
	}

	private IChannel getChannel(IMessage message) {
		Object key = message.withdraw(this);
		return key == null ? null : m_channels.get(key);
	}

	private boolean scheduleIdleTimeout(IChannel channel) {
		int timeout = m_conf.sessionIdleTimeout();
		if (timeout > 0)
			return channel.scheduleIdleTimeout(timeout);

		if (timeout == 0)
			channel.close();

		return true;
	}
}
