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
import java.util.Enumeration;

import org.apache.felix.service.command.Converter;
import org.jruyi.common.StringBuilder;
import org.osgi.service.cm.Configuration;

final class ConfigurationConverter implements Converter {

	private static final String CRLF = "\r\n";

	@Override
	public Object convert(Class<?> desiredType, Object in) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CharSequence format(Object obj, int level, Converter converter) {
		StringBuilder builder = StringBuilder.get();
		try {
			if (obj.getClass().isArray()) {
				Configuration[] confs = (Configuration[]) obj;
				if (confs.length == 1)
					format(builder, confs[0], level);
				else {
					for (Configuration conf : confs) {
						format(builder, conf, Converter.LINE);
						builder.append(CRLF);
					}
				}
			} else
				format(builder, (Configuration) obj, level);

			return builder.toString();
		} finally {
			builder.close();
		}
	}

	private void format(StringBuilder builder, Configuration conf, int level) {
		switch (level) {
		case Converter.INSPECT:
			inspect(builder, conf);
			break;
		case Converter.LINE:
			line(builder, conf);
			break;
		case Converter.PART:
			builder.append(conf.getPid());
			break;
		}
	}

	private void inspect(StringBuilder builder, Configuration conf) {
		String pid = conf.getPid();
		String factoryPid = conf.getFactoryPid();
		builder.append("pid: ").append(pid);
		if (factoryPid != null)
			builder.append(CRLF + "factoryPid: ").append(factoryPid);

		builder.append(CRLF + "bundleLocation: ").append(
				conf.getBundleLocation());
		builder.append(CRLF + "properties: ");

		@SuppressWarnings("unchecked")
		Dictionary<String, ?> props = conf.getProperties();
		Enumeration<String> keys = props.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			builder.append(CRLF + "\t").append(key).append('=');
			Object value = props.get(key);
			if (value.getClass().isArray()) {
				Object[] values = (Object[]) value;
				builder.append(values[0]);
				int n = values.length;
				for (int i = 1; i < n; ++i)
					builder.append(", ").append(values[i]);
			} else
				builder.append(value);
		}
	}

	private void line(StringBuilder builder, Configuration conf) {
		builder.append('{');
		@SuppressWarnings("unchecked")
		Dictionary<String, ?> props = conf.getProperties();
		Enumeration<String> keys = props.keys();
		if (keys.hasMoreElements()) {
			String key = keys.nextElement();
			builder.append(key).append('=').deeplyAppend(props.get(key));
		}

		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			builder.append(", ").append(key).append('=')
					.deeplyAppend(props.get(key));
		}

		builder.append('}');
	}
}
