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
package org.jruyi.common.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.jruyi.common.BiListNode;
import org.jruyi.common.Blob;
import org.jruyi.common.ByteKmp;
import org.jruyi.common.BytesBuilder;
import org.jruyi.common.CharsetCodec;
import org.jruyi.common.IBuffer;
import org.jruyi.common.IBufferReader;
import org.jruyi.common.IBufferWriter;
import org.jruyi.common.IByteSequence;
import org.jruyi.common.ICharsetCodec;
import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.InsufficientDataException;
import org.jruyi.common.StringBuilder;
import org.jruyi.common.ThreadLocalCache;

final class Buffer implements IBuffer {

	private static final IThreadLocalCache<Buffer> m_bufferCache = ThreadLocalCache
			.weakLinkedCache();
	private BufferFactory m_factory;
	private int m_position;
	private int m_mark;
	private int m_size;
	// the unit that holds the position
	private BiListNode<Unit> m_posNode;
	// the unit that holds the mark
	private BiListNode<Unit> m_markNode;
	// the first node
	private BiListNode<Unit> m_head;

	static final class Unit {

		// offset of the next byte to be read
		private int m_position;
		// offset of the marked byte
		private int m_mark;
		// number of bytes contained in this unit
		private int m_size;
		// index of the first byte
		private int m_start;
		// the cyclic byte array containing the data
		private byte[] m_data;
		private ByteBuffer m_bb;

		Unit(int capacity) {
			byte[] data = new byte[capacity];
			m_data = data;
			m_bb = ByteBuffer.wrap(data);
		}

		void setCapacity(int newCapacity) {
			byte[] data = new byte[newCapacity];
			m_data = data;
			m_bb = ByteBuffer.wrap(data);
		}

		void start(int start) {
			m_start = start;
		}

		int start() {
			return m_start;
		}

		void clear() {
			m_start = 0;
			m_position = 0;
			m_mark = 0;
			m_size = 0;
		}

		ByteBuffer byteBuffer() {
			return m_bb;
		}

		int capacity() {
			return m_data.length;
		}

		int position() {
			return m_position;
		}

		/**
		 * Return the number of bytes can be read.
		 */
		int remaining() {
			return m_size - m_position;
		}

		/**
		 * Return the number of bytes can be written.
		 */
		int available() {
			return m_data.length - m_size - m_start;
		}

		int headAvailable() {
			return m_start;
		}

		int size() {
			return m_size;
		}

		void mark() {
			m_mark = m_position;
		}

		void reset() {
			m_position = m_mark;
		}

		void rewind() {
			m_position = m_mark = 0;
		}

		boolean isEmpty() {
			return m_position >= m_size;
		}

		boolean isFull() {
			return m_start + m_size >= m_data.length;
		}

		boolean isHeadFull() {
			return m_start <= 0;
		}

		void size(int newSize) {
			m_size = newSize;
			if (m_position > newSize) {
				m_position = newSize;
				if (m_mark > newSize)
					m_mark = 0;
			}
		}

		/**
		 * {@code fromIndex} must be less than m_size and non-negative.
		 */
		int indexOf(byte b, int fromIndex) {
			byte[] data = m_data;
			int start = m_start;
			int end = start + m_size;
			fromIndex += start;
			while (fromIndex < end) {
				if (data[fromIndex] == b)
					return fromIndex - start;

				++fromIndex;
			}

			return -1;
		}

		int indexOf(byte[] bytes, int leftIndex) {
			byte[] data = m_data;
			int start = m_start;
			int end = start + m_size;
			int index = start + leftIndex;
			int length = bytes.length;

			next: for (; index < end; ++index) {
				leftIndex = index;
				int rightIndex = index + length;
				if (rightIndex > end)
					rightIndex = end;

				int i = 0;
				for (; leftIndex < rightIndex; ++leftIndex, ++i) {
					if (data[leftIndex] != bytes[i])
						continue next;
				}

				return index - start;
			}

			return -1;
		}

		boolean startsWith(byte[] bytes, int offset) {
			byte[] data = m_data;
			int start = m_start;
			int end = bytes.length - offset;
			if (end > m_size)
				end = m_size;

			end += start;
			for (; start < end; ++start, ++offset) {
				if (data[start] != bytes[offset])
					return false;
			}

			return true;
		}

		/**
		 * {@code fromIndex} must be less than m_size and non-negative.
		 */
		int lastIndexOf(byte b, int fromIndex) {
			byte[] data = m_data;
			int start = m_start;
			fromIndex += start;
			while (fromIndex >= start) {
				if (data[fromIndex] == b)
					return fromIndex - start;

				--fromIndex;
			}

			return -1;
		}

		int lastIndexOf(byte[] bytes, int rightIndex) {
			byte[] data = m_data;
			int start = m_start;
			int end = start + rightIndex;
			int length = bytes.length;

			next: for (; end > start; --end) {
				rightIndex = end;
				int leftIndex = end - length;
				if (leftIndex < start)
					leftIndex = start;

				int i = length;
				while (rightIndex > leftIndex) {
					if (data[--rightIndex] != bytes[--i])
						continue next;
				}

				return end - start;
			}

			return -1;
		}

		boolean endsWith(byte[] bytes, int offset) {
			byte[] data = m_data;
			int start = m_start;
			int size = m_size;
			int end = start + size;
			if (offset < size)
				start = end - offset;

			while (end > start) {
				if (data[--end] != bytes[--offset])
					return false;
			}

			return true;
		}

		byte getByte(int position) {
			return m_data[m_start + position];
		}

		void setByte(int position, byte b) {
			m_data[m_start + position] = b;
		}

		int getBytes(int position, byte[] dst, int offset, int length) {
			int n = m_size - position;
			if (n > length)
				n = length;
			else
				length = n;

			System.arraycopy(m_data, m_start + position, dst, offset, length);
			return n;
		}

		int setBytes(int position, byte[] src, int offset, int length) {
			int n = m_size - position;
			if (n > length)
				n = length;
			else
				length = n;

			System.arraycopy(src, offset, m_data, m_start + position, length);
			return n;
		}

		int getBytes(BytesBuilder out, int position, int length) {
			int n = m_size - position;
			if (n > length)
				n = length;
			else
				length = n;

			out.append(m_data, m_start + position, length);
			return n;
		}

		/**
		 * Return an int value by left shifting the next {@code length} bytes
		 * starting at {@code position} into the given {@code i} sequentially.
		 * The {@code length} passed in must be not greater than {@code size()
		 * - position}.
		 * 
		 * @param position
		 *            the offset of the first byte to be left shifted
		 * @param i
		 *            the base int value to be left shifted into
		 * @param length
		 *            number of bytes to be left shifted into {@code i}
		 * @return the resultant int value
		 */
		int getIntB(int position, int i, final int length) {
			position += m_start;
			int end = position + length;
			byte[] data = m_data;
			for (; position < end; ++position)
				i = (i << 8) | (data[position] & 0xFF);

			return i;
		}

		int setIntB(int position, int i, final int length) {
			position += m_start;
			int end = position + length;
			byte[] data = m_data;
			for (; position < end; ++position) {
				data[position] = (byte) (i >>> 24);
				i <<= 8;
			}

			return i;
		}

		/**
		 * Return an int value by right shifting the next {@code length} bytes
		 * starting at {@code position} into the given {@code i} sequentially.
		 * The {@code length} passed in must be not greater than {@code size()
		 * - position}.
		 * 
		 * @param position
		 *            the offset of the first byte to be right shifted
		 * @param i
		 *            the base int value to be right shifted into
		 * @param length
		 *            number of bytes to be right shifted into {@code i}
		 * @return the resultant int value
		 */
		int getIntL(int position, int i, final int length) {
			position += m_start;
			int end = position + length;
			byte[] data = m_data;
			for (; position < end; ++position)
				i = (i >>> 8) | (data[position] << 24);

			return i;
		}

		int setIntL(int position, int i, final int length) {
			position += m_start;
			int end = position + length;
			byte[] data = m_data;
			for (; position < end; ++position) {
				data[position] = (byte) i;
				i >>>= 8;
			}

			return i;
		}

		/**
		 * Return a long value by left shifting the next {@code length} bytes
		 * starting at {@code position} into the given {@code l} sequentially.
		 * The {@code length} passed in must be not greater than {@code size()
		 * - position}.
		 * 
		 * @param position
		 *            the offset of the first byte to be left shifted
		 * @param l
		 *            the base int value to be left shifted into
		 * @param length
		 *            number of bytes to be left shifted into {@code l}
		 * @return the resultant long value
		 */
		long getLongB(int position, long l, final int length) {
			position += m_start;
			int end = position + length;
			byte[] data = m_data;
			for (; position < end; ++position)
				l = (l << 8) | (data[position] & 0xFF);

			return l;
		}

		long setLongB(int position, long l, final int length) {
			position += m_start;
			int end = position + length;
			byte[] data = m_data;
			for (; position < end; ++position) {
				data[position] = (byte) (l >>> 56);
				l <<= 8;
			}

			return l;
		}

		/**
		 * Return a long value by right shifting the next {@code length} bytes
		 * starting at {@code position} into the given {@code l} sequentially.
		 * The {@code length} passed in must be not greater than {@code size()
		 * - position}.
		 * 
		 * @param position
		 *            the offset of the first byte to be right shifted
		 * @param l
		 *            the base int value to be right shifted into
		 * @param length
		 *            number of bytes to be right shifted into {@code l}
		 * @return the resultant long value
		 */
		long getLongL(int position, long l, final int length) {
			position += m_start;
			int end = position + length;
			byte[] data = m_data;
			for (; position < end; ++position)
				l = (l >>> 8) | (((long) data[position]) << 56);

			return l;
		}

		long setLongL(int position, long l, final int length) {
			position += m_start;
			int end = position + length;
			byte[] data = m_data;
			for (; position < end; ++position) {
				data[position] = (byte) l;
				l >>>= 8;
			}

			return l;
		}

		int setFill(int position, byte b, int count) {
			int n = m_size - position;
			if (n > count)
				n = count;
			else
				count = n;

			byte[] data = m_data;
			position += m_start;
			while (count-- > 0)
				data[position++] = b;

			return n;
		}

		/**
		 * Ensure this unit is not empty before calling this method.
		 */
		byte read() {
			return m_data[m_start + m_position++];
		}

		int read(byte[] dst, int offset, int length) {
			int n = remaining();
			if (n > length)
				n = length;
			else
				length = n;

			System.arraycopy(m_data, m_start + m_position, dst, offset, length);
			m_position += n;
			return n;
		}

		int read(BytesBuilder dst, int length) {
			int n = remaining();
			if (n > length)
				n = length;
			else
				length = n;

			dst.append(m_data, m_start + m_position, length);
			m_position += n;
			return n;
		}

		int read(ByteBuffer dst, int length) {
			int n = remaining();
			if (n > length)
				n = length;
			else
				length = n;

			dst.put(m_data, m_start + m_position, length);
			m_position += n;
			return n;
		}

		/**
		 * Return an int value by left shifting the next {@code length} bytes in
		 * the unit into the given {@code i} sequentially. The {@code length}
		 * passed in must be not greater than {@code remaining()}. So
		 * {@code remaining()} should be called to decide {@code length} before
		 * calling this method.
		 * 
		 * @param i
		 *            the base int value to be left shifted into
		 * @param length
		 *            number of bytes to be left shifted into {@code i}
		 * @return the resultant int value
		 */
		int readIntB(int i, final int length) {
			int index = m_start + m_position;
			int end = index + length;
			byte[] data = m_data;
			for (; index < end; ++index)
				i = (i << 8) | (data[index] & 0xFF);

			m_position += length;
			return i;
		}

		/**
		 * Return an int value by right shifting the next {@code length} bytes
		 * in the unit into the given {@code i} sequentially. The {@code length}
		 * passed in must be not greater than {@code remaining()}. So
		 * {@code remaining()} should be called to decide {@code length} before
		 * calling this method.
		 * 
		 * @param i
		 *            the base int value to be right shifted into
		 * @param length
		 *            number of bytes to be right shifted into {@code i}
		 * @return the resultant int value
		 */
		int readIntL(int i, final int length) {
			int index = m_start + m_position;
			int end = index + length;
			byte[] data = m_data;
			for (; index < end; ++index)
				i = (i >>> 8) | (data[index] << 24);

			m_position += length;
			return i;
		}

