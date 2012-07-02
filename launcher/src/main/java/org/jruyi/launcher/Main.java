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
package org.jruyi.launcher;

import static org.jruyi.system.Constants.JRUYI_HOME_DIR;
import static org.jruyi.system.Constants.JRUYI_INST_CONF_URL;
import static org.jruyi.system.Constants.JRUYI_INST_NAME;
import static org.jruyi.system.Constants.JRUYI_URL;
import static org.jruyi.system.Constants.JRUYI_VENDOR;
import static org.jruyi.system.Constants.JRUYI_VERSION;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.osgi.framework.Constants;

public final class Main {

	private static final String LOGBACK_CONF = "logback.configurationFile";
	private static Object m_ruyi;

	private Main() {
	}

	public static void main(String[] args) {

		try {
			init();

			if (args.length > 0 && !processCommandLines(args))
				return;

			if (System.getProperty(JRUYI_INST_NAME) == null)
				System.setProperty(JRUYI_INST_NAME, "default");

			Runtime.getRuntime().addShutdownHook(new ShutdownHook());

		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		try {
			m_ruyi.getClass().getMethod("startAndWait").invoke(m_ruyi);
		} catch (Exception e) {
			e.printStackTrace();
			stop();
		}
	}

	public static void start(String[] args) {
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		try {
			m_ruyi.getClass().getMethod("startAndWait").invoke(m_ruyi);
		} catch (Exception e) {
			e.printStackTrace();
			stop();
		}
	}

	public static void stop(String[] args) {
		stop();
	}

	private static void stop() {
		try {
			m_ruyi.getClass().getMethod("stop").invoke(m_ruyi);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void init() throws Exception {
		ClassLoader classLoader = Main.class.getClassLoader();
		if (!(classLoader instanceof URLClassLoader))
			classLoader = new URLClassLoader(new URL[0], classLoader);

		Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL",
				URL.class);
		boolean accessible = addUrl.isAccessible();
		if (!accessible)
			addUrl.setAccessible(true);

		ArrayList<String> pkgList = new ArrayList<String>();
		File[] jars = getLibJars();
		for (File jar : jars) {
			String exportPackages = new JarFile(jar).getManifest()
					.getMainAttributes().getValue("Export-Package");
			if (exportPackages != null)
				pkgList.add(exportPackages);
			addUrl.invoke(classLoader, jar.getCanonicalFile().toURI().toURL());
		}

		if (!accessible)
			addUrl.setAccessible(false);

		int n = pkgList.size();
		if (n < 1)
			return;

		StringBuilder builder = new StringBuilder(pkgList.get(0));
		for (int i = 1; i < n; ++i)
			builder.append(',').append(pkgList.get(i));

		HashMap<String, String> props = new HashMap<String, String>(3);
		props.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, builder.toString());

		Class<?> clazz = classLoader.loadClass("org.jruyi.system.main.Ruyi");
		Object ruyi = clazz.getMethod("getInstance").invoke(null);
		clazz.getMethod("setProperties", Map.class).invoke(ruyi, props);
		String instConfUrl = (String) clazz.getMethod("getProperty",
				String.class).invoke(ruyi, JRUYI_INST_CONF_URL);
		System.setProperty(LOGBACK_CONF, instConfUrl + "logback.xml");

		m_ruyi = ruyi;
	}

	private static File[] getLibJars() throws Exception {
		// Home Dir
		File homeDir = null;
		String temp = System.getProperty(JRUYI_HOME_DIR);
		if (temp == null) {
			String classpath = System.getProperty("java.class.path");
			int index = classpath.toLowerCase().indexOf("jruyi-launcher");
			int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
			if (index >= start) {
				temp = classpath.substring(start, index);
				homeDir = new File(temp).getParentFile();
			} else
				// use current dir
				homeDir = new File(System.getProperty("user.dir"));
		} else
			homeDir = new File(temp);

		homeDir = homeDir.getCanonicalFile();

		System.setProperty(JRUYI_HOME_DIR, homeDir.getCanonicalPath());

		return new File(homeDir, "lib").listFiles(new JarFileFilter());
	}

	private static boolean processCommandLines(String[] args) throws Exception {
		Options options = new Options();
		options.addOption("?", "help", false, null);
		options.addOption("v", "version", false, null);
		Option o = new Option("D", true, null);
		o.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(o);
		options.addOption("r", "run", true, null);

		CommandLine line = new PosixParser().parse(options, args);

		Option[] opts = line.getOptions();
		for (Option option : opts) {
			String opt = option.getOpt();
			if (opt.equals("?")) {
				printHelp();
				return false;
			} else if (opt.equals("v")) {
				printVersion();
				return false;
			} else if (opt.equals("D")) {
				handleSystemProps(option.getValues());
			} else if (opt.equals("r")) {
				System.setProperty(JRUYI_INST_NAME, option.getValue().trim());
			} else
				throw new Exception("Unknown option: " + option);
		}

		return true;
	}

	private static void printHelp() {
		String programName = System.getProperty("program.name");
		System.out.println();
		System.out.println("Usage: " + programName + " [options]");
		System.out.println();
		System.out.println("options:");
		System.out
				.println("    -?, --help              print this help message");
		System.out
				.println("    -v, --version           print version information");
		System.out.println("    -D<name>[=<value>]      add a system property");
		System.out
				.println("    -r, --run=<instance>    run the specified instance");
		System.out.println();
	}

	private static void printVersion() throws Exception {
		Object ruyi = m_ruyi;
		Method getProperty = ruyi.getClass().getMethod("getProperty",
				String.class);

		System.out.print("JRuyi Version: ");
		System.out.println(getProperty.invoke(ruyi, JRUYI_VERSION));

		System.out.print("JRuyi Vendor: ");
		System.out.println(getProperty.invoke(ruyi, JRUYI_VENDOR));

		System.out.print("JRuyi URL: ");
		System.out.println(getProperty.invoke(ruyi, JRUYI_URL));

		System.out.println();
	}

	private static void handleSystemProps(String[] args) {
		for (String arg : args) {
			String name = null;
			String value = null;
			int i = arg.indexOf('=');
			if (i < 0) {
				name = arg;
				value = "true";
			} else {
				name = arg.substring(0, i);
				value = arg.substring(i + 1, arg.length());
			}
			System.setProperty(name, value);
		}
	}
}
