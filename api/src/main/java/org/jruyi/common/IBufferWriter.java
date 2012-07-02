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
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * This interface provides the write-only access to the underlying buffer. All
 * those <i>set</i> methods operate the bytes in the underlying buffer via
 * <i>index</i> and never increase the size of the underlying buffer. While all
 * those <i>write</i> methods do.
 * 
 * <p>
 * All the methods suffixed with <i>B</i> operate the bytes in big-endian order.
 * All those suffixed with <i>L</i> operate the bytes in little-endian order.
 * 
 * @see IByteSequence
 * @see IBufferReader
 * @see IBuffer
 * @see IBufferFactory
 */
public interface IBufferWriter extends IByteSequence {

	/**
	 * Write the given byte {@code b} to the end of the underlying buffer's
	 * content. The <i>size</i> is incremented by {@code 1}.
	 * 
	 * @param b
	 *            the {@code byte} to be written
	 * @return this buffer writer
	 */
	public IBufferWriter writeByte(byte b);

	/**
	 * Write the bytes in the given byte array {@code src} to the end of the
	 * underlying buffer's content. The <i>size</i> is incremented by
	 * {@code src.length}.
	 * 
	 * <p>
	 * The method {@code writeBytes(src)} behaves exactly the same way as
	 * 
	 * <pre>
	 * {@code writeBytes(src, 0, src.length)}
	 * </pre>
	 * 
	 * @param src
	 *            the source byte array from which the bytes to be written
	 * @return this buffer writer
	 * @see #writeBytes(byte[], int, int)
	 */
	public IBufferWriter writeBytes(byte[] src);

	/**
	 * Write {@code length} bytes starting at the {@code offset} from the given
	 * byte array {@code src} to the end of the underlying buffer's content. The
	 * <i>size</i> is incremented by {@code length}.
	 * 
	 * @param src
	 *            the source byte array from which the bytes to be written
	 * @param offset
	 *            the index of the first byte in the byte array {@code src} to
	 *            be written
	 * @param length
	 *            the number of bytes to be written
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code (offset + length)} is greater than
	 *             {@code src.length}
	 * @see #writeBytes(byte[])
	 */
	public IBufferWriter writeBytes(byte[] src, int offset, int length);

	/**
	 * Write the 4 bytes, interpreted from the given int {@code i} as in the
	 * big-endian byte order, to the end of the underlying buffer's content. The
	 * <i>size</i> is incremented by {@code 4}.
	 * 
	 * @param i
	 *            the {@code int} value to be interpreted and written
	 * @return this buffer writer
	 */
	public IBufferWriter writeIntB(int i);

	/**
	 * Write the 4 bytes, interpreted from the given int {@code i} as in the
	 * little-endian byte order, to the end of the underlying buffer's content.
	 * The <i>size</i> is incremented by {@code 4}.
	 * 
	 * @param i
	 *            the {@code int} value to be interpreted and written
	 * @return this buffer writer
	 */
	public IBufferWriter writeIntL(int i);

	/**
	 * Write the 8 bytes, interpreted from the given long {@code l} as in the
	 * big-endian byte order, to the end of the underlying buffer's content. The
	 * <i>size</i> is incremented by {@code 8}.
	 * 
	 * @param l
	 *            the {@code long} value to be interpreted and written
	 * @return this buffer writer
	 */
	public IBufferWriter writeLongB(long l);

	/**
	 * Write the 8 bytes, interpreted from the given long {@code l} as in the
	 * little-endian byte order, to the end of the underlying buffer's content.
	 * The <i>size</i> is incremented by {@code 8}.
	 * 
	 * @param l
	 *            the {@code long} value to be interpreted and written
	 * @return this buffer writer
	 */
	public IBufferWriter writeLongL(long l);

	/**
	 * Write the 2 bytes, interpreted from the given short {@code s} as in the
	 * big-endian byte order, to the end of the underlying buffer's content. The
	 * <i>size</i> is incremented by {@code 2}.
	 * 
	 * @param s
	 *            the {@code short} value to be interpreted and written
	 * @return this buffer writer
	 */
	public IBufferWriter writeShortB(short s);

	/**
	 * Write the 2 bytes, interpreted from the given short {@code s} as in the
	 * little-endian byte order, to the end of the underlying buffer's content.
	 * The <i>size</i> is incremented by {@code 2}.
	 * 
	 * @param s
	 *            the {@code short} value to be interpreted and written
	 * @return this buffer writer
	 */
	public IBufferWriter writeShortL(short s);

