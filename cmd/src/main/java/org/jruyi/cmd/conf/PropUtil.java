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
package org.jruyi.cmd.conf;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Map;

import org.jruyi.common.Properties;
import org.jruyi.common.StringBuilder;
import org.jruyi.common.StrUtil;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

public final class PropUtil {

	enum Type {

		STRING {

			@Override
			public Object convert(String str) {
				return str;
			}

			@Override
			public Object[] convert(String[] strs) {
				return strs;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == String.class;
			}
		},
		LONG {

			@Override
			public Object convert(String str) {
				return Long.valueOf(str);
			}

			@Override
			public Object[] convert(String[] strs) {
				int n = strs.length;
				Long[] objs = new Long[n];
				for (int i = 0; i < n; ++i)
					objs[i] = Long.valueOf(strs[i]);

				return objs;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Long.class;
			}
		},
		INTEGER {

			@Override
			public Object convert(String str) {
				return Integer.valueOf(str);
			}

			@Override
			public Object[] convert(String[] strs) {
				int n = strs.length;
				Integer[] objs = new Integer[n];
				for (int i = 0; i < n; ++i)
					objs[i] = Integer.valueOf(strs[i]);

				return objs;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Integer.class;
			}
		},
		SHORT {

			@Override
			public Object convert(String str) {
				return Short.valueOf(str);
			}

			@Override
			public Object[] convert(String[] strs) {
				int n = strs.length;
				Short[] objs = new Short[n];
				for (int i = 0; i < n; ++i)
					objs[i] = Short.valueOf(strs[i]);

				return objs;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Short.class;
			}
		},
		CHARACTER {

			@Override
			public Object convert(String str) {
				return str.charAt(0);
			}

			@Override
			public Object[] convert(String[] strs) {
				int n = strs.length;
				Character[] objs = new Character[n];
				for (int i = 0; i < n; ++i)
					objs[i] = strs[i].charAt(0);

				return objs;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Character.class;
			}
		},
		BYTE {

			@Override
			public Object convert(String str) {
				return Byte.valueOf(str);
			}

			@Override
			public Object[] convert(String[] strs) {
				int n = strs.length;
				Byte[] objs = new Byte[n];
				for (int i = 0; i < n; ++i)
					objs[i] = Byte.valueOf(strs[i]);

				return objs;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Byte.class;
			}
		},
		DOUBLE {

			@Override
			public Object convert(String str) {
				return Double.valueOf(str);
			}

			@Override
			public Object[] convert(String[] strs) {
				int n = strs.length;
				Double[] objs = new Double[n];
				for (int i = 0; i < n; ++i)
					objs[i] = Double.valueOf(strs[i]);

				return objs;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Double.class;
			}
		},
		FLOAT {

			@Override
			public Object convert(String str) {
				return Float.valueOf(str);
			}

			@Override
			public Object[] convert(String[] strs) {
				int n = strs.length;
				Float[] objs = new Float[n];
				for (int i = 0; i < n; ++i)
					objs[i] = Float.valueOf(strs[i]);

				return objs;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Float.class;
			}
		},
		BOOLEAN {

			@Override
			public Object convert(String str) {
				return Boolean.valueOf(str);
			}

			@Override
			public Object[] convert(String[] strs) {
				int n = strs.length;
				Boolean[] objs = new Boolean[n];
				for (int i = 0; i < n; ++i)
					objs[i] = Boolean.valueOf(strs[i]);

				return objs;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Boolean.class;
			}
		};

		public abstract Object convert(String str);

		public abstract Object[] convert(String[] strs);

		public abstract boolean checkType(Class<?> clazz);

		public static Type valueOf(int type) {

			switch (type) {
			case AttributeDefinition.STRING:
				return STRING;
			case AttributeDefinition.INTEGER:
				return INTEGER;
			case AttributeDefinition.LONG:
				return LONG;
			case AttributeDefinition.BOOLEAN:
				return BOOLEAN;
			case AttributeDefinition.SHORT:
				return SHORT;
			case AttributeDefinition.FLOAT:
				return FLOAT;
			case AttributeDefinition.DOUBLE:
				return DOUBLE;
			case AttributeDefinition.BYTE:
				return BYTE;
			case AttributeDefinition.CHARACTER:
				return CHARACTER;
			}

			throw new RuntimeException(StrUtil.buildString("Unknown Type: ",
					type));
		}
	}

	private PropUtil() {
	}

	public static Properties normalize(Dictionary<String, ?> props,
			ObjectClassDefinition ocd) throws Exception {
		AttributeDefinition[] ads = ocd
				.getAttributeDefinitions(ObjectClassDefinition.REQUIRED);
		if (ads != null && ads.length > 0 && props == null)
			throw new RuntimeException(StrUtil.buildString("Property[",
					ads[0].getID(), "] is required"));

		Properties conf = new Properties();
		if (ads != null)
			normalize(props, conf, ads, true);

		ads = ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL);
		if (ads != null)
			normalize(props, conf, ads, false);

