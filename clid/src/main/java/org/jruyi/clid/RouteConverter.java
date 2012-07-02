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
import org.jruyi.common.StringBuilder;
import org.jruyi.me.IRoute;

final class RouteConverter implements Converter {

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
				IRoute[] routes = (IRoute[]) obj;
				for (IRoute route : routes) {
					format(builder, route);
					builder.append(CRLF);
				}

			} else
				format(builder, (IRoute) obj);

			return builder.toString();
		} finally {
			builder.close();
		}
	}

	private void format(StringBuilder builder, IRoute route) {
		builder.append('[').append(route.getFrom()).append("] -> [")
				.append(route.getTo()).append("]: ").append(route.getFilter());
	}
}
