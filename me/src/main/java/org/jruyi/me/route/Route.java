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
package org.jruyi.me.route;

import java.util.Dictionary;

import org.jruyi.common.IDumpable;
import org.jruyi.common.StringBuilder;
import org.jruyi.common.StrUtil;
import org.jruyi.me.IRoute;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

final class Route implements IRoute, IDumpable {

	static final String FILTER_ALL = "ALL";
	private final Router m_router;
	private final String m_to;
	private Filter m_filter = AlwaysTrueFilter.getInstance();

	static final class AlwaysTrueFilter implements Filter {

		private static final Filter m_inst = new AlwaysTrueFilter();

		static Filter getInstance() {
			return m_inst;
		}

		@Override
		public String toString() {
			return FILTER_ALL;
		}

		@Override
		public boolean match(ServiceReference reference) {
			return true;
		}

		@Override
		public boolean match(@SuppressWarnings("rawtypes") Dictionary dictionary) {
			return true;
		}

		@Override
		public boolean matchCase(
				@SuppressWarnings("rawtypes") Dictionary dictionary) {
			return true;
		}
	}

	Route(Router router, String to) {
		m_router = router;
		m_to = to;
	}

	Route(Router router, String to, String filter)
			throws InvalidSyntaxException {
		this(router, to);
		setFilter(filter);
	}

	Route(Router router, String to, Filter filter) {
		this(router, to);
		filter(filter);
	}

	@Override
	public String getFilter() {
		return m_filter.toString();
	}

	@Override
	public String getFrom() {
		return m_router.getFrom();
	}

	@Override
	public String getTo() {
		return m_to;
	}

	@Override
	public boolean isFilterAll() {
		return m_filter == AlwaysTrueFilter.getInstance();
	}

	@Override
	public String toString() {
		return StrUtil.buildString("Route[(", m_router.getFrom(), ")->(", m_to,
				"):", m_filter, "]");
	}

	@Override
	public void dump(StringBuilder builder) {
		builder.append("Route[(").append(m_router.getFrom()).append(")->(")
				.append(m_to).append("):").append(m_filter).append(']');
	}

	boolean match(Dictionary<String, ?> routingInfo) {
		return m_filter.matchCase(routingInfo);
	}

	void setFilter(String filter) throws InvalidSyntaxException {
		if (filter.equals(FILTER_ALL))
			m_filter = AlwaysTrueFilter.getInstance();
		else
			m_filter = FrameworkUtil.createFilter(filter);
	}

	void filter(Filter filter) {
		m_filter = filter;
	}

	Filter filter() {
		return m_filter;
	}
}