		return conf;
	}

	private static void normalize(Dictionary<String, ?> props,
			Map<String, Object> conf, AttributeDefinition[] ads,
			boolean required) throws Exception {
		for (AttributeDefinition ad : ads) {
			String id = ad.getID();
			Object value = props.get(id);
			if (value == null) {
				handleDefaultValues(ad, conf, required);
				continue;
			}

			Class<?> clazz = value.getClass();
			if (clazz == String.class) {
				handleStringValue(ad, (String) value, conf, required);
				continue;
			}

			Type type = Type.valueOf(ad.getType());
			if (clazz.isArray()) {
				Object[] values = (Object[]) value;
				if (!type.checkType(values[0].getClass()))
					throw new Exception(StrUtil.buildString("Property[", id,
							"] should be of type: ", type));
				for (Object obj : values) {
					String message = ad.validate(String.valueOf(obj));
					if (message != null && message.length() > 0)
						throw new Exception(StrUtil.buildString(
								"Error Property[", id, "]: ", message));
				}
			} else {
				if (!type.checkType(clazz))
					throw new Exception(StrUtil.buildString("Property[", id,
							"] should be of type: ", type));
				String message = ad.validate(String.valueOf(value));
				if (message != null && message.length() > 0)
					throw new Exception(StrUtil.buildString("Error Property[",
							id, "]: ", message));
			}

			conf.put(id, value);
		}
	}

	private static void handleStringValue(AttributeDefinition ad, String value,
			Map<String, Object> conf, boolean required) throws Exception {
		String id = ad.getID();
		String[] values = ad.getCardinality() != 0 ? split(value) : null;
		String[] optionValues = ad.getOptionValues();
		if (optionValues != null && optionValues.length > 0) {
			if (values != null) {
				for (String v : values)
					validate(id, v, optionValues);
			} else
				validate(id, value, optionValues);
		} else {
			if (values != null) {
				for (String v : values)
					validate(id, v, ad);
			} else
				validate(id, value, ad);
		}

		Type type = Type.valueOf(ad.getType());
		if (values == null)
			conf.put(id, type.convert(value));
		else
			conf.put(id, type.convert(values));
	}

	private static void handleDefaultValues(AttributeDefinition ad,
			Map<String, Object> conf, boolean required) throws Exception {
		String id = ad.getID();
		String[] defaultValues = ad.getDefaultValue();
		if (defaultValues != null) {
			Type type = Type.valueOf(ad.getType());
			if (ad.getCardinality() == 0)
				conf.put(id, type.convert(defaultValues[0]));
			else
				conf.put(id, type.convert(defaultValues));
		} else if (required)
			throw new Exception(StrUtil.buildString("Property[", id,
					"] is required"));
	}

	private static String[] split(String value) {
		ArrayList<String> list = new ArrayList<String>();
		StringBuilder builder = StringBuilder.get();
		try {
			int n = value.length();
			boolean filter = true;
			for (int i = 0; i < n; ++i) {
				char c = value.charAt(i);
				if (filter) {
					if (Character.isWhitespace(c))
						continue;

					filter = false;
				}

				if (c == '\\') {
					if (++i >= n)
						break;

					builder.append(value.charAt(i));
				} else if (c != ',')
					builder.append(c);
				else {
					addString(list, builder);
					filter = true;
				}
			}

			addString(list, builder);

			return list.size() < 1 ? null : list
					.toArray(new String[list.size()]);
		} finally {
			builder.close();
		}
	}

	private static void addString(ArrayList<String> list, StringBuilder builder) {
		int j = builder.length() - 1;
		while (j >= 0 && Character.isWhitespace(builder.charAt(j)))
			--j;

		if (j >= 0)
			list.add(builder.substring(0, ++j));
		builder.setLength(0);
	}

	private static void makeStringTo(StringBuilder builder, String[] values) {
		builder.append(values[0]);
		int n = values.length;
		for (int i = 1; i < n; ++i)
			builder.append(',').append(values[i]);
	}

	private static void validate(String id, String value, String[] optionValues)
			throws Exception {
		for (String optionValue : optionValues) {
			if (optionValue.equals(value))
				return;
		}

		StringBuilder builder = StringBuilder.get();
		try {
			builder.append("Illegal Property[").append(id).append('=')
					.append(value).append("]: {");
			makeStringTo(builder, optionValues);
			builder.append('}');
			throw new Exception(builder.toString());
		} finally {
			builder.close();
		}
	}

	private static void validate(String id, String value, AttributeDefinition ad)
			throws Exception {
		String message = ad.validate(value);
		if (message != null && message.length() > 0)
			throw new Exception(StrUtil.buildString("Error Property[", id,
					"]: ", message));
	}
}
