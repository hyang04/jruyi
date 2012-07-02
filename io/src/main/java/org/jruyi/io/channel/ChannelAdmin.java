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
package org.jruyi.io.channel;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.jruyi.common.StrUtil;
import org.jruyi.io.common.SyncPutQueue;
import org.jruyi.timeoutadmin.ITimeoutAdmin;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.jruyi.workshop.IWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChannelAdmin implements IChannelAdmin, Runnable {

	private static final Logger m_logger = LoggerFactory
			.getLogger(ChannelAdmin.class);
	private SyncPutQueue<ISelectableChannel> m_registerQueue;
	private SyncPutQueue<ISelectableChannel> m_connectQueue;
	private SyncPutQueue<ISelectableChannel> m_readQueue;
	private SyncPutQueue<ISelectableChannel> m_writeQueue;
	private Thread m_thread;
	private Selector m_selector;
	private IWorker m_worker;
	private ITimeoutAdmin m_tm;

	@Override
	public void run() {
		m_logger.info("ChannelAdmin thread started");

		IWorker worker = m_worker;
		Selector selector = m_selector;
		Thread currentThread = Thread.currentThread();
		try {
			for (;;) {
				int n = selector.select();
				if (currentThread.isInterrupted())
					break;

				procConnect();
				procRegister();
				procRead();
				procWrite();

				if (n < 1)
					continue;

				Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
				while (iter.hasNext()) {
					SelectionKey key = iter.next();
					iter.remove();

					ISelectableChannel channel = (ISelectableChannel) key
							.attachment();
					try {
						if (key.isConnectable()) {
							key.interestOps(key.interestOps()
									& ~SelectionKey.OP_CONNECT);
							channel.onConnect();
						} else {
							if (key.isReadable()) {
								key.interestOps(key.interestOps()
										& ~SelectionKey.OP_READ);
								worker.run(channel.onRead());
							}

							if (key.isWritable()) {
								key.interestOps(key.interestOps()
										& ~SelectionKey.OP_WRITE);
								worker.run(channel.onWrite());
							}
						}
					} catch (RejectedExecutionException e) {
					} catch (CancelledKeyException e) {
					} catch (Exception e) {
						m_logger.warn(StrUtil.buildString("Error - ", channel), e);
					}
				}
			}
		} catch (ClosedSelectorException e) {
			m_logger.error("Selector closed unexpectedly", e);
		} catch (IOException e) {
			m_logger.error("Selector Error", e);
		} catch (Throwable t) {
			m_logger.error("Unexpected Error", t);
		}

		m_logger.info("ChannelAdmin thread stopped");
	}

	@Override
	public void onRegisterRequired(ISelectableChannel channel) {
		m_registerQueue.put(channel);
		m_selector.wakeup();
	}

	@Override
	public void onConnectRequired(ISelectableChannel channel) {
		m_connectQueue.put(channel);
		m_selector.wakeup();
	}

	@Override
	public void onReadRequired(ISelectableChannel channel) {
		m_readQueue.put(channel);
		m_selector.wakeup();
	}

	@Override
	public void onWriteRequired(ISelectableChannel channel) {
		m_writeQueue.put(channel);
		m_selector.wakeup();
	}

	@Override
	public ITimeoutNotifier createTimeoutNotifier(ISelectableChannel channel) {
		return m_tm.createNotifier(channel);
	}

	protected void setWorker(IWorker worker) {
		m_worker = worker;
	}

	protected void unsetWorker(IWorker worker) {
		m_worker = null;
	}

	protected void setTimeoutAdmin(ITimeoutAdmin tm) {
		m_tm = tm;
	}

	protected void unsetTimeoutAdmin(ITimeoutAdmin tm) {
		m_tm = null;
	}

	protected void activate(Map<String, ?> properties) throws Exception {
		m_logger.info("Activating ChannelAdmin...");

		m_registerQueue = new SyncPutQueue<ISelectableChannel>();
		m_connectQueue = new SyncPutQueue<ISelectableChannel>();
		m_readQueue = new SyncPutQueue<ISelectableChannel>();
		m_writeQueue = new SyncPutQueue<ISelectableChannel>();

		m_selector = Selector.open();
		m_thread = new Thread(this, "ChannelAdmin");
		m_thread.start();

		m_logger.info("ChannelAdmin activated");
	}

	protected void deactivate() {
		m_logger.info("Deactivating ChannelAdmin...");

		m_thread.interrupt();
		try {
			m_thread.join();
		} catch (InterruptedException e) {
		} finally {
			m_thread = null;
		}

		try {
			m_selector.close();
		} catch (Exception e) {
			m_logger.error("Failed to close the selector", e);
		}
		m_selector = null;

		m_writeQueue = null;
		m_readQueue = null;
		m_connectQueue = null;
		m_registerQueue = null;

		m_logger.info("ChannelAdmin deactivated");
	}

	IWorker getWorker() {
		return m_worker;
	}

	private void procRegister() {
		ISelectableChannel channel = null;
		SyncPutQueue<ISelectableChannel> registerQueue = m_registerQueue;
		Selector selector = m_selector;
		while ((channel = registerQueue.poll()) != null)
			channel.register(selector, SelectionKey.OP_READ);
	}

	private void procConnect() {
		ISelectableChannel channel = null;
		SyncPutQueue<ISelectableChannel> connectQueue = m_connectQueue;
		Selector selector = m_selector;
		while ((channel = connectQueue.poll()) != null)
			channel.register(selector, SelectionKey.OP_CONNECT);
	}

	private void procRead() {
		ISelectableChannel channel = null;
		SyncPutQueue<ISelectableChannel> readQueue = m_readQueue;
		while ((channel = readQueue.poll()) != null) {
			try {
				channel.interestOps(SelectionKey.OP_READ);
			} catch (CancelledKeyException e) {
			} catch (Exception e) {
				channel.onException(e);
			}
		}
	}

	private void procWrite() {
		ISelectableChannel channel = null;
		SyncPutQueue<ISelectableChannel> writeQueue = m_writeQueue;
		while ((channel = writeQueue.poll()) != null) {
			try {
				channel.interestOps(SelectionKey.OP_WRITE);
			} catch (CancelledKeyException e) {
			} catch (Exception e) {
				channel.onException(e);
			}
		}
	}
}
