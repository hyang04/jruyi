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
package org.jruyi.io.tcpserver;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;

import org.jruyi.common.StrUtil;
import org.jruyi.io.common.StopThread;
import org.jruyi.io.common.SyncPutQueue;
import org.jruyi.io.tcp.TcpChannel;
import org.jruyi.workshop.IWorker;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TcpAcceptor implements ITcpAcceptor, Runnable {

	private static final Logger m_logger = LoggerFactory
			.getLogger(TcpAcceptor.class);
	private String m_name;
	private Selector m_selector;
	private Thread m_thread;
	private SyncPutQueue<TcpServer> m_queue;
	private IWorker m_worker;
	private ComponentContext m_context;

	@Override
	public void doAccept(TcpServer server) throws Exception {
		SelectableChannel selectableChannel = server.getSelectableChannel();
		selectableChannel.configureBlocking(false);
		m_queue.put(server);
		m_selector.wakeup();
	}

	@Override
	public void run() {
		IWorker worker = m_worker;
		SyncPutQueue<TcpServer> queue = m_queue;
		Selector selector = m_selector;
		Thread currentThread = Thread.currentThread();
		TcpServer server = null;
		try {
			for (;;) {
				int n = selector.select();
				if (currentThread.isInterrupted())
					break;

				// Register
				while ((server = queue.poll()) != null) {
					SelectableChannel channel = server.getSelectableChannel();
					try {
						channel.register(selector, SelectionKey.OP_ACCEPT,
								server);
					} catch (Exception e) {
						m_logger.error(StrUtil.buildString(
								"Failed to register ", channel), e);
						// stop tcp server
						worker.run(new StopThread(server));
					}
				}

				if (n < 1)
					continue;

				Iterator<SelectionKey> iter = selector.selectedKeys()
						.iterator();
				while (iter.hasNext()) {
					SelectionKey key = iter.next();
					iter.remove();

					if (!key.isValid())
						continue;

					try {
						server = (TcpServer) key.attachment();
						SocketChannel socketChannel = ((ServerSocketChannel) key
								.channel()).accept();

						TcpChannel channel = new TcpChannel(server,
								socketChannel);
						worker.run(channel.onAccept());
					} catch (ClosedChannelException e) {
					} catch (Exception e) {
						m_logger.error(StrUtil.buildString(server,
								" failed to accept"), e);
					}
				}
			}
		} catch (ClosedSelectorException e) {
		} catch (Exception e) {
			m_logger.error("TcpAcceptor Error", e);

			// disable itself
			m_context.disableComponent(m_name);
		}
	}

	protected void setWorker(IWorker worker) {
		m_worker = worker;
	}

	protected void unsetWorker(IWorker worker) {
		m_worker = null;
	}

	protected void activate(ComponentContext context, Map<String, ?> properties)
			throws Exception {
		m_logger.info("Starting TcpAcceptor...");

		m_name = (String) properties.get(ComponentConstants.COMPONENT_NAME);
		m_context = context;

		m_selector = Selector.open();
		m_queue = new SyncPutQueue<TcpServer>();
		m_thread = new Thread(this, "TcpAcceptor");
		m_thread.start();

		m_logger.info("TcpAcceptor started");
	}

	protected void deactivate() {
		m_logger.info("Stopping TcpAcceptor...");

		m_thread.interrupt();
		try {
			m_thread.join();
		} catch (InterruptedException e) {
		}
		m_thread = null;

		m_queue = null;

		try {
			m_selector.close();
		} catch (Exception e) {
			m_logger.error("Failed to close the selector", e);
		}
		m_selector = null;

		m_context = null;
		m_name = null;

		m_logger.info("TcpAcceptor stopped");
	}
}