	/**
	 * Write the 4 bytes, interpreted from the given float {@code f} as in the
	 * big-endian byte order, to the end of the underlying buffer's content.
	 * 
	 * <p>
	 * This method actually does
	 * 
	 * <pre>
	 * {@code writeIntB(Float.floatToRawIntBits(f))}
	 * </pre>
	 * 
	 * The <i>size</i> is incremented by {@code 4}.
	 * 
	 * @param f
	 *            the {@code float} value to be interpreted and written
	 * @return this buffer writer
	 */
	public IBufferWriter writeFloatB(float f);

	/**
	 * Write the 4 bytes, interpreted from the given float {@code f} as in the
	 * little-endian byte order, to the end of the underlying buffer's content.
	 * 
	 * <p>
	 * This method actually does
	 * 
	 * <pre>
	 * {@code writeIntL(Float.floatToRawIntBits(f))}
	 * </pre>
	 * 
	 * The <i>size</i> is incremented by {@code 4}.
	 * 
	 * @param f
	 *            the {@code float} value to be interpreted and written
	 * @return this buffer writer
	 */
	public IBufferWriter writeFloatL(float f);

	/**
	 * Write the 8 bytes, interpreted from the given double {@code d} as in the
	 * big-endian byte order, to the end of the underlying buffer's content.
	 * 
	 * <p>
	 * This method actually does
	 * 
	 * <pre>
	 * {@code writeLongB(Double.doubleToRawLongBits(d))}
	 * </pre>
	 * 
	 * The <i>size</i> is incremented by {@code 8}.
	 * 
	 * @param d
	 *            the {@code double} value to be interpreted and written
	 * @return this buffer writer
	 */
	public IBufferWriter writeDoubleB(double d);