		/**
		 * Return a long value by left shifting the next {@code length} bytes in
		 * the unit into the given {@code l} sequentially. The {@code length}
		 * passed in must be not greater than {@code remaining()}. So
		 * {@code remaining()} should be called to decide {@code length} before
		 * calling this method.
		 * 
		 * @param l
		 *            the base long value to be left shifted into
		 * @param length
		 *            number of bytes to be left shifted into {@code l}
		 * @return the resultant long value
		 */
		long readLongB(long l, final int length) {
			int index = m_start + m_position;
			int end = index + length;
			byte[] data = m_data;
			for (; index < end; ++index)
				l = (l << 8) | (data[index] & 0xFF);

			m_position += length;
			return l;
		}

		/**
		 * Return a long value by right shifting the next {@code length} bytes
		 * in the unit into the given {@code l} sequentially. The {@code length}
		 * passed in must be not greater than {@code remaining()}. So
		 * {@code remaining()} should be called to decide {@code length} before
		 * calling this method.
		 * 
		 * @param l
		 *            the base long value to be right shifted into
		 * @param length
		 *            number of bytes to be right shifted into {@code l}
		 * @return the resultant long value
		 */
		long readLongL(long l, final int length) {
			int index = m_start + m_position;
			int end = index + length;
			byte[] data = m_data;
			for (; index < end; ++index)
				l = (l >>> 8) | (((long) data[index]) << 56);

			m_position += length;
			return l;
		}

		int skip(int n) {
			int m = remaining();
			if (m > n)
				m = n;

			m_position += m;
			return m;
		}

		/**
		 * Make sure this unit is not full before invoking any {@code write}
		 * method.
		 */
		void writeByte(byte b) {
			m_data[m_start + m_size++] = b;
		}

		int writeFill(byte b, int count) {
			int n = available();
			if (count > n)
				count = n;
			else
				n = count;

			int size = m_start + m_size;
			byte[] data = m_data;
			while (count-- > 0)
				data[size++] = b;

			m_size += n;
			return n;
		}

		void headWriteByte(byte b) {
			m_data[--m_start] = b;

			if (m_position > 0) {
				++m_position;
				++m_mark;
			}

			++m_size;
		}

		int writeBytes(byte[] src, int offset, int length) {
			int n = available();
			if (length > n)
				length = n;
			else
				n = length;

			System.arraycopy(src, offset, m_data, m_start + m_size, length);
			m_size += n;
			return n;
		}

		int headWriteBytes(byte[] src, int offset, int length) {
			int n = headAvailable();
			if (length > n) {
				offset += length - n;
				length = n;
			} else
				n = length;

			int index = m_start - length;
			m_start = index;
			System.arraycopy(src, offset, m_data, index, length);

			if (m_position > 0) {
				m_position += n;
				m_mark += n;
			}

			m_size += n;
			return n;
		}

		int headWriteFill(byte b, int count) {
			int n = headAvailable();
			if (count > n)
				count = n;
			else
				n = count;

			byte[] data = m_data;
			int index = m_start;
			while (count-- > 0)
				data[--index] = b;
			
			m_start = index;

			if (m_position > 0) {
				m_position += n;
				m_mark += n;
			}

			m_size += n;
			return n;
		}

		int writeBytes(IByteSequence src, int offset, int length) {
			int n = available();
			if (length > n)
				length = n;
			else
				n = length;

			src.getBytes(offset, offset + length, m_data, m_start + m_size);
			m_size += n;
			return n;
		}

		int writeBytes(IBufferReader src, int length) {
			int n = available();
			if (length > n)
				length = n;
			else
				n = length;

			src.read(m_data, m_start + m_size, length);
			m_size += n;
			return n;
		}

		int headWriteBytes(IByteSequence src, int offset, int length) {
			int n = headAvailable();
			if (length > n) {
				offset += length - n;
				length = n;
			} else
				n = length;

			int index = m_start - length;
			m_start = index;
			src.getBytes(offset, offset + length, m_data, index);

			if (m_position > 0) {
				m_position += n;
				m_mark += n;
			}

			m_size += n;
			return n;
		}

		int write(ByteBuffer src, int length) {
			int n = available();
			if (length > n)
				length = n;
			else
				n = length;

			src.get(m_data, m_start + m_size, length);
			m_size += n;
			return n;
		}

		int headWrite(ByteBuffer src, int offset, int length) {
			int n = headAvailable();
			if (length > n) {
				offset += length - n;
				length = n;
				src.position(offset);
			} else
				n = length;

			int index = m_start - length;
			m_start = index;
			src.get(m_data, index, length);

			if (m_position > 0) {
				m_position += n;
				m_mark += n;
			}

			m_size += n;
			return n;
		}

		/**
		 * Return an {@code int} value by left shifting the given {@code i} by
		 * {@code (length * 8)} bits. The shifted {@code length} bytes are
		 * written into this unit sequentially. The {@code length} passed in
		 * must be not greater than {@code available()}. So {@code available()}
		 * should be called to decide {@code length} before calling this method.
		 */
		int writeIntB(int i, final int length) {
			byte[] data = m_data;
			int end = m_size;
			m_size = end + length;
			int index = m_start + end;
			end = index + length;
			for (; index < end; ++index) {
				data[index] = (byte) (i >>> 24);
				i <<= 8;
			}

			return i;
		}

		/**
		 * Return an {@code int} value by right shifting the given {@code i} by
		 * {@code (length * 8)} bits. The shifted {@code length} bytes are
		 * written to the head of this unit sequentially. The {@code length}
		 * passed in must be not greater than {@code available()}. So
		 * {@code available()} should be called to decide {@code length} before
		 * calling this method.
		 */
		int headWriteIntB(int i, final int length) {
			byte[] data = m_data;
			int end = m_start;
			int index = end - length;
			while (end > index) {
				data[--end] = (byte) i;
				i >>= 8;
			}

			m_start = end;

			if (m_position > 0) {
				m_position += length;
				m_mark += length;
			}

			m_size += length;

			return i;
		}

		/**
		 * Return an {@code int} value by right shifting the given {@code i} by
		 * {@code (length * 8)} bits. The shifted {@code length} bytes are
		 * written into this unit sequentially. The {@code length} passed in
		 * must be not greater than {@code available()}. So {@code available()}
		 * should be called to decide {@code length} before calling this method.
		 */
		int writeIntL(int i, final int length) {
			byte[] data = m_data;
			int end = m_size;
			m_size = end + length;
			int index = m_start + end;
			end = index + length;
			for (; index < end; ++index) {
				data[index] = (byte) i;
				i >>= 8;
			}

			return i;
		}

		/**
		 * Return an {@code int} value by left shifting the given {@code i} by
		 * {@code (length * 8)} bits. The shifted {@code length} bytes are
		 * written to the head of this unit sequentially. The {@code length}
		 * passed in must be not greater than {@code available()}. So
		 * {@code available()} should be called to decide {@code length} before
		 * calling this method.
		 */
		int headWriteIntL(int i, final int length) {
			byte[] data = m_data;
			int end = m_start;
			int index = end - length;
			while (end > index) {
				data[--end] = (byte) (i >> 24);
				i <<= 8;
			}

			m_start = end;

			if (m_position > 0) {
				m_position += length;
				m_mark += length;
			}

			m_size += length;

			return i;
		}

		/**
		 * Return a {@code long} value by left shifting the given {@code l} by
		 * {@code (length * 8)} bits. The shifted {@code length} bytes are
		 * written into this unit sequentially. The {@code length} passed in
		 * must be not greater than {@code available()}. So {@code available()}
		 * should be called to decide {@code length} before calling this method.
		 */
		long writeLongB(long l, final int length) {
			byte[] data = m_data;
			int end = m_size;
			m_size = end + length;
			int index = m_start + end;
			end = index + length;
			for (; index < end; ++index) {
				data[index] = (byte) (l >>> 56);
				l <<= 8;
			}

			return l;
		}

		/**
		 * Return a {@code long} value by right shifting the given {@code l} by
		 * {@code (length * 8)} bits. The shifted {@code length} bytes are
		 * written to the head of this unit sequentially. The {@code length}
		 * passed in must be not greater than {@code available()}. So
		 * {@code available()} should be called to decide {@code length} before
		 * calling this method.
		 */
		long headWriteLongB(long l, final int length) {
			byte[] data = m_data;
			int end = m_start;
			int index = end - length;
			while (end > index) {
				data[--end] = (byte) l;
				l >>= 8;
			}

			m_start = end;

			if (m_position > 0) {
				m_position += length;
				m_mark += length;
			}

			m_size += length;
			return l;
		}

		/**
		 * Return a {@code long} value by right shifting the given {@code l} by
		 * {@code (length * 8)} bits. The shifted {@code length} bytes are
		 * written into this unit sequentially. The {@code length} passed in
		 * must be not greater than {@code available()}. So {@code available()}
		 * should be called to decide {@code length} before calling this method.
		 */
		long writeLongL(long l, final int length) {
			byte[] data = m_data;
			int end = m_size;
			m_size = end + length;
			int index = m_start + end;
			end = index + length;
			for (; index < end; ++index) {
				data[index] = (byte) l;
				l >>= 8;
			}

			return l;
		}

		/**
		 * Return a {@code long} value by left shifting the given {@code l} by
		 * {@code (length * 8)} bits. The shifted {@code length} bytes are
		 * written to the head of this unit sequentially. The {@code length}
		 * passed in must be not greater than {@code available()}. So
		 * {@code available()} should be called to decide {@code length} before
		 * calling this method.
		 */
		long headWriteLongL(long l, final int length) {
			byte[] data = m_data;
			int end = m_start;
			int index = end - length;
			while (end > index) {
				data[--end] = (byte) (l >> 56);
				l <<= 8;
			}

			m_start = end;

			if (m_position > 0) {
				m_position += length;
				m_mark += length;
			}

			m_size += length;
			return l;
		}

		/**
		 * Slice {@code n} bytes from the end of the data.
		 * 
		 * @param n
		 *            the number of bytes to be sliced from the end
		 */
		Unit cut(int n, BufferFactory factory) {
			Unit unit = null;
			int size = m_size - n;
			if (size >= n) { // copy the slice
				unit = factory.getUnit(n);
				getBytes(size, unit.m_data, 0, n);
				unit.m_start = 0;
			} else { // copy the rest
				unit = factory.getUnit(size);
				getBytes(0, unit.m_data, 0, size);
				byte[] temp = m_data;
				ByteBuffer tempBb = m_bb;
				m_data = unit.m_data;
				m_bb = unit.m_bb;
				unit.m_data = temp;
				unit.m_bb = tempBb;

				unit.m_start = m_start + size;
				m_start = 0;
			}

			unit.m_size = n;
			m_size = size;

			// mark
			n = m_mark;
			if (n > size) {
				unit.m_mark = n - size;
				m_mark = size;
			} else
				unit.m_mark = 0;

			// position
			n = m_position;
			if (n > size) {
				unit.m_position = n - size;
				m_position = size;
			} else
				unit.m_position = 0;

			return unit;
		}

		ByteBuffer getByteBufferForWrite() {
			ByteBuffer bb = m_bb;
			bb.limit(m_data.length);
			bb.position(m_start + m_size);
			return bb;
		}

		void syncWrite() {
			m_size = m_bb.position() - m_start;
		}

		ByteBuffer getByteBufferForRead() {
			ByteBuffer bb = m_bb;
			int start = m_start;
			bb.position(start + m_position);
			bb.limit(start + m_size);
			return bb;
		}

		ByteBuffer getByteBufferForRead(int offset, int length) {
			ByteBuffer bb = m_bb;
			int start = m_start;
			bb.position(start + offset);
			length += offset;
			if (length > m_size)
				length = m_size;
			bb.limit(start + length);
			return bb;
		}

		void syncRead() {
			m_position = m_bb.position() - m_start;
		}

		void addTo(Blob blob, int offset) {
			blob.add(m_data, m_start + offset, m_size - offset);
		}

