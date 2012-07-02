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
package org.jruyi.io.udpclient;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
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

public final class UdpClient extends Service implements IChannelService,
		IConsumer, IEndpoint {

	private static final Logger m_logger = LoggerFactory.getLogger(UdpClient.class);
	private String m_caption;
	private Configuration m_conf;
	private IProducer m_producer;
	private IChannelAdmin m_ca;
	private IFilterManager m_fm;
	private IBufferFactory m_bf;
	private IFilter[] m_filters;
	private boolean m_closed = true;
	private volatile IChannel m_channel;
	private final ReentrantLock m_channelLock = new ReentrantLock();
	private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

	@Override
	public void producer(IProducer producer) {
		m_producer = producer;
	}

	@Override
	public IConsumer consumer() {
		return this;
	}

	@Override
	public void onMessage(IMessage message) {
		Object data = message.detach();
		if (data == null) {
			m_logger.warn(StrUtil.buildString(this,
					" consumes a message with null data: ", message));
			message.close();
			return;
		}

		IChannel channel = null;
		try {
			channel = getChannel();
			channel.write(data, false);
		} catch (Exception e) {
			m_logger.error(
					StrUtil.buildString("Failed to send message: ", message), e);

			if (data instanceof ICloseable)
				((ICloseable) data).close();

		} finally {
			message.close();
		}
	}

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
	public void onChannelOpened(IChannel channel) {
		m_logger.debug("{}: OPENED", channel);

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
			m_channel = channel;
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		m_channel = null;
	}

	@Override
	public void onMessageReceived(IChannel channel, Object data) {
		IProducer producer = m_producer;
		IMessage message = producer.createMessage();
		message.attach(data);
		producer.send(message);
	}

	@Override
	public void onMessageSent(IChannel channel, Object data) {
	}

	@Override
	public void onChannelException(IChannel channel, Throwable t) {
		channel.close();
		m_logger.error(
				StrUtil.buildString(this, " got an error on sending/recving"),
				t);
	}

	@Override
	public void onChannelIdleTimedOut(IChannel channel) {
		throw new UnsupportedOperationException("Not supported yet.");
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
	public String toString() {
		return m_caption;
	}

	@Override
	public void update(Map<String, ?> properties) throws Exception {
		Configuration oldConf = updateConf(properties);
		Configuration newConf = m_conf;
		updateFilters(oldConf, newConf);

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

		m_logger.info(StrUtil.buildString(this, " started"));
	}

	@Override
	protected void stopInternal() {
		m_logger.info(StrUtil.buildString("Stopping ", this, "..."));

		final WriteLock writeLock = m_lock.writeLock();
		writeLock.lock();
		try {
			m_closed = true;
		} finally {
			writeLock.unlock();
		}

		IChannel channel = m_channel;
		if (channel != null)
			channel.close(); // m_channel will be set to null in method onClosed

		m_logger.info(StrUtil.buildString(this, " stopped"));
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
		m_caption = StrUtil.buildString("UdpClient[", id, "]");

		updateFilters(updateConf(properties), m_conf);

		start();
	}

	protected void deactivate() {
		stop();

		updateFilters(updateConf(null), null);
	}

	private Configuration updateConf(Map<String, ?> props) {
		Configuration conf = m_conf;
		if (props == null)
			m_conf = null;
		else {
			Configuration newConf = new Configuration();
			newConf.initialize(props);
			m_conf = newConf;
		}

		return conf;
	}

	private IChannel getChannel() throws Exception {
		IChannel channel = m_channel;
		if (channel != null)
			return channel;

		final ReentrantLock channelLock = m_channelLock;
		channelLock.lock();
		try {
			channel = m_channel;
			if (channel == null) {
				channel = new UdpClientChannel(this);
				channel.connect(-1);
			}
		} finally {
			channelLock.unlock();
		}

		return channel;
	}

	private void updateFilters(Configuration oldConf, Configuration newConf) {
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
}