	/**
	 * Write the 8 bytes, interpreted from the given double {@code d} as in the
	 * little-endian byte order, to the end of the underlying buffer's content.
	 * 
	 * <p>
	 * This method actually does
	 * 
	 * <pre>
	 * {@code writeLongL(Double.doubleToRawLongBits(d))}
	 * </pre>
	 * 
	 * The <i>size</i> is incremented by {@code 8}.
	 * 
	 * @param d
	 *            the {@code double} value to be interpreted and written
	 * @return this buffer writer
	 */
	public IBufferWriter writeDoubleL(double d);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} using
	 * the platform's default charset, to the end of the underlying buffer. The
	 * number of bytes written depends on the charset. If {@code n} bytes are
	 * written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to be encoded and written
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} is {@code null}
	 */
	public IBufferWriter write(char[] chars);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} using
	 * the specified charset, to the end of the underlying buffer. The number of
	 * bytes written depends on the charset. If {@code n} bytes are written,
	 * then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to be encoded and written
	 * @param charset
	 *            the charset to encode the given {@code chars}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} or {@code charset} is {@code null}
	 */
	public IBufferWriter write(char[] chars, Charset charset);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} using
	 * the specified charset, to the end of the underlying buffer. The number of
	 * bytes written depends on the charset. If {@code n} bytes are written,
	 * then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to be encoded and written
	 * @param charsetName
	 *            the name of the charset to encode the given {@code chars}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} or {@code charsetName} is {@code null}
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 */
	public IBufferWriter write(char[] chars, String charsetName);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} using
	 * the specified charset codec, to the end of the underlying buffer. The
	 * number of bytes written depends on the charset. If {@code n} bytes are
	 * written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to be encoded and written
	 * @param ICharsetCodec
	 *            the charset codec to encode the given {@code chars}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} or {@code ICharsetCodec} is {@code null}
	 */
	public IBufferWriter write(char[] chars, ICharsetCodec ICharsetCodec);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} starting
	 * at {@code offset}(inclusive) ending at {@code (offset + length)}
	 * (exclusive) using the platform's default charset, to the end of the
	 * underlying buffer's content. The number of bytes written depends on the
	 * charset. If {@code n} bytes are written, then the <i>size</i> is
	 * incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to be encoded and written
	 * @param offset
	 *            the index of the first character(inclusive) in {@code chars}
	 *            to write
	 * @param length
	 *            the number of characters in {@code chars} to write
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (str.length() - length)}
	 */
	public IBufferWriter write(char[] chars, int offset, int length);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} starting
	 * at {@code offset}(inclusive) ending at {@code (offset + length)}
	 * (exclusive) using the specified charset, to the end of the underlying
	 * buffer's content. The number of bytes written depends on the charset. If
	 * {@code n} bytes are written, then the <i>size</i> is incremented by
	 * {@code n}.
	 * 
	 * @param chars
	 *            the char array to be encoded and written
	 * @param offset
	 *            the index of the first character(inclusive) in {@code chars}
	 *            to write
	 * @param length
	 *            the number of characters in {@code chars} to write
	 * @param charset
	 *            the charset to encode the given {@code chars}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} or {@code charset} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (str.length() - length)}
	 */
	public IBufferWriter write(char[] chars, int offset, int length,
			Charset charset);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} starting
	 * at {@code offset}(inclusive) ending at {@code (offset + length)}
	 * (exclusive) using the specified charset, to the end of the underlying
	 * buffer's content. The number of bytes written depends on the charset. If
	 * {@code n} bytes are written, then the <i>size</i> is incremented by
	 * {@code n}.
	 * 
	 * @param chars
	 *            the char array to be encoded and written
	 * @param offset
	 *            the index of the first character(inclusive) in {@code chars}
	 *            to write
	 * @param length
	 *            the number of characters in {@code chars} to write
	 * @param charsetName
	 *            the name of the charset to encode the given {@code chars}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} or {@code charsetName} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (str.length() - length)}
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 */
	public IBufferWriter write(char[] chars, int offset, int length,
			String charsetName);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} starting
	 * at {@code offset}(inclusive) ending at {@code (offset + length)}
	 * (exclusive) using the specified charset codec, to the end of the
	 * underlying buffer's content. The number of bytes written depends on the
	 * charset. If {@code n} bytes are written, then the <i>size</i> is
	 * incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to be encoded and written
	 * @param offset
	 *            the index of the first character(inclusive) in {@code chars}
	 *            to write
	 * @param length
	 *            the number of characters in {@code chars} to write
	 * @param ICharsetCodec
	 *            the charset codec to encode the given {@code chars}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} or {@code ICharsetCodec} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (str.length() - length)}
	 */
	public IBufferWriter write(char[] chars, int offset, int length,
			ICharsetCodec ICharsetCodec);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * using the platform's default charset, to the end of the underlying
	 * buffer. The number of bytes written depends on the charset. If {@code n}
	 * bytes are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to be encoded and written
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} is {@code null}
	 */
	public IBufferWriter write(CharSequence cs);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * using the specified charset, to the end of the underlying buffer. The
	 * number of bytes written depends on the charset. If {@code n} bytes are
	 * written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to be encoded and written
	 * @param charset
	 *            the charset to encode the given {@code cs}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} or {@code charset} is {@code null}
	 */
	public IBufferWriter write(CharSequence cs, Charset charset);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * using the specified charset, to the end of the underlying buffer. The
	 * number of bytes written depends on the charset. If {@code n} bytes are
	 * written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to be encoded and written
	 * @param charsetName
	 *            the name of the charset to encode the given {@code cs}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} or {@code charsetName} is {@code null}
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 */
	public IBufferWriter write(CharSequence cs, String charsetName);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * using the specified charset codec, to the end of the underlying buffer.
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to be encoded and written
	 * @param ICharsetCodec
	 *            the charset codec to encode the given {@code cs}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} or {@code ICharsetCodec} is {@code null}
	 */
	public IBufferWriter write(CharSequence cs, ICharsetCodec ICharsetCodec);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * starting at {@code offset}(inclusive) ending at {@code (offset + length)}
	 * (exclusive) using the platform's default charset, to the end of the
	 * underlying buffer. The number of bytes written depends on the charset. If
	 * {@code n} bytes are written, then the <i>size</i> is incremented by
	 * {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to be encoded and written
	 * @param offset
	 *            the index of the first character(inclusive) in {@code cs} to
	 *            write
	 * @param length
	 *            the number of characters in {@code cs} to write
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (cs.length() - length)}
	 */
	public IBufferWriter write(CharSequence cs, int offset, int length);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * starting at {@code offset}(inclusive) ending at {@code (offset + length)}
	 * (exclusive) using the specified charset, to the end of the underlying
	 * buffer. The number of bytes written depends on the charset. If {@code n}
	 * bytes are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to be encoded and written
	 * @param offset
	 *            the index of the first character(inclusive) in {@code cs} to
	 *            write
	 * @param length
	 *            the number of characters in {@code cs} to write
	 * @param charset
	 *            the charset to encode {@code cs}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} or {@code charset} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (cs.length() - length)}
	 */
	public IBufferWriter write(CharSequence cs, int offset, int length,
			Charset charset);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * starting at {@code offset}(inclusive) ending at {@code (offset + length)}
	 * (exclusive) using the specified charset, to the end of the underlying
	 * buffer. The number of bytes written depends on the charset. If {@code n}
	 * bytes are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to be encoded and written
	 * @param offset
	 *            the index of the first character(inclusive) in {@code cs} to
	 *            write
	 * @param length
	 *            the number of characters in {@code cs} to write
	 * @param charsetName
	 *            the name of the charset to encode {@code cs}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} or {@code charset} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (cs.length() - length)}
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 */
	public IBufferWriter write(CharSequence cs, int offset, int length,
			String charsetName);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * starting at {@code offset}(inclusive) ending at {@code (offset + length)}
	 * (exclusive) using the specified charset codec, to the end of the
	 * underlying buffer. The number of bytes written depends on the charset. If
	 * {@code n} bytes are written, then the <i>size</i> is incremented by
	 * {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to be encoded and written
	 * @param offset
	 *            the index of the first character(inclusive) in {@code cs} to
	 *            write
	 * @param length
	 *            the number of characters in {@code cs} to write
	 * @param ICharsetCodec
	 *            the charset codec to encode {@code cs}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} or {@code ICharsetCodec} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (cs.length() - length)}
	 */
	public IBufferWriter write(CharSequence cs, int offset, int length,
			ICharsetCodec ICharsetCodec);

	/**
	 * Write the bytes read from the given {@code IBufferReader}, starting at
	 * the position the {@code IBufferReader} holds, to the end of the
	 * underlying buffer's content. The <i>size</i> is incremented by
	 * {@code in.remaining()}.
	 * <p>
	 * The method {@code write(in)} behaves exactly the same way as
	 * 
	 * <pre>
	 * {@code write(in, in.remaining())}
	 * </pre>
	 * 
	 * @param in
	 *            the source {@code IBufferReader} from which bytes are to be
	 *            read
	 * @return this buffer writer
	 * @throws InsufficientDataException
	 *             if there are no bytes remaining in the underlying buffer of
	 *             the given {@code IBufferReader}
	 */
	public IBufferWriter write(IBufferReader in);

	/**
	 * Write {@code length} bytes from the given {@code IBufferReader}, starting
	 * at the position the {@code IBufferReader} holds, to the end of the
	 * underlying buffer's content. The <i>size</i> of this buffer writer is
	 * incremented by {@code length}. The <i>position</i> of the given
	 * {@code in} is incremented by {@code length}.
	 * 
	 * <p>
	 * If {@code length} is not positive, then this method does nothing.
	 * 
	 * @param in
	 *            the source {@code IBufferReader} from which bytes are to be
	 *            read
	 * @param length
	 *            the number of bytes to be read from {@code IBufferReader}
	 * @return this buffer writer
	 * @throws IllegalArgumentException
	 *             if {@code length} is negative
	 * @throws InsufficientDataException
	 *             if there are fewer than {@code length} bytes remaining in the
	 *             underlying buffer of {@code in}
	 */
	public IBufferWriter write(IBufferReader in, int length);

	/**
	 * Write the given byte sequence {@code in} to the end of the underlying
	 * buffer. The <i>size</i> is incremented by {@code in.length()}.
	 * 
	 * <p>
	 * The method {@code write(in)} behaves exactly the same way as
	 * 
	 * <pre>
	 * {@code write(in, 0, in.length())}
	 * </pre>
	 * 
	 * @param in
	 *            the byte sequence to write
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code in} is {@code null}
	 */
	public IBufferWriter write(IByteSequence in);

	/**
	 * Write the given byte sequence {@code in} to the end of the underlying
	 * buffer. The <i>size</i> is incremented by {@code length}.
	 * 
	 * @param in
	 *            the byte sequence to write
	 * @param offset
	 *            the index of the first byte to write
	 * @param length
	 *            the number of the bytes to write
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code in} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negatvie, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (in.length() - length)}
	 */
	public IBufferWriter write(IByteSequence in, int offset, int length);

	/**
	 * Write the data remained in the given {@code byteBuffer} into the
	 * underlying buffer. The <i>size</i> is incremented by
	 * {@code byteBuffer.remaining()}.
	 * 
	 * @param byteBuffer
	 *            the {@code ByteBuffer} to write from
	 * @return this buffer writer
	 */
	public IBufferWriter write(ByteBuffer byteBuffer);

	/**
	 * Write {@code count} {@code b} values into the underlying buffer. The
	 * <i>size</i> is incremented by {@code count}.
	 *
	 * @param b
	 *            the byte value to write
	 * @param count
	 *            the number of {@code b} values to write
	 * @return this buffer writer
	 * @throws IllegalArgumentException
	 *             if {@code count} is negative
	 */
	public IBufferWriter writeFill(byte b, int count);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} using
	 * the platform's default charset, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to encode and write
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} is {@code null}
	 */
	public IBufferWriter headWrite(char[] chars);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} using
	 * the specified charset, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to encode and write
	 * @param charset
	 *            the charset to encode {@code chars}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} {@code charset} is {@code null}
	 */
	public IBufferWriter headWrite(char[] chars, Charset charset);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} using
	 * the specified charset, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to encode and write
	 * @param charsetName
	 *            the name of the charset to encode {@code chars}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} {@code charsetName} is {@code null}
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 */
	public IBufferWriter headWrite(char[] chars, String charsetName);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} using
	 * the specified charset codec, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to encode and write
	 * @param ICharsetCodec
	 *            the charset codec to encode {@code chars}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} {@code ICharsetCodec} is {@code null}
	 */
	public IBufferWriter headWrite(char[] chars, ICharsetCodec ICharsetCodec);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} starting
	 * at {@code offset} ending at {@code (offset + length)} using the
	 * platform's default charset, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to encode and write
	 * @param offset
	 *            the index of the first character in {@code chars} to encode
	 * @param length
	 *            the number of characters in {@code chars} to encode
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (chars.length() - length)}
	 */
	public IBufferWriter headWrite(char[] chars, int offset, int length);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} starting
	 * at {@code offset} ending at {@code (offset + length)} using the specified
	 * charset, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to encode and write
	 * @param offset
	 *            the index of the first character in {@code chars} to encode
	 * @param length
	 *            the number of characters in {@code chars} to encode
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} or {@code charset} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (chars.length() - length)}
	 */
	public IBufferWriter headWrite(char[] chars, int offset, int length,
			Charset charset);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} starting
	 * at {@code offset} ending at {@code (offset + length)} using the specified
	 * charset, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to encode and write
	 * @param offset
	 *            the index of the first character in {@code chars} to encode
	 * @param length
	 *            the number of characters in {@code chars} to encode
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} or {@code charsetName} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (chars.length() - length)}
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 */
	public IBufferWriter headWrite(char[] chars, int offset, int length,
			String charsetName);

	/**
	 * Write the bytes, encoded from the given char array {@code chars} starting
	 * at {@code offset} ending at {@code (offset + length)} using the specified
	 * charset codec, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param chars
	 *            the char array to encode and write
	 * @param offset
	 *            the index of the first character in {@code chars} to encode
	 * @param length
	 *            the number of characters in {@code chars} to encode
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code chars} or {@code ICharsetCodec} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (chars.length() - length)}
	 */
	public IBufferWriter headWrite(char[] chars, int offset, int length,
			ICharsetCodec ICharsetCodec);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * using the platform's default charset, to the head of the underlying
	 * buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to encode and write
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} is {@code null}
	 */
	public IBufferWriter headWrite(CharSequence cs);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * using the specified charset, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to encode and write
	 * @param charset
	 *            the charset to encode {@code cs}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} or {@code charset} is {@code null}
	 */
	public IBufferWriter headWrite(CharSequence cs, Charset charset);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * using the specified charset, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to encode and write
	 * @param charsetName
	 *            the name of the charset to encode {@code cs}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} or {@code charset} is {@code null}
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 */
	public IBufferWriter headWrite(CharSequence cs, String charsetName);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * using the specified charset codec, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to encode and write
	 * @param ICharsetCodec
	 *            the charset codec to encode {@code cs}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} or {@code ICharsetCodec} is {@code null}
	 */
	public IBufferWriter headWrite(CharSequence cs, ICharsetCodec ICharsetCodec);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * starting at {@code offset} ending at {@code (offset + length)} using the
	 * platform's default charset, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to encode and write
	 * @param offset
	 *            the index of the first character in {@code cs} to encode
	 * @param length
	 *            the number of characters in {@code cs} to encode
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (cs.length() - length)}
	 */
	public IBufferWriter headWrite(CharSequence cs, int offset, int length);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * starting at {@code offset} ending at {@code (offset + length)} using the
	 * specified charset, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to encode and write
	 * @param offset
	 *            the index of the first character in {@code cs} to encode
	 * @param length
	 *            the number of characters in {@code cs} to encode
	 * @param charset
	 *            the charset to encode {@code cs}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} or {@code charset} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (cs.length() - length)}
	 */
	public IBufferWriter headWrite(CharSequence cs, int offset, int length,
			Charset charset);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * starting at {@code offset} ending at {@code (offset + length)} using the
	 * specified charset, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to encode and write
	 * @param offset
	 *            the index of the first character in {@code cs} to encode
	 * @param length
	 *            the number of characters in {@code cs} to encode
	 * @param charsetName
	 *            the name of the charset to encode {@code cs}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} or {@code charsetName} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (cs.length() - length)}
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 */
	public IBufferWriter headWrite(CharSequence cs, int offset, int length,
			String charsetName);

	/**
	 * Write the bytes, encoded from the given character sequence {@code cs}
	 * starting at {@code offset} ending at {@code (offset + length)} using the
	 * specified charset codec, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The number of bytes written depends on the charset. If {@code n} bytes
	 * are written, then the <i>size</i> is incremented by {@code n}.
	 * 
	 * @param cs
	 *            the character sequence to encode and write
	 * @param offset
	 *            the index of the first character in {@code cs} to encode
	 * @param length
	 *            the number of characters in {@code cs} to encode
	 * @param ICharsetCodec
	 *            the charset codec to encode {@code cs}
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code cs} or {@code ICharsetCodec} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (cs.length() - length)}
	 */
	public IBufferWriter headWrite(CharSequence cs, int offset, int length,
			ICharsetCodec ICharsetCodec);

	/**
	 * Write the given byte {@code b} to the head of the underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code 1}.
	 * 
	 * @param b
	 *            the {@code byte} value to write
	 * @return this buffer writer
	 */
	public IBufferWriter headWriteByte(byte b);

	/**
	 * Write the 2 bytes, interpreted from the given short {@code s} in the
	 * big-endian byte order, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code 2}.
	 * 
	 * @param s
	 *            the {@code short} value to write
	 * @return this buffer writer
	 */
	public IBufferWriter headWriteShortB(short s);

	/**
	 * Write the 2 bytes, interpreted from the given short {@code s} in the
	 * little-endian byte order, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code 2}.
	 * 
	 * @param s
	 *            the {@code short} value to write
	 * @return this buffer writer
	 */
	public IBufferWriter headWriteShortL(short s);

	/**
	 * Write the 4 bytes, interpreted from the given int {@code i} in the
	 * big-endian byte order, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code 4}.
	 * 
	 * @param i
	 *            the {@code int} value to write
	 * @return this buffer writer
	 */
	public IBufferWriter headWriteIntB(int i);

	/**
	 * Write the 4 bytes, interpreted from the given int {@code i} in the
	 * little-endian byte order, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code 4}.
	 * 
	 * @param i
	 *            the {@code int} value to write
	 * @return this buffer writer
	 */
	public IBufferWriter headWriteIntL(int i);

	/**
	 * Write the 8 bytes, interpreted from the given long {@code l} in the
	 * big-endian byte order, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code 8}.
	 * 
	 * @param l
	 *            the {@code long} value to write
	 * @return this buffer writer
	 */
	public IBufferWriter headWriteLongB(long l);

	/**
	 * Write the 8 bytes, interpreted from the given long {@code l} in the
	 * little-endian byte order, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code 8}.
	 * 
	 * @param l
	 *            the {@code long} value to write
	 * @return this buffer writer
	 */
	public IBufferWriter headWriteLongL(long l);

	/**
	 * Write the 4 bytes, interpreted from the given float {@code f} in the
	 * big-endian byte order, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code 4}.
	 * 
	 * @param f
	 *            the {@code float} value to write
	 * @return this buffer writer
	 */
	public IBufferWriter headWriteFloatB(float f);

	/**
	 * Write the 4 bytes, interpreted from the given float {@code f} in the
	 * little-endian byte order, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code 4}.
	 * 
	 * @param f
	 *            the {@code float} value to write
	 * @return this buffer writer
	 */
	public IBufferWriter headWriteFloatL(float f);

	/**
	 * Write the 8 bytes, interpreted from the given double {@code d} in the
	 * big-endian byte order, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code 8}.
	 * 
	 * @param d
	 *            the {@code double} value to write
	 * @return this buffer writer
	 */
	public IBufferWriter headWriteDoubleB(double d);

	/**
	 * Write the 8 bytes, interpreted from the given double {@code d} in the
	 * little-endian byte order, to the head of the underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code 8}.
	 * 
	 * @param d
	 *            the {@code double} value to write
	 * @return this buffer writer
	 */
	public IBufferWriter headWriteDoubleL(double d);

	/**
	 * Write the content of the given byte array {@code src} to the head of the
	 * underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code src.length}.
	 * 
	 * <p>
	 * The method {@code headWriteBytes(src} behaves exactly the same way as
	 * 
	 * <pre>
	 * {@code headWriteBytes(src, 0, src.length)}
	 * </pre>
	 * 
	 * @param src
	 *            the bytes from which to write
	 * @return this buffer writer
	 */
	public IBufferWriter headWriteBytes(byte[] src);

	/**
	 * Write {@code length} bytes of {@code src} starting at the specified
	 * {@code offset} to the head of the underlying buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code length}.
	 * 
	 * @param src
	 *            bytes of which to write
	 * @param offset
	 *            the offset of the byte in {@code src} to write as the first
	 *            byte
	 * @param length
	 *            the number of bytes from {@code src} to write
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code length} is greater than
	 *             {@code (src.length - offset)}
	 */
	public IBufferWriter headWriteBytes(byte[] src, int offset, int length);

	/**
	 * Write the bytes read from the given {@code IBufferReader}, starting at
	 * the position the {@code IBufferReader} holds, to the head of the
	 * underlying buffer's content. The <i>size</i> is incremented by
	 * {@code in.remaining()}.
	 * 
	 * <p>
	 * The method {@code write(in)} behaves exactly the same way as
	 * 
	 * <pre>
	 * {@code headWrite(in, in.remaining())}
	 * </pre>
	 * 
	 * @param in
	 *            the source {@code IBufferReader} from which bytes are to be
	 *            read
	 * @return this buffer writer
	 * @throws InsufficientDataException
	 *             if there are no bytes remaining in the underlying buffer of
	 *             the given {@code IBufferReader}
	 */
	public IBufferWriter headWrite(IBufferReader in);

	/**
	 * Write {@code length} bytes from the given {@code IBufferReader}, starting
	 * at the position the {@code IBufferReader} holds, to the head of the
	 * underlying buffer's content. The <i>size</i> of this buffer writer is
	 * incremented by {@code length}.
	 * 
	 * <p>
	 * If {@code length} is not positive, then this method does nothing.
	 * 
	 * @param in
	 *            the source {@code IBufferReader} from which bytes are to be
	 *            read
	 * @param length
	 *            the number of bytes to be read from {@code IBufferReader}
	 * @return this buffer writer
	 * @throws IllegalArgumentException
	 *             if {@code length} is negative
	 * @throws InsufficientDataException
	 *             if there are fewer than {@code length} bytes remaining in the
	 *             underlying buffer of {@code in}
	 */
	public IBufferWriter headWrite(IBufferReader in, int length);

	/**
	 * Write the given byte sequence {@code in} to the head of the underlying
	 * buffer. The <i>size</i> is incremented by {@code in.length()}.
	 * 
	 * <p>
	 * The method {@code write(in)} behaves exactly the same way as
	 * 
	 * <pre>
	 * {@code headWrite(in, 0, in.length())}
	 * </pre>
	 * 
	 * @param in
	 *            the byte sequence to write
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code in} is {@code null}
	 */
	public IBufferWriter headWrite(IByteSequence in);

	/**
	 * Write the given byte sequence {@code in} to the head of the underlying
	 * buffer. The <i>size</i> is incremented by {@code length}.
	 * 
	 * @param in
	 *            the byte sequence to write
	 * @param offset
	 *            the index of the first byte to write
	 * @param length
	 *            the number of the bytes to write
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code in} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (in.length() - length)}
	 */
	public IBufferWriter headWrite(IByteSequence in, int offset, int length);

	/**
	 * Write the data remained in the given {@code byteBuffer} to the head of
	 * the underlying buffer. The <i>size</i> is incremented by
	 * {@code byteBuffer.remaining()}.
	 * 
	 * @param byteBuffer
	 *            the {@code ByteBuffer} to write from
	 * @return this buffer writer
	 * @throws NullPointerException
	 *             if {@code byteBuffer} is {@code null}
	 */
	public IBufferWriter headWrite(ByteBuffer byteBuffer);

	/**
	 * Write {@code count} {@code b} values to the head of the underlying buffer.
	 * The <i>size</i> is incremented by {@code count}.
	 *
	 * @param b
	 *            the byte to write
	 * @param count
	 *            the number of {@code b} values to write
	 * @return this buffer writer
	 * @throws IllegalArgumentException
	 *             if {@code count} is negative
	 */
	public IBufferWriter headWriteFill(byte b, int count);

	/**
	 * Set the byte at the specified position to the given byte {@code b}.
	 * 
	 * @param index
	 *            the index of the byte to be set
	 * @param b
	 *            the {@code byte} value to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>
	 */
	public IBufferWriter setByte(int index, byte b);

	/**
	 * Set the 2 bytes starting at the specified position to the ones
	 * interpreted from the given short {@code s}, in the big-endian byte order.
	 * 
	 * @param start
	 *            the index of the first byte to be set
	 * @param s
	 *            the {@code short} value to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus one
	 */
	public IBufferWriter setShortB(int start, short s);

	/**
	 * Set the 2 bytes starting at the specified position to the ones
	 * interpreted from the given short {@code s}, in the little-endian byte
	 * order.
	 * 
	 * @param start
	 *            the index of the first byte to be set
	 * @param s
	 *            the {@code short} value to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus one
	 */
	public IBufferWriter setShortL(int start, short s);

	/**
	 * Set the 4 bytes starting at the specified position to the ones
	 * interpreted from the given int {@code i}, in the big-endian byte order.
	 * 
	 * @param start
	 *            the index of the first byte to be set
	 * @param i
	 *            the {@code int} value to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus three
	 */
	public IBufferWriter setIntB(int start, int i);

	/**
	 * Set the 4 bytes starting at the specified position to the ones
	 * interpreted from the given int {@code i}, in the little-endian byte
	 * order.
	 * 
	 * @param start
	 *            the index of the first byte to be set
	 * @param i
	 *            the {@code int} value to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus three
	 */
	public IBufferWriter setIntL(int start, int i);

	/**
	 * Set the 8 bytes starting at the specified position to the ones
	 * interpreted from the given long {@code l}, in the big-endian byte order.
	 * 
	 * @param start
	 *            the index of the first byte to be set
	 * @param l
	 *            the {@code long} value to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus seven
	 */
	public IBufferWriter setLongB(int start, long l);

	/**
	 * Set the 8 bytes starting at the specified position to the ones
	 * interpreted from the given long {@code l}, in the little-endian byte
	 * order.
	 * 
	 * @param start
	 *            the index of the first byte to be set
	 * @param l
	 *            the {@code long} value to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus seven
	 */
	public IBufferWriter setLongL(int start, long l);

	/**
	 * Set the 4 bytes starting at the specified position to the ones
	 * interpreted from the given float {@code f}, in the big-endian byte order.
	 * 
	 * @param start
	 *            the index of the first byte to be set
	 * @param f
	 *            the {@code float} value to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus three
	 */
	public IBufferWriter setFloatB(int start, float f);

	/**
	 * Set the 4 bytes starting at the specified position to the ones
	 * interpreted from the given float {@code f}, in the little-endian byte
	 * order.
	 * 
	 * @param start
	 *            the index of the first byte to be set
	 * @param f
	 *            the {@code float} value to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus three
	 */
	public IBufferWriter setFloatL(int start, float f);

	/**
	 * Set the 8 bytes starting at the specified position to the ones
	 * interpreted from the given double {@code d}, in the big-endian byte
	 * order.
	 * 
	 * @param start
	 *            the index of the first byte to be set
	 * @param d
	 *            the {@code double} value to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus seven
	 */
	public IBufferWriter setDoubleB(int start, double d);

	/**
	 * Set the 8 bytes starting at the specified position to the ones
	 * interpreted from the given double {@code d}, in the little-endian byte
	 * order.
	 * 
	 * @param start
	 *            the index of the first byte to be set
	 * @param d
	 *            the {@code double} value to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus seven
	 */
	public IBufferWriter setDoubleL(int start, double d);

	/**
	 * Set {@code src.length} bytes starting at the specified {@code start} to
	 * the ones contained in the given byte array {@code src}.
	 * 
	 * <p>
	 * The method {@code setBytes(start, src} behaves exactly the same way as
	 * 
	 * <pre>
	 * {@code setBytes(start, src, 0, src.length)}
	 * </pre>
	 * 
	 * @param start
	 *            the index of the first byte to be set
	 * @param src
	 *            the bytes from which to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus {@code src.length - 1}
	 */
	public IBufferWriter setBytes(int start, byte[] src);

	/**
	 * Set {@code length} bytes starting at the specified {@code start} to the
	 * ones contained in the given byte array {@code src}, starting at the
	 * specified {@code offset}.
	 * 
	 * @param start
	 *            the index of the first byte to be set
	 * @param src
	 *            the bytes from which to set
	 * @param offset
	 *            the offset of the byte in {@code src} to set as the first byte
	 * @param length
	 *            the number of bytes from {@code src} to set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus {@code length - 1}, or
	 *             {@code offset} is negative, or {@code length} is negative, or
	 *             {@code length} is greater than {@code (src.length - offset)}
	 */
	public IBufferWriter setBytes(int start, byte[] src, int offset, int length);

	/**
	 * Set all {@code count} bytes starting at the specified {@code start} to
	 * the specified byte {@code b}.
	 *
	 * @param start
	 *            the index of the first byte to be set
	 * @param b
	 *            the byte to set
	 * @param count
	 *            the number of bytes to be set
	 * @return this buffer writer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus {@code count - 1}, or
	 *             {@code count} is negative
	 */
	public IBufferWriter setFill(int start, byte b, int count);

	/**
	 * Write the data read from the specified channel {@code in} to this
	 * {@code IBufferWriter}.
	 * 
	 * @param in
	 *            the channel to read
	 * @return the number of bytes read, possibly zero
	 * @throws IOException
	 *             if an I/O error occur
	 */
	public int readIn(ReadableByteChannel in) throws IOException;

	/**
	 * Receive a datagram from the specified channel {@code in} and write the
	 * data into this buffer.
	 * 
	 * @param in
	 *            the datagram channel to receive
	 * @return the datagram's source address, or {@code null} if the channel is
	 *         in non-blocking mode and no datagram was immediately available
	 * @throws IOException
	 *             if an I/O error occur
	 */
	public SocketAddress receive(DatagramChannel in) throws IOException;

	/**
	 * Return the number of bytes contained in the underlying buffer.
	 * 
	 * @return the number of bytes contained in the underlying buffer
	 */
	public int size();

	/**
	 * Return an {@code OutputStream} object that represents this
	 * {@code IBufferWriter}. Operations made on either one of them will be
	 * reflected in another one.
	 * 
	 * @return an {@code OutputStream} object that represents this
	 *         {@code IBufferWriter}
	 */
	public OutputStream getOutputStream();
}
