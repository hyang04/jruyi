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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.jruyi.common.ListNode;
import org.jruyi.common.StrUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public final class RuyiCmd {

	private static final byte[] CRLF = { '\r', '\n' };
	private final BundleContext m_context;

	public RuyiCmd(BundleContext context) {
		m_context = context;
	}

	public void help() throws Exception {
		ServiceReference<?>[] references = m_context.getAllServiceReferences(
				null, "(" + CommandProcessor.COMMAND_SCOPE + "=*)");
		final ListNode<String> head = ListNode.create();
		for (ServiceReference<?> reference : references) {
			String scope = String.valueOf(reference
					.getProperty(CommandProcessor.COMMAND_SCOPE));
			Object v = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);
			if (v instanceof String[]) {
				String[] funcs = (String[]) v;
				for (String func : funcs)
					add(head, StrUtil.buildString(scope, ":", func));
			} else
				add(head, StrUtil.buildString(scope, ":", v));
		}

		ListNode<String> node = head.next();
		while (node != null) {
			System.out.print(node.get());
			System.out.write(CRLF);
			head.next(node.next());
			node.close();
			node = head.next();
		}

		head.close();
	}

	public void help(String command) throws Exception {
		int i = command.indexOf(':');
		if (i == command.length() - 1) {
			System.out.print(StrUtil.buildString("Illegal Command: ", command));
			return;
		}

		String scope = "";
		String function = command;
		if (i >= 0) {
			scope = command.substring(0, i);
			function = command.substring(i + 1);
		}

		if (scope.length() < 1)
			scope = "*";

		String filter = StrUtil.buildString("(&("
				+ CommandProcessor.COMMAND_SCOPE + "=", scope, ")("
				+ CommandProcessor.COMMAND_FUNCTION + "=", function, "))");

		BundleContext context = m_context;
		ServiceReference<?>[] references = context.getAllServiceReferences(
				null, filter);
		if (references == null || references.length < 1) {
			System.out.print(StrUtil
					.buildString("Command Not Found: ", command));
			return;
		}

		ServiceReference<?> reference = references[0];
		scope = (String) reference.getProperty(CommandProcessor.COMMAND_SCOPE);
		Bundle bundle = reference.getBundle();
		URL url = bundle.getEntry(StrUtil.buildString("/HELP-INF/", scope, "/",
				function));
		if (url == null)
			return;

		byte[] buffer = new byte[512];
		int n = 0;
		InputStream in = url.openStream();
		try {
			while ((n = in.read(buffer)) > 0)
				System.out.write(buffer, 0, n);
		} finally {
			in.close();
		}
	}

	public void echo(Object[] args) {
		if (args == null || args.length < 1)
			return;

		System.out.print(args[0]);
		int n = args.length;
		for (int i = 1; i < n; ++i) {
			System.out.print(' ');
			System.out.print(args[i]);
		}
	}

	public void grep(
			@Parameter(names = { "-i", "--ignore-case" }, presentValue = "true", absentValue = "false") @Descriptor("ignore case distinctions") boolean ignoreCase,
			@Parameter(names = { "-v", "--invert-match" }, presentValue = "true", absentValue = "false") @Descriptor("select non-matching lines") boolean invertMatch,
			String regex) throws Exception {

		if (ignoreCase)
			regex = StrUtil.buildString("(?i)", regex);

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher("");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (matcher.reset(line).find() ^ invertMatch) {
					System.out.print(line);
					System.out.write(CRLF);
				}
			}
		} finally {
			reader.close();
		}
	}

	private void add(ListNode<String> node, String cmd) {
		ListNode<String> prev = node;
		while ((node = prev.next()) != null && node.get().compareTo(cmd) < 0)
			prev = node;

		ListNode<String> newNode = ListNode.create();
		newNode.set(cmd);
		prev.next(newNode);
		newNode.next(node);
	}
}