		void addToWithSize(Blob blob, int size) {
			blob.add(m_data, m_start, size);
		}

		void addTo(Blob blob) {
			blob.add(m_data, m_start, m_size);
		}

		void compact() {
			int position = m_position;
			if (position < 1)
				return;

			m_start += position;
			m_size -= position;
			m_position = 0;
			m_mark = 0;
		}

		int compare(IByteSequence sequence, int from, int len) {
			byte[] data = m_data;
			int i = m_start + m_position;
			len += i;
			for (; i < len; ++i, ++from) {
				byte b1 = data[i];
				byte b2 = sequence.byteAt(from);
				if (b1 != b2)
					return b1 < b2 ? -1 : 1;
			}

			return 0;
		}

		int compare(int i, Unit that, int j, int len) {
			byte[] data = m_data;
			byte[] thatData = that.m_data;
			for (; len > 0; ++i, ++j, --len) {
				byte b1 = data[i];
				byte b2 = thatData[j];
				if (b1 != b2)
					return (b1 < b2) ? -1 : 1;
			}

			return 0;
		}
	}

	final class BufferInputStream extends InputStream {

		private final Buffer m_buffer;

		public BufferInputStream(Buffer buffer) {
			m_buffer = buffer;
		}

		@Override
		public int available() throws IOException {
			return m_buffer.remaining();
		}

		@Override
		public void close() throws IOException {
			m_buffer.close();
		}

		@Override
		public synchronized void mark(int readlimit) {
			m_buffer.mark();
		}

		@Override
		public boolean markSupported() {
			return true;
		}

		@Override
		public int read() throws IOException {
			return m_buffer.readUByte();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			return m_buffer.read(b, off, len);
		}

		@Override
		public int read(byte[] b) throws IOException {
			return m_buffer.read(b, 0, b.length);
		}

		@Override
		public synchronized void reset() throws IOException {
			m_buffer.reset();
		}

		@Override
		public long skip(long n) throws IOException {
			return m_buffer.skip((int) n);
		}
	}

	static final class BufferOutputStream extends OutputStream {

		private final Buffer m_buffer;

		public BufferOutputStream(Buffer buffer) {
			m_buffer = buffer;
		}

		@Override
		public void close() throws IOException {
			m_buffer.close();
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			m_buffer.writeBytes(b, off, len);
		}

		@Override
		public void write(byte[] b) throws IOException {
			m_buffer.writeBytes(b, 0, b.length);
		}

		@Override
		public void write(int b) throws IOException {
			m_buffer.writeByte((byte) b);
		}
	}

	private Buffer() {
		// m_units = new Unit[unitListInitialCapacity];
		BiListNode<Unit> head = BiListNode.create();
		head.previous(head);
		head.next(head);
		m_head = head;
		m_markNode = head;
		m_posNode = head;
	}

	static Buffer get(BufferFactory factory) {
		Buffer buffer = m_bufferCache.take();
		if (buffer == null)
			buffer = new Buffer();

		// buffer.m_units[buffer.m_lastUnitIndex] = factory.getUnit();
		buffer.m_head.set(factory.getUnit());
		buffer.m_factory = factory;

		return buffer;
	}

	private static Buffer getForSlice(BufferFactory factory) {
		Buffer buffer = m_bufferCache.take();
		if (buffer == null)
			buffer = new Buffer();

		buffer.m_factory = factory;
		return buffer;
	}

	@Override
	public void close() {
		m_position = 0;
		m_size = 0;
		m_mark = 0;
		BiListNode<Unit> head = m_head;
		m_posNode = head;
		m_markNode = head;

		BufferFactory factory = m_factory;
		m_factory = null;

		factory.putUnit(head.get());
		head.set(null);
		BiListNode<Unit> node = head.next();
		if (node != head) {
			do {
				factory.putUnit(node.get());
				BiListNode<Unit> temp = node;
				node = node.next();
				temp.close();
			} while (node != head);
			head.previous(head);
			head.next(head);
		}

		m_bufferCache.put(this);
	}

	@Override
	public InputStream getInputStream() {
		return new BufferInputStream(this);
	}

	@Override
	public void mark() {
		BiListNode<Unit> node = m_posNode;
		m_markNode = node;
		node.get().mark();
		m_mark = m_position;
	}

	@Override
	public int position() {
		return m_position;
	}

	@Override
	public int read(byte[] dst) {
		return read(dst, 0, dst.length);
	}

	@Override
	public int read(byte[] dst, int offset, int length) {
		if ((offset | length | (offset + length) | (dst.length - (offset + length))) < 0)
			throw new IndexOutOfBoundsException();

		if (length == 0)
			return 0;

		int n = remaining();
		if (n < 1)
			return -1;

		if (length >= n)
			length = n;
		else
			n = length;

		Unit unit = getCurrentUnitToRead();
		int i = 0;
		while ((i = unit.read(dst, offset, length)) < length) {
			length -= i;
			offset += i;
			unit = getNextUnitToRead();
		}

		m_position += n;
		return n;
	}

	@Override
	public int read(ByteBuffer byteBuffer) {
		int length = byteBuffer.remaining();
		if (length < 1)
			return 0;

		int n = remaining();
		if (n < 1)
			return -1;

		if (length >= n)
			length = n;
		else
			n = length;

		Unit unit = getCurrentUnitToRead();
		while ((length -= unit.read(byteBuffer, length)) > 0)
			unit = getNextUnitToRead();

		m_position += n;
		return n;
	}

	@Override
	public byte readByte() {
		if (m_position >= m_size)
			throw new InsufficientDataException(errMsg(m_position, 1, m_size));

		++m_position;
		return getCurrentUnitToRead().read();
	}

	@Override
	public byte[] readBytes() {
		return readBytes(remaining());
	}

	@Override
	public byte[] readBytes(int length) {
		if (length < 0)
			throw new IllegalArgumentException(errMsg(m_position, length,
					m_size));

		int offset = m_position + length;
		if (offset > m_size)
			throw new InsufficientDataException(errMsg(m_position, length,
					m_size));

		byte[] dst = new byte[length];

		if (length > 0) {
			m_position = offset;

			Unit unit = getCurrentUnitToRead();
			offset = 0;
			int i = 0;
			while ((i = unit.read(dst, offset, length)) < length) {
				length -= i;
				offset += i;
				unit = getNextUnitToRead();
			}
		}

		return dst;
	}

	@Override
	public int readIntB() {
		int n = m_position + 4;
		if (n > m_size)
			throw new InsufficientDataException(errMsg(m_position, 4, m_size));

		m_position = n;
		n = 4;
		int i = 0;
		Unit unit = getCurrentUnitToRead();
		for (;;) {
			int length = unit.remaining();
			if (length >= n)
				return unit.readIntB(i, n);

			i = unit.readIntB(i, length);
			n -= length;
			unit = getNextUnitToRead();
		}
	}

	@Override
	public int readIntL() {
		int n = m_position + 4;
		if (n > m_size)
			throw new InsufficientDataException(errMsg(m_position, 4, m_size));

		m_position = n;
		n = 4;
		int i = 0;
		Unit unit = getCurrentUnitToRead();
		for (;;) {
			int length = unit.remaining();
			if (length >= n)
				return unit.readIntL(i, n);

			i = unit.readIntL(i, length);
			n -= length;
			unit = getNextUnitToRead();
		}
	}

	@Override
	public long readLongB() {
		int n = m_position + 8;
		if (n > m_size)
			throw new InsufficientDataException(errMsg(m_position, 8, m_size));

		m_position = n;
		n = 8;
		long l = 0L;
		Unit unit = getCurrentUnitToRead();
		for (;;) {
			int length = unit.remaining();
			if (length >= n)
				return unit.readLongB(l, n);

			l = unit.readLongB(l, length);
			n -= length;
			unit = getNextUnitToRead();
		}
	}

	@Override
	public long readLongL() {
		int n = m_position + 8;
		if (n > m_size)
			throw new InsufficientDataException(errMsg(m_position, 8, m_size));

		m_position = n;
		n = 8;
		long l = 0L;
		Unit unit = getCurrentUnitToRead();
		for (;;) {
			int length = unit.remaining();
			if (length >= n)
				return unit.readLongL(l, n);

			l = unit.readLongL(l, length);
			n -= length;
			unit = getNextUnitToRead();
		}
	}

	@Override
	public short readShortB() {
		return (short) readUShortB();
	}

	@Override
	public short readShortL() {
		return (short) readUShortL();
	}

	@Override
	public double readDoubleB() {
		return Double.longBitsToDouble(readLongB());
	}

	@Override
	public double readDoubleL() {
		return Double.longBitsToDouble(readLongL());
	}

	@Override
	public float readFloatB() {
		return Float.intBitsToFloat(readIntB());
	}

	@Override
	public float readFloatL() {
		return Float.intBitsToFloat(readIntL());
	}

	@Override
	public String readString() {
		return readString(CharsetCodec.get());
	}

	@Override
	public String readString(String charsetName) {
		return readString(CharsetCodec.get(charsetName));
	}

	@Override
	public String readString(Charset charset) {
		return readString(CharsetCodec.get(charset));
	}

	@Override
	public String readString(ICharsetCodec charsetCodec) {
		int length = m_size - m_position;
		if (length < 1)
			throw new InsufficientDataException();

		m_position = m_size;

		ByteBufferArray bba = ByteBufferArray.get();
		try {
			Unit unit = getCurrentUnitToRead();
			bba.add(unit.getByteBufferForRead());
			while ((length -= unit.skip(length)) > 0) {
				unit = getNextUnitToRead();
				bba.add(unit.getByteBufferForRead());
			}

			return charsetCodec.toString(bba.array(), 0, bba.size());
		} finally {
			bba.clear();
		}
	}

	@Override
	public String readString(int length) {
		return readString(length, CharsetCodec.get());
	}

	@Override
	public String readString(int length, String charsetName) {
		return readString(length, CharsetCodec.get(charsetName));
	}

	@Override
	public String readString(int length, Charset charset) {
		return readString(length, CharsetCodec.get(charset));
	}

	@Override
	public String readString(int length, ICharsetCodec charsetCodec) {
		if (length < 0)
			throw new IllegalArgumentException();

		if (length == 0)
			return "";

		int n = m_position + length;
		if (n > m_size)
			throw new InsufficientDataException(errMsg(m_position, length,
					m_size));

		m_position = n;

		ByteBufferArray bba = ByteBufferArray.get();
		try {
			Unit unit = getCurrentUnitToRead();
			bba.add(unit.getByteBufferForRead());
			while ((length -= unit.skip(length)) > 0) {
				unit = getNextUnitToRead();
				bba.add(unit.getByteBufferForRead(unit.position(), length));
			}

			return charsetCodec.toString(bba.array(), 0, bba.size());
		} finally {
			bba.clear();
		}
	}

	@Override
	public int readUByte() {
		return readByte() & 0xFF;
	}

	@Override
	public int readUShortB() {
		int n = m_position + 2;
		if (n > m_size)
			throw new InsufficientDataException(errMsg(m_position, 2, m_size));

		m_position = n;

		int s = 0;
		Unit unit = getCurrentUnitToRead();
		s = unit.read() & 0xFF;
		if (unit.isEmpty())
			unit = getNextUnitToRead();

		s = (s << 8) | (unit.read() & 0xFF);

		return s;
	}

	@Override
	public int readUShortL() {
		int n = m_position + 2;
		if (n > m_size)
			throw new InsufficientDataException(errMsg(m_position, 2, m_size));

		m_position = n;

		int s = 0;
		Unit unit = getCurrentUnitToRead();
		s = unit.read() & 0xFF;
		if (unit.isEmpty())
			unit = getNextUnitToRead();

		s = ((unit.read() & 0xFF) << 8) | s;

		return s;
	}

	@Override
	public int remaining() {
		return m_size - m_position;
	}

	@Override
	public void reset() {
		if (m_position > m_mark) {
			BiListNode<Unit> node = m_markNode;
			m_posNode = node;
			node.get().reset();
			m_position = m_mark;
		}
	}

