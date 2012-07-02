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

import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.IBuffer;
import org.jruyi.common.ICloseable;
import org.jruyi.common.IDumpable;
import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.ListNode;
import org.jruyi.common.StringBuilder;
import org.jruyi.common.StrUtil;
import org.jruyi.common.ThreadLocalCache;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.timeoutadmin.ITimeoutEvent;
import org.jruyi.timeoutadmin.ITimeoutListener;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Channel implements IChannel, IDumpable, Runnable {

	private static final Logger m_logger = LoggerFactory.getLogger(Channel.class);
	private static final AtomicLong m_idSeed = new AtomicLong(0L);
	private static final Object EOF = new Object();
	private static final ThreadLocal<FilterVars> m_filterVars;
	private Long m_id;
	private final IChannelService m_channelService;
	private final ReentrantLock m_lock;
	private ConcurrentHashMap<String, Object> m_attributes;
	private IdentityHashMap<Object, Object> m_storage;
	private Object m_attachment;
	private volatile boolean m_closed;
	private SelectionKey m_selectionKey;
	private ITimeoutNotifier m_timeoutNotifier;
	private Runnable m_readThread;
	private WriteThread m_writeThread;

	static {

		m_filterVars = new ThreadLocal<FilterVars>() {

			@Override
			protected FilterVars initialValue() {
				return new FilterVars();
			}
		};
	}

	static final class FilterOutput implements IFilterOutput {

		private Object m_out;

		@Override
		public void put(Object out) {
			m_out = out;
		}

		public Object take() {
			Object out = m_out;
			m_out = null;
			return out;
		}
	}

	static final class MsgArrayList {

		private Object[] m_msgs;
		private int m_size;

		MsgArrayList() {
			this(16);
		}

		MsgArrayList(int initialCapacity) {
			m_msgs = new Object[initialCapacity];
		}

		int size() {
			return m_size;
		}

		void size(int newSize) {
			m_size = newSize;
		}

		Object[] msgs() {
			return m_msgs;
		}

		Object take(int index) {
			Object msg = m_msgs[index];
			m_msgs[index] = null;
			return msg;
		}

		boolean isEmpty() {
			return m_size < 1;
		}

		void add(Object msg) {
			int minCapacity = ++m_size;
			int oldCapacity = m_msgs.length;
			if (minCapacity > oldCapacity) {
				int newCapacity = (oldCapacity * 3) / 2 + 1;
				Object[] oldMsgs = m_msgs;
				m_msgs = new Object[newCapacity];
				System.arraycopy(oldMsgs, 0, m_msgs, 0, oldCapacity);
			}
			m_msgs[minCapacity - 1] = msg;
		}

		void release() {
			Object[] msgs = m_msgs;
			int size = m_size;
			for (int i = 0; i < size; ++i) {
				Object msg = msgs[i];
				if (msg != null) {
					if (msg instanceof ICloseable)
						((ICloseable) msg).close();
					msgs[i] = null;
				}
			}
			m_size = 0;
		}
	}

	static final class FilterVars {

		private final MsgArrayList m_msgs1;
		private final MsgArrayList m_msgs2;
		private final FilterOutput m_output;

		FilterVars() {
			m_msgs1 = new MsgArrayList();
			m_msgs2 = new MsgArrayList();
			m_output = new FilterOutput();
		}

		MsgArrayList msgs1() {
			return m_msgs1;
		}

		MsgArrayList msgs2() {
			return m_msgs2;
		}

		FilterOutput output() {
			return m_output;
		}
	}

	static final class FilterContext implements ICloseable {

		private static final IThreadLocalCache<FilterContext> m_cache = ThreadLocalCache.weakLinkedCache();
		private int m_msgLen;
		private IBuffer m_data;

		private FilterContext() {
		}

		static FilterContext get() {
			FilterContext context = m_cache.take();
			if (context == null)
				context = new FilterContext();

			return context;
		}

		int msgLen() {
			return m_msgLen;
		}

		void msgLen(int msgLen) {
			m_msgLen = msgLen;
		}

		IBuffer data() {
			return m_data;
		}

		void data(IBuffer data) {
			m_data = data;
		}

		void clear() {
			m_msgLen = 0;
			m_data = null;
		}

		@Override
		public void close() {
			m_msgLen = 0;
			m_data = null;
			m_cache.put(this);
		}
	}

	static final class ReadThread implements Runnable {

		private final Channel m_channel;

		ReadThread(Channel channel) {
			m_channel = channel;
		}

		@Override
		public void run() {
			Channel channel = m_channel;
			IChannelService cs = channel.channelService();
			IBuffer in = cs.getBufferFactory().create();
			ScatteringByteChannel rbc = channel.scatteringByteChannel();
			int n = 0;
			try {
				int readThreshold = cs.readThreshold();
				for (;;) {
					n = in.readIn(rbc);
					if (n > 0) {
						if (in.length() > readThreshold)
							break;
					} else if (in.isEmpty()) {
						in.close();
						channel.close();
						return;
					} else
						break;
				}
			} catch (Exception e) {
				in.close();
				if (!channel.isClosed())
					channel.onException(e);
				return;
			}

			try {
				if (n < 0) {
					channel.close();
					channel.onReadIn(in);
				} else if (channel.onReadIn(in))
					cs.getChannelAdmin().onReadRequired(channel);
				else
					channel.close();
			} catch (Exception e) {
				channel.onException(e);
			}
		}
	}

	static final class WriteThread implements Runnable {

		private final Channel m_channel;
		// indicates whether writing is in process
		private Object m_data;
		private ListNode<Object> m_head;
		private ListNode<Object> m_tail;
		private final ReentrantLock m_lock;

		WriteThread(Channel channel) {
			m_channel = channel;
			m_head = m_tail = ListNode.create();
			m_lock = new ReentrantLock();
		}

		@Override
		public void run() {
			Channel channel = m_channel;
			IBuffer data = null;
			try {
				IChannelService cs = channel.channelService();
				GatheringByteChannel gbc = channel.gatheringByteChannel();
				Object msg = peek();
				do {
					if (msg == EOF) {
						channel.close();
						return;
					}

					Object out = msg;
					IFilter[] filters = cs.getFilterChain();
					int i = filters.length;
					if (i > 0) {
						FilterOutput output = m_filterVars.get().output();
						do {
							if (!filters[--i].onMsgDepart(channel, out, output))
								return;

							out = output.take();
						} while (i > 0 && msg != null);
					}

					try {
						data = (IBuffer) out;
					} catch (ClassCastException e) {
						throw new RuntimeException(StrUtil.buildString(filters[0],
								"has to produce departure data of type " + IBuffer.class.getName()));
					}

					while (data.remaining() > 0) {
						if (data.writeOut(gbc) == 0) {
							cs.getChannelAdmin().onWriteRequired(channel);
							return;
						}
					}

					cs.onMessageSent(channel, msg);
					data.close();
					data = null;
					msg = poll();
				} while (msg != null);
			} catch (Exception e) {
				if (data != null)
					data.close();

				if (!channel.isClosed())
					channel.onException(e);
			}
		}

		void write(Object data) {
			if (put(data)) // in writing
				return;

			run();
		}

		private boolean put(Object data) {
			final ReentrantLock lock = m_lock;
			lock.lock();
			try {
				if (m_data == null)
					m_data = data;
				else {
					ListNode<Object> node = ListNode.create();
					node.set(data);
					m_tail.next(node);
					m_tail = node;
					return true;
				}
			} finally {
				lock.unlock();
			}

			return false;
		}

		private Object peek() {
			return m_data;
		}

		private Object poll() {
			final ReentrantLock lock = m_lock;
			lock.lock();
			try {
				if (m_head == m_tail) {
					m_data = null;
					return null;
				}
			} finally {
				lock.unlock();
			}

			ListNode<Object> head = m_head;
			ListNode<Object> node = head.next();
			Object data = node.get();
			node.set(null);
			m_head = node;
			head.close();
			m_data = data;

			return data;
		}

		void clear() {
			final ReentrantLock lock = m_lock;
			lock.lock();
			try {
				ListNode<Object> head = m_head;
				ListNode<Object> tail = m_tail;
				if (head != tail) {
					do {
						ListNode<Object> node = head.next();
						head.close();
						Object msg = node.get();
						if (msg instanceof ICloseable)
							((ICloseable) msg).close();
						node.set(null);
						head = node;
					} while (head != tail);
					m_head = head;
				}
			} finally {
				lock.unlock();
			}
		}
	}

	static final class IdleTimeoutListener implements ITimeoutListener {

		static final ITimeoutListener INST = new IdleTimeoutListener();

		@Override
		public void onTimeout(ITimeoutEvent event) {
			Channel channel = (Channel) event.getSubject();
			try {
				channel.channelService().onChannelIdleTimedOut(channel);
			} catch (Exception e) {
				channel.onException(e);
			}
		}
	}

	static final class ConnectTimeoutListener implements ITimeoutListener {

		static final ITimeoutListener INST = new ConnectTimeoutListener();

		@Override
		public void onTimeout(ITimeoutEvent event) {
			Channel channel = (Channel) event.getSubject();
			try {
				channel.channelService().onChannelConnectTimedOut(channel);
			} catch (Exception e) {
				channel.onException(e);
			}
		}
	}

	static final class ReadTimeoutListener implements ITimeoutListener {

		static final ITimeoutListener INST = new ReadTimeoutListener();

		@Override
		public void onTimeout(ITimeoutEvent event) {
			Channel channel = (Channel) event.getSubject();
			try {
				channel.channelService().onChannelReadTimedOut(channel);
			} catch (Exception e) {
				channel.onException(e);
			}
		}
	}

	protected Channel(IChannelService channelService) {
		m_channelService = channelService;
		m_lock = new ReentrantLock();
	}

	/**
	 * Runs on accept event
	 */
	@Override
	public void run() {
		IChannelService channelService = m_channelService;
		try {
			m_id = m_idSeed.incrementAndGet();
			m_attributes = new ConcurrentHashMap<String, Object>();
			m_storage = new IdentityHashMap<Object, Object>();

			onAccepted();
			m_readThread = new ReadThread(this);
			m_writeThread = new WriteThread(this);

			IChannelAdmin ca = channelService.getChannelAdmin();
			m_timeoutNotifier = createTimeoutNotifier(ca);
			selectableChannel().configureBlocking(false);

			channelService.onChannelOpened(this);

			ca.onRegisterRequired(this);

		} catch (Exception e) {
			onException(e);
		}
	}

	@Override
	public final boolean scheduleIdleTimeout(int timeout) {
		ITimeoutNotifier timeoutNotifier = m_timeoutNotifier;
		if (timeoutNotifier == null)
			return false;

		timeoutNotifier.setListener(IdleTimeoutListener.INST);
		return timeoutNotifier.schedule(timeout);
	}

	@Override
	public final boolean scheduleConnectTimeout(int timeout) {
		ITimeoutNotifier timeoutNotifier = m_timeoutNotifier;
		if (timeoutNotifier == null)
			return false;

		timeoutNotifier.setListener(ConnectTimeoutListener.INST);
		return timeoutNotifier.schedule(timeout);
	}

	@Override
	public final boolean scheduleReadTimeout(int timeout) {
		ITimeoutNotifier timeoutNotifier = m_timeoutNotifier;
		if (timeoutNotifier == null)
			return false;

		timeoutNotifier.setListener(ReadTimeoutListener.INST);
		return timeoutNotifier.schedule(timeout);
	}

	@Override
	public final boolean cancelTimeout() {
		ITimeoutNotifier timeoutNotifier = m_timeoutNotifier;
		return timeoutNotifier != null && timeoutNotifier.cancel();
	}

	@Override
	public Object deposit(Object id, Object something) {
		return m_storage.put(id, something);
	}

	@Override
	public Object withdraw(Object id) {
		return m_storage.remove(id);
	}

	@Override
	public Object inquiry(Object id) {
		return m_storage.get(id);
	}

	@Override
	public final void write(Object data, boolean close) {
		try {
			if (data != null)
				m_writeThread.write(data);

			if (close)
				m_writeThread.write(EOF);

		} catch (Exception e) {
			onException(e);
		}
	}

	@Override
	public final void close() {
		if (m_closed)
			return;

		final ReentrantLock lock = m_lock;
		if (!lock.tryLock())
			return;
		try {
			if (m_closed)
				return;
			m_closed = true;
		} finally {
			lock.unlock();
		}

		try {
			onClose();
		} catch (Exception e) {
			onException(e);
		}

		ITimeoutNotifier tn = m_timeoutNotifier;
		if (tn != null)
			tn.close();

		WriteThread wt = m_writeThread;
		if (wt != null)
			wt.clear();

		try {
			m_channelService.onChannelClosed(this);
		} catch (RuntimeException e) {
			m_logger.error("Unexpected Error", e);
		}
	}

	@Override
	public final void dump(StringBuilder builder) {
		builder.append(m_channelService).append(" Session#").append(m_id);
	}

	@Override
	public final String toString() {
		StringBuilder builder = StringBuilder.get();
		try {
			dump(builder);
			return builder.toString();
		} finally {
			builder.close();
		}
	}

	@Override
	public final Long id() {
		return m_id;
	}

	@Override
	public final Object get(String name) {
		return m_attributes.get(name);
	}

	@Override
	public final Object put(String name, Object value) {
		return m_attributes.put(name, value);
	}

	@Override
	public final Object remove(String name) {
		return m_attributes.remove(name);
	}

	@Override
	public boolean isClosed() {
		return m_closed;
	}

	@Override
	public final Object attach(Object attachment) {
		Object oldAttachment = m_attachment;
		m_attachment = attachment;
		return oldAttachment;
	}

	@Override
	public final Object attachment() {
		return m_attachment;
	}

	@Override
	public final Object detach() {
		Object oldAttachment = m_attachment;
		m_attachment = null;
		return oldAttachment;
	}

	@Override
	public final IChannelService channelService() {
		return m_channelService;
	}

	@Override
	public final void register(Selector selector, int ops) {
		try {
			m_selectionKey = selectableChannel().register(selector, ops, this);
		} catch (Exception e) {
			// Ignore
		}
	}

	@Override
	public final void interestOps(int ops) {
		SelectionKey selectionKey = m_selectionKey;
		try {
			selectionKey.interestOps(selectionKey.interestOps() | ops);
		} catch (Exception e) {
			// Ignore
		}
	}

	@Override
	public final void connect(int timeout) {
		try {
			m_id = m_idSeed.incrementAndGet();

			m_attributes = new ConcurrentHashMap<String, Object>();
			m_storage = new IdentityHashMap<Object, Object>();

			IChannelAdmin ca = m_channelService.getChannelAdmin();
			m_timeoutNotifier = createTimeoutNotifier(ca);
			if (connect()) {
				onConnectInternal(true);
			} else {
				if (timeout > 0)
					scheduleConnectTimeout(timeout);
				ca.onConnectRequired(this);
			}
		} catch (Exception e) {
			onException(e);
		}
	}

	@Override
	public final void onConnect() {
		// if false, it's timeout
		if (!cancelTimeout())
			return;

		onConnectInternal(false);
	}

	@Override
	public final Runnable onRead() {
		return m_readThread;
	}

	@Override
	public final Runnable onWrite() {
		return m_writeThread;
	}

	@Override
	public final void receive(IBuffer in) {
		try {
			if (onReadIn(in))
				return;
			close();
		} catch (Exception e) {
			onException(e);
		}
	}

	@Override
	public final void onException(Throwable t) {
		try {
			m_channelService.onChannelException(this, t);
		} catch (RuntimeException e) {
			m_logger.error(StrUtil.buildString("Unexpected Error: ", this), e);
		}
	}

	public final Runnable onAccept() {
		return this;
	}

	protected abstract SelectableChannel selectableChannel();

	protected abstract void onAccepted() throws Exception;

	protected abstract void onClose() throws Exception;

	protected abstract boolean connect() throws Exception;

	protected abstract void onConnected() throws Exception;

	protected abstract ScatteringByteChannel scatteringByteChannel();

	protected abstract GatheringByteChannel gatheringByteChannel();

	protected ITimeoutNotifier createTimeoutNotifier(IChannelAdmin ca) {
		return ca.createTimeoutNotifier(this);
	}

	protected final void readThread(Runnable readThread) {
		m_readThread = readThread;
	}

	/**
	 * If returns false, this channel need be closed
	 * 
	 * @param in
	 * @return
	 */
	final boolean onReadIn(Object in) {
		FilterVars vars = m_filterVars.get();
		FilterOutput output = vars.output();
		MsgArrayList inMsgs = vars.msgs1();
		MsgArrayList outMsgs = vars.msgs2();
		IChannelService cs = m_channelService;
		IFilter[] filters = cs.getFilterChain();
		try {
			for (int k = 0, m = filters.length; k < m; ++k) {
				if (in instanceof IBuffer) {
					if (!onAccumulate(k, filters, inMsgs, outMsgs, (IBuffer) in, output))
						return false;
				} else {
					int i = 0;
					int n = inMsgs.size();
					for (;;) {
						if (!onMsgArrive(k, filters, outMsgs, in, output))
							return false;
						if (++i >= n)
							break;
						in = inMsgs.take(i);
					}
				}

				if (outMsgs.isEmpty())
					return true;

				MsgArrayList temp = inMsgs;
				inMsgs = outMsgs;
				outMsgs = temp;

				in = inMsgs.take(0);
			}

			cs.onMessageReceived(this, in);
			for (int i = 1, n = inMsgs.size(); i < n; ++i)
				cs.onMessageReceived(this, inMsgs.take(i));
		} finally {
			inMsgs.release();
			outMsgs.release();
		}

		return true;
	}

	private boolean onAccumulate(int k, IFilter[] filters,
			MsgArrayList inMsgs, MsgArrayList outMsgs,
			IBuffer in, FilterOutput output) {
		IFilter filter = filters[k];
		// mergeContext -start
		int msgLen = 0;
		FilterContext context = (FilterContext) withdraw(filter);
		if (context != null) {
			IBuffer prevData = context.data();
			if (prevData != null) {
				in.drainTo(prevData);
				in.close();
				in = prevData;
			}

			msgLen = context.msgLen();
			context.close();
			context = null;
		}
		// mergeContext -end

		int i = 1;
		int n = inMsgs.size();
		for (;;) {
			if (msgLen == 0) {
				msgLen = filter.tellBoundary(this, in);
				if (msgLen < 0) { // ERROR
					in.close();
					return false;
				}
			}

			int inLen = in.length();
			if (msgLen == 0 || inLen < msgLen) {
				if (i < n) {
					IBuffer data = (IBuffer) inMsgs.take(i);
					++i;
					data.drainTo(in);
					data.close();
					continue;
				}
			} else if (inLen > msgLen) {
				if (!onMsgArrive(k, filters, outMsgs, in.split(msgLen),
						output))
					return false;
				in.rewind();
				msgLen = 0;
				continue;
			} else {
				if (!onMsgArrive(k, filters, outMsgs, in, output))
					return false;
				msgLen = 0;
				if (i < n) {
					in = (IBuffer) inMsgs.take(i);
					++i;
					continue;
				} else
					in = null;
			}

			break;
		}

		inMsgs.size(0); // clear

		// storeContext - start
		if (in != null) {
			context = FilterContext.get();
			context.data(in);
			context.msgLen(msgLen);
			deposit(filter, context);
		}
		// storeContext - end

		return true;
	}

	private void onConnectInternal(boolean requireRegister) {
		try {
			onConnected();

			m_readThread = new ReadThread(this);
			m_writeThread = new WriteThread(this);

			IChannelService channelService = m_channelService;
			channelService.onChannelOpened(this);

			IChannelAdmin ca = channelService.getChannelAdmin();
			if (requireRegister)
				ca.onRegisterRequired(this);
			else
				ca.onReadRequired(this);
		} catch (Exception e) {
			onException(e);
		}
	}

	private boolean onMsgArrive(int index, IFilter[] filters,
			MsgArrayList outMsgs, Object msg, FilterOutput output) {
		boolean ok = filters[index].onMsgArrive(this, msg, output);
		Object out = output.take();
		if (out == null)
			return ok;

		if (ok) {
			outMsgs.add(out);
			return true;
		}

		do {
			if (!filters[index].onMsgDepart(this, out, output))
				return false;

			out = output.take();
			if (out == null)
				return true;
		} while (--index >= 0);

		IBuffer data;
		try {
			data = (IBuffer) out;
		} catch (ClassCastException e) {
			throw new RuntimeException(StrUtil.buildString(filters[0],
					"has to produce departure data of type " + IBuffer.class.getName()));
		}

		if (data.remaining() > 0)
			m_writeThread.write(data);
		else
			data.close();

		return true;
	}
}
