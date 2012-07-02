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

import java.util.Dictionary;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.service.command.Descriptor;
import org.jruyi.common.Properties;
import org.jruyi.common.StrUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Conf {

	private static final Logger m_logger = LoggerFactory.getLogger(Conf.class);
	private OcdTracker m_tracker;
	private ConfigurationAdmin m_ca;

	static final class IdsPair {

		private String[] m_pids;
		private String[] m_factoryIds;

		void pids(String[] pids) {
			m_pids = pids;
		}

		String[] pids() {
			return m_pids;
		}

		void factoryPids(String[] factoryIds) {
			m_factoryIds = factoryIds;
		}

		String[] factoryPids() {
			return m_factoryIds;
		}
	}

	static final class OcdTracker extends BundleTracker {

		private final ConcurrentHashMap<String, ObjectClassDefinition> m_pidOcds;
		private final ConcurrentHashMap<String, ObjectClassDefinition> m_factoryPidOcds;
		private final MetaTypeService m_mts;

		public OcdTracker(BundleContext context, MetaTypeService mts) {
			super(context, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING
					| Bundle.ACTIVE | Bundle.STOPPING, null);

			m_pidOcds = new ConcurrentHashMap<String, ObjectClassDefinition>(
					256);
			m_factoryPidOcds = new ConcurrentHashMap<String, ObjectClassDefinition>(
					256);
			m_mts = mts;
		}

		@Override
		public Object addingBundle(Bundle bundle, BundleEvent event) {
			IdsPair idsPair = new IdsPair();
			setOcds(bundle, idsPair);

			return idsPair;
		}

		@Override
		public void modifiedBundle(Bundle bundle, BundleEvent event, Object obj) {
			if (event.getType() != BundleEvent.UPDATED)
				return;

			IdsPair idsPair = (IdsPair) obj;
			unsetOcds(idsPair);
			setOcds(bundle, idsPair);
		}

		@Override
		public void removedBundle(Bundle bundle, BundleEvent event, Object obj) {
			unsetOcds((IdsPair) obj);
		}

		ObjectClassDefinition pidOcd(String pid) {
			return m_pidOcds.get(pid);
		}

		ObjectClassDefinition factoryPidOcd(String factoryPid) {
			return m_factoryPidOcds.get(factoryPid);
		}

		private void setOcds(Bundle bundle, IdsPair idsPair) {
			MetaTypeInformation mti = m_mts.getMetaTypeInformation(bundle);
			ConcurrentHashMap<String, ObjectClassDefinition> ocds = m_pidOcds;
			String[] ids = mti.getPids();
			for (String id : ids) {
				if (ocds.putIfAbsent(id, mti.getObjectClassDefinition(id, null)) != null)
					m_logger.error(StrUtil.buildString(
							"ObjectClassDefinition for pid=", id,
							" is not unique"));
			}

			idsPair.pids(ids);

			ocds = m_factoryPidOcds;
			ids = mti.getFactoryPids();
			for (String id : ids) {
				if (ocds.putIfAbsent(id, mti.getObjectClassDefinition(id, null)) != null)
					m_logger.error(StrUtil.buildString(
							"ObjectClassDefinition for factoryPid=", id,
							" is not unique"));
			}

			idsPair.factoryPids(ids);
		}

		private void unsetOcds(IdsPair idsPair) {
			ConcurrentHashMap<String, ObjectClassDefinition> ocds = m_pidOcds;
			String[] ids = idsPair.pids();
			for (String id : ids)
				ocds.remove(id);

			ocds = m_factoryPidOcds;
			ids = idsPair.factoryPids();
			for (String id : ids)
				ocds.remove(id);
		}
	}

	@Descriptor("Create a configuration")
	public void create(@Descriptor("<pid|factoryPid>") String id,
			@Descriptor("[name=value] ...") String[] args) throws Exception {

		if (args == null || args.length < 1)
			return;

		Properties props = new Properties(args.length);
		for (String arg : args) {
			int i = arg.indexOf('=');
			if (i < 1 || i >= arg.length() - 1)
				continue;

			props.put(arg.substring(0, i), arg.substring(i + 1));
		}

		ObjectClassDefinition ocd = m_tracker.factoryPidOcd(id);
		boolean factory = true;
		if (ocd == null) {
			ocd = m_tracker.pidOcd(id);
			if (ocd == null)
				throw new Exception(StrUtil.buildString("Metatype NOT Found: ",
						id));

			factory = false;
		}

		props = PropUtil.normalize(props, ocd);

		Configuration conf = factory ? m_ca
				.createFactoryConfiguration(id, null) : m_ca.getConfiguration(
				id, null);

		conf.update(props);
	}

	@Descriptor("Update configuration(s)")
	public void update(@Descriptor("<pid|filter>") String filter,
			@Descriptor("[name[=value]] ...") String[] args) throws Exception {
		if (args == null || args.length < 1)
			return;

		Configuration[] confs = m_ca
				.listConfigurations(normalizeFilter(filter));

		if (confs == null || confs.length < 1) {
			System.err.println(StrUtil.buildString(
					"Configuration(s) NOT Found: ", filter));
			return;
		}

		for (Configuration conf : confs) {
			@SuppressWarnings("unchecked")
			Dictionary<String, Object> props = conf.getProperties();
			boolean modified = false;
			for (String arg : args) {
				int i = arg.indexOf('=');
				String name = i < 0 ? arg : arg.substring(0, i);
				if (i < 0) {
					if (props.remove(name) != null)
						modified = true;
				} else {
					Object newValue = arg.substring(i + 1);
					Object oldValue = props.put(name, newValue);
					if (!newValue.equals(oldValue))
						modified = true;
				}
			}

			if (modified) {
				String id = conf.getFactoryPid();
				ObjectClassDefinition ocd = id == null ? m_tracker.pidOcd(conf
						.getPid()) : m_tracker.factoryPidOcd(id);
				props = PropUtil.normalize(props, ocd);
				conf.update(props);
			}
		}
	}

	@Descriptor("Delete configuration(s)")
	public void delete(@Descriptor("[pid|filter]") String filter)
			throws Exception {
		Configuration[] confs = m_ca
				.listConfigurations(normalizeFilter(filter));
		if (confs == null)
			return;

		for (Configuration conf : confs)
			conf.delete();
	}

	@Descriptor("List configuration(s)")
	public Configuration[] list(@Descriptor("[pid|filter]") String[] args) throws Exception {
		String filter = (args != null && args.length > 0) ? normalizeFilter(args[0])
				: null;
		return m_ca.listConfigurations(filter);
	}

	protected void setConfigurationAdmin(ConfigurationAdmin ca) {
		m_ca = ca;
	}

	protected void unsetConfigurationAdmin(ConfigurationAdmin ca) {
		m_ca = null;
	}

	protected void activate(ComponentContext context) {
		MetaTypeService mts = (MetaTypeService) context.locateService("mts");
		m_tracker = new OcdTracker(context.getBundleContext(), mts);
		m_tracker.open();
	}

	protected void deactivate() {
		m_tracker.close();
		m_tracker = null;
	}

	private String normalizeFilter(String filter) {
		if (filter == null)
			return null;

		filter = filter.trim();
		if (filter.charAt(0) != '(')
			filter = StrUtil.buildString("(" + Constants.SERVICE_PID + "=",
					filter, ')');

		return filter;
	}
}
