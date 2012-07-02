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

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * A byte sequence is a readable sequence of {@code byte} values. This interface
 * provides uniform, read-only access to the byte sequence. It doesn't change
 * any properties of the underlying buffer, such as <i>position</i>, <i>size</i>
 * and so on.
 * 
 * <p>
 * All the methods suffixed with <i>B</i> operate the bytes in big-endian order.
 * All those suffixed with <i>L</i> operate the bytes in little-endian order.
 */
public interface IByteSequence {

	/**
	 * Return the {@code byte} value at the specified {@code index}.
	 * 
	 * @param index
	 *            the index of the byte to be got
	 * @return the {@code byte} value at the specified {@code index}
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or not smaller than
	 *             {@code length()}
	 */
	public byte byteAt(int index);

	/**
	 * Interpret the 4 bytes starting at the specified {@code start} into an
	 * {@code int} value in the big-endian byte order and return the resultant
	 * {@code int} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code int} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 4}
	 */
	public int getIntB(int start);

	/**
	 * Interpret the 4 bytes starting at the specified {@code start} into an
	 * {@code int} value in the little-endian byte order and return the
	 * resultant {@code int} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code int} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 4}
	 */
	public int getIntL(int start);

	/**
	 * Interpret the 8 bytes starting at the specified {@code start} into a
	 * {@code long} value in the big-endian byte order and return the resultant
	 * {@code long} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code long} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 8}
	 */
	public long getLongB(int start);

	/**
	 * Interpret the 8 bytes starting at the specified {@code start} into a
	 * {@code long} value in the little-endian byte order and return the
	 * resultant {@code long} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code long} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 8}
	 */
	public long getLongL(int start);

	/**
	 * Interpret the 2 bytes starting at the specified {@code start} into a
	 * {@code short} value in the big-endian byte order and return the resultant
	 * {@code short} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code short} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 2}
	 */
	public short getShortB(int start);

	/**
	 * Interpret the 2 bytes starting at the specified {@code start} into a
	 * {@code short} value in the little-endian byte order and return the
	 * resultant {@code short} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code short} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 2}
	 */
	public short getShortL(int start);

	/**
	 * Interpret the 4 bytes starting at the specified {@code start} into a
	 * {@code float} value in the big-endian byte order and return the resultant
	 * {@code float} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code float} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 4}
	 */
	public float getFloatB(int start);

	/**
	 * Interpret the 4 bytes starting at the specified {@code start} into a
	 * {@code float} value in the little-endian byte order and return the
	 * resultant {@code float} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code float} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 4}
	 */
	public float getFloatL(int start);

	/**
	 * Interpret the 8 bytes starting at the specified {@code start} into a
	 * {@code double} value in the big-endian byte order and return the
	 * resultant {@code double} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code double} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 8}
	 */
	public double getDoubleB(int start);

	/**
	 * Interpret the 8 bytes starting at the specified {@code start} into a
	 * {@code double} value in the little-endian byte order and return the
	 * resultant {@code double} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code double} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 8}
	 */
	public double getDoubleL(int start);

	/**
	 * Return the bytes starting from the specified {@code start} to the end of
	 * the byte sequence.
	 * 
	 * <p>
	 * If {@code start} is equal to {@code length()}, then a zero length byte
	 * array is returned.
	 * 
	 * <p>
	 * The method {@code getBytes(start)} behaves exactly the same way as
	 * 
	 * <pre>
	 * {@code getBytes(start, (length() - start))}
	 * </pre>
	 * 
	 * @param start
	 *            the index of the first byte to be got
	 * @return a byte array containing the bytes got as required
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or greater than {@code length()}
	 */
	public byte[] getBytes(int start);

	/**
	 * Return {@code length} bytes starting at the specified {@code start}. If
	 * {@code length} is zero, then a zero length byte array is returned.
	 * 
	 * @param start
	 *            the index of the first byte to be got
	 * @param length
	 *            the number of bytes to be got
	 * @return a byte array containing the bytes got as required
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative, or length is negative, or
	 *             {@code (start + length) > length()}
	 */
	public byte[] getBytes(int start, int length);