	@Override
	public void rewind() {
		if (m_position > 0) {
			BiListNode<Unit> node = m_head;
			m_markNode = node;
			m_posNode = node;
			node.get().rewind();
			m_mark = 0;
			m_position = 0;
		}
	}

	@Override
	public int size() {
		return m_size;
	}

	@Override
	public int skip(int n) {
		if (n <= 0)
			return 0;

		int m = remaining();
		if (m > n)
			m = n;
		else
			n = m;

		Unit unit = getCurrentUnitToRead();
		while ((n -= unit.skip(n)) > 0)
			unit = getNextUnitToRead();

		m_position += m;
		return m;
	}

	@Override
	public byte byteAt(int index) {
		if (index < 0 || index >= m_size)
			throw new IndexOutOfBoundsException(errMsg(index, 1, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (index >= (n = unit.size())) {
			index -= n;
			node = node.next();
			unit = node.get();
		}

		return unit.getByte(index);
	}

	@Override
	public byte[] getBytes(int start) {
		return getBytes(start, m_size - start);
	}

	@Override
	public byte[] getBytes(int start, int length) {
		if (start < 0 || length < 0 || start > m_size - length)
			throw new IndexOutOfBoundsException(errMsg(start, length, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		byte[] dst = new byte[length];
		n = unit.getBytes(start, dst, 0, length);
		if (n < length) {
			start = 0;
			do {
				start += n;
				length -= n;
				node = node.next();
				unit = node.get();
				n = unit.getBytes(0, dst, start, length);
			} while (n < length);
		}

		return dst;
	}

	@Override
	public int getIntB(int start) {
		if (start < 0 || start > m_size - 4)
			throw new IndexOutOfBoundsException(errMsg(start, 4, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		int length = n - start;
		if (length >= 4)
			return unit.getIntB(start, 0, 4);

		n = 4;
		int i = unit.getIntB(start, 0, length);
		for (;;) {
			n -= length;
			node = node.next();
			unit = node.get();
			length = unit.size();
			if (length >= n)
				return unit.getIntB(0, i, n);

			i = unit.getIntB(0, i, length);
		}
	}

	@Override
	public int getIntL(int start) {
		if (start < 0 || start > m_size - 4)
			throw new IndexOutOfBoundsException(errMsg(start, 4, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		int length = n - start;
		if (length >= 4)
			return unit.getIntL(start, 0, 4);

		n = 4;
		int i = unit.getIntL(start, 0, length);
		for (;;) {
			n -= length;
			node = node.next();
			unit = node.get();
			length = unit.size();
			if (length >= n)
				return unit.getIntL(0, i, n);

			i = unit.getIntL(0, i, length);
		}
	}

	@Override
	public long getLongB(int start) {
		if (start < 0 || start > m_size - 8)
			throw new IndexOutOfBoundsException(errMsg(start, 8, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		int length = n - start;
		if (length >= 8)
			return unit.getLongB(start, 0, 8);

		n = 8;
		long l = unit.getLongB(start, 0, length);
		for (;;) {
			n -= length;
			node = node.next();
			unit = node.get();
			length = unit.size();
			if (length >= n)
				return unit.getLongB(0, l, n);

			l = unit.getLongB(0, l, length);
		}
	}

	@Override
	public long getLongL(int start) {
		if (start < 0 || start > m_size - 8)
			throw new IndexOutOfBoundsException(errMsg(start, 8, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		int length = n - start;
		if (length >= 8)
			return unit.getLongL(start, 0, 8);

		n = 8;
		long l = unit.getLongL(start, 0, length);
		for (;;) {
			n -= length;
			node = node.next();
			unit = node.get();
			length = unit.size();
			if (length >= n)
				return unit.getLongL(0, l, n);

			l = unit.getLongL(0, l, length);
		}
	}

	@Override
	public short getShortB(int start) {
		return (short) getUShortB(start);
	}

	@Override
	public short getShortL(int start) {
		return (short) getUShortL(start);
	}

	@Override
	public double getDoubleB(int start) {
		return Double.longBitsToDouble(getLongB(start));
	}

	@Override
	public double getDoubleL(int start) {
		return Double.longBitsToDouble(getLongL(start));
	}

	@Override
	public float getFloatB(int start) {
		return Float.intBitsToFloat(getIntB(start));
	}

	@Override
	public float getFloatL(int start) {
		return Float.intBitsToFloat(getIntL(start));
	}

	@Override
	public String getString(int start) {
		return getString(start, CharsetCodec.get());
	}

	@Override
	public String getString(int start, Charset charset) {
		return getString(start, CharsetCodec.get(charset));
	}

	@Override
	public String getString(int start, String charsetName) {
		return getString(start, CharsetCodec.get(charsetName));
	}

	@Override
	public String getString(int start, ICharsetCodec charsetCodec) {
		int length = m_size - start;
		if (start < 0 || length < 0)
			throw new IndexOutOfBoundsException();

		if (length == 0)
			return "";

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		ByteBufferArray bba = ByteBufferArray.get();
		try {
			bba.add(unit.getByteBufferForRead(start, length));
			length -= (unit.size() - start);
			while (length > 0) {
				node = node.next();
				unit = node.get();
				bba.add(unit.getByteBufferForRead(0, length));
				length -= unit.size();
			}

			return charsetCodec.toString(bba.array(), 0, bba.size());
		} finally {
			bba.clear();
		}
	}

	@Override
	public String getString(int start, int length) {
		return getString(start, length, CharsetCodec.get());
	}

	@Override
	public String getString(int start, int length, Charset charset) {
		return getString(start, length, CharsetCodec.get(charset));
	}

	@Override
	public String getString(int start, int length, ICharsetCodec charsetCodec) {
		if (start < 0 || length < 0 || start > m_size - length)
			throw new IndexOutOfBoundsException(errMsg(start, length, m_size));

		if (length == 0)
			return "";

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		ByteBufferArray bba = ByteBufferArray.get();
		try {
			bba.add(unit.getByteBufferForRead(start, length));
			length -= (unit.size() - start);
			while (length > 0) {
				node = node.next();
				unit = node.get();
				bba.add(unit.getByteBufferForRead(0, length));
				length -= unit.size();
			}

			return charsetCodec.toString(bba.array(), 0, bba.size());
		} finally {
			bba.clear();
		}
	}

	@Override
	public String getString(int start, int length, String charsetName) {
		return getString(start, length, CharsetCodec.get(charsetName));
	}

	@Override
	public int getUByte(int index) {
		return byteAt(index) & 0xFF;
	}

	@Override
	public int getUShortB(int start) {
		if (start < 0 || start > m_size - 2)
			throw new IndexOutOfBoundsException(errMsg(start, 2, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		int length = n - start;
		int s = unit.getByte(start) & 0xFF;
		if (length < 2) {
			node = node.next();
			unit = node.get();
			start = 0;
		} else
			++start;

		return (s << 8) | (unit.getByte(start) & 0xFF);
	}

	@Override
	public int getUShortL(int start) {
		if (start < 0 || start > m_size - 2)
			throw new IndexOutOfBoundsException(errMsg(start, 2, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		int length = n - start;
		int s = unit.getByte(start) & 0xFF;
		if (length < 2) {
			node = node.next();
			unit = node.get();
			start = 0;
		} else
			++start;

		return ((unit.getByte(start) & 0xFF) << 8) | s;
	}

	@Override
	public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
		int length = srcEnd - srcBegin;
		if (srcBegin < 0 || srcEnd < 0 || srcEnd > m_size || srcBegin > srcEnd
				|| dstBegin > dst.length - length)
			throw new IndexOutOfBoundsException();

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (srcBegin >= (n = unit.size())) {
			srcBegin -= n;
			node = node.next();
			unit = node.get();
		}

		if ((n = unit.getBytes(srcBegin, dst, 0, length)) < length) {
			srcBegin = 0;
			do {
				srcBegin += n;
				length -= n;
				node = node.next();
				unit = node.get();
			} while ((n = unit.getBytes(0, dst, srcBegin, length)) < length);
		}
	}

	@Override
	public int indexOf(byte b) {
		return indexOf(b, 0);
	}

	@Override
	public int indexOf(byte b, int fromIndex) {
		int size = m_size;
		if (fromIndex >= size)
			return -1;

		if (fromIndex < 0)
			fromIndex = 0;

		int index = fromIndex;

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (fromIndex >= (n = unit.size())) {
			fromIndex -= n;
			node = node.next();
			unit = node.get();
		}

		index -= fromIndex;
		n = unit.indexOf(b, fromIndex);
		for (;;) {
			if (n >= 0)
				return index + n;

			index += unit.size();
			if (index >= size)
				break;

			node = node.next();
			unit = node.get();
			n = unit.indexOf(b, 0);
		}

		return -1;
	}

	@Override
	public int indexOf(byte[] bytes) {
		return indexOf(bytes, 0);
	}

	@Override
	public int indexOf(byte[] bytes, int fromIndex) {
		int length = bytes.length;
		int size = m_size - length;
		if (fromIndex > size)
			return length < 1 ? size : -1;

		if (fromIndex < 0)
			fromIndex = 0;

		if (length < 1)
			return fromIndex;

		int index = fromIndex;
		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int unitSize = 0;
		while (fromIndex >= (unitSize = unit.size())) {
			fromIndex -= unitSize;
			node = node.next();
			unit = node.get();
		}

		// index - base index
		// fromIndex - leftIndex
		index -= fromIndex;
		int n = unit.indexOf(bytes, fromIndex);
		for (;;) {
			if (n >= 0) {
				int m = unitSize - n;
				n += index;
				if (m >= length)
					return n;

				if (n <= size) {
					BiListNode<Unit> temp = node.next();
					while ((unit = temp.get()).startsWith(bytes, m)) {
						if ((m += unit.size()) >= length)
							return n;
						temp = temp.next();
					}
				}
			}

			index += unitSize;
			if (index > size)
				break;

			node = node.next();
			unit = node.get();
			unitSize = unit.size();
			n = unit.indexOf(bytes, 0);
		}

		return -1;
	}

	@Override
	public int indexOf(ByteKmp pattern) {
		return indexOf(pattern, 0);
	}

	@Override
	public int indexOf(ByteKmp pattern, int fromIndex) {
		int length = pattern.length();
		int size = m_size - length;
		if (fromIndex > size)
			return length < 1 ? size : -1;

		if (fromIndex < 0)
			fromIndex = 0;

		if (length < 1)
			return fromIndex;

		BiListNode<Unit> head = m_head;
		BiListNode<Unit> node = head;
		Unit unit = node.get();
		int index = fromIndex;
		while (index >= (size = unit.size())) {
			index -= size;
			node = node.next();
			unit = node.get();
		}

		int n = 0;
		Blob blob = Blob.get();
		try {
			unit.addTo(blob, index);
			while ((node = node.next()) != head)
				node.get().addTo(blob);

			n = blob.indexOf(pattern);
		} finally {
			blob.close();
		}

		return n < 0 ? n : fromIndex + n;
	}

	@Override
	public int lastIndexOf(byte b) {
		return lastIndexOf(b, m_size);
	}

	@Override
	public int lastIndexOf(byte b, int fromIndex) {
		if (fromIndex < 0)
			return -1;

		int index = m_size;
		if (fromIndex >= index)
			fromIndex = index - 1;

		BiListNode<Unit> node = m_head.previous();
		Unit unit = node.get();
		while (fromIndex < (index -= unit.size())) {
			node = node.previous();
			unit = node.get();
		}

		fromIndex -= index;
		int n = unit.lastIndexOf(b, fromIndex);
		for (;;) {
			if (n >= 0)
				return index + n;

			if (index <= 0)
				break;

			node = node.previous();
			unit = node.get();
			n = unit.size();
			index -= n;
			n = unit.lastIndexOf(b, n - 1);
		}

		return -1;
	}

	@Override
	public int lastIndexOf(byte[] bytes) {
		return lastIndexOf(bytes, m_size);
	}

	@Override
	public int lastIndexOf(byte[] bytes, int fromIndex) {
		int length = bytes.length;
		int index = m_size;
		int maxIndex = index - length;
		if (fromIndex > maxIndex)
			fromIndex = maxIndex;

		if (fromIndex < 0)
			return -1;

		if (length < 1)
			return fromIndex;

		fromIndex += length;

		BiListNode<Unit> node = m_head.previous();
		Unit unit = node.get();
		while (fromIndex < (index -= unit.size())) {
			node = node.previous();
			unit = node.get();
		}

		// index - base index
		// fromIndex - right index
		fromIndex -= index;
		int n = unit.lastIndexOf(bytes, fromIndex);
		for (;;) {
			if (n > 0) {
				int m = length - n;
				n = index - m;
				if (m <= 0)
					return n;

				if (n >= 0) {
					BiListNode<Unit> temp = node.previous();
					while ((unit = temp.get()).endsWith(bytes, m)) {
						if ((m -= unit.size()) <= 0)
							return n;
						temp = temp.previous();
					}
				}
			}

			if (index <= 0)
				break;

			node = node.previous();
			unit = node.get();
			int unitSize = unit.size();
			index -= unitSize;
			n = unit.lastIndexOf(bytes, unitSize);
		}

		return -1;
	}

	@Override
	public int lastIndexOf(ByteKmp pattern) {
		return lastIndexOf(pattern, m_size);
	}

	@Override
	public int lastIndexOf(ByteKmp pattern, int fromIndex) {
		int length = pattern.length();
		int index = m_size;
		int n = index - length;
		if (fromIndex > n)
			fromIndex = n;

		if (fromIndex < 0)
			return -1;

		if (length < 1)
			return fromIndex;

		fromIndex += length;

		BiListNode<Unit> head = m_head;
		BiListNode<Unit> last = head.previous();
		Unit unit = last.get();
		while (fromIndex < (index -= unit.size())) {
			last = last.previous();
			unit = last.get();
		}

		fromIndex -= index;
		Blob blob = Blob.get();
		try {
			for (BiListNode<Unit> node = head; node != last; node = node.next())
				node.get().addTo(blob);

			unit.addToWithSize(blob, fromIndex);
			return blob.lastIndexOf(pattern);
		} finally {
			blob.close();
		}
	}

	@Override
	public boolean startsWith(byte[] bytes) {
		int length = bytes.length;
		if (length > m_size)
			return false;

		if (length < 1)
			return true;

		BiListNode<Unit> node = m_head;
		Unit unit;
		int n = 0;
		while ((unit = node.get()).startsWith(bytes, n)) {
			if ((n += unit.size()) >= length)
				return true;
			node = node.next();
		}

		return false;
	}

	@Override
	public boolean endsWith(byte[] bytes) {
		int n = bytes.length;
		if (n > m_size)
			return false;

		if (n < 1)
			return true;

		BiListNode<Unit> node = m_head.previous();
		Unit unit;
		while ((unit = node.get()).endsWith(bytes, n)) {
			if ((n -= unit.size()) <= 0)
				return true;
			node = node.previous();
		}

		return false;
	}

	@Override
	public int length() {
		return m_size;
	}

	@Override
	public boolean isEmpty() {
		return m_size < 1;
	}

	@Override
	public OutputStream getOutputStream() {
		return new BufferOutputStream(this);
	}

	@Override
	public IBufferWriter setByte(int index, byte b) {
		if (index < 0 || index >= m_size)
			throw new IndexOutOfBoundsException(errMsg(index, 1, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (index >= (n = unit.size())) {
			index -= n;
			node = node.next();
			unit = node.get();
		}

		unit.setByte(index, b);

		return this;
	}

	@Override
	public IBufferWriter setBytes(int start, byte[] src) {
		return setBytes(start, src, 0, src.length);
	}

	@Override
	public IBufferWriter setBytes(int start, byte[] src, int offset, int length) {
		if (start < 0 || length < 0 || start > m_size - length)
			throw new IndexOutOfBoundsException(errMsg(start, length, m_size));

		if (length < 1)
			return this;

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		if ((n = unit.setBytes(start, src, offset, length)) < length) {
			do {
				offset += n;
				length -= n;
				node = node.next();
				unit = node.get();
			} while ((n = unit.setBytes(0, src, offset, length)) < length);
		}

		return this;
	}

	@Override
	public IBufferWriter setIntB(int start, int i) {
		if (start < 0 || start > m_size - 4)
			throw new IndexOutOfBoundsException(errMsg(start, 4, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		int length = n - start;
		if (length >= 4) {
			unit.setIntB(start, i, 4);
			return this;
		}

		n = 4;
		i = unit.setIntB(start, i, length);
		for (;;) {
			n -= length;
			node = node.next();
			unit = node.get();
			length = unit.size();
			if (length >= n) {
				unit.setIntB(0, i, n);
				break;
			}

			i = unit.setIntB(0, i, length);
		}

		return this;
	}

	@Override
	public IBufferWriter setIntL(int start, int i) {
		if (start < 0 || start > m_size - 4)
			throw new IndexOutOfBoundsException(errMsg(start, 4, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		int length = n - start;
		if (length >= 4) {
			unit.setIntL(start, i, 4);
			return this;
		}

		n = 4;
		i = unit.setIntL(start, i, length);
		for (;;) {
			n -= length;
			node = node.next();
			unit = node.get();
			length = unit.size();
			if (length >= n) {
				unit.setIntL(0, i, n);
				break;
			}

			i = unit.setIntL(0, i, length);
		}

		return this;
	}

	@Override
	public IBufferWriter setLongB(int start, long l) {
		if (start < 0 || start > m_size - 8)
			throw new IndexOutOfBoundsException(errMsg(start, 8, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		int length = n - start;
		if (length >= 8) {
			unit.setLongB(start, l, 8);
			return this;
		}

		n = 8;
		l = unit.setLongB(start, l, length);
		for (;;) {
			n -= length;
			node = node.next();
			unit = node.get();
			length = unit.size();
			if (length >= n) {
				unit.setLongB(0, l, n);
				break;
			}

			l = unit.setLongB(0, l, length);
		}

		return this;
	}

	@Override
	public IBufferWriter setLongL(int start, long l) {
		if (start < 0 || start > m_size - 8)
			throw new IndexOutOfBoundsException(errMsg(start, 8, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		int length = n - start;
		if (length >= 8) {
			unit.setLongL(start, l, 8);
			return this;
		}

		n = 8;
		l = unit.setLongL(start, l, length);
		for (;;) {
			n -= length;
			node = node.next();
			unit = node.get();
			length = unit.size();
			if (length >= n) {
				unit.setLongL(0, l, n);
				break;
			}

			l = unit.setLongL(0, l, length);
		}

		return this;
	}

	@Override
	public IBufferWriter setShortB(int start, short s) {
		if (start < 0 || start > m_size - 2)
			throw new IndexOutOfBoundsException(errMsg(start, 2, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		int length = n - start;
		unit.setByte(start, (byte) (s >> 8));
		if (length < 2) {
			node = node.next();
			unit = node.get();
			start = 0;
		} else
			++start;

		unit.setByte(start, (byte) s);

		return this;
	}

	@Override
	public IBufferWriter setShortL(int start, short s) {
		if (start < 0 || start > m_size - 2)
			throw new IndexOutOfBoundsException(errMsg(start, 2, m_size));

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		int length = n - start;
		unit.setByte(start, (byte) s);
		if (length < 2) {
			node = node.next();
			unit = node.get();
			start = 0;
		} else
			++start;

		unit.setByte(start, (byte) (s >> 8));

		return this;
	}

	@Override
	public IBufferWriter setFloatB(int start, float f) {
		return setIntB(start, Float.floatToRawIntBits(f));
	}

	@Override
	public IBufferWriter setFloatL(int start, float f) {
		return setIntL(start, Float.floatToRawIntBits(f));
	}

	@Override
	public IBufferWriter setDoubleB(int start, double d) {
		return setLongB(start, Double.doubleToRawLongBits(d));
	}

	@Override
	public IBufferWriter setDoubleL(int start, double d) {
		return setLongL(start, Double.doubleToRawLongBits(d));
	}

	@Override
	public IBufferWriter setFill(int start, byte b, int count) {
		if (start < 0 || count < 0 || start > m_size - count)
			throw new IndexOutOfBoundsException();

		if (count < 1)
			return this;

		BiListNode<Unit> node = m_head;
		Unit unit = node.get();
		int n = 0;
		while (start >= (n = unit.size())) {
			start -= n;
			node = node.next();
			unit = node.get();
		}

		if ((n = unit.setFill(start, b, count)) < count) {
			do {
				count -= n;
				node = node.next();
				unit = node.get();
			} while ((n = unit.setFill(0, b, count)) < count);
		}

		return this;
	}

	@Override
	public IBufferWriter headWriteByte(byte b) {
		Unit unit = getHeadUnitToWrite();
		unit.headWriteByte(b);
		if (m_position > 0)
			++m_position;

		++m_size;
		return this;
	}

	@Override
	public IBufferWriter headWriteBytes(byte[] src) {
		return headWriteBytes(src, 0, src.length);
	}

	@Override
	public IBufferWriter headWriteBytes(byte[] src, int offset, int length) {
		if ((offset | length | (offset + length) | (src.length - (offset + length))) < 0)
			throw new IndexOutOfBoundsException();

		if (length < 1)
			return this;

		if (m_position > 0)
			m_position += length;

		m_size += length;

		int n = 0;
		Unit unit = getHeadUnitToWrite();
		while ((n = unit.headWriteBytes(src, offset, length)) < length) {
			offset += n;
			length -= n;
			unit = addHeadUnitToWrite();
		}

		return this;
	}

	@Override
	public IBufferWriter headWriteDoubleB(double d) {
		return headWriteLongB(Double.doubleToRawLongBits(d));
	}

	@Override
	public IBufferWriter headWriteDoubleL(double d) {
		return headWriteLongL(Double.doubleToRawLongBits(d));
	}

	@Override
	public IBufferWriter headWriteFloatB(float f) {
		return headWriteIntB(Float.floatToRawIntBits(f));
	}

	@Override
	public IBufferWriter headWriteFloatL(float f) {
		return headWriteIntL(Float.floatToRawIntBits(f));
	}

	@Override
	public IBufferWriter headWriteIntB(int i) {
		int n = 4;
		Unit unit = getHeadUnitToWrite();
		for (;;) {
			int length = unit.headAvailable();
			if (length >= n) {
				unit.headWriteIntB(i, n);
				break;
			}

			i = unit.headWriteIntB(i, length);
			n -= length;
			unit = addHeadUnitToWrite();
		}

		if (m_position > 0)
			m_position += 4;

		m_size += 4;
		return this;
	}

	@Override
	public IBufferWriter headWriteIntL(int i) {
		int n = 4;
		Unit unit = getHeadUnitToWrite();
		for (;;) {
			int length = unit.headAvailable();
			if (length >= n) {
				unit.headWriteIntL(i, n);
				break;
			}

			i = unit.headWriteIntL(i, length);
			n -= length;
			unit = addHeadUnitToWrite();
		}

		if (m_position > 0)
			m_position += 4;

		m_size += 4;
		return this;
	}

	@Override
	public IBufferWriter headWriteLongB(long l) {
		int n = 8;
		Unit unit = getHeadUnitToWrite();
		for (;;) {
			int length = unit.headAvailable();
			if (length >= n) {
				unit.headWriteLongB(l, n);
				break;
			}

			l = unit.headWriteLongB(l, length);
			n -= length;
			unit = addHeadUnitToWrite();
		}

		if (m_position > 0)
			m_position += 8;

		m_size += 8;
		return this;
	}

	@Override
	public IBufferWriter headWriteLongL(long l) {
		int n = 8;
		Unit unit = getHeadUnitToWrite();
		for (;;) {
			int length = unit.headAvailable();
			if (length >= n) {
				unit.headWriteLongL(l, n);
				break;
			}

			l = unit.headWriteLongL(l, length);
			n -= length;
			unit = addHeadUnitToWrite();
		}

		if (m_position > 0)
			m_position += 8;

		m_size += 8;
		return this;
	}

	@Override
	public IBufferWriter headWriteShortB(short s) {
		Unit unit = getHeadUnitToWrite();
		unit.headWriteByte((byte) s);
		if (unit.isHeadFull())
			unit = addHeadUnitToWrite();

		unit.headWriteByte((byte) (s >> 8));

		if (m_position > 0)
			m_position += 2;

		m_size += 2;
		return this;
	}

	@Override
	public IBufferWriter headWriteShortL(short s) {
		Unit unit = getHeadUnitToWrite();
		unit.headWriteByte((byte) (s >> 8));
		if (unit.isHeadFull())
			unit = addHeadUnitToWrite();

		unit.headWriteByte((byte) s);

		if (m_position > 0)
			m_position += 2;

		m_size += 2;
		return this;
	}

	@Override
	public IBufferWriter headWriteFill(byte b, int count) {
		if (count < 0)
			throw new IllegalArgumentException();

		if (count < 1)
			return this;

		if (m_position > 0)
			m_position += count;

		m_size += count;

		int n = 0;
		Unit unit = getHeadUnitToWrite();
		while ((n = unit.headWriteFill(b, count)) < count) {
			count -= n;
			unit = addHeadUnitToWrite();
		}

		return this;
	}

	@Override
	public IBufferWriter write(IByteSequence in) {
		return write(in, 0, in.length());
	}

	@Override
	public IBufferWriter write(IByteSequence in, int offset, int length) {
		if (offset < 0 || length < 0 || offset > in.length() - length)
			throw new IndexOutOfBoundsException();

		m_size += length;
		Unit unit = getCurrentUnitToWrite();
		int n = 0;
		while ((n = unit.writeBytes(in, offset, length)) < length) {
			offset += n;
			length -= n;
			unit = getNextUnitToWrite();
		}

		return this;
	}

	@Override
	public IBufferWriter write(IBufferReader in) {
		int length = in.remaining();
		if (length < 1)
			return this;

		m_size += length;

		for (Unit unit = getCurrentUnitToWrite(); (length -= unit.writeBytes(
				in, length)) > 0; unit = getNextUnitToWrite())
			;

		return this;
	}

	@Override
	public IBufferWriter write(IBufferReader in, int length) {
		if (length < 0)
			throw new IllegalArgumentException();

		if (length > in.remaining())
			throw new InsufficientDataException();

		if (length < 1)
			return this;

		m_size += length;

		for (Unit unit = getCurrentUnitToWrite(); (length -= unit.writeBytes(
				in, length)) > 0; unit = getNextUnitToWrite())
			;

		return this;
	}

	@Override
	public IBufferWriter write(ByteBuffer byteBuffer) {
		int length = byteBuffer.remaining();
		if (length < 1)
			return this;

		m_size += length;

		for (Unit unit = getCurrentUnitToWrite(); (length -= unit.write(
				byteBuffer, length)) > 0; unit = getNextUnitToWrite())
			;

		return this;
	}

	@Override
	public IBufferWriter headWrite(IBufferReader in) {
		int length = in.remaining();
		if (length < 1)
			return this;

		m_size += length;
		if (m_position > 0)
			m_position += length;

		int offset = in.position();
		in.skip(length);

		Unit unit = getHeadUnitToWrite();
		while ((length -= unit.headWriteBytes(in, offset, length)) > 0)
			unit = addHeadUnitToWrite();

		return this;
	}

	@Override
	public IBufferWriter headWrite(IBufferReader in, int length) {
		if (length < 0)
			throw new IllegalArgumentException();

		if (length > in.remaining())
			throw new InsufficientDataException();

		m_size += length;
		if (m_position > 0)
			m_position += length;

		int offset = in.position();
		in.skip(length);

		Unit unit = getHeadUnitToWrite();
		while ((length -= unit.writeBytes(in, offset, length)) > 0)
			unit = addHeadUnitToWrite();

		return this;
	}

	@Override
	public IBufferWriter headWrite(IByteSequence in) {
		int length = in.length();
		m_size += length;
		if (m_position > 0)
			m_position += length;
		Unit unit = getHeadUnitToWrite();
		while ((length -= unit.headWriteBytes(in, 0, length)) > 0)
			unit = addHeadUnitToWrite();

		return this;
	}

	@Override
	public IBufferWriter headWrite(IByteSequence in, int offset, int length) {
		if (offset < 0 || length < 0 || offset > in.length() - length)
			throw new IndexOutOfBoundsException();

		m_size += length;
		if (m_position > 0)
			m_position += length;
		Unit unit = getHeadUnitToWrite();
		while ((length -= unit.headWriteBytes(in, offset, length)) > 0)
			unit = addHeadUnitToWrite();

		return this;
	}

	@Override
	public IBufferWriter headWrite(ByteBuffer byteBuffer) {
		int length = byteBuffer.remaining();
		if (length < 1)
			return this;

		int n = length;
		int offset = byteBuffer.position();

		Unit unit = getHeadUnitToWrite();
		while ((length -= unit.headWrite(byteBuffer, offset, length)) > 0)
			unit = addHeadUnitToWrite();

		m_size += n;
		if (m_position > 0)
			m_position += n;
		byteBuffer.position(offset + n);

		return this;
	}

	@Override
	public IBufferWriter writeByte(byte b) {
		getCurrentUnitToWrite().writeByte(b);
		++m_size;
		return this;
	}

	@Override
	public IBufferWriter writeBytes(byte[] src) {
		return writeBytes(src, 0, src.length);
	}

	@Override
	public IBufferWriter writeBytes(byte[] src, int offset, int length) {
		if ((offset | length | (offset + length) | (src.length - (offset + length))) < 0)
			throw new IndexOutOfBoundsException();

		if (length < 1)
			return this;

		m_size += length;

		Unit unit = getCurrentUnitToWrite();
		int n = 0;
		while ((n = unit.writeBytes(src, offset, length)) < length) {
			offset += n;
			length -= n;
			unit = getNextUnitToWrite();
		}

		return this;
	}

	@Override
	public IBufferWriter writeIntB(int i) {
		int n = 4;
		Unit unit = getCurrentUnitToWrite();
		for (;;) {
			int length = unit.available();
			if (length >= n) {
				unit.writeIntB(i, n);
				break;
			}

			i = unit.writeIntB(i, length);
			n -= length;
			unit = getNextUnitToWrite();
		}

		m_size += 4;

		return this;
	}

	@Override
	public IBufferWriter writeIntL(int i) {
		int n = 4;
		Unit unit = getCurrentUnitToWrite();
		for (;;) {
			int length = unit.available();
			if (length >= n) {
				unit.writeIntL(i, n);
				break;
			}

			i = unit.writeIntL(i, length);
			n -= length;
			unit = getNextUnitToWrite();
		}

		m_size += 4;

		return this;
	}

	@Override
	public IBufferWriter writeLongB(long l) {
		int n = 8;
		Unit unit = getCurrentUnitToWrite();
		for (;;) {
			int length = unit.available();
			if (length >= n) {
				unit.writeLongB(l, n);
				break;
			}

			l = unit.writeLongB(l, length);
			n -= length;
			unit = getNextUnitToWrite();
		}

		m_size += 8;

		return this;
	}

	@Override
	public IBufferWriter writeLongL(long l) {
		int n = 8;
		Unit unit = getCurrentUnitToWrite();
		for (;;) {
			int length = unit.available();
			if (length >= n) {
				unit.writeLongL(l, n);
				break;
			}

			l = unit.writeLongL(l, length);
			n -= length;
			unit = getNextUnitToWrite();
		}

		m_size += 8;

		return this;
	}

	@Override
	public IBufferWriter writeShortB(short s) {
		Unit unit = getCurrentUnitToWrite();
		unit.writeByte((byte) (s >> 8));
		if (unit.isFull())
			unit = getNextUnitToWrite();

		unit.writeByte((byte) s);

		m_size += 2;

		return this;
	}

	@Override
	public IBufferWriter writeShortL(short s) {
		Unit unit = getCurrentUnitToWrite();
		unit.writeByte((byte) s);
		if (unit.isFull())
			unit = getNextUnitToWrite();

		unit.writeByte((byte) (s >> 8));

		m_size += 2;

		return this;
	}

	@Override
	public IBufferWriter writeDoubleB(double d) {
		return writeLongB(Double.doubleToRawLongBits(d));
	}

	@Override
	public IBufferWriter writeDoubleL(double d) {
		return writeLongL(Double.doubleToRawLongBits(d));
	}

	@Override
	public IBufferWriter writeFloatB(float f) {
		return writeIntB(Float.floatToRawIntBits(f));
	}

	@Override
	public IBufferWriter writeFloatL(float f) {
		return writeIntL(Float.floatToRawIntBits(f));
	}

	@Override
	public IBufferWriter writeFill(byte b, int count) {
		if (count < 0)
			throw new IllegalArgumentException();

		if (count < 1)
			return this;

		m_size += count;

		Unit unit = getCurrentUnitToWrite();
		int n = 0;
		while ((n = unit.writeFill(b, count)) < count) {
			count -= n;
			unit = getNextUnitToWrite();
		}
	
		return this;
	}

	@Override
	public IBufferWriter headWrite(char[] chars) {
		return headWrite(chars, CharsetCodec.get());
	}

	@Override
	public IBufferWriter headWrite(char[] chars, Charset charset) {
		return headWrite(chars, CharsetCodec.get(charset));
	}

	@Override
	public IBufferWriter headWrite(char[] chars, String charsetName) {
		return headWrite(chars, CharsetCodec.get(charsetName));
	}

	@Override
	public IBufferWriter headWrite(char[] chars, ICharsetCodec charsetCodec) {
		BytesBuilder bb = BytesBuilder.get();
		try {
			charsetCodec.encode(chars, bb);

			int length = bb.length();
			m_size += length;
			if (m_position > 0)
				m_position += length;

			Unit unit = getHeadUnitToWrite();
			while ((length -= unit.headWriteBytes(bb, 0, length)) > 0)
				unit = addHeadUnitToWrite();
		} finally {
			bb.close();
		}

		return this;
	}

	@Override
	public IBufferWriter headWrite(char[] chars, int offset, int length) {
		return headWrite(chars, offset, length, CharsetCodec.get());
	}

	@Override
	public IBufferWriter headWrite(char[] chars, int offset, int length,
			Charset charset) {
		return headWrite(chars, offset, length, CharsetCodec.get(charset));
	}

	@Override
	public IBufferWriter headWrite(char[] chars, int offset, int length,
			String charsetName) {
		return headWrite(chars, offset, length, CharsetCodec.get(charsetName));
	}

	@Override
	public IBufferWriter headWrite(char[] chars, int offset, int length,
			ICharsetCodec charsetCodec) {
		BytesBuilder bb = BytesBuilder.get();
		try {
			charsetCodec.encode(chars, offset, length, bb);

			length = bb.length();
			m_size += length;
			if (m_position > 0)
				m_position += length;

			Unit unit = getHeadUnitToWrite();
			while ((length -= unit.headWriteBytes(bb, 0, length)) > 0)
				unit = addHeadUnitToWrite();
		} finally {
			bb.close();
		}

		return this;
	}

	@Override
	public IBufferWriter headWrite(CharSequence cs) {
		return headWrite(cs, CharsetCodec.get());
	}

	@Override
	public IBufferWriter headWrite(CharSequence cs, Charset charset) {
		return headWrite(cs, CharsetCodec.get(charset));
	}

	@Override
	public IBufferWriter headWrite(CharSequence cs, String charsetName) {
		return headWrite(cs, CharsetCodec.get(charsetName));
	}

	@Override
	public IBufferWriter headWrite(CharSequence cs, ICharsetCodec charsetCodec) {
		if (cs == null)
			throw new NullPointerException();

		boolean isStringBuilder = cs instanceof StringBuilder;
		StringBuilder sb = isStringBuilder ? (StringBuilder) cs : StringBuilder
				.get(cs);
		BytesBuilder bb = BytesBuilder.get();
		try {
			charsetCodec.encode(sb, bb);

			int length = bb.length();
			m_size += length;
			if (m_position > 0)
				m_position += length;

			Unit unit = getHeadUnitToWrite();
			while ((length -= unit.headWriteBytes(bb, 0, length)) > 0)
				unit = addHeadUnitToWrite();
		} finally {
			bb.close();
			if (!isStringBuilder)
				sb.close();
		}

		return this;
	}

	@Override
	public IBufferWriter headWrite(CharSequence cs, int offset, int length) {
		return headWrite(cs, offset, length, CharsetCodec.get());
	}

	@Override
	public IBufferWriter headWrite(CharSequence cs, int offset, int length,
			Charset charset) {
		return headWrite(cs, offset, length, CharsetCodec.get(charset));
	}

	@Override
	public IBufferWriter headWrite(CharSequence cs, int offset, int length,
			String charsetName) {
		return headWrite(cs, offset, length, CharsetCodec.get(charsetName));
	}

	@Override
	public IBufferWriter headWrite(CharSequence cs, int offset, int length,
			ICharsetCodec charsetCodec) {
		if (cs == null)
			throw new NullPointerException();

		boolean isStringBuilder = cs instanceof StringBuilder;
		StringBuilder sb = isStringBuilder ? (StringBuilder) cs : StringBuilder
				.get();
		BytesBuilder bb = BytesBuilder.get();
		try {
			if (!isStringBuilder) {
				sb.append(cs, offset, offset + length);
				charsetCodec.encode(sb, bb);
			} else
				charsetCodec.encode(sb, offset, length, bb);

			length = bb.length();
			m_size += length;
			if (m_position > 0)
				m_position += length;

			Unit unit = getHeadUnitToWrite();
			while ((length -= unit.headWriteBytes(bb, 0, length)) > 0)
				unit = addHeadUnitToWrite();
		} finally {
			bb.close();
			if (!isStringBuilder)
				sb.close();
		}

		return this;
	}

	@Override
	public IBufferWriter write(char[] chars) {
		return write(chars, CharsetCodec.get());
	}

	@Override
	public IBufferWriter write(char[] chars, Charset charset) {
		return write(chars, CharsetCodec.get(charset));
	}

	@Override
	public IBufferWriter write(char[] chars, String charsetName) {
		return write(chars, CharsetCodec.get(charsetName));
	}

	@Override
	public IBufferWriter write(char[] chars, ICharsetCodec charsetCodec) {
		CharBuffer cb = CharBuffer.wrap(chars);

		int size = m_size;
		CharsetEncoder encoder = charsetCodec.getEncoder();
		try {
			Unit unit = getCurrentUnitToWrite();
			ByteBuffer bb = unit.getByteBufferForWrite();

			boolean flush = false;
			encoder.reset();
			for (;;) {
				CoderResult cr = flush ? encoder.flush(bb) : encoder.encode(cb,
						bb, true);

				int n = unit.size();
				unit.syncWrite();
				size += unit.size() - n;

				if (cr.isOverflow()) {
					unit = getNextUnitToWrite();
					bb = unit.getByteBufferForWrite();
					continue;
				}

				if (!cr.isUnderflow())
					cr.throwException();

				if (flush)
					break;
				else
					flush = true;
			}
		} catch (CharacterCodingException e) {
			throw new Error(e);
		} finally {
			m_size = size;
			charsetCodec.releaseEncoder(encoder);
		}

		return this;
	}

	@Override
	public IBufferWriter write(char[] chars, int offset, int length) {
		return write(chars, offset, length, CharsetCodec.get());
	}

	@Override
	public IBufferWriter write(char[] chars, int offset, int length,
			Charset charset) {
		return write(chars, offset, length, CharsetCodec.get(charset));
	}

	@Override
	public IBufferWriter write(char[] chars, int offset, int length,
			String charsetName) {
		return write(chars, offset, length, CharsetCodec.get(charsetName));
	}

	@Override
	public IBufferWriter write(char[] chars, int offset, int length,
			ICharsetCodec charsetCodec) {
		CharBuffer cb = CharBuffer.wrap(chars, offset, length);
		int size = m_size;
		CharsetEncoder encoder = charsetCodec.getEncoder();
		try {
			Unit unit = getCurrentUnitToWrite();
			ByteBuffer bb = unit.getByteBufferForWrite();

			boolean flush = false;
			encoder.reset();
			for (;;) {
				CoderResult cr = flush ? encoder.flush(bb) : encoder.encode(cb,
						bb, true);

				int n = unit.size();
				unit.syncWrite();
				size += unit.size() - n;

				if (cr.isOverflow()) {
					unit = getNextUnitToWrite();
					bb = unit.getByteBufferForWrite();
					continue;
				}

				if (!cr.isUnderflow())
					cr.throwException();

				if (flush)
					break;
				else
					flush = true;
			}
		} catch (CharacterCodingException e) {
			throw new Error(e);
		} finally {
			m_size = size;
			charsetCodec.releaseEncoder(encoder);
		}

		return this;
	}

	@Override
	public IBufferWriter write(CharSequence cs) {
		return write(cs, CharsetCodec.get());
	}

	@Override
	public IBufferWriter write(CharSequence cs, Charset charset) {
		return write(cs, CharsetCodec.get(charset));
	}

	@Override
	public IBufferWriter write(CharSequence cs, String charsetName) {
		return write(cs, CharsetCodec.get(charsetName));
	}

	@Override
	public IBufferWriter write(CharSequence cs, ICharsetCodec charsetCodec) {
		CharBuffer cb = cs instanceof StringBuilder ? ((StringBuilder) cs)
				.getCharBuffer(0, cs.length()) : CharBuffer.wrap(cs);

		int size = m_size;
		CharsetEncoder encoder = charsetCodec.getEncoder();
		try {
			Unit unit = getCurrentUnitToWrite();
			ByteBuffer bb = unit.getByteBufferForWrite();

			boolean flush = false;
			encoder.reset();
			for (;;) {
				CoderResult cr = flush ? encoder.flush(bb) : encoder.encode(cb,
						bb, true);

				int n = unit.size();
				unit.syncWrite();
				size += unit.size() - n;

				if (cr.isOverflow()) {
					unit = getNextUnitToWrite();
					bb = unit.getByteBufferForWrite();
					continue;
				}

				if (!cr.isUnderflow())
					cr.throwException();

				if (flush)
					break;
				else
					flush = true;
			}
		} catch (CharacterCodingException e) {
			throw new Error(e);
		} finally {
			m_size = size;
			charsetCodec.releaseEncoder(encoder);
		}

		return this;
	}

	@Override
	public IBufferWriter write(CharSequence cs, int offset, int length) {
		return write(cs, offset, length, CharsetCodec.get());
	}

	@Override
	public IBufferWriter write(CharSequence cs, int offset, int length,
			Charset charset) {
		return write(cs, offset, length, CharsetCodec.get(charset));
	}

	@Override
	public IBufferWriter write(CharSequence cs, int offset, int length,
			String charsetName) {
		return write(cs, offset, length, CharsetCodec.get(charsetName));
	}

	@Override
	public IBufferWriter write(CharSequence cs, int offset, int length,
			ICharsetCodec charsetCodec) {
		CharBuffer cb = cs instanceof StringBuilder ? ((StringBuilder) cs)
				.getCharBuffer(offset, length) : CharBuffer.wrap(cs, offset,
				offset + length);

		int size = m_size;
		CharsetEncoder encoder = charsetCodec.getEncoder();
		try {
			Unit unit = getCurrentUnitToWrite();
			ByteBuffer bb = unit.getByteBufferForWrite();

			boolean flush = false;
			encoder.reset();
			for (;;) {
				CoderResult cr = flush ? encoder.flush(bb) : encoder.encode(cb,
						bb, true);
				int n = unit.size();
				unit.syncWrite();
				size += unit.size() - n;

				if (cr.isOverflow()) {
					unit = getNextUnitToWrite();
					bb = unit.getByteBufferForWrite();
					continue;
				}

				if (!cr.isUnderflow())
					cr.throwException();

				if (flush)
					break;
				else
					flush = true;
			}
		} catch (CharacterCodingException e) {
			throw new Error(e);
		} finally {
			m_size = size;
			charsetCodec.releaseEncoder(encoder);
		}

		return this;
	}

	@Override
	public int readIn(ReadableByteChannel in) throws IOException {
		Unit unit = getCurrentUnitToWrite();
		int n = in.read(unit.getByteBufferForWrite());
		if (n > 0) {
			unit.syncWrite();
			m_size += n;
		}
		return n;
	}

	@Override
	public SocketAddress receive(DatagramChannel in) throws IOException {
		Unit unit = getCurrentUnitToWrite();
		ByteBuffer bb = unit.getByteBufferForWrite();
		int n = unit.size();
		SocketAddress address = in.receive(bb);
		unit.syncWrite();
		n = unit.size() - n;
		if (n > 0)
			m_size += n;

		return address;
	}

	@Override
	public int writeOut(WritableByteChannel out) throws IOException {
		if (m_position >= m_size)
			return 0;

		Unit unit = getCurrentUnitToRead();
		BiListNode<Unit> node = m_posNode;
		BiListNode<Unit> tail = m_head.previous();
		int n = 0;
		if (node == tail || !(out instanceof GatheringByteChannel)) {
			n = out.write(unit.getByteBufferForRead());
			if (n > 0) {
				unit.syncRead();
				m_position += n;
			}
		} else {
			ByteBufferArray bba = ByteBufferArray.get();
			try {
				bba.add(unit.getByteBufferForRead());
				while (node != tail) {
					node = node.next();
					unit = node.get();
					unit.rewind();
					bba.add(unit.getByteBufferForRead());
				}
				n = (int) ((GatheringByteChannel) out).write(bba.array(), 0,
						bba.size());
			} finally {
				bba.clear();
			}

			if (n > 0) {
				int m = n;
				node = m_posNode;
				unit = node.get();
				while ((m -= unit.skip(m)) > 0) {
					node = node.next();
					unit = node.get();
				}

				m_posNode = node;
				m_position += n;
			}
		}

		return n;
	}

	@Override
	public int compareTo(IBuffer that) {
		if (!(that instanceof Buffer))
			return compareInternal(that);

		Buffer thatBuf = (Buffer) that;
		int remaining = remaining();
		int thatRemaining = thatBuf.remaining();
		int ret = 0;
		if (remaining > thatRemaining)
			ret = 1;
		else if (remaining < thatRemaining)
			ret = -1;

		if (remaining < 1 || thatRemaining < 1)
			return ret;

		Unit unit = getCurrentUnitToRead();
		BiListNode<Unit> p = m_posNode;
		BiListNode<Unit> s = m_head.previous();

		Unit thatUnit = thatBuf.getCurrentUnitToRead();
		BiListNode<Unit> q = thatBuf.m_posNode;
		BiListNode<Unit> t = thatBuf.m_head.previous();

		int i = unit.position();
		int j = thatUnit.position();

		remaining = unit.remaining();
		thatRemaining = thatUnit.remaining();
		for (;;) {
			if (remaining > thatRemaining) {
				int n = unit.compare(i, thatUnit, j, thatRemaining);
				if (n != 0)
					return n;

				if (q == t)
					break;

				i = thatRemaining;
				remaining -= thatRemaining;
				j = 0;
				q = q.next();
				thatUnit = q.get();
				thatRemaining = thatUnit.size();
			} else {
				int n = unit.compare(i, thatUnit, j, remaining);
				if (n != 0)
					return n;

				if (p == s)
					break;

				j = remaining;
				thatRemaining -= remaining;
				i = 0;
				p = p.next();
				unit = p.get();
				remaining = unit.size();
			}
		}

		return ret;
	}

	@Override
	public void dump(StringBuilder builder) {
		BiListNode<Unit> head = m_head;
		BiListNode<Unit> node = head;
		Blob blob = Blob.get();
		try {
			do {
				node.get().addTo(blob);
				node = node.next();
			} while (node != head);
			blob.dump(builder);
		} finally {
			blob.close();
		}
	}

	@Override
	public IBuffer compact() {
		BiListNode<Unit> head = m_head;
		Unit unit = head.get();
		if (!unit.isEmpty()) {
			unit.compact();
			return this;
		}

		BufferFactory factory = m_factory;
		BiListNode<Unit> prev = head.previous();
		BiListNode<Unit> node = head;
		BiListNode<Unit> next = node.next();
		if (next != head) {
			do {
				factory.putUnit(unit);
				node.close();
				node = next;
				next = next.next();
				unit = node.get();
			} while (next != head && unit.isEmpty());
			m_head = node;
			node.previous(prev);
			prev.next(node);
			m_markNode = node;
			m_posNode = node;
		}

		if (unit.isEmpty())
			unit.clear();
		else
			unit.compact();

		m_size -= m_position;
		m_position = 0;
		m_mark = 0;

		return this;
	}

	@Override
	public IBuffer split(int size) {
		if (size == 0)
			return newBuffer();

		final int thisSize = m_size;
		if (size == thisSize) {
			Buffer buffer = newBuffer();
			drainTo(buffer);
			return buffer;
		}

		if (size > thisSize)
			throw new IllegalArgumentException();

		Buffer slice = getForSlice(m_factory);
		BiListNode<Unit> head = m_head;
		slice.m_head = head;

		// size
		slice.m_size = size;
		int n = thisSize - size;
		m_size = n;

		BiListNode<Unit> node = head;
		Unit unit = null;
		if (n >= size) {
			unit = node.get();
			while (size > (n = unit.size())) {
				size -= n;
				node = node.next();
				unit = node.get();
			}
			size = n - size;
		} else {
			size = n;
			node = node.previous();
			unit = node.get();
			while (size >= (n = unit.size())) {
				size -= n;
				node = node.previous();
				unit = node.get();
			}
		}

		if (size > 0) {
			BiListNode<Unit> temp = BiListNode.create();
			temp.set(unit.cut(size, m_factory));
			m_head = temp;
			BiListNode<Unit> next = node.next();
			if (next == head) {
				temp.next(temp);
				temp.previous(temp);
			} else {
				BiListNode<Unit> tail = head.previous();
				temp.next(next);
				temp.previous(tail);
				next.previous(temp);
				tail.next(temp);
				head.previous(node);
				node.next(head);
			}
		} else {
			m_head = node;
			BiListNode<Unit> prev = node.previous();
			BiListNode<Unit> tail = head.previous();
			head.previous(prev);
			prev.next(head);
			node.previous(tail);
			tail.next(node);
		}

		// mark
		n = m_mark;
		size = slice.m_size;
		if (n > size) {
			slice.m_markNode = node;
			unit.mark();
			slice.m_mark = size;
			m_mark = n - size;
		} else {
			slice.m_markNode = m_markNode;
			slice.m_mark = n;
			m_mark = 0;
			m_markNode = m_head;
		}

		// position
		n = m_position;
		if (n > size) {
			slice.m_posNode = node;
			slice.m_position = size;
			m_position = n - size;
		} else {
			slice.m_posNode = m_posNode;
			slice.m_position = n;
			m_position = 0;
			m_posNode = m_head;
		}

		return slice;
	}

	@Override
	public Buffer newBuffer() {
		return Buffer.get(m_factory);
	}

	@Override
	public void drain() {
		m_position = 0;
		m_size = 0;
		m_mark = 0;
		BiListNode<Unit> head = m_head;
		m_posNode = head;
		m_markNode = head;

		BufferFactory factory = m_factory;
		BiListNode<Unit> node = head.next();
		if (node != head) {
			do {
				factory.putUnit(node.get());
				BiListNode<Unit> temp = node;
				node = node.next();
				temp.close();
			} while (node != head);
			head.previous(head);
			head.next(head);
		}

		head.get().clear();
	}

	@Override
	public void drainTo(IBufferWriter writer) {
		if (!(writer instanceof Buffer)) {
			writer.write(this);
			drain();
			return;
		}

		BufferFactory factory = m_factory;
		Buffer dst = (Buffer) writer;
		BiListNode<Unit> thisPos = m_posNode;
		BiListNode<Unit> node = m_head;
		BiListNode<Unit> thisTail = node.previous();
		while (node != thisPos) {
			factory.putUnit(node.get());
			BiListNode<Unit> temp = node;
			node = node.next();
			temp.close();
		}

		thisPos.get().compact();
		node = dst.m_head;
		BiListNode<Unit> thatTail = node.previous();
		thisPos.previous(thatTail);
		thatTail.next(thisPos);
		thisTail.next(node);
		node.previous(thisTail);

		dst.m_size += m_size;

		node = BiListNode.create();
		node.next(node);
		node.previous(node);
		node.set(factory.getUnit());
		m_head = node;
		m_posNode = node;
		m_markNode = node;

		m_position = 0;
		m_size = 0;
		m_mark = 0;
	}

	@Override
	public int reserveHead(int size) {
		if (size == 0 || m_size > 0)
			return 0;

		Unit unit = m_head.get();
		size %= unit.capacity();
		if (size < 0)
			size += unit.capacity();
		unit.start(size);
		return size;
	}

	@Override
	public int headReserved() {
		return m_head.get().start();
	}
	
	@Override
	public IBuffer setLength(int newLength) {
		if (newLength < 0)
			throw new IllegalArgumentException();

		int len = length();
		if (newLength == len)
			return this;

		if (newLength > len) {
			writeFill((byte) 0, newLength - len);
			return this;
		}

		if (newLength == 0) {
			drain();
			return this;
		}

		BufferFactory factory = m_factory;
		len = len - newLength;
		BiListNode<Unit> node = m_head.previous();
		Unit unit = node.get();
		while ((len -= unit.size()) >= 0) {
			BiListNode<Unit> next = node.next();
			BiListNode<Unit> previous = node.previous();
			previous.next(next);
			next.previous(previous);
			node.close();
			factory.putUnit(unit);

			node = previous;
			unit = node.get();
		}

		unit.size(-len);

		if (m_position > newLength) {
			m_position = newLength;
			m_posNode = node;
			if (m_mark > newLength) {
				m_mark = 0;
				m_markNode = m_head;
			}
		}
		m_size = newLength;

		return this;
	}

	@Override
	public SSLEngineResult unwrap(IBuffer src, SSLEngine engine)
			throws SSLException {
		ByteBuffer netData = null;
		if (src instanceof Buffer) {
			Buffer srcBuf = (Buffer) src;
			Unit unit = null;
			if (srcBuf.remaining() > 0) {
				unit = srcBuf.getCurrentUnitToRead();
				if (srcBuf.m_posNode == srcBuf.m_head.previous())
					netData = unit.getByteBufferForRead();
			} else
				netData = srcBuf.m_posNode.get().getByteBufferForRead();
		}

		BytesBuilder builder = null;
		if (netData == null) {
			builder = BytesBuilder.get(src.remaining());
			netData = builder.getByteBuffer(0, builder.capacity());
			src.mark();
			src.read(netData);
			src.reset();
			netData.flip();
		}

		SSLEngineResult result = null;
		ByteBufferArray bba = ByteBufferArray.get();
		try {
			int pos = netData.position();
			bba.add(getCurrentUnitToWrite().getByteBufferForWrite());
			BiListNode<Unit> head = m_head;
			BiListNode<Unit> node = head.previous();
			for (;;) {
				result = engine.unwrap(netData, bba.array(), 0, bba.size());
				Status status = result.getStatus();
				if (status != Status.BUFFER_OVERFLOW)
					break;

				bba.add(getNextUnitToWrite().getByteBufferForWrite());
			}

			src.skip(netData.position() - pos);

			int n = 0;
			while (node != head) {
				Unit unit = node.get();
				int size = unit.size();
				unit.syncWrite();
				n += unit.size() - size;
				node = node.next();
			}
			m_size += n;
		} finally {
			if (builder != null)
				builder.close();

			bba.clear();
		}

		return result;
	}

	@Override
	public SSLEngineResult wrap(IBuffer dst, SSLEngine engine)
			throws SSLException {
		BiListNode<Unit> node = m_posNode;
		BiListNode<Unit> head = m_head;
		ByteBufferArray bba = ByteBufferArray.get();
		if (remaining() <= 0)
			bba.add(node.get().getByteBufferForRead());
		else {
			bba.add(getCurrentUnitToRead().getByteBufferForRead());
			while (node != head) {
				Unit unit = node.get();
				unit.rewind();
				bba.add(unit.getByteBufferForRead());
				node = node.next();
			}
		}

		BytesBuilder builder = null;
		ByteBuffer netBuf = null;
		Buffer dstBuf = null;
		Unit writeUnit = null;
		if (dst instanceof Buffer) {
			dstBuf = (Buffer) dst;
			writeUnit = dstBuf.getCurrentUnitToWrite();
			if (writeUnit.size() > 0)
				writeUnit = dstBuf.getNextUnitToWrite();
			netBuf = writeUnit.getByteBufferForWrite();
		} else {
			SSLSession session = engine.getSession();
			builder = BytesBuilder.get(session.getPacketBufferSize());
			netBuf = builder.getByteBuffer(0, builder.capacity());
		}

		SSLEngineResult result = null;
		try {
			int pos = netBuf.position();
			for (;;) {
				result = engine.wrap(bba.array(), 0, bba.size(), netBuf);
				Status status = result.getStatus();
				if (status != Status.BUFFER_OVERFLOW)
					break;

				SSLSession session = engine.getSession();
				if (builder == null)
					builder = BytesBuilder.get(session.getPacketBufferSize());
				else
					builder.ensureCapacity(session.getPacketBufferSize());

				netBuf = builder.getByteBuffer(0, builder.capacity());
			}
			if (builder != null) {
				netBuf.flip();
				dst.write(netBuf);
			} else {
				writeUnit.syncWrite();
				dstBuf.m_size += netBuf.position() - pos;
			}

			int n = 0;
			node = m_posNode;
			while (node != head) {
				Unit unit = node.get();
				pos = unit.position();
				unit.syncRead();
				n += unit.position() - pos;
				if (unit.remaining() > 0)
					break;
				node = node.next();
			}
			m_posNode = head.previous();
			m_position += n;
		} finally {
			if (builder != null)
				builder.close();

			bba.clear();
		}

		return result;
	}

	private Unit getCurrentUnitToRead() {
		Unit unit = m_posNode.get();
		if (unit.isEmpty())
			unit = getNextUnitToRead();

		return unit;
	}

	private Unit getNextUnitToRead() {
		BiListNode<Unit> node = m_posNode.next();
		m_posNode = node;
		Unit unit = node.get();
		unit.rewind();
		return unit;
	}

	private Unit getCurrentUnitToWrite() {
		Unit unit = m_head.previous().get();
		if (unit.isFull())
			unit = getNextUnitToWrite();

		return unit;
	}

	private Unit getNextUnitToWrite() {
		BiListNode<Unit> node = BiListNode.create();
		Unit unit = m_factory.getUnit();
		node.set(unit);
		BiListNode<Unit> head = m_head;
		BiListNode<Unit> prev = head.previous();
		node.next(head);
		node.previous(prev);
		prev.next(node);
		head.previous(node);
		return unit;
	}

	private Unit getHeadUnitToWrite() {
		Unit unit = m_head.get();
		if (unit.isHeadFull())
			unit = addHeadUnitToWrite();

		return unit;
	}

	private Unit addHeadUnitToWrite() {
		BiListNode<Unit> node = BiListNode.create();
		Unit unit = m_factory.getUnit();
		unit.start(unit.capacity());
		node.set(unit);

		BiListNode<Unit> head = m_head;
		BiListNode<Unit> prev = head.previous();
		node.next(head);
		node.previous(prev);
		prev.next(node);
		head.previous(node);
		m_head = node;
		return unit;
	}

	private static String errMsg(int start, int length, int size) {
		return StringBuilder.get().append("start[").append(start)
				.append("], length[").append(length).append("], size[")
				.append(size).append(']').toStringAndRelease();
	}

	private int compareInternal(IBuffer that) {
		int n = that.remaining();
		int remaining = remaining();
		if (n < remaining) {
			remaining = n;
			n = 1;
		} else if (n > remaining)
			n = -1;
		else
			n = 0;

		if (remaining > 0) {
			Unit unit = getCurrentUnitToRead();
			BiListNode<Unit> node = m_posNode;
			int size = unit.remaining();
			int i = that.position();
			for (;;) {
				if (remaining < size)
					size = remaining;

				int ret = unit.compare(that, i, size);
				if (ret != 0)
					return ret;

				i += size;
				remaining -= size;

				if (remaining <= 0)
					break;

				node = node.next();
				unit = node.get();
				size = unit.size();
			}
		}

		return n;
	}
}
