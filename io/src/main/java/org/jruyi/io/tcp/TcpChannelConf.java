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
package org.jruyi.io.tcp;

import java.lang.reflect.Method;
import java.util.Map;

import org.jruyi.common.StrUtil;
import org.jruyi.io.common.SessionEventMask;

public class TcpChannelConf {

	private String m_ip;
	private Integer m_port;
	private Integer m_readThreshold;
	private String[] m_filters;
	private SessionEventMask m_sem;
	private Boolean m_reuseAddr;
	private Boolean m_keepAlive;
	private Integer m_soLinger;
	private Integer m_recvBufSize;
	private Integer m_sendBufSize;
	private Boolean m_tcpNoDelay;
	private Integer m_trafficClass;
	private Boolean m_oobInline;
	private Integer[] m_performancePreferences;

	public void initialize(Map<String, ?> properties) {
		port((Integer) properties.get("port"));
		readThreshold((Integer) properties.get("readThreshold"));
		filters((String[]) properties.get("filters"));
		sessionEventMask((String[]) properties.get("notifySessionEvents"));
		reuseAddr((Boolean) properties.get("reuseAddr"));
		keepAlive((Boolean) properties.get("keepAlive"));
		soLinger((Integer) properties.get("soLinger"));
		recvBufSize((Integer) properties.get("recvBufSize"));
		sendBufSize((Integer) properties.get("sendBufSize"));
		tcpNoDelay((Boolean) properties.get("tcpNoDelay"));
		trafficClass((Integer) properties.get("trafficClass"));
		oobInline((Boolean) properties.get("oobInline"));
		performancePreferences((Integer[]) properties
				.get("performancePreferences"));
	}

	public final String ip() {
		return m_ip;
	}

	public final void ip(String ip) {
		m_ip = ip;
	}

	public final Integer port() {
		return m_port;
	}

	public final void port(Integer port) {
		m_port = port;
	}

	public final Integer readThreshold() {
		return m_readThreshold;
	}

	public final void readThreshold(Integer readThreshold) {
		m_readThreshold = readThreshold == null ? 0 : readThreshold;
	}

	public final String[] filters() {
		return m_filters;
	}

	public final void filters(String[] filters) {
		m_filters = filters == null ? StrUtil.getEmptyStringArray() : filters;
	}

	public final Boolean reuseAddr() {
		return m_reuseAddr;
	}

	public final void reuseAddr(Boolean reuseAddr) {
		m_reuseAddr = reuseAddr == null ? Boolean.FALSE : reuseAddr;
	}

	public final void keepAlive(Boolean keepAlive) {
		m_keepAlive = keepAlive;
	}

	public final Boolean keepAlive() {
		return m_keepAlive;
	}

	public final Integer soLinger() {
		return m_soLinger;
	}

	public final void soLinger(Integer soLinger) {
		m_soLinger = soLinger;
	}

	public final Integer recvBufSize() {
		return m_recvBufSize;
	}

	public final void recvBufSize(Integer recvBufSize) {
		m_recvBufSize = recvBufSize;
	}

	public final Integer sendBufSize() {
		return m_sendBufSize;
	}

	public final void sendBufSize(Integer sendBufSize) {
		m_sendBufSize = sendBufSize;
	}

	public final Boolean tcpNoDelay() {
		return m_tcpNoDelay;
	}

	public final void tcpNoDelay(Boolean tcpNoDelay) {
		m_tcpNoDelay = tcpNoDelay;
	}

	public final Integer trafficClass() {
		return m_trafficClass;
	}

	public final void trafficClass(Integer trafficClass) {
		m_trafficClass = trafficClass;
	}

	public final Boolean oobInline() {
		return m_oobInline;
	}

	public final void oobInline(Boolean oobInline) {
		m_oobInline = oobInline;
	}

	public final void performancePreferences(Integer[] performancePreferences) {
		m_performancePreferences = performancePreferences;
	}

	public final Integer[] performancePreferences() {
		return m_performancePreferences;
	}

	public final boolean isMandatoryChanged(TcpChannelConf newConf,
			Method[] mProps) throws Exception {
		for (Method m : mProps) {
			Object v1 = m.invoke(this);
			Object v2 = m.invoke(newConf);
			if (v1 == v2)
				continue;

			if (!(v1 == null ? v2.equals(v1) : v1.equals(v2)))
				return true;
		}

		return false;
	}

	public SessionEventMask sessionEventMask() {
		return m_sem;
	}

	private void sessionEventMask(String[] notifySessionEvents) {
		m_sem = new SessionEventMask(notifySessionEvents);
	}
}