	/**
	 * Decode the sub-bytesquence starting from {@code start}(inclusive) to the
	 * end into a {@code String} using the platform's default charset, and
	 * return the resultant {@code String}. The length of the resultant
	 * {@code String} is a function of the charset, and hence may not be equal
	 * to {@code (length() - start)}.
	 * 
	 * <p>
	 * The returned {@code String} is constructed in the way below.
	 * 
	 * <pre>
	 * {@code new String(getBytes(start))}
	 * </pre>
	 * 
	 * @param start
	 *            the starting index of the sub-bytesquence to be decoded
	 * @return the resultant {@code String} as specified
	 * @throws IndexOutOfBoundsException
	 *             if {@code start < 0} or {@code start > length()}
	 */
	public String getString(int start);

	/**
	 * Decode the sub-bytesquence starting from {@code start}(inclusive) to the
	 * end into a {@code String} using the charset whose name is the given
	 * {@code charsetName}, and return the resultant {@code String}. The length
	 * of the resultant {@code String} is a function of the charset, and hence
	 * may not be equal to {@code (length() - start)}.
	 * 
	 * <p>
	 * The returned {@code String} is constructed in the way below.
	 * 
	 * <pre>
	 * {@code new String(getBytes(start), charsetName)}
	 * </pre>
	 * 
	 * @param start
	 *            the starting index of the sub-bytesquence to be decoded
	 * @param charsetName
	 *            the name of the charset to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws IllegalArgumentException
	 *             if the {@code charsetName} is null
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 * @throws IndexOutOfBoundsException
	 *             if {@code start < 0} or {@code start > length()}
	 */
	public String getString(int start, String charsetName);

	/**
	 * Decode the sub-bytesquence starting from {@code start}(inclusive) to the
	 * end into a {@code String} using the given {@code charset}, and return the
	 * resultant {@code String}. The length of the resultant {@code String} is a
	 * function of the charset, and hence may not be equal to
	 * {@code (length() - start)}.
	 * 
	 * @param start
	 *            the starting index of the sub-bytesquence to be decoded
	 * @param charset
	 *            the charset to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IndexOutOfBoundsException
	 *             if {@code start < 0} or {@code start > length()}
	 */
	public String getString(int start, Charset charset);

	/**
	 * Decode the sub-bytesquence starting from {@code start}(inclusive) to the
	 * end into a {@code String} using the given {@code charsetCodec}, and
	 * return the resultant {@code String}. The length of the resultant
	 * {@code String} is a function of the charset, and hence may not be equal
	 * to {@code (length() - start)}.
	 * 
	 * @param start
	 *            the starting index of the sub-bytesquence to be decoded
	 * @param charsetCodec
	 *            the charset codec to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IndexOutOfBoundsException
	 *             if {@code start < 0} or {@code start > length()}
	 */
	public String getString(int start, ICharsetCodec charsetCodec);

	/**
	 * Decode the sub-bytesquence starting from {@code start}(inclusive) to
	 * {@code (start + length)}(exclusive) into a {@code String} using the
	 * platform's default charset, and return the resultant {@code String}. The
	 * length of the resultant {@code String} is a function of the charset, and
	 * hence may not be equal to {@code length}.
	 * 
	 * @param start
	 *            the starting index of the sub-bytesequence to be decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @return the resultant {@code String} as specified
	 * @throws IndexOutOfBoundsException
	 *             if the {@code start} and {@code length} arguments index
	 *             characters outside this byte sequence
	 */
	public String getString(int start, int length);

	/**
	 * Decode the sub-bytesequence starting from {@code start}(inclusive) to
	 * {@code (start + length)}(exclusive) into a {@code String} using the
	 * charset whose name is the given {@code charsetName}, and return the
	 * resultant {@code String}. The length of the resultant {@code String} is a
	 * function of the charset, and hence may not be equal to {@code length}.
	 * 
	 * @param start
	 *            the starting index of the sub-bytesequence to be decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @param charsetName
	 *            the name of the charset to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws IllegalArgumentException
	 *             if the {@code charsetName} is null
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 * @throws IndexOutOfBoundsException
	 *             if the {@code start} and {@code length} arguments index
	 *             characters outside this byte sequence
	 */
	public String getString(int start, int length, String charsetName);

	/**
	 * Decode the sub-bytesequence starting from {@code start}(inclusive) to
	 * {@code (start + length)}(exclusive) into a {@code String} using the given
	 * {@code charset}, and return the resultant {@code String}. The length of
	 * the resultant {@code String} is a function of the charset, and hence may
	 * not be equal to {@code length}.
	 * 
	 * @param start
	 *            the starting index of the sub-bytesequence to be decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @param charset
	 *            the charset to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IndexOutOfBoundsException
	 *             if the {@code start} and {@code length} arguments index
	 *             characters outside this byte sequence
	 */
	public String getString(int start, int length, Charset charset);

