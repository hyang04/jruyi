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
package org.jruyi.clid;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.jruyi.common.CharsetCodec;
import org.jruyi.common.IBuffer;
import org.jruyi.common.IBufferFactory;
import org.jruyi.common.IBufferReader;
import org.jruyi.common.IService;
import org.jruyi.common.InsufficientDataException;
import org.jruyi.common.IntStack;
import org.jruyi.common.Properties;
import org.jruyi.common.StringBuilder;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.ISession;
import org.jruyi.io.IoConstants;
import org.jruyi.io.SessionEvent;
import org.jruyi.me.IMessage;
import org.jruyi.me.IProcessor;
import org.jruyi.me.IRoutingTable;
import org.jruyi.me.MeConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

public final class CliProcessor implements IProcessor, IFilter {

	private static final String BRANDING_URL = "org.jruyi.clid.branding.url";
	private static final String BINDADDR = "org.jruyi.clid.bindAddr";
	private static final String PORT = "org.jruyi.clid.port";
	private static final String SESSIONIDLETIMEOUT = "org.jruyi.clid.sessionIdleTimeout";
	private static final String NOTIFY_SESSION_EVENTS = "notifySessionEvents";
	private static final String SRC_ID = "jruyi.clid.tcpsvr";
	private static final String THIS_ID = "jruyi.clid.proc";
	private static final String CLID_OUT = "jruyi.clid.out";
	private static final String WELCOME = "welcome";
	private static final String PROMPT = "prompt";
	private static final byte[] CRLF = { '\r', '\n' };
	private static final int HEAD_RESERVE_SIZE = 4;
	private BundleContext m_context;
	private CommandProcessor m_cp;
	private IBufferFactory m_bf;
	private ComponentInstance m_tcpServer;
	private Properties m_conf;
	private ConcurrentHashMap<Long, CommandSession> m_css;
	private byte[] m_welcome;
	private String m_prompt;

	@Override
	public int tellBoundary(ISession session, IBufferReader in) {
		int b;
		int n = 0;
		try {
			do {
				b = in.readByte();
				n = (n << 7) | (b & 0x7F);
			} while (b < 0);
			return n + in.position();
		} catch (InsufficientDataException e) {
			in.rewind();
			return IFilter.E_UNDERFLOW;
		}
	}

	@Override
	public boolean onMsgArrive(ISession session, Object msg, IFilterOutput output) {
		((IBuffer) msg).compact();
		output.put(msg);
		return true;
	}

	@Override
	public boolean onMsgDepart(ISession session, Object msg, IFilterOutput output) {
		IBuffer data = (IBuffer) msg;
		int n = data.length();
		data.headWriteByte((byte) (n & 0x7F));
		while ((n >>= 7) > 0)
			data.headWriteByte((byte) ((n & 0x7F) | 0x80));

		output.put(data);
		return true;
	}

	@Override
	public void process(IMessage message) {
		Object sessionEvent = message
				.removeProperty(IoConstants.MP_SESSION_EVENT);
		if (sessionEvent == SessionEvent.OPENED) {
			onSessionOpened(message);
			return;
		}

		if (sessionEvent == SessionEvent.CLOSED) {
			onSessionClosed(message);
			return;
		}

		IBuffer buffer = (IBuffer) message.attachment();
		String cmdline = buffer.remaining() > 0 ? buffer
				.readString(CharsetCodec.UTF_8) : null;
		buffer.drain();
		buffer.reserveHead(HEAD_RESERVE_SIZE);

		ISession session = (ISession) message
				.getProperty(IoConstants.MP_PASSIVE_SESSION);
		CommandSession cs = m_css.get(session.id());

		if (cmdline != null) {

			cmdline = filterProps(cmdline, cs, m_context);

			BufferStream bs = (BufferStream) session.get(CLID_OUT);
			bs.buffer(buffer);

			try {
				Object result = cs.execute(cmdline);
				if (result != null)
					buffer.write(cs.format(result, Converter.INSPECT),
							CharsetCodec.UTF_8);
			} catch (Exception e) {
				buffer.write(e.getMessage(), CharsetCodec.UTF_8);
			}

			bs.buffer(null);
		}

		writeOut(buffer, (String) cs.get(PROMPT));
	}

	protected void setCommandProcessor(CommandProcessor cp) {
		m_cp = cp;
	}

	protected void unsetCommandProcessor(CommandProcessor cp) {
		m_cp = null;
	}

	protected void setBufferFactory(IBufferFactory bf) {
		m_bf = bf;
	}

	protected void unsetBufferFactory(IBufferFactory bf) {
		if (m_bf == bf)
			m_bf = null;
	}

	protected void modified(Map<String, ?> properties) throws Exception {
		IService inst = (IService) m_tcpServer.getInstance();
		inst.update(normalizeConf(properties));
	}

