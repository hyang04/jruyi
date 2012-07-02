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

import org.apache.felix.service.command.Converter;
import org.jruyi.me.IRoute;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;

public final class ConverterImpl implements Converter {

	private static final Class<?>[] CLAZZES = { Configuration.class,
			IRoute.class, Bundle.class };
	private final Converter[] FORMATTERS;
	private BundleContext m_context;

	public ConverterImpl() {
		FORMATTERS = new Converter[] { new ConfigurationConverter(),
				new RouteConverter(), new BundleConverter(this) };
	}

	@Override
	public Object convert(Class<?> desiredType, Object in) throws Exception {
		// TODO
		return null;
	}

	@Override
	public CharSequence format(Object target, int level, Converter converter)
			throws Exception {
		Class<?> clazz = target.getClass();
		if (clazz.isArray())
			clazz = clazz.getComponentType();

		if (clazz.isPrimitive())
			return null;

		int n = CLAZZES.length;
		for (int i = 0; i < n; ++i) {
			if (CLAZZES[i].isAssignableFrom(clazz))
				return FORMATTERS[i].format(target, level, converter);
		}

		return null;
	}

	protected void activate(BundleContext context) {
		m_context = context;
	}

	protected void deactivate() {
		m_context = null;
	}

	BundleContext bundleContext() {
		return m_context;
	}
}