	/**
	 * Decode the sub-bytesequence starting from {@code start}(inclusive) to
	 * {@code (start + length)}(exclusive) into a {@code String} using the given
	 * {@code charsetCodec}, and return the resultant {@code String}. The length
	 * of the resultant {@code String} is a function of the charset, and hence
	 * may not be equal to {@code length}.
	 * 
	 * @param start
	 *            the starting index of the sub-bytesequence to be decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @param charsetCodec
	 *            the charset codec to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IndexOutOfBoundsException
	 *             if the {@code start} and {@code length} arguments index
	 *             characters outside this byte sequence
	 */
	public String getString(int start, int length, ICharsetCodec charsetCodec);

	/**
	 * Zero-extend the {@code byte} value at the specified index to type
	 * {@code int} and return the result, which is therefore in the range
	 * {@code 0} through {@code 255}.
	 * 
	 * @param index
	 *            the index of the byte value
	 * @return the byte at the specified index, interpreted as an unsigned 8-bit
	 *         value
	 * @throws IndexOutOfBoundsException
	 *             if {@code index < 0} or {@code index >= length()}
	 */
	public int getUByte(int index);

	/**
	 * Interpret the 2 bytes starting from the specified {@code start} index as
	 * an {@code int} value in the big-endian byte order, in the range {@code 0}
	 * through {@code 65535} and returns.
	 * 
	 * <p>
	 * Let {@code a} be the first byte and {@code b} be the second byte. The
	 * value returned is:
	 * 
	 * <pre>
	 * {@code ((a &amp; 0xff) &lt;&lt; 8) | (b &amp; 0xff)}
	 * </pre>
	 * 
	 * @param start
	 *            the starting index of the 2 bytes
	 * @return the 2 bytes starting from the specified {@code start} index,
	 *         interpreted as an unsigned 16-bit value in the big-endian byte
	 *         order
	 * @throws IndexOutOfBoundsException
	 *             if {@code start < 0} or {@code (start + 2) > length()}
	 */
	public int getUShortB(int start);

	/**
	 * Interpret the 2 bytes starting from the specified {@code start} index as
	 * an {@code int} value in the little-endian byte order, in the range
	 * {@code 0} through {@code 65535} and returns.
	 * 
	 * <p>
	 * Let {@code a} be the first byte and {@code b} be the second byte. The
	 * value returned is:
	 * 
	 * <pre>
	 * {@code ((b &amp; 0xff) &lt;&lt; 8) | (a &amp; 0xff)}
	 * </pre>
	 * 
	 * @param start
	 *            the starting index of the 2 bytes
	 * @return the 2 bytes starting from the specified {@code start} index,
	 *         interpreted as an unsigned 16-bit value in the little-endian byte
	 *         order
	 * @throws IndexOutOfBoundsException
	 *             if {@code start < 0} or {@code (start + 2) > length()}
	 */
	public int getUShortL(int start);

	/**
	 * Return the index of the first occurrence of the specified byte {@code b}
	 * in this byte sequence. If no such byte occurs in this byte sequence, then
	 * {@code -1} is returned.
	 * 
	 * <p>
	 * This method behaves exactly as below.
	 * 
	 * <pre>
	 * indexOf(b, 0)
	 * </pre>
	 * 
	 * @param b
	 *            the byte whose index of the first occurrence is to be returned
	 * @return the index of the first occurrence of the given {@code b} in this
	 *         byte sequence, {@code -1} if the byte does not occur
	 */
	public int indexOf(byte b);

	/**
	 * Return the index of the first occurrence of the specified byte {@code b}
	 * in this byte sequence, starting search at the given {@code fromIndex}. If
	 * no such byte occurs after index {@code fromIndex}(inclusive) in this byte
	 * sequence, then {@code -1} is returned.
	 * 
	 * <p>
	 * There is no restriction on the value of {@code fromIndex}. If it is
	 * negative, it has the same effect as if it were {@code 0}. If it's greater
	 * than or equal to the length of this byte sequence, then {@code -1} is
	 * returned.
	 * 
	 * @param b
	 *            the byte whose index of the first occurrence is to be returned
	 * @param fromIndex
	 *            the index to start the search from
	 * @return the index of the first occurrence of the given {@code b} after
	 *         {@code fromIndex}(inclusive) in this byte sequence, {@code - 1}
	 *         if the byte does not occur
	 */
	public int indexOf(byte b, int fromIndex);

