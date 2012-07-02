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
package org.jruyi.me;

import java.io.IOException;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * A {@code IRouteSet} consists of and administers all the {@code IRoute}s
 * having the same <i>from</i>. So {@code IRouteSet} itself is grouped by source
 * end point.
 */
public interface IRouteSet {

	/**
	 * Get the ID of the source end point.
	 * 
	 * @return the ID of the source end point
	 */
	public String getFrom();

	/**
	 * Return the route whose destination end point is the specified {@code to}.
	 * 
	 * @param to
	 *            the ID of the destination end point of the route need return
	 * @return the route {@literal [from -> to]}
	 */
	public IRoute getRoute(String to);

	/**
	 * Define a route {@literal [from -> to]} with the specified {@code filter}.
	 * And put this route into this route set.
	 * <p>
	 * If a message is produced by the <i>from</i> of this route set and its
	 * properties matches the specified {@code filter}, then it will be routed
	 * to the specified {@code to}.
	 * 
	 * @param to
	 *            the ID of the destination end point
	 * @param filter
	 *            the filter string to filter the messages
	 * @return the route object as described
	 * @throws IllegalArgumentException
	 *             if {@code to} is null or {@code to.trim()} is empty
	 * @throws InvalidSyntaxException
	 *             if the syntax of the filter string is not correct
	 */
	public IRoute setRoute(String to, String filter)
			throws InvalidSyntaxException;

	/**
	 * Define a route {@literal [from -> to]} with the specified {@code filter}.
	 * And put this route into this route set. If the route ever exists, the old
	 * filter will be overwritten.
	 * <p>
	 * If a message is produced by the <i>from</i> of this route set and its
	 * properties matches the specified {@code filter}, then it will be routed
	 * to the specified {@code to}.
	 * 
	 * @param to
	 *            the ID of the destination end point
	 * @param filter
	 *            the filter to filter the messages
	 * @return the route object as described
	 * @throws IllegalArgumentException
	 *             if {@code to} is null or {@code to.trim()} is empty
	 */
	public IRoute setRoute(String to, Filter filter);

	/**
	 * Define a route {@literal [from -> to]}. And put this route into the route
	 * set. If the route ever exists, the old filter will be removed.
	 * <p>
	 * Any message produced by the <i>from</i> of this route set will be routed
	 * to the specified {@code to}.
	 * 
	 * @param to
	 *            the ID of the destination end point
	 * @return the route object as described
	 * @throws IllegalArgumentException
	 *             if {@code to} is null or {@code to.trim()} is empty
	 */
	public IRoute setRoute(String to);

	/**
	 * Get all the routes in this route set.
	 * 
	 * @return all the routes in this route set
	 */
	public IRoute[] getRoutes();

	/**
	 * Remove the route {@literal [from -> to]}.
	 * 
	 * @param to
	 *            the ID of the destination end point
	 */
	public void removeRoute(String to);

	/**
	 * Clear all the routes in this route set.
	 */
	public void clear();

	/**
	 * Persist the current route set. So all the route definitions will be kept
	 * across application's life.
	 * 
	 * @throws IOException
	 *             If there's an IO error
	 */
	public void save() throws IOException;
}
