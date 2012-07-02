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

import java.util.Dictionary;

import org.apache.felix.service.command.Converter;
import org.jruyi.common.StringBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

public final class BundleConverter implements Converter {

	private static final String CRLF = "\r\n";
	private ConverterImpl m_converterImpl;

	public BundleConverter(ConverterImpl converterImpl) {
		m_converterImpl = converterImpl;
	}

	@Override
	public Object convert(Class<?> desiredType, Object in) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CharSequence format(Object obj, int level, Converter converter) {
		StringBuilder builder = StringBuilder.get();
		try {
			StartLevel startLevel = null;
			if (level == Converter.INSPECT) {
				BundleContext context = m_converterImpl.bundleContext();
				ServiceReference reference = context
						.getServiceReference(StartLevel.class.getName());
				startLevel = (StartLevel) context.getService(reference);
			}

			if (!obj.getClass().isArray()) {
				format(builder, (Bundle) obj, level, startLevel);
				return builder.toString();
			}

			if (level == Converter.INSPECT) {
				builder.append("START LEVEL ")
						.append(startLevel.getStartLevel()).append(CRLF);
				// [ ID ][ State ][Level][ Name ]
				builder.append("[ ID ]").append("[  State  ]")
						.append("[Level]").append("[     Name     ]" + CRLF);

				level = Converter.LINE;
			}

			Bundle[] bundles = (Bundle[]) obj;
			for (Bundle bundle : bundles) {
				format(builder, bundle, level, startLevel);
				builder.append(CRLF);
			}

			return builder.toString();
		} finally {
			builder.close();
		}
	}

	private void format(StringBuilder builder, Bundle bundle, int level,
			StartLevel startLevel) {
		switch (level) {
		case Converter.LINE:
			// ID
			builder.append(' ');
			int i = builder.length();
			builder.append(bundle.getBundleId());
			int n = 4 - (builder.length() - i);
			if (n > 0)
				builder.insertFill(i, ' ', n);
			builder.append("  ");
			// State
			String state = getState(bundle.getState());
			n = 9 - state.length();
			if (n > 0)
				builder.appendFill(' ', n);
			builder.append(state);
			builder.append("   ");
			// Start Level
			i = builder.length();
			builder.append(startLevel.getBundleStartLevel(bundle));
			n = 2 - (builder.length() - i);
			if (n > 0)
				builder.insertFill(i, ' ', n);
			builder.append("    ");
			// Name
			builder.append(bundle.getSymbolicName()).append('-')
					.append(bundle.getVersion());
			break;
		case Converter.INSPECT:
			@SuppressWarnings("unchecked")
			Dictionary<String, String> headers = bundle.getHeaders();

			builder.append("     Bundle ID: ").append(bundle.getBundleId())
					.append(CRLF);
			builder.append(" Symbolic Name: ").append(bundle.getSymbolicName())
					.append(CRLF);
			builder.append("       Version: ").append(bundle.getVersion())
					.append(CRLF);
			builder.append("   Bundle Name: ")
					.append(headers.get("Bundle-Name")).append(CRLF);
			builder.append("   Description: ")
					.append(headers.get("Bundle-Description")).append(CRLF);
			builder.append(" Bundle Vendor: ")
					.append(headers.get("Bundle-Vendor")).append(CRLF);
			builder.append("Bundle License: ")
					.append(headers.get("Bundle-License")).append(CRLF);
			builder.append(" Last Modified: ").append(bundle.getLastModified())
					.append(CRLF);
			builder.append("         State: ")
					.append(getState(bundle.getState())).append(CRLF);
			builder.append("   Start Level: ")
					.append(startLevel.getBundleStartLevel(bundle))
					.append(CRLF);
			builder.append("Export-Package: ")
					.append(headers.get("Export-Package")).append(CRLF);
			builder.append("Import-Package: ")
					.append(headers.get("Import-Package")).append(CRLF);
			builder.append("Offer Services: ")
					.deeplyAppend(bundle.getRegisteredServices()).append(CRLF);
			builder.append("  Use Services: ")
					.deeplyAppend(bundle.getServicesInUse()).append(CRLF);
			builder.append("      Location: ").append(bundle.getLocation())
					.append(CRLF);
			break;
		case Converter.PART:
			builder.append(bundle.getBundleId());
			break;
		}
	}

	private String getState(int state) {
		switch (state) {
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		}

		return "UNKNOWN";
	}
}
