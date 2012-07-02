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
package org.jruyi.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * This interface provides the read-only access to the underlying buffer which
 * contains a linear, finite sequence of {@code byte}s. Aside from its content,
 * the essential properties of the underlying buffer are its <i>size</i>,
 * <i>position</i> and <i>mark</i>.
 * <ul>
 * <li><i>size</i> is the number of {@code byte}s that the underlying buffer
 * contains. It is never negative and never changes.
 * <li><i>position</i> is the index of the next {@code byte} to be read. It is
 * never negative and is never greater than <i>size</i>.
 * <li><i>mark</i> is the index to which <i>position</i> will be reset when the
 * method {@link #reset reset} is invoked. It is never negative and is never
 * greater than <i>position</i>. Its initial value is {@code 0}.
 * </ul>
 * 
 * <p>
 * All the methods suffixed with <i>B</i> operate the bytes in big-endian order.
 * All those suffixed with <i>L</i> operate the bytes in little-endian order.
 * 
 * @see IByteSequence
 * @see IBufferWriter
 * @see IBuffer
 * @see IBufferFactory
 */
public interface IBufferReader extends IByteSequence {

	/**
	 * Return the next byte from the underlying buffer. <i>position</i> is
	 * incremented by {@code 1}.
	 * 
	 * @return the next byte read from the underlying buffer
	 * @throws InsufficientDataException
	 *             if there are no bytes remaining in the underlying buffer
	 */
	public byte readByte();

	/**
	 * Interpret the next 2 bytes from the underlying buffer as a {@code short}
	 * value in the big-endian byte order.
	 * 
	 * <p>
	 * Let {@code a} be the first byte and {@code b} be the second byte. The
	 * value returned is:
	 * 
	 * <pre>
	 * {@code (short)((a &lt;&lt; 8) | (b &amp; 0xff))}
	 * </pre>
	 * 
	 * The <i>position</i> is incremented by 2.
	 * 
	 * @return the next 2 bytes, interpreted as a {@code short} value in the
	 *         big-endian byte order
	 * @throws InsufficientDataException
	 *             if there are fewer than 2 bytes remaining in the underlying
	 *             buffer
	 */
	public short readShortB();

	/**
	 * Interpret the next 2 bytes from the underlying buffer as a {@code short}
	 * value in the little-endian byte order.
	 * 
	 * <p>
	 * Let {@code a} be the first byte and {@code b} be the second byte. The
	 * value returned is:
	 * 
	 * <pre>
	 * {@code (short)((b &lt;&lt; 8) | (a &amp; 0xff))}
	 * </pre>
	 * 
	 * The <i>position</i> is incremented by 2.
	 * 
	 * @return the next 2 bytes, interpreted as a {@code short} value in the
	 *         little-endian byte order
	 * @throws InsufficientDataException
	 *             if there are fewer than 2 bytes remaining in the underlying
	 *             buffer
	 */
	public short readShortL();

	/**
	 * Interpret the next 4 bytes from the underlying buffer as an {@code int}
	 * value in the big-endian byte order.
	 * 
	 * <p>
	 * Let {@code a-d} be the first through fourth bytes. The value returned is:
	 * 
	 * <pre>
	 * {@code
	 * ((a &lt;&lt; 24) | ((b &amp; 0xff) &lt;&lt; 16) | ((c &amp; 0xff) &lt;&lt; 8) | (d &amp; 0xff))
	 * }
	 * </pre>
	 * 
	 * The <i>position</i> is incremented by 4.
	 * 
	 * @return the next 4 bytes, interpreted as an {@code int} value in the
	 *         big-endian byte order
	 * @throws InsufficientDataException
	 *             if there are fewer than 4 bytes remaining in the underlying
	 *             buffer
	 */
	public int readIntB();

	/**
	 * Interpret the next 4 bytes from the underlying buffer as an {@code int}
	 * value in the little-endian byte order.
	 * 
	 * <p>
	 * Let {@code a-d} be the first through fourth bytes. The value returned is:
	 * 
	 * <pre>
	 * {@code
	 * ((d &lt;&lt; 24) | ((c &amp; 0xff) &lt;&lt; 16) | ((b &amp; 0xff) &lt;&lt; 8) | (a &amp; 0xff))
	 * }
	 * </pre>
	 * 
	 * The <i>position</i> is incremented by 4.
	 * 
	 * @return the next 4 bytes, interpreted as an {@code int} value in the
	 *         little-endian byte order
	 * @throws InsufficientDataException
	 *             if there are fewer than 4 bytes remaining in the underlying
	 *             buffer
	 */
	public int readIntL();

	/**
	 * Interpret the next 8 bytes from the underlying buffer as a {@code long}
	 * value in the big-endian byte order.
	 * 
	 * <p>
	 * Let {@code a-h} be the first through eighth bytes. The value returned is:
	 * 
	 * <pre>
	 * {@literal
	 * (((long)a << 56) |
	 * ((long)(b & 0xff) << 48) |
	 * ((long)(c & 0xff) << 40) |
	 * ((long)(d & 0xff) << 32) |
	 * ((long)(e & 0xff) << 24) |
	 * ((long)(f & 0xff) << 16) |
	 * ((long)(g & 0xff) << 8) |
	 * ((long)h & 0xff))
	 * }
	 * </pre>
	 * 
	 * The <i>position</i> is incremented by 8.
	 * 
	 * @return the next 8 bytes, interpreted as a {@code long} value in the
	 *         big-endian byte order
	 * @throws InsufficientDataException
	 *             if there are fewer than 8 bytes remaining in the underlying
	 *             buffer
	 */
	public long readLongB();

	/**
	 * Interpret the next 8 bytes from the underlying buffer as a {@code long}
	 * value in the little-endian byte order.
	 * 
	 * <p>
	 * Let {@code a-h} be the first through eighth bytes. The value returned is:
	 * 
	 * <pre>
	 * {@literal
	 * (((long)h << 56) |
	 * ((long)(g & 0xff) << 48) |
	 * ((long)(f & 0xff) << 40) |
	 * ((long)(e & 0xff) << 32) |
	 * ((long)(d & 0xff) << 24) |
	 * ((long)(c & 0xff) << 16) |
	 * ((long)(b & 0xff) << 8) |
	 * ((long)a & 0xff))
	 * }
	 * </pre>
	 * 
	 * The <i>position</i> is incremented by 8.
	 * 
	 * @return the next 8 bytes, interpreted as a {@code long} value in the
	 *         little-endian byte order
	 * @throws InsufficientDataException
	 *             if there are fewer than 8 bytes remaining in the underlying
	 *             buffer
	 */
	public long readLongL();

	/**
	 * Interpret the next 4 bytes from the underlying buffer as a {@code float}
	 * value in the big-endian byte order.
	 * 
	 * <p>
	 * This method actually does
	 * 
	 * <pre>
	 * {@code Float.intBitsToFloat(readIntB())}
	 * </pre>
	 * 
	 * The <i>position</i> is incremented by 4.
	 * 
	 * @return the next 4 bytes, interpreted as a {@code float} value in the
	 *         big-endian byte order
	 * @throws InsufficientDataException
	 *             if there are fewer than 4 bytes remaining in the underlying
	 *             buffer
	 */
	public float readFloatB();

	/**
	 * Interpret the next 4 bytes from the underlying buffer as a {@code float}
	 * value in the little-endian byte order.
	 * 
	 * <p>
	 * This method actually does
	 * 
	 * <pre>
	 * {@code Float.intBitsToFloat(readIntL())}
	 * </pre>
	 * 
	 * The <i>position</i> is incremented by 4.
	 * 
	 * @return the next 4 bytes, interpreted as a {@code float} value in the
	 *         little-endian byte order
	 * @throws InsufficientDataException
	 *             if there are fewer than 4 bytes remaining in the underlying
	 *             buffer
	 */
	public float readFloatL();

	/**
	 * Interpret the next 8 bytes from the underlying buffer as a {@code double}
	 * value in the big-endian byte order.
	 * 
	 * <p>
	 * This method actually does
	 * 
	 * <pre>
	 * {@code Double.longBitsToDouble(readLongB())}
	 * </pre>
	 * 
	 * The <i>position</i> is incremented by 8.
	 * 
	 * @return the next 8 bytes, interpreted as a {@code double} value in the
	 *         big-endian byte order
	 * @throws InsufficientDataException
	 *             if there are fewer than 8 bytes remaining in the underlying
	 *             buffer
	 */
	public double readDoubleB();

	/**
	 * Interpret the next 8 bytes from the underlying buffer as a {@code double}
	 * value in the little-endian byte order.
	 * 
	 * <p>
	 * This method actually does
	 * 
	 * <pre>
	 * {@code Float.longBitsToDouble(readLongL())}
	 * </pre>
	 * 
	 * The <i>position</i> is incremented by 8.
	 * 
	 * @return the next 8 bytes, interpreted as a {@code double} value in the
	 *         little-endian byte order
	 * @throws InsufficientDataException
	 *             if there are fewer than 8 bytes remaining in the underlying
	 *             buffer
	 */
	public double readDoubleL();

	/**
	 * Read some number of bytes from the underlying buffer and store them into
	 * the buffer array {@code dst}. The number of bytes actually read is
	 * {@code Math.min(dst.length, remaining())}, and returned as an integer by
	 * which the <i>position</i> is incremented.
	 * 
	 * <p>
	 * If the length of {@code dst} is zero, then no bytes are read and
	 * {@code 0} is returned. If there is no remaining data, the value
	 * {@code -1} is returned.
	 * 
	 * <p>
	 * The {@code read(dst)} method has the same effect as:
	 * 
	 * <pre>
	 * {@code read(dst, 0, dst.length)}
	 * </pre>
	 * 
	 * @param dst
	 *            the buffer into which the data is read.
	 * @return the total number of bytes read into the buffer, or {@code -1} if
	 *         there is no remaining data
	 * @throws NullPointerException
	 *             if {@code dst} is {@code null}
	 * @see #read(byte[], int, int)
	 */
	public int read(byte[] dst);

	/**
	 * Read up to {@code length} bytes from the underlying buffer into the given
	 * byte array {@code dst}. The number of bytes actually read is
	 * {@code Math.min(length, remaining())}, and returned as an integer by
	 * which the <i>position</i> is incremented.
	 * 
	 * <p>
	 * If {@code length} is zero, then no bytes are read and {@code 0} is
	 * returned. If there is no remaining data, the value {@code -1} is
	 * returned.
	 * 
	 * @param dst
	 *            the buffer into which the data is read
	 * @param offset
	 *            the start offset in array {@code dst} at which the data is
	 *            written
	 * @param length
	 *            the maximum number of bytes to read
	 * @return the total number of bytes read into the buffer, or {@code -1} if
	 *         there is no remaining data
	 * @throws NullPointerException
	 *             if {@code dst} is null
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, {@code length} is negative, or
	 *             {@code length} is greater than {@code dst.length - offset}
	 * @see #read(byte[])
	 */
	public int read(byte[] dst, int offset, int length);

	/**
	 * Read the data remained in the underlying buffer into the given
	 * {@code byteBuffer}. The number of bytes actually read would be the
	 * minimum of {@code remaining()} and {@code byteBuffer.remaining()}.
	 * 
	 * @param byteBuffer
	 *            the {@code ByteBuffer} to read into
	 * @return the total number of bytes read into the {@code byteBuffer}, or
	 *         {@code -1} if there is no remaining data
	 * @throws NullPointerException
	 *             if {@code byteBuffer} is null
	 */
	public int read(ByteBuffer byteBuffer);

	/**
	 * Read the remaining bytes from the underlying buffer, and return as a
	 * {@code byte} array. The <i>position</i> is set to <i>size</i>.
	 * 
	 * <p>
	 * If there are no bytes remaining in the underlying buffer, then a
	 * zero-length byte array is returned.
	 * 
	 * <p>
	 * The method {@code readBytes()} behaves exactly the same way as:
	 * 
	 * <pre>
	 * {@code readBytes(remaining())}
	 * </pre>
	 * 
	 * @return a {@code byte} array that contains the remaining bytes in the
	 *         underlying buffer
	 * @see #readBytes(int)
	 */
	public byte[] readBytes();

	/**
	 * Read the next {@code length} bytes from the underlying buffer, and return
	 * as a {@code byte} array. The <i>position</i> is incremented by
	 * {@code length}.
	 * 
	 * @param length
	 *            the number of bytes to be read
	 * @return If {@code length} is positive, a {@code byte} array that contains
	 *         the bytes read from the underlying buffer is returned.<br>
	 *         If {@code length} is zero, then {@code null} is returned.
	 * @throws IllegalArgumentException
	 *             if {@code length} is negative
	 * @throws InsufficientDataException
	 *             if there are fewer than {@code length} bytes remaining in the
	 *             underlying buffer
	 * @see #readBytes()
	 */
	public byte[] readBytes(int length);

	/**
	 * Decode the remaining bytes in the underlying buffer into a {@code String}
	 * using the platform's default charset, and return the resultant
	 * {@code String}. The length of the resultant {@code String} is a function
	 * of the charset, and hence may not be equal to {@code remaining()}.
	 * 
	 * <p>
	 * The <i>position</i> is set to <i>size</i>.
	 * 
	 * @return the resultant {@code String} as specified
	 * @throws InsufficientDataException
	 *             if there are no bytes remaining in the underlying buffer
	 */
	public String readString();

	/**
	 * Decode the remaining bytes in the underlying buffer into a {@code String}
	 * using the charset whose name is the given {@code charsetName}, and return
	 * the resultant {@code String}. The length of the resultant {@code String}
	 * is a function of the charset, and hence may not be equal to
	 * {@code remaining()}.
	 * 
	 * <p>
	 * The <i>position</i> is set to <i>size</i>.
	 * 
	 * @param charsetName
	 *            the name of the charset to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws IllegalArgumentException
	 *             if the {@code charsetName} is null
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 * @throws InsufficientDataException
	 *             if there are no bytes remaining in the underlying buffer
	 */
	public String readString(String charsetName);

	/**
	 * Decode the remaining bytes in the underlying buffer into a {@code String}
	 * using the given {@code charset}, and return the resultant {@code String}.
	 * The length of the resultant {@code String} is a function of the charset,
	 * and hence may not be equal to {@code remaining()}.
	 * 
	 * <p>
	 * The <i>position</i> is set to <i>size</i>.
	 * 
	 * @param charset
	 *            the charset to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws InsufficientDataException
	 *             if there are no bytes remaining in the underlying buffer
	 */
	public String readString(Charset charset);

	/**
	 * Decode the remaining bytes in the underlying buffer into a {@code String}
	 * using the given {@code ICharsetCodec}, and return the resultant
	 * {@code String}. The length of the resultant {@code String} is a function
	 * of the charset, and hence may not be equal to {@code remaining()}.
	 * 
	 * <p>
	 * The <i>position</i> is set to <i>size</i>.
	 * 
	 * @param ICharsetCodec
	 *            the charset codec to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws InsufficientDataException
	 *             if there are no bytes remaining in the underlying buffer
	 */
	public String readString(ICharsetCodec ICharsetCodec);

	/**
	 * Decode {@code length} bytes remaining in the underlying buffer into a
	 * {@code String} using the platform's default charset, and return the
	 * resultant {@code String}. The length of the resultant {@code String} is a
	 * function of the charset, and hence may not be equal to
	 * {@code remaining()}.
	 * 
	 * <p>
	 * The <i>position</i> is incremented by {@code length}.
	 * 
	 * @param length
	 *            the number of bytes to be decoded
	 * @return the resultant {@code String} as specified
	 * @throws IllegalArgumentException
	 *             if {@code length} is negative
	 * @throws InsufficientDataException
	 *             if there are fewer than {@code length} bytes remaining in the
	 *             underlying buffer
	 */
	public String readString(int length);

	/**
	 * Decode {@code length} bytes remaining in the underlying buffer into a
	 * {@code String} using the charset whose name is the given
	 * {@code charsetName}, and return the resultant {@code String}. The length
	 * of the resultant {@code String} is a function of the charset, and hence
	 * may not be equal to {@code remaining()}.
	 * 
	 * <p>
	 * The <i>position</i> is incremented by {@code length}.
	 * 
	 * @param length
	 *            the number of bytes to be decoded
	 * @param charsetName
	 *            the name of the charset to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws IllegalArgumentException
	 *             if {@code length} is negative or the {@code charsetName} is
	 *             null
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 * @throws InsufficientDataException
	 *             if there are fewer than {@code length} bytes remaining in the
	 *             underlying buffer
	 */
	public String readString(int length, String charsetName);

	/**
	 * Decode {@code length} bytes remaining in the underlying buffer into a
	 * {@code String} using the given {@code charset}, and return the resultant
	 * {@code String}. The length of the resultant {@code String} is a function
	 * of the charset, and hence may not be equal to {@code remaining()}.
	 * 
	 * <p>
	 * The <i>position</i> is incremented by {@code length}.
	 * 
	 * @param length
	 *            the number of bytes to be decoded
	 * @param charset
	 *            the charset to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IllegalArgumentException
	 *             if {@code length} is negative
	 * @throws InsufficientDataException
	 *             if there are fewer than {@code length} bytes remaining in the
	 *             underlying buffer
	 */
	public String readString(int length, Charset charset);

	/**
	 * Decode {@code length} bytes remaining in the underlying buffer into a
	 * {@code String} using the given {@code ICharsetCodec}, and return the
	 * resultant {@code String}. The length of the resultant {@code String} is a
	 * function of the charset, and hence may not be equal to
	 * {@code remaining()}.
	 * 
	 * <p>
	 * The <i>position</i> is incremented by {@code length}.
	 * 
	 * @param length
	 *            the number of bytes to be decoded
	 * @param ICharsetCodec
	 *            the charset codec to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IllegalArgumentException
	 *             if {@code length} is negative
	 * @throws InsufficientDataException
	 *             if there are fewer than {@code length} bytes remaining in the
	 *             underlying buffer
	 */
	public String readString(int length, ICharsetCodec ICharsetCodec);

	/**
	 * Return the next byte from the underlying buffer as an unsigned 8-bit
	 * value. <i>position</i> is incremented by {@code 1}.
	 * 
	 * @return the next byte read as an unsigned 8-bit value
	 * @throws InsufficientDataException
	 *             if there are no bytes remaining in the underlying buffer
	 */
	public int readUByte();

	/**
	 * Interpret the next 2 bytes from the underlying buffer as an unsigned
	 * 16-bit {@code int} value in the big-endian byte order and return the
	 * result. <i>position</i> is incremented by {@code 2}.
	 * 
	 * <p>
	 * Let {@code a} be the first byte and {@code b} be the second byte. The
	 * value returned is:
	 * 
	 * <pre>
	 * {@literal ((a & 0xff) << 8) | (b & 0xff)}
	 * </pre>
	 * 
	 * @return the next 2 bytes, interpreted as an unsigned 16-bit value in the
	 *         big-endian byte order
	 * @throws InsufficientDataException
	 *             if there are fewer than 2 bytes remaining in the underlying
	 *             buffer
	 */
	public int readUShortB();

	/**
	 * Interpret the next 2 bytes from the underlying buffer as an unsigned
	 * 16-bit {@code int} value in the little-endian byte order and return the
	 * result. <i>position</i> is incremented by {@code 2}.
	 * 
	 * <p>
	 * Let {@code a} be the first byte and {@code b} be the second byte. The
	 * value returned is:
	 * 
	 * <pre>
	 * {@literal ((b & 0xff) << 8) | (a & 0xff)}
	 * </pre>
	 * 
	 * @return the next 2 bytes, interpreted as an unsigned 16-bit value in the
	 *         little-endian byte order
	 * @throws InsufficientDataException
	 *             if there are fewer than 2 bytes remaining in the underlying
	 *             buffer
	 */
	public int readUShortL();

	/**
	 * Write the data read from this {@code IBufferReader} to the specified
	 * channel {@code out}.
	 * 
	 * @param out
	 *            the channel to write
	 * @return the number of bytes written, possibly zero
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public int writeOut(WritableByteChannel out) throws IOException;

	/**
	 * Return the number of bytes remaining in the underlying buffer.
	 * 
	 * @return the number of bytes remaining in the underlying buffer
	 */
	public int remaining();

	/**
	 * Set <i>position</i> to the previously marked position.
	 */
	public void reset();

	/**
	 * Set <i>position</i> and <i>mark</i> to {@code 0}.
	 */
	public void rewind();

	/**
	 * Return the number of bytes contained in the underlying buffer.
	 * 
	 * @return the number of bytes contained in the underlying buffer
	 */
	public int size();

	/**
	 * Skip over the next {@code Math.min(n, remaining())} bytes. If {@code n}
	 * is negative, no bytes are skipped.
	 * 
	 * <p>
	 * If m is the actual number of bytes are skipped, then <i>position</i> is
	 * incremented by m.
	 * 
	 * @param n
	 *            the number of bytes to be skipped
	 * @return the actual number of bytes skipped
	 */
	public int skip(int n);

	/**
	 * Set <i>mark</i> to the current <i>position</i>
	 */
	public void mark();

	/**
	 * Return the <i>position</i> of the underlying buffer
	 * 
	 * @return the <i>position</i> of the underlying buffer
	 */
	public int position();

	/**
	 * Return an {@code InputStream} object that represents this
	 * {@code IBufferReader}. Operations made on either one of them will be
	 * reflected in another one.
	 * 
	 * @return an {@code InputStream} object that represents this
	 *         {@code IBufferReader}
	 */
	public InputStream getInputStream();
}