	/**
	 * Return the index of the first occurrence of the specified {@code bytes}
	 * in this byte sequence. If no such byte array occurs, then {@code -1} is
	 * returned.
	 * 
	 * <p>
	 * This method behaves exactly as below.
	 * 
	 * <pre>
	 * indexOf(bytes, 0)
	 * </pre>
	 * 
	 * @param bytes
	 *            the target byte array
	 * @return the index of the first occurrence of the given {@code bytes} in
	 *         this byte sequence, {@code -1} if the target byte array does not
	 *         occur
	 */
	public int indexOf(byte[] bytes);

	/**
	 * Return the index of the first occurrence of the specified {@code bytes}
	 * in this byte sequence, starting search at the given {@code fromIndex}. If
	 * no such byte array occurs, then {@code -1} is returned.
	 * 
	 * <p>
	 * There is no restriction on the value of {@code fromIndex}. If it is
	 * negative, it has the same effect as if it were {@code 0}. If it's greater
	 * than or equal to the length of this byte sequence, then {@code -1} is
	 * returned.
	 * 
	 * @param bytes
	 *            the target byte array
	 * @return the index of the first occurrence of the given {@code bytes}
	 *         after {@code fromIndex}(inclusive) in this byte sequence,
	 *         {@code -1} if the target byte array does not occur
	 */
	public int indexOf(byte[] bytes, int fromIndex);

	/**
	 * Return the index within this sequence of the first occurrence of the
	 * specified {@code pattern}. If no such subsequence exists, then {@code -1}
	 * is returned.
	 * 
	 * @param pattern
	 *            the KMP pattern holding the subsequence for which to search
	 * @return if the given {@code pattern} occurs as a subsequence within this
	 *         sequence, then the index of the first character of the first such
	 *         subsequence is returned; if it does not occur as a subsequence,
	 *         {@code - 1} is returned
	 * @throws NullPointerException
	 *             if {@code pattern} is {@code null}
	 */
	public int indexOf(ByteKmp pattern);

	/**
	 * Return the index within this sequence of the first occurrence of the
	 * specified {@code pattern}, starting at the specified {@code fromIndex}.
	 * If no such subsequence exists, then {@code -1} is returned.
	 * 
	 * @param pattern
	 *            the KMP pattern holding the subsequence for which to search
	 * @param fromIndex
	 *            the index from which to start the search
	 * @return if the given {@code pattern} occurs as a subsequence within this
	 *         sequence, then the index of the first character of the first such
	 *         subsequence is returned; if it does not occur as a subsequence,
	 *         {@code - 1} is returned
	 * @throws NullPointerException
	 *             if {@code pattern} is {@code null}
	 */
	public int indexOf(ByteKmp pattern, int fromIndex);

	/**
	 * Return the index of the last occurrence of the specified byte {@code b}
	 * in this byte sequence, searching backward starting at
	 * {@code (length() - 1)}. If no such byte occurs, then {@code -1} is
	 * returned.
	 * 
	 * <p>
	 * This method behaves exactly as below.
	 * 
	 * <pre>
	 * lastIndexOf(b, length() - 1)
	 * </pre>
	 * 
	 * @param b
	 *            the byte whose index of the last occurrence is to be returned
	 * @return the index of the last occurrence of the given {@code b} in this
	 *         byte sequence, {@code -1} if the byte does not occur
	 */
	public int lastIndexOf(byte b);

	/**
	 * Return the index of the last occurrence of the specified byte {@code b}
	 * in this byte sequence, searching backward starting at the specified
	 * {@code fromIndex}. If no such byte occurs before index {@code fromIndex}
	 * (inclusive) in this byte sequence, then {@code -1} is returned.
	 * 
	 * <p>
	 * There is no restriction on the value of {@code fromIndex}. If it is
	 * greater than or equal to the length of this byte sequence, then the
	 * entire byte sequence would be searched. If it is negative, then it has
	 * the same effect as if it were {@code -1}: {@code -1} is returned.
	 * 
	 * @param b
	 *            the byte whose index of the last occurrence is to be returned
	 * @param fromIndex
	 *            the index to start the backward search from
	 * @return the index of the last occurrence of the given {@code b} before
	 *         {@code fromIndex}(inclusive) in this byte sequence, {@code -1} if
	 *         the byte does not occur
	 */
	public int lastIndexOf(byte b, int fromIndex);