	protected void activate(ComponentContext context, Map<String, ?> properties)
			throws Exception {

		BundleContext bundleContext = context.getBundleContext();
		loadBrandingInfo(context.getBundleContext().getProperty(BRANDING_URL),
				bundleContext);

		Properties conf = normalizeConf(properties);
		if (conf.get("bindAddr") == null) {
			String bindAddr = context.getBundleContext().getProperty(BINDADDR);
			if (bindAddr == null || (bindAddr = bindAddr.trim()).length() < 1)
				bindAddr = "localhost";
			conf.put("bindAddr", bindAddr);
		}

		if (conf.get("port") == null) {
			String v = context.getBundleContext().getProperty(PORT);
			Integer port = v == null ? 6060 : Integer.valueOf(v);
			conf.put("port", port);
		}

		if (conf.get("sessionIdleTimeout") == null) {
			String v = context.getBundleContext().getProperty(
					SESSIONIDLETIMEOUT);
			Integer sessionIdleTimeout = v == null ? 300 : Integer.valueOf(v);
			conf.put("sessionIdleTimeout", sessionIdleTimeout);
		}

		ComponentFactory factory = (ComponentFactory) context
				.locateService("tcpServerFactory");
		m_tcpServer = factory.newInstance(conf);

		IRoutingTable rt = (IRoutingTable) context
				.locateService("routingTable");
		rt.getRouteSet(SRC_ID).setRoute(THIS_ID);
		rt.getRouteSet(THIS_ID).setRoute(SRC_ID);

		m_css = new ConcurrentHashMap<Long, CommandSession>(10);
		m_context = bundleContext;
	}

	protected void deactivate(ComponentContext context) {
		m_context = null;

		HashMap<String, Object> properties = new HashMap<String, Object>();
		properties.put(NOTIFY_SESSION_EVENTS,
				new String[] { SessionEvent.OPENED.name() });

		try {
			modified(properties);
		} catch (Exception e) {
			// ignore
		}

		m_tcpServer.dispose();
		m_tcpServer = null;
		m_conf = null;

		IRoutingTable rt = (IRoutingTable) context
				.locateService("routingTable");
		rt.getRouteSet(SRC_ID).clear();
		rt.getRouteSet(THIS_ID).clear();

		Collection<CommandSession> css = m_css.values();
		m_css = null;
		for (CommandSession cs : css)
			cs.close();
	}

	private Properties normalizeConf(Map<String, ?> properties) {
		Properties conf = m_conf;
		if (conf == null) {
			conf = new Properties(properties);
			m_conf = conf;
		} else
			conf.putAll(properties);

		String[] filters = (Boolean) conf.get("debug") ? new String[] {
				"org.jruyi.clid.filter", "org.jruyi.io.filter.msglog" }
				: new String[] { "org.jruyi.clid.filter" };
		conf.put(MeConstants.EP_ID, SRC_ID);
		conf.put("initCapacityOfChannelMap", 8);
		conf.put("filters", filters);
		conf.put("reuseAddr", Boolean.TRUE);
		if (!conf.containsKey(NOTIFY_SESSION_EVENTS))
			conf.put(NOTIFY_SESSION_EVENTS,
					new String[] { SessionEvent.OPENED.name(),
							SessionEvent.CLOSED.name() });
		return conf;
	}

	private void onSessionOpened(IMessage message) {
		ISession session = (ISession) message
				.getProperty(IoConstants.MP_PASSIVE_SESSION);

		BufferStream bs = new BufferStream();
		PrintStream out = new PrintStream(bs);
		CommandSession cs = m_cp.createSession(null, out, out);
		cs.put(PROMPT, m_prompt);

		session.put(CLID_OUT, bs);
		m_css.put(session.id(), cs);

		IBuffer buffer = m_bf.create();
		buffer.reserveHead(HEAD_RESERVE_SIZE);
		buffer.writeBytes(m_welcome).write(m_prompt, CharsetCodec.UTF_8);
		message.attach(buffer);
	}

	private void onSessionClosed(IMessage message) {
		ISession session = (ISession) message
				.getProperty(IoConstants.MP_PASSIVE_SESSION);
		CommandSession cs = m_css.remove(session.id());
		if (cs != null)
			cs.close();
	}

	private void writeOut(IBuffer out, String prompt) {
		if (out.size() > 0 && !out.endsWith(CRLF))
			out.writeBytes(CRLF);
		out.write(prompt, CharsetCodec.UTF_8);
	}

	private void loadBrandingInfo(String url, BundleContext context)
			throws Exception {
		java.util.Properties brandingInfo = new java.util.Properties();
		InputStream in = url == null ? CliProcessor.class
				.getResourceAsStream("branding.properties") : new URL(url)
				.openStream();
		try {
			in = new BufferedInputStream(in);
			brandingInfo.load(in);
		} finally {
			in.close();
		}

		m_welcome = CharsetCodec.get(CharsetCodec.UTF_8)
				.toBytes(
						StrUtil.filterProps(brandingInfo.getProperty(WELCOME),
								context));
		m_prompt = StrUtil.filterProps(brandingInfo.getProperty(PROMPT),
				context);
	}

	private static String filterProps(String target, CommandSession cs,
			BundleContext context) {
		if (target.length() < 2)
			return target;

		StringBuilder builder = StringBuilder.get();
		IntStack stack = IntStack.get();
		String propValue = null;
		int j = target.length();
		for (int i = 0; i < j; ++i) {
			char c = target.charAt(i);
			switch (c) {
			case '\\':
				if (++i < j)
					c = target.charAt(i);
				break;
			case '$':
				builder.append(c);
				if (++i < j && (c = target.charAt(i)) == '{')
					stack.push(builder.length() - 1);
				break;
			case '}':
				if (!stack.isEmpty()) {
					int index = stack.pop();
					propValue = getPropValue(builder.substring(index + 2), cs,
							context);
					if (propValue != null) {
						builder.setLength(index);
						builder.append(propValue);
						continue;
					}
				}
			}

			builder.append(c);
		}
		stack.close();

		if (propValue != null || builder.length() != j)
			target = builder.toString();

		builder.close();

		return target;
	}

	private static String getPropValue(String name, CommandSession cs,
			BundleContext context) {
		Object value = cs.get(name);
		if (value != null)
			return value.toString();

		return context.getProperty(name);
	}
}
