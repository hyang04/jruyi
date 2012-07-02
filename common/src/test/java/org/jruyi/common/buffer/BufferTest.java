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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jruyi.common.ByteKmp;
import org.jruyi.common.IBuffer;
import org.jruyi.common.StringBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class BufferTest {

	private static final String UNIT_CAPACITY = "unitCapacity";
	private BufferFactory m_factory;
	private Map<String, Object> m_props;

	@DataProvider(name = "bytes")
	public Object[][] createBytes() {
		Random random = new Random();
		int n = random.nextInt(155) + 100;
		byte[] bytes = new byte[n];
		for (int i = 0; i < n; ++i)
			bytes[i] = (byte) i;
		return new Object[][] { new Object[] { bytes } };
	}

	@BeforeClass
	public void setUp() {
		System.out.println("Testing Buffer...");
		m_factory = new BufferFactory();
		m_props = new HashMap<String, Object>();
		m_props.put(UNIT_CAPACITY, 8192);
		m_factory.modified(m_props);
	}

	@AfterClass
	public void tearDown() {
	}

	@Test(dataProvider = "bytes")
	public void test_writeReadBytes(byte[] bytes) {
		StringBuilder builder = StringBuilder.get();
		String hexDump1 = null;
		String hexDump2 = null;
		try {
			builder.appendHexDump(bytes);

			hexDump1 = builder.toString();
			builder.setLength(0);

			for (int i = 1; i < bytes.length + 11; i += 10) {
				BufferFactory factory = initializeFactory(i);
				IBuffer buffer = factory.create();
				buffer.writeBytes(bytes);

				Assert.assertEquals(buffer.position(), 0);
				Assert.assertEquals(buffer.size(), bytes.length);

				buffer.dump(builder);
				hexDump2 = builder.toString();

				builder.setLength(0);

				Assert.assertEquals(hexDump2, hexDump1);

				byte[] bytes2 = buffer.readBytes(bytes.length);

				Assert.assertEquals(bytes2, bytes);
				Assert.assertEquals(buffer.remaining(), 0);
			}
		} finally {
			builder.close();
		}
	}

	@Test(dataProvider = "bytes")
	public void test_writeReadInt(byte[] bytes) {
		int v = new Random().nextInt();
		int t = 0x12345678;
		byte[] r1 = { 0x12, 0x34, 0x56, 0x78 };
		byte[] r2 = { 0x78, 0x56, 0x34, 0x12 };
		for (int i = bytes.length / 2 + 1; i < bytes.length + 10; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.writeBytes(bytes);

			buffer.writeIntB(v);
			buffer.writeIntL(v);

			Assert.assertEquals(buffer.size(), bytes.length + 8);

			int n = bytes.length;
			buffer.skip(n);

			int read = buffer.readIntB();
			Assert.assertEquals(read, v);
			read = buffer.getIntB(n);
			Assert.assertEquals(read, v);

			read = buffer.readIntL();
			Assert.assertEquals(read, v);
			read = buffer.getIntL(n + 4);
			Assert.assertEquals(read, v);

			Assert.assertEquals(buffer.remaining(), 0);

			buffer.writeIntB(t);
			byte[] result = buffer.readBytes();
			if (!Arrays.equals(result, r1)) {
				StringBuilder builder = StringBuilder.get();
				System.out.println(builder.appendHexDump(result));
				builder.close();
				buffer.getBytes(n + 8);
			}
			Assert.assertEquals(result, r1);

			buffer.writeIntL(t);
			Assert.assertEquals(buffer.readBytes(), r2);
		}
	}

	@Test(dataProvider = "bytes")
	public void test_writeReadShort(byte[] bytes) {
		short v = (short) new Random().nextInt();
		short t = 0x1234;
		byte[] r1 = { 0x12, 0x34 };
		byte[] r2 = { 0x34, 0x12 };
		for (int i = bytes.length / 2 + 1; i < bytes.length + 9; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.writeBytes(bytes);

			buffer.writeShortB(v);
			buffer.writeShortL(v);
			buffer.writeShortB(v);
			buffer.writeShortL(v);

			Assert.assertEquals(buffer.size(), bytes.length + 8);

			int n = bytes.length;
			buffer.skip(n);

			short read = buffer.readShortB();
			Assert.assertEquals(read, v);
			read = buffer.getShortB(n);
			Assert.assertEquals(read, v);

			read = buffer.readShortL();
			Assert.assertEquals(read, v);
			read = buffer.getShortL(n + 2);
			Assert.assertEquals(read, v);

			int u = buffer.readUShortB();
			Assert.assertEquals(u, v & 0xFFFF);
			u = buffer.getUShortB(n + 4);
			Assert.assertEquals(u, v & 0xFFFF);

			u = buffer.readUShortL();
			Assert.assertEquals(u, v & 0xFFFF);
			u = buffer.getUShortL(n + 6);
			Assert.assertEquals(u, v & 0xFFFF);

			Assert.assertEquals(buffer.remaining(), 0);

			buffer.writeShortB(t);
			Assert.assertEquals(buffer.readBytes(), r1);

			buffer.writeShortL(t);
			Assert.assertEquals(buffer.readBytes(), r2);
		}
	}

	@Test(dataProvider = "bytes")
	public void test_writeReadLong(byte[] bytes) {
		long v = new Random().nextLong();
		long t = 0x1234567890abcdefL;
		byte[] r1 = { 0x12, 0x34, 0x56, 0x78, (byte) 0x90, (byte) 0xab,
				(byte) 0xcd, (byte) 0xef };
		byte[] r2 = { (byte) 0xef, (byte) 0xcd, (byte) 0xab, (byte) 0x90, 0x78,
				0x56, 0x34, 0x12 };
		for (int i = bytes.length / 2 + 1; i < bytes.length + 17; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.writeBytes(bytes);

			buffer.writeLongB(v);
			buffer.writeLongL(v);

			Assert.assertEquals(buffer.size(), bytes.length + 16);

			int n = bytes.length;
			buffer.skip(n);

			long read = buffer.readLongB();
			Assert.assertEquals(read, v);
			read = buffer.getLongB(n);
			Assert.assertEquals(read, v);

			read = buffer.readLongL();
			Assert.assertEquals(read, v);
			read = buffer.getLongL(n + 8);
			Assert.assertEquals(read, v);

			Assert.assertEquals(buffer.remaining(), 0);

			buffer.writeLongB(t);
			Assert.assertEquals(buffer.readBytes(), r1);

			buffer.writeLongL(t);
			Assert.assertEquals(buffer.readBytes(), r2);
		}
	}

	@Test
	public void test_writeReadString() throws UnsupportedEncodingException {
		String testStr = "Test Buffer.readString/Buffer.writeString;对read和write的测试";
		byte[] bytes = testStr.getBytes("UTF-8");
		for (int i = 1; i < testStr.length() + 2; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.writeBytes(bytes);

			String result = buffer.getString(0, "UTF-8");
			Assert.assertEquals(result, testStr);

			IBuffer buffer2 = factory.create();
			buffer2.write(testStr, "UTF-8");
			String result2 = buffer2.getString(0, "UTF-8");
			Assert.assertEquals(result2, testStr);

			result = buffer.readString("UTF-8");
			Assert.assertEquals(result, testStr);

			result2 = buffer2.readString("UTF-8");
			Assert.assertEquals(result2, testStr);
		}
	}

	@Test(dataProvider = "bytes")
	public void test_indexOf(byte[] bytes) {
		Random random = new Random();
		int n = random.nextInt(bytes.length);
		int len = random.nextInt(bytes.length - n);
		if (len < 1)
			++len;

		byte[] target = new byte[len];
		for (int t = 0; t < len; ++t)
			target[t] = (byte) (n + t);

		byte[] zeroBytes = new byte[0];
		ByteKmp kmp = new ByteKmp(target);
		ByteKmp emptyKmp = new ByteKmp(zeroBytes);

		byte b = (byte) n;

		for (int i = 1; i < bytes.length + 2; ++i) {
			BufferFactory factory = initializeFactory(i);

			IBuffer buffer = factory.create();
			buffer.writeBytes(target);
			Assert.assertEquals(buffer.indexOf(bytes), -1);
			buffer.close();

			buffer = factory.create();
			buffer.writeBytes(bytes);

			Assert.assertEquals(buffer.indexOf((byte) bytes.length, 0), -1);

			n = b & 0xFF;
			for (int j = -1; j <= n; ++j) {
				Assert.assertEquals(buffer.indexOf(b, j), n);

				Assert.assertEquals(buffer.indexOf(zeroBytes, j), j < 0 ? 0 : j);
				Assert.assertEquals(buffer.indexOf(emptyKmp, j), j < 0 ? 0 : j);

				Assert.assertEquals(buffer.indexOf(target, j), n);
				Assert.assertEquals(buffer.indexOf(kmp, j), n);
			}

			Assert.assertEquals(buffer.indexOf(target, n), n);
			Assert.assertEquals(buffer.indexOf(kmp, n), n);

			n = bytes.length + 1;
			for (int j = (b & 0xFF) + 1; j <= n; ++j) {
				Assert.assertEquals(buffer.indexOf(b, j), -1);

				Assert.assertEquals(buffer.indexOf(zeroBytes, j),
						j > bytes.length ? bytes.length : j);
				Assert.assertEquals(buffer.indexOf(emptyKmp, j),
						j > bytes.length ? bytes.length : j);

				Assert.assertEquals(buffer.indexOf(target, j), -1);
				Assert.assertEquals(buffer.indexOf(kmp, j), -1);
			}
		}
	}

	@Test(dataProvider = "bytes")
	public void test_lastIndexOf(byte[] bytes) {
		Random random = new Random();
		int n = random.nextInt(bytes.length);
		int len = random.nextInt(bytes.length - n);
		if (len < 1)
			++len;
		byte[] target = new byte[len];
		for (int t = 0; t < len; ++t)
			target[t] = (byte) (n + t);

		ByteKmp kmp = new ByteKmp(target);

		byte[] zeroBytes = new byte[0];
		ByteKmp emptyKmp = new ByteKmp(zeroBytes);

		byte b = (byte) n;

		for (int i = 1; i < bytes.length + 2; ++i) {
			BufferFactory factory = initializeFactory(i);

			IBuffer buffer = factory.create();
			buffer.writeBytes(target);
			Assert.assertEquals(buffer.lastIndexOf(bytes), -1);
			buffer.close();

			buffer = factory.create();
			buffer.writeBytes(bytes);

			Assert.assertEquals(
					buffer.lastIndexOf((byte) bytes.length, buffer.size()), -1);

			n = b & 0xFF;
			for (int j = -1; j < n; ++j) {
				Assert.assertEquals(buffer.lastIndexOf(b, j), -1);

				Assert.assertEquals(buffer.lastIndexOf(zeroBytes, j),
						j < -1 ? 0 : j);
				Assert.assertEquals(buffer.lastIndexOf(emptyKmp, j), j < -1 ? 0
						: j);

				Assert.assertEquals(buffer.lastIndexOf(target, j), -1);
				Assert.assertEquals(buffer.lastIndexOf(kmp, j), -1);
			}

			for (int j = n; j <= bytes.length + 1; ++j) {
				Assert.assertEquals(buffer.lastIndexOf(b, j), n);

				Assert.assertEquals(buffer.lastIndexOf(zeroBytes, j),
						j > bytes.length ? bytes.length : j);
				Assert.assertEquals(buffer.lastIndexOf(emptyKmp, j),
						j > bytes.length ? bytes.length : j);

				Assert.assertEquals(buffer.lastIndexOf(target, j), n);
				Assert.assertEquals(buffer.lastIndexOf(kmp, j), n);
			}
		}
	}

	@Test(dataProvider = "bytes")
	public void test_startsWith(byte[] bytes) {
		Random random = new Random();
		int n = random.nextInt(bytes.length) + 1;
		byte[] target = new byte[n];
		System.arraycopy(bytes, 0, target, 0, n);
		byte[] target2 = new byte[n];
		System.arraycopy(target, 0, target2, 0, n - 1);
		target2[n - 1] = (byte) 255;

		byte[] zeroBytes = new byte[0];

		for (int i = 1; i < bytes.length + 2; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.writeBytes(bytes);

			Assert.assertTrue(buffer.startsWith(zeroBytes));
			Assert.assertTrue(buffer.startsWith(target));

			Assert.assertFalse(buffer.startsWith(target2));
		}
	}

	@Test(dataProvider = "bytes")
	public void test_endsWith(byte[] bytes) {
		Random random = new Random();
		int n = random.nextInt(bytes.length) + 1;
		byte[] target = new byte[n];
		System.arraycopy(bytes, bytes.length - n, target, 0, n);
		byte[] target2 = new byte[n];
		System.arraycopy(target, 1, target2, 1, n - 1);
		target2[0] = (byte) 255;

		byte[] zeroBytes = new byte[0];
		for (int i = 1; i < bytes.length + 2; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.writeBytes(bytes);

			Assert.assertTrue(buffer.endsWith(zeroBytes));
			Assert.assertTrue(buffer.endsWith(target));

			Assert.assertFalse(buffer.endsWith(target2));
		}
	}

	@Test(dataProvider = "bytes")
	public void test_drainTo(byte[] bytes) {
		Random random = new Random();
		int n = random.nextInt(bytes.length + 1) + 1;
		BufferFactory factory = initializeFactory(n);
		for (int i = 1; i < bytes.length; ++i) {
			IBuffer dst = factory.create();
			dst.writeBytes(bytes, 0, i);

			IBuffer src = factory.create();
			src.writeBytes(bytes, i, bytes.length - i);

			src.drainTo(dst);

			byte[] results = dst.readBytes();

			Assert.assertTrue(src.isEmpty());
			Assert.assertEquals(bytes, results);
		}
	}

	@Test(dataProvider = "bytes")
	public void test_compareTo(byte[] bytes) {
		for (int i = 1; i < bytes.length + 2; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer thisBuf = factory.create();
			IBuffer thatBuf = factory.create();
			Assert.assertEquals(thisBuf.compareTo(thatBuf), 0);

			thisBuf.writeBytes(bytes);
			thisBuf.readBytes();
			Assert.assertEquals(thisBuf.compareTo(thatBuf), 0);

			thatBuf.writeBytes(bytes);
			thatBuf.readBytes();
			Assert.assertEquals(thisBuf.compareTo(thatBuf), 0);

			thisBuf.rewind();
			thatBuf.rewind();
			Assert.assertEquals(thisBuf.compareTo(thatBuf), 0);

			thatBuf.writeIntB(i);
			Assert.assertEquals(thisBuf.compareTo(thatBuf), -1);
			Assert.assertEquals(thatBuf.compareTo(thisBuf), 1);
		}
	}

	private BufferFactory initializeFactory(int unitCapacity) {
		Map<String, Object> props = m_props;
		props.put(UNIT_CAPACITY, unitCapacity);
		BufferFactory factory = m_factory;
		factory.modified(props);
		return factory;
	}
}