	/**
	 * Return the index of the last occurrence of the specified byte array
	 * {@code bytes} in this byte sequence, searching backward starting at
	 * {@code (length() - bytes.length)}. If no such byte array occurs, then
	 * {@code -1} is returned.
	 * 
	 * @param bytes
	 *            the byte array whose index of the last occurrence is to be
	 *            returned
	 * @return the index of the last occurrence of the given {@code bytes} in
	 *         this byte sequence, {@code -1} if the given byte sequence does
	 *         not occur
	 */
	public int lastIndexOf(byte[] bytes);

	/**
	 * Return the index of the last occurrence of the specified byte array
	 * {@code bytes} in this byte sequence, searching backward starting at the
	 * specified {@code fromIndex}. If no such byte sequence occurs before index
	 * {@code fromIndex}(inclusive) in this byte sequence, then {@code -1} is
	 * returned.
	 * 
	 * @param bytes
	 *            the byte sequence whose index of the last occurrence is to be
	 *            returned
	 * @param fromIndex
	 *            the index to start the backward search from
	 * @return the index of the last occurrence of the given {@code bytes}
	 *         before {@code fromIndex}(inclusive) in this byte sequence,
	 *         {@code -1} if the given byte sequence does not occur
	 */
	public int lastIndexOf(byte[] bytes, int fromIndex);

	/**
	 * Return the index within this sequence of the rightmost occurrence of the
	 * specified subsequence by searching with the KMP algorithm.
	 * 
	 * @param pattern
	 *            the subsequence to search for
	 * @return if the given {@code pattern} occurs one or more times as a
	 *         subsequence within this object, then the index of the first
	 *         character of the last such subsequence is returned. If it does
	 *         not occur as a subsequence, {@code -1} is returned.
	 * @throws NullPointerException
	 *             if {@code pattern} is {@code null}
	 */
	public int lastIndexOf(ByteKmp pattern);

	/**
	 * Return the index of the last occurrence of the specified {@code pattern}
	 * in this byte sequence, searching backward starting at the specified
	 * {@code fromIndex} using the KMP algorithm. If no such byte sequence
	 * occurs in this byte sequence, then {@code -1} is returned.
	 * 
	 * @param pattern
	 *            the subsequence to search for
	 * @param fromIndex
	 *            the index to start the backward search from
	 * @return the index of the last occurrence of the given {@code pattern},
	 *         {@code -1} if the given subsequence does not occur
	 * @throws NullPointerException
	 *             if {@code pattern} is {@code null}
	 */
	public int lastIndexOf(ByteKmp pattern, int fromIndex);

	/**
	 * Test whether this byte sequence starts with the specified {@code bytes}.
	 * 
	 * @param bytes
	 *            the byte sequence to test
	 * @return true if this byte sequence starts with the specified
	 *         {@code bytes}, otherwise false
	 */
	public boolean startsWith(byte[] bytes);

	/**
	 * Test whether this byte sequence ends with the specified {@code bytes}.
	 * 
	 * @param bytes
	 *            the byte sequence to test
	 * @return true if this byte sequence ends with the specified {@code bytes},
	 *         otherwise false
	 */
	public boolean endsWith(byte[] bytes);

	/**
	 * Copy the requested sequence of bytes to the given {@code dst} starting at
	 * {@code dstBegin}.
	 * 
	 * @param srcBegin
	 *            start copying at this offset.
	 * @param srcEnd
	 *            stop copying at this offset.
	 * @param dst
	 *            the array to copy the data into.
	 * @param dstBegin
	 *            offset into {@code dst}.
	 * @throws NullPointerException
	 *             if {@code dst} is {@code null}.
	 * @throws IndexOutOfBoundsException
	 *             if any of the following is true:
	 *             <ul>
	 *             <li>{@code srcBegin} is negative
	 *             <li>{@code dstBegin} is negative
	 *             <li>the {@code srcBegin} argument is greater than the
	 *             {@code srcEnd} argument.
	 *             <li>{@code srcEnd} is greater than {@code this.length()}.
	 *             <li>{@code dstBegin+srcEnd-srcBegin} is greater than
	 *             {@code dst.length}
	 *             </ul>
	 */
	public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin);

	/**
	 * Return the length of this byte sequence. The length is the number of
	 * {@code byte}s in the sequence.
	 * 
	 * @return the number of {@code byte}s in the sequence
	 */
	public int length();
}
