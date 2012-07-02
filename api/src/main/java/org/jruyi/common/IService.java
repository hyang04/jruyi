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

import java.util.Map;

/**
 * Defines the life cycle methods that a service need implement.
 */
public interface IService {

	/**
	 * An {@code int} value representing Stopped state.
	 */
	public static final int STOPPED = 0x01;
	/**
	 * An {@code int} value representing Active state.
	 */
	public static final int ACTIVE = 0x02;
	/**
	 * An {@code int} value representing Starting state.
	 */
	public static final int STARTING = 0x04;
	/**
	 * An {@code int} value representing Stopping state.
	 */
	public static final int STOPPING = 0x08;

	/**
	 * Start this service.
	 * 
	 * @throws Exception
	 *             thrown if this service failed to start
	 */
	public void start() throws Exception;

	/**
	 * Start this service with the specified {@code options}.  It's up to the
	 * service implementation to define its own start options.
	 *
	 * @param options start options
	 * @throws Exception 
	 *             thrown if this service failed to start
	 */
	public void start(int options) throws Exception;

	/**
	 * Stop this service.
	 */
	public void stop();

	/**
	 * Stop this service with the specified {@code options}.  It's up to the
	 * service implementation to define its own stop options.
	 *
	 * @param options stop options
	 */
	public void stop(int options);

	/**
	 * Update this service's properties.
	 * 
	 * @param properties
	 *            the properties to be updated to
	 * @throws Exception
	 *             if any error occurs
	 */
	public void update(Map<String, ?> properties) throws Exception;

	/**
	 * Get the current state of this service.
	 * 
	 * @return the current state of this service
	 */
	public int state();
}
