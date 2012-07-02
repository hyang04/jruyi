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
package org.jruyi.io.filter;

import org.jruyi.common.IServiceHolderManager;
import org.jruyi.common.ServiceHolderManager;
import org.jruyi.io.IFilter;
import org.jruyi.io.IoConstants;
import org.osgi.framework.BundleContext;

public final class FilterManager implements IFilterManager {

	private static final IFilter[] EMPTY = new IFilter[0];
	private IServiceHolderManager<IFilter> m_manager;

	@Override
	public IFilter[] getFilters(String[] filterIds) {
		int n = filterIds.length;
		if (n < 1)
			return EMPTY;

		IServiceHolderManager<IFilter> manager = m_manager;
		IFilter[] filters = new IFilter[n];
		for (int i = 0; i < n; ++i)
			filters[i] = new FilterDelegator(manager.getServiceHolder(filterIds[i]));

		return filters;
	}

	@Override
	public void ungetFilters(String[] filterIds) {
		IServiceHolderManager<IFilter> manager = m_manager;
		for (String filterId : filterIds)
			manager.ungetServiceHolder(filterId);
	}

	protected void activate(BundleContext context) {
		IServiceHolderManager<IFilter> manager = ServiceHolderManager.newInstance(
				context, IFilter.class, IoConstants.FILTER_ID);
		manager.open();
		m_manager = manager;
	}

	protected void deactivate() {
		m_manager.close();
		m_manager = null;
	}
}
