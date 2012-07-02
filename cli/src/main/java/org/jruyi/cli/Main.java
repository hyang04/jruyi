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
package org.jruyi.cli;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

public final class Main {

	public static final Main INST = new Main();
	private final Session m_session = new Session();
	private String m_host = "localhost";
	private int m_port = 6060;
	private int m_timeout;

	static final class JarFileFilter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String name) {
			return name.startsWith("commons-cli") && name.endsWith(".jar");
		}
	}

	static final class ShutdownHook extends Thread {

		ShutdownHook() {
			super("JRuyi-CLI Shutdown Hook");
		}

		@Override
		public void run() {
			try {
				INST.shutdown();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Main() {
	}

	public static void main(String[] args) {
		try {
			init();

			if (args.length > 0 && !INST.processCommandLines(args))
				return;

			Runtime.getRuntime().addShutdownHook(new ShutdownHook());

			INST.start();

		} catch (InterruptedException e) {
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			INST.shutdown();
		}
	}

	void shutdown() {
		m_session.close();
	}

	private static void init() throws Exception {
		ClassLoader classLoader = Main.class.getClassLoader();
		Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL",
				URL.class);
		boolean accessible = addUrl.isAccessible();
		if (!accessible)
			addUrl.setAccessible(true);

		File[] jars = getLibJars();
		for (File jar : jars)
			addUrl.invoke(classLoader, jar.getCanonicalFile().toURI().toURL());

		if (!accessible)
			addUrl.setAccessible(false);
	}

	private static File[] getLibJars() throws Exception {
		// Home Dir
		File homeDir = null;
		String temp = System.getProperty("jruyi.home.dir");
		if (temp == null) {
			temp = Main.class.getProtectionDomain().getCodeSource()
					.getLocation().getFile();
			temp = URLDecoder.decode(temp, "UTF-8");
			homeDir = new File(temp).getParentFile().getParentFile()
					.getCanonicalFile();
		} else
			homeDir = new File(temp).getCanonicalFile();

		return new File(homeDir, "lib").listFiles(new JarFileFilter());
	}

	private void start() throws Exception {
		Console console = System.console();
		Session session = m_session;
		session.open(m_host, m_port, m_timeout);
		session.recv(console.writer());

		String cmdLine = null;
		do {
			cmdLine = console.readLine();
			if (cmdLine == null || cmdLine.equalsIgnoreCase("quit")
					|| cmdLine.equalsIgnoreCase("exit"))
				break;
		} while (session.send(cmdLine));
	}

	// Exit if false is returned.
	private boolean processCommandLines(String[] args) throws Exception {

		Options options = new Options();
		options.addOption("?", "help", false, null);
		options.addOption("v", "version", false, null);
		options.addOption("h", "host", true, null);
		options.addOption("p", "port", true, null);
		options.addOption("t", "timeout", true, null);

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
			} else if (opt.equals("h")) {
				String v = option.getValue();
				if (v != null)
					m_host = v;
			} else if (opt.equals("p")) {
				String v = option.getValue();
				if (v != null)
					m_port = Integer.parseInt(v);
			} else if (opt.equals("t")) {
				String v = option.getValue();
				if (v != null)
					m_timeout = Integer.parseInt(v) * 1000;
			} else
				throw new Exception("Unknown option: " + option);
		}

		final String[] scripts = line.getArgs();
		if (scripts.length < 1)
			return true;

		run(scripts);

		return false;
	}

	private void printHelp() {
		String programName = System.getProperty("program.name");
		System.out.println();
		System.out.println("Usage: " + programName
				+ " [options] [script] [script1] ...");
		System.out.println();
		System.out.println("options:");
		System.out
				.println("    -?, --help                print this help message");
		System.out
				.println("    -v, --version             print version information");
		System.out
				.println("    -h, --host=<host_name>    the remote host to connect");
		System.out
				.println("    -p, --port=<port_num>     the remote port to connect");
		System.out
				.println("    -t, --timeout=<seconds>   the time to wait for response");
		System.out.println();
	}

	private void printVersion() {
	}

	private void run(String[] scripts) throws Exception {
		Session session = m_session;
		session.open(m_host, m_port, m_timeout);
		session.recv(DummyWriter.INST);

		String cmd = "prompt=''";
		session.send(cmd);

		Writer writer = System.console().writer();
		byte[] buffer = new byte[1024 * 16];
		for (String name : scripts) {
			File script = new File(name);
			if (!script.exists())
				throw new Exception("File Not Found: " + name);

			if (!script.isFile())
				throw new Exception("Invalid script file: " + name);

			int n = (int) script.length();
			if (n < 1)
				continue;

			session.writeLength(n);

			InputStream in = new FileInputStream(script);
			try {
				do {
					int len = buffer.length;
					int off = 0;
					while (len > 0 && (n = in.read(buffer, off, len)) > 0) {
						off += n;
						len -= n;
					}
					session.writeChunk(buffer, 0, off);
				} while (n > 0);

				session.flush();
			} finally {
				in.close();
			}

			session.recv(writer);
		}
	}
}
