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

import java.io.IOException;
import java.io.OutputStream;

import org.jruyi.common.IBuffer;

final class BufferStream extends OutputStream {

	private IBuffer m_buffer;

	public void buffer(IBuffer buffer) {
		m_buffer = buffer;
	}

	@Override
	public void close() throws IOException {
		m_buffer.close();
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		m_buffer.writeBytes(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		m_buffer.writeBytes(b);
	}

	@Override
	public void write(int b) throws IOException {
		m_buffer.writeByte((byte) b);
	}
}
