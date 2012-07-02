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
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.*;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.common.SyncQueue;
import org.jruyi.me.IMessage;
import org.jruyi.workshop.IRunnable;
import org.jruyi.workshop.IWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConnPool extends TcpClient implements IRunnable {

	private static final Logger m_logger = LoggerFactory.getLogger(ConnPool.class);
	private Configuration m_conf;
	private IWorker m_worker;
	private final SyncQueue<IMessage> m_messages;
	private final ReentrantLock m_channelQueueLock;
	private final BiListNode<IChannel> m_channelQueueHead;
	private int m_channelQueueSize;
	private volatile int m_poolSize;
	private final ReentrantLock m_poolSizeLock;

	static final class Configuration extends TcpClientConf {

		private Integer m_minPoolSize;
		private Integer m_maxPoolSize;
		private Integer m_idleTimeout;

		@Override
		public void initialize(Map<String, ?> properties) {
			super.initialize(properties);

			minPoolSize((Integer) properties.get("minPoolSize"));
			maxPoolSize((Integer) properties.get("maxPoolSize"));
			idleTimeout((Integer) properties.get("idleTimeout"));
		}

		public Integer minPoolSize() {
			return m_minPoolSize;
		}

		public void minPoolSize(Integer minPoolSize) {
			m_minPoolSize = minPoolSize == null ? 5 : minPoolSize;
		}

		public Integer maxPoolSize() {
			return m_maxPoolSize;
		}

		public void maxPoolSize(Integer maxPoolSize) {
			m_maxPoolSize = maxPoolSize == null ? 10 : maxPoolSize;
		}

		public Integer idleTimeout() {
			return m_idleTimeout;
		}

		public void idleTimeout(Integer idleTimeout) {
			m_idleTimeout = idleTimeout == null ? 60 : idleTimeout;
		}
	}

	public ConnPool() {
		BiListNode<IChannel> node = BiListNode.create();
		node.previous(node);
		node.next(node);
		m_channelQueueHead = node;

		m_messages = new SyncQueue<IMessage>();
		m_channelQueueLock = new ReentrantLock();
		m_poolSizeLock = new ReentrantLock();
	}

	@Override
	public void onMessage(IMessage message) {
		Configuration conf = m_conf;
		if (compareAndIncrement(conf.minPoolSize())) {
			connect(message);
			return;
		}

		// fetch an idle channel in the pool if any
		IChannel channel = fetchChannel();
		if (channel != null) {
			sendOut(channel, message);
			return;
		}

		// No idle channel found
		if (compareAndIncrement(conf.maxPoolSize())) {
			connect(message);
			return;
		}

		// Enqueue the message for being processed on any channel available.
		// The message has to be enqueued before polling the channel pool again.
		// Otherwise, if a free channel is put into the pool after polling out
		// a null channel but before the message is enqueued, then the message
		// could be left in the queue with never being processed.
		m_messages.put(message);
		channel = fetchChannel();
		if (channel != null) {
			if ((message = m_messages.poll()) != null)
				sendOut(channel, message);
			else
				poolChannel(channel);
		}
	}

	@Override
	public void onMessageSent(IChannel channel, Object data) {
		int timeout = m_conf.readTimeout();
		if (timeout < 0)
			return;

		if (timeout > 0)
			channel.scheduleReadTimeout(timeout);
		else {
			// readTimeout == 0, means no response is expected
			IMessage message = (IMessage) channel.detach();
			message.close();

			if ((message = poolChannelIfNoMsg(channel)) != null)
				m_worker.run(this, ArgList.create(channel, message));
		}
	}

	@Override
	public void onMessageReceived(IChannel channel, Object data) {
		if (!channel.cancelTimeout() // channel has timed out
				|| m_conf.readTimeout() == 0 // no response is expected
				) {
			if (data instanceof ICloseable)
				((ICloseable) data).close();
			return;
		}

		// Put the message into the JRuyi MQ for routing
		IMessage message = (IMessage) channel.detach();
		if (message != null) {
			message.attach(data);
			enqueue(message);
		}

		if ((message = poolChannelIfNoMsg(channel)) != null)
			sendOut(channel, message);
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		super.onChannelOpened(channel);

		IMessage message = (IMessage) channel.detach();
		sendOut(channel, message);
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		super.onChannelClosed(channel);

		final ReentrantLock poolSizeLock = m_poolSizeLock;
		poolSizeLock.lock();
		try {
			--m_poolSize;
		} finally {
			poolSizeLock.unlock();
		}

		// If all the channels are closing and there are still some messages
		// left in queue with no new messages coming, those messages will never
		// be processed. So when the channel pool size is below minPoolSize and
		// the message queue is not empty, then make a new connection to process
		// the message.
		int minPoolSize = m_conf.minPoolSize();
		IMessage message = null;
		SyncQueue<IMessage> messages = m_messages;
		poolSizeLock.lock();
		try {
			if (m_poolSize < minPoolSize && (message = messages.poll()) != null)
				++m_poolSize;
		} finally {
			poolSizeLock.unlock();
		}

		if (message != null)
			connect(message);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onChannelIdleTimedOut(IChannel channel) {
		super.onChannelIdleTimedOut(channel);

		BiListNode<IChannel> node = null;
		final ReentrantLock lock = m_channelQueueLock;
		lock.lock();
		try {
			// If null, this node has already been removed.
			// There would be a race condition between fetchChannel and
			// idleTimedOut.
			// Both are gonna remove the node. So null-check of the element the
			// node holding is used to tell whether this node has already been
			// removed.
			if ((node = (BiListNode<IChannel>) channel.detach()) == null)
				return;

			removeNode(node);
		} finally {
			lock.unlock();
			channel.close();
		}

		node.close();
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
	public void run(IArgList args) {
		IChannel channel = (IChannel) args.arg(0);
		IMessage message = (IMessage) args.arg(1);

		Object data = message.detach();
		channel.attach(message);
		channel.write(data, false);
	}

	protected void setWorker(IWorker worker) {
		m_worker = worker;
	}

	protected void unsetWorker(IWorker worker) {
		if (m_worker == worker)
			m_worker = null;
	}

	@Override
	TcpClientConf configuration() {
		return m_conf;
	}

	@Override
	String getFactoryPid() {
		return "org.jruyi.io.tcpclient.connpool";
	}

	@Override
	TcpClientConf updateConf(Map<String, ?> props) {
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

	/**
	 * Increments the pool size if it is less than the given {@code limit}.
	 * 
	 * @return true if pool size is incremented, otherwise false
	 */
	private boolean compareAndIncrement(int limit) {
		if (m_poolSize < limit) {
			final ReentrantLock poolSizeLock = m_poolSizeLock;
			poolSizeLock.lock();
			try {
				if (m_poolSize < limit) {
					++m_poolSize;
					return true;
				}
			} finally {
				poolSizeLock.unlock();
			}
		}

		return false;
	}

	private void sendOut(IChannel channel, IMessage message) {
		Object data = message.detach();
		channel.attach(message);
		channel.write(data, false);
	}

	private IChannel fetchChannel() {
		BiListNode<IChannel> node = null;
		IChannel channel = null;
		final BiListNode<IChannel> head = m_channelQueueHead;
		final ReentrantLock lock = m_channelQueueLock;
		do {
			lock.lock();
			try {
				node = head.next();
				if (node == head)
					return null;

				removeNode(node);
				// The attachement of channel is used to tell whether the bound
				// node has been removed. So detach operation has to been synchronized.
				channel = node.get();
				channel.detach();
			} finally {
				lock.unlock();
			}
			node.close();
		} while (!channel.cancelTimeout());

		return channel;
	}

	private void poolChannel(IChannel channel) {
		Configuration conf = m_conf;
		final int keepAliveTime = conf.idleTimeout();
		final ReentrantLock lock = m_channelQueueLock;
		lock.lock();
		try {
			if (m_channelQueueSize < conf.minPoolSize() || keepAliveTime < 0) {
				putNode(newNode(channel));
				return;
			}

			if (keepAliveTime > 0) {
				putNode(newNode(channel));
				channel.scheduleIdleTimeout(keepAliveTime);
				return;
			}
		} finally {
			lock.unlock();
		}

		// keepAliveTime == 0, the channel need be closed immediately
		channel.close();
	}

	private IMessage poolChannelIfNoMsg(IChannel channel) {
		IMessage message = m_messages.poll();
		if (message != null)
			return message;

		final Configuration conf = m_conf;
		final int keepAliveTime = conf.idleTimeout();
		final ReentrantLock lock = m_channelQueueLock;
		lock.lock();
		try {
			if ((message = m_messages.poll()) != null)
				return message;

			if (m_channelQueueSize < conf.minPoolSize() || keepAliveTime < 0) {
				putNode(newNode(channel));
				return null;
			}

			if (keepAliveTime > 0) {
				putNode(newNode(channel));
				channel.scheduleIdleTimeout(keepAliveTime);
				return null;
			}
		} finally {
			lock.unlock();
		}

		// keepAliveTime == 0, the channel need be closed immediately
		channel.close();
		return null;
	}

	private BiListNode<IChannel> newNode(IChannel channel) {
		final BiListNode<IChannel> node = BiListNode.create();
		node.set(channel);
		channel.attach(node);
		return node;
	}

	private void putNode(BiListNode<IChannel> newNode) {
		final BiListNode<IChannel> head = m_channelQueueHead;
		final BiListNode<IChannel> headNext = head.next();
		newNode.previous(head);
		newNode.next(headNext);
		head.next(newNode);
		headNext.previous(newNode);

		++m_channelQueueSize;
	}

	private void removeNode(BiListNode<IChannel> node) {
		final BiListNode<IChannel> previousNode = node.previous();
		final BiListNode<IChannel> nextNode = node.next();
		previousNode.next(nextNode);
		nextNode.previous(previousNode);

		--m_channelQueueSize;
	}
}
