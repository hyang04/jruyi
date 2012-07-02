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
package org.jruyi.cmd.internal;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.jruyi.common.StrUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.FrameworkWiring;

public final class BundleCmd {

	private final BundleContext m_context;

	BundleCmd(BundleContext context) {
		m_context = context;
	}

	public Object list() {
		return m_context.getBundles();
	}

	public Object inspect(String bundleId) {
		return m_context.getBundle(Long.parseLong(bundleId));
	}

	public String start(String[] bundles) throws Exception {
		if (bundles == null)
			return null;

		BundleContext context = m_context;
		for (String s : bundles) {
			Bundle bundle = Character.isDigit(s.charAt(0)) ? context
					.getBundle(Long.parseLong(s)) : context.installBundle(s);
			if (bundle == null)
				return StrUtil.buildString("Failed to start bundle: ", s);

			bundle.start();
		}

		return null;
	}

	public String stop(String[] bundleIds) throws Exception {
		if (bundleIds == null)
			return null;

		BundleContext context = m_context;
		for (String bundleId : bundleIds) {
			Bundle bundle = context.getBundle(Long.parseLong(bundleId));
			if (bundle == null)
				return StrUtil.buildString("Bundle[", bundleId, "] Not Found");

			bundle.stop();
		}

		return null;
	}

	public void install(String[] urls) throws Exception {
		if (urls == null)
			return;

		BundleContext context = m_context;
		for (String url : urls)
			context.installBundle(url);
	}

	public String uninstall(String[] bundleIds) throws Exception {
		if (bundleIds == null)
			return null;

		BundleContext context = m_context;
		for (String bundleId : bundleIds) {
			Bundle bundle = context.getBundle(Long.parseLong(bundleId));
			if (bundle != null)
				bundle.uninstall();
			else
				return StrUtil.buildString("Bundle[", bundleId, "] Not Found");
		}

		return null;
	}

	public String update(String[] bundleIds) throws Exception {
		BundleContext context = m_context;
		if (bundleIds == null) {
			Bundle[] bundles = context.getBundles();
			for (Bundle bundle : bundles)
				bundle.update();
			return null;
		}

		for (String bundleId : bundleIds) {
			Bundle bundle = context.getBundle(Long.parseLong(bundleId));
			if (bundle != null)
				bundle.update();
			else
				return StrUtil.buildString("Bundle[", bundleId, "] Not Found");
		}

		return null;
	}

	public String update(String bundleId, String url) throws Exception {
		Bundle bundle = m_context.getBundle(Long.parseLong(bundleId));
		if (bundle == null)
			return StrUtil.buildString("Bundle[", bundleId, "] Not Found");

		InputStream in = new URL(url).openStream();
		try {
			bundle.update(in);
		} finally {
			in.close();
		}

		return null;
	}

	public String refresh(String[] bundleIds) throws Exception {
		BundleContext context = m_context;
		Collection<Bundle> bundles = null;
		if (bundleIds != null && bundleIds.length > 0) {
			bundles = new ArrayList<Bundle>(bundleIds.length);
			for (String id : bundleIds) {
				Bundle bundle = context.getBundle(Long.parseLong(id));
				if (bundle != null)
					bundles.add(bundle);
				else
					return StrUtil.buildString("Bundle[", id, "] Not Found");
			}
		}

		context.getBundle(0).adapt(FrameworkWiring.class)
				.refreshBundles(bundles);
		return null;
	}
}
