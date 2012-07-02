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

import java.util.Map;

import org.jruyi.common.IBuffer;
import org.jruyi.common.IBufferFactory;
import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.ThreadLocalCache;
import org.jruyi.common.buffer.Buffer.Unit;

public final class BufferFactory implements IBufferFactory {

	private static final int MIN_UNIT_CAPACITY = 8;
	private static final String UNIT_CAPACITY = "unitCapacity";
	private final IThreadLocalCache<Unit> m_unitCache = ThreadLocalCache
			.weakLinkedCache();
	private int m_unitCapacity;

	@Override
	public IBuffer create() {
		return Buffer.get(this);
	}

	protected void modified(Map<String, ?> properties) {
		int value = (Integer) properties.get(UNIT_CAPACITY);
		m_unitCapacity = value > MIN_UNIT_CAPACITY ? value : MIN_UNIT_CAPACITY;
	}

	protected void activate(Map<String, ?> properties) {
		modified(properties);
	}

	Unit getUnit() {
		Unit unit = m_unitCache.take();
		if (unit == null)
			unit = new Unit(m_unitCapacity);
		else {
			int capacity = m_unitCapacity;
			if (unit.capacity() < capacity)
				unit.setCapacity(capacity);

			unit.clear();
		}

		return unit;
	}

	Unit getUnit(int capacity) {
		if (capacity < m_unitCapacity)
			capacity = m_unitCapacity;

		Unit unit = m_unitCache.take();
		if (unit == null)
			unit = new Unit(capacity);
		else {
			if (unit.capacity() < capacity)
				unit.setCapacity(capacity);

			unit.clear();
		}

		return unit;
	}
	
	void putUnit(Unit unit) {
		m_unitCache.put(unit);
	}
}
