/**
 * Copyright 2012 JRuyi.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use file except in compliance with the License.
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
package org.jruyi.io.tcpserver;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
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
import org.jruyi.io.IoConstants;
import org.jruyi.io.SessionAction;
import org.jruyi.io.SessionEvent;
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

public final class TcpServer extends Service implements IChannelService,
		IConsumer, IEndpoint {

	private static final Logger m_logger = LoggerFactory
			.getLogger(TcpServer.class);
	private String m_caption;
	private Configuration m_conf;
	private IProducer m_producer;
	private ServerSocketChannel m_ssc;
	private IChannelAdmin m_ca;
	private ITcpAcceptor m_acceptor;
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
		return m_conf.readThreshold();
	}

	@Override
	public IFilter[] getFilterChain() {
		return m_filters;
	}

	@Override
	public void onChannelIdleTimedOut(IChannel channel) {
		m_logger.debug("{}: IDLE_TIMEOUT", channel);
		channel.close();
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onChannelException(IChannel channel, Throwable t) {
		channel.close();
		m_logger.error(StrUtil.buildString(channel, " got an error"), t);
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

		// failed to schedule, channel has been closed
		if (!scheduleIdleTimeout(channel))
			return;

		if (m_conf.sessionEventMask().notifyOpened())
			fireSessionEvent(channel, SessionEvent.OPENED);
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		m_logger.debug("{}: CLOSED", channel);

		ConcurrentHashMap<Object, IChannel> channels = m_channels;
		if (channels != null)
			channels.remove(channel.id());

		if (m_conf.sessionEventMask().notifyClosed())
			fireSessionEvent(channel, SessionEvent.CLOSED);
	}

	@Override
	public void onMessageReceived(IChannel channel, Object data) {
		// failed to reschedule, channel timed out
		if (!scheduleIdleTimeout(channel))
			return;

		enqueue(channel, data);
	}

	@Override
	public void onMessageSent(IChannel channel, Object data) {
	}

	@Override
	public void onMessage(IMessage message) {
		try {
			Object data = message.detach();
			if (data == null) {
				checkAction(message);
				return;
			}

			IChannel channel = getChannel(message);
			if (channel == null) {
				m_logger.warn(StrUtil.buildString(this,
						" failed to send(channel closed): ", message));

				if (data instanceof ICloseable)
					((ICloseable) data).close();

				// TODO: need notify failure on writing out?
				return;
			}

			channel.write(data, needCloseChannel(message));
		} finally {
			message.close();
		}
	}

	@Override
	public void producer(IProducer producer) {
		m_producer = producer;
	}

	@Override
	public IConsumer consumer() {
		return this;
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

		if (m_channels == null)
			m_channels = new ConcurrentHashMap<Object, IChannel>(
				conf.initCapacityOfChannelMap());

		ServerSocketChannel ssc = ServerSocketChannel.open();
		try {
			ServerSocket socket = ssc.socket();
			initSocket(socket, conf);
			SocketAddress ep = new InetSocketAddress(bindAddr, conf.port());
			Integer backlog = conf.backlog();
			if (backlog == null)
				socket.bind(ep);
			else
				socket.bind(ep, backlog);

			m_ssc = ssc;

			m_acceptor.doAccept(this);

			m_logger.info(StrUtil.buildString(this, " started, listening on ",
					socket.getLocalSocketAddress()));
		} catch (Exception e) {
			try {
				ssc.close();
			} catch (Exception e2) {
			}
			m_ssc = null;
			throw e;
		}
	}

	@Override
	protected void stopInternal() {
		stopInternal(0);
	}

	@Override
	protected void stopInternal(int options) {
		m_logger.info(StrUtil.buildString("Stopping ", this, "..."));

		try {
			m_ssc.close();
		} catch (Exception e) {
			m_logger.error(StrUtil.buildString(this,
					" failed to close ServerSocketChannel"), e);
		}
		m_ssc = null;

		final WriteLock writeLock = m_lock.writeLock();
		writeLock.lock();
		try {
			m_closed = true;
		} finally {
			writeLock.unlock();
		}

		if (options == 0) 
			closeChannels();

		m_logger.info(StrUtil.buildString(this, " stopped"));
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

	protected void setChannelAdmin(IChannelAdmin ca) {
		m_ca = ca;
	}

	protected void unsetChannelAdmin(IChannelAdmin ca) {
		m_ca = null;
	}

	protected void setTcpAcceptor(ITcpAcceptor acceptor) {
		m_acceptor = acceptor;
	}

	protected void unsetTcpAcceptor(ITcpAcceptor acceptor) {
		m_acceptor = null;
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

		m_caption = StrUtil.buildString("TcpServer[", id, "]");

		Configuration conf = new Configuration();
		conf.initialize(properties);
		updateConf(conf);

		start();
	}

	protected void deactivate() {
		stop();

		closeChannels();

		updateConf(null);
	}

	SelectableChannel getSelectableChannel() {
		return m_ssc;
	}

	private IChannel getChannel(IMessage message) {
		Object channelId = message.withdraw(this);
		if (channelId != null)
			return m_channels.get(channelId);

		return (IChannel) message.getProperty(IoConstants.MP_PASSIVE_SESSION);
	}

	private boolean needCloseChannel(IMessage message) {
		Object action = message.removeProperty(IoConstants.MP_SESSION_ACTION);
		return (action == SessionAction.CLOSE);
	}

	private boolean scheduleIdleTimeout(IChannel channel) {
		int timeout = m_conf.sessionIdleTimeout();
		if (timeout > 0)
			return channel.scheduleIdleTimeout(timeout);

		if (timeout == 0)
			channel.close();

		return true;
	}

	private void checkAction(IMessage message) {
		if (!needCloseChannel(message))
			return;

		IChannel channel = getChannel(message);
		if (channel != null)
			channel.close();
	}

	private void fireSessionEvent(IChannel channel, SessionEvent event) {
		IProducer producer = m_producer;
		IMessage message = producer.createMessage();
		message.deposit(this, channel.id());

		message.putProperty(IoConstants.MP_SESSION_EVENT, event);
		message.putProperty(IoConstants.MP_PASSIVE_SESSION, channel);

		producer.send(message);
	}

	private void enqueue(IChannel channel, Object data) {
		IProducer producer = m_producer;
		IMessage message = producer.createMessage();
		message.deposit(this, channel.id());

		message.putProperty(IoConstants.MP_PASSIVE_SESSION, channel);
		message.attach(data);

		producer.send(message);
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

	private void closeChannels() {
		if (m_channels == null)
			return;

		if (m_channels.size() > 0) {
			Collection<IChannel> channels = m_channels.values();
			for (IChannel channel : channels)
				channel.close();
		}

		m_channels = null;
	}

	private static void initSocket(ServerSocket socket, Configuration conf)
			throws SocketException {
		Integer[] performancePreferences = conf.performancePreferences();
		if (performancePreferences != null) {
			int n = performancePreferences.length;
			int connectionTime = 0;
			int latency = 0;
			int bandWidth = 0;
			if (n > 2) {
				connectionTime = performancePreferences[0];
				latency = performancePreferences[1];
				bandWidth = performancePreferences[2];
			} else if (n > 1) {
				connectionTime = performancePreferences[0];
				latency = performancePreferences[1];
			} else if (n > 0)
				connectionTime = performancePreferences[0];

			socket.setPerformancePreferences(connectionTime, latency, bandWidth);
		}

		if (conf.reuseAddr())
			socket.setReuseAddress(true);

		Integer recvBufSize = conf.recvBufSize();
		if (recvBufSize != null)
			socket.setReceiveBufferSize(recvBufSize);
	}
}
