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

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

/**
 * A byte buffer.
 * 
 * @see IBufferReader
 * @see IBufferWriter
 */
public interface IBuffer extends IBufferReader, IBufferWriter,
		Comparable<IBuffer>, IDumpable, ICloseable {

	/**
	 * Test whether this buffer is empty.
	 * 
	 * @return {@code true} if the buffer is not empty, otherwise {@code false}
	 */
	public boolean isEmpty();

	/**
	 * Compact this buffer by dropping all the data before the current
	 * <i>position</i>.
	 * 
	 * <p>
	 * This buffer's position is set to zero. Its size is set to
	 * {@code remaining()}. And its mark is discarded if defined.
	 * 
	 * @return this buffer
	 */
	public IBuffer compact();

	/**
	 * Split this buffer into two pieces and return the first piece. This buffer
	 * holds the rest second piece.
	 * 
	 * @param size
	 *            number of bytes of the first piece
	 * @return the first piece
	 * @throws IllegalArgumentException
	 *             if {@code size} is negative or greater than {@link #length()}
	 */
	public IBuffer split(int size);

	/**
	 * Adjust the length of the buffer to the specified {@code newLength}. If
	 * {@code newLength} is greater than the current length, then the last 
	 * {@code newLength - length()} bytes will be filled with zeroes. If
	 * {@code newLength} is smaller than the current length, then the last
	 * {@code length() - newLength} bytes will be dropped.
	 * 
	 * <p>
	 * If the buffer's position is larger than {@code newLength}, then it is set
	 * to {@code newLength}. If the buffer's mark is larger than
	 * {@code newLength}, then it is discarded.
	 *
	 * @param newLength
	 *            the new length of the buffer to be adjusted to
	 * @return this buffer
	 * @throws IllegalArgumentException
	 *             if {@code newLength} is negative
	 */
	public IBuffer setLength(int newLength);

	/**
	 * Create a new empty buffer using the same buffer factory instance.
	 * 
	 * @return a new empty buffer
	 */
	public IBuffer newBuffer();

	/**
	 * Empty this buffer.
	 */
	public void drain();

	/**
	 * Write all the remaining data into the end of the given {@code writer}.
	 * Then empty this buffer.
	 * 
	 * @param writer
	 *            the {@code BufferWriter} to transfer the data into
	 * @throws NullPointerException
	 *             if {@code writer} is null
	 * @throws IllegalArgumentException
	 *             if {@code writer} is this buffer
	 */
	public void drainTo(IBufferWriter writer);

	/**
	 * Reserve the given {@code size} bytes in the head of the buffer for the
	 * efficiency of buffer using when calling <i>head</i> write methods. The
	 * actual effective reserved size is returned and may be smaller.
	 * 
	 * @param size
	 *            the number of bytes to be reserved in the head
	 * @return the actual effective size that has been reserved
	 */
	public int reserveHead(int size);

	/**
	 * Return the number of bytes currently reserved for head.
	 *
	 * @return the current number of bytes reserved for head
	 */
	public int headReserved();

	/**
	 * Attempt to decode the SSL/TLS network data in the specified {@code src}
	 * into plain text application data using the specified {@code engine}, and
	 * write resultant data into this buffer.
	 * 
	 * @param src
	 *            the buffer containing SSL/TLS network data
	 * @param engine
	 *            the {@code SSLEngine} to decode the SSL/TLS network data
	 * @return an {@code SSLEngineResult} describing the result of this
	 *         operation
	 * @throws SSLException
	 *             a problem was encountered while processing the data that
	 *             caused the SSLEngine to abort
	 * @throws IllegalStateException
	 *             if the client/server mode has not yet been set
	 */
	public SSLEngineResult unwrap(IBuffer src, SSLEngine engine)
			throws SSLException;

	/**
	 * Attempt to encode the plain text application data in this buffer into
	 * SSL/TLS network data using the specified {@code engine}, and write the
	 * resultant data into the specified buffer {@code dst}.
	 * 
	 * @param dst
	 *            the buffer to hold the resultant SSL/TLS network data
	 * @param engine
	 *            the {@code SSLEngine} to encode the plain text application
	 *            data
	 * @return an {@code SSLEngineResult} describing the result of this
	 *         operation
	 * @throws SSLException
	 *             a problem was encountered while processing the data that
	 *             caused the SSLEngine to abort
	 * @throws IllegalStateException
	 *             if the client/server mode has not yet been set
	 */
	public SSLEngineResult wrap(IBuffer dst, SSLEngine engine)
			throws SSLException;
}
