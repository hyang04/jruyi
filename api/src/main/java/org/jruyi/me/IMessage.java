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

import java.util.Map;

import org.jruyi.common.ICloseable;

/**
 * A message is a data carrier to be routed in the message queue.
 * <p>
 * The message properties are used for routing.
 */
public interface IMessage extends ICloseable {

	/**
	 * Return the message ID.
	 * 
	 * @return the message ID
	 */
	public long id();

	/**
	 * Return the sender ID of the message.
	 * 
	 * @return the sender ID
	 */
	public String from();

	/**
	 * Return the receiver ID of the message.
	 * 
	 * @return the receiver ID
	 */
	public String to();

	/**
	 * Set the receiver of the message.
	 * 
	 * @param to
	 *            the receiver ID
	 */
	public void to(String to);

	/**
	 * Set the receiver to be null to drop the message.
	 */
	public void toNull();

	/**
	 * Test whether the message is to be dropped
	 * 
	 * @return true if yes, otherwise false.
	 */
	public boolean isToNull();

	/**
	 * Return the value of the property with the specified {@code name}. This
	 * method returns {@code null} if the property is not found.
	 * 
	 * @param name
	 *            the name of the property
	 * @return the property value, or {@code null} if no such property
	 */
	public Object getProperty(String name);

	/**
	 * Set property {@code name} to {@code value}.
	 * 
	 * @param name
	 *            the name of the property
	 * @param value
	 *            the value of the property to be set
	 * @return the previous property value, or {@code null} if there was no such
	 *         property
	 */
	public Object putProperty(String name, Object value);

	/**
	 * Put all the mappings in the specified {@code properties} to this
	 * message's properties.
	 * 
	 * @param properties
	 *            a map containing the properties to be put
	 */
	public void putProperties(Map<String, ?> properties);

	/**
	 * Remove the property with the specified {@code name}.
	 * 
	 * @param name
	 *            the name of the property to be removed
	 * @return the value of the removed property, or {@code null} if no such
	 *         property
	 */
	public Object removeProperty(String name);

	/**
	 * Get all the message's properties.
	 * 
	 * @return a map containing all the message's properties
	 */
	public Map<String, ?> getProperties();

	/**
	 * Clear all the message properties.
	 */
	public void clearProperties();

	/**
	 * Deposit the specified {@code stuff} to this message with the specified
	 * {@code id} reference as the key.
	 * 
	 * @param id
	 *            the reference to which is used as the key
	 * @param stuff
	 *            the object to be deposited
	 * @return the previous object deposited with key {@code id}, or
	 *         {@code null} if there was no such deposition
	 */
	public Object deposit(Object id, Object stuff);

	/**
	 * Withdraw the deposited object with the key {@code id}.
	 * 
	 * @param id
	 *            the reference to which is used as the key
	 * @return the deposited object, or {@code null} if no such deposition
	 */
	public Object withdraw(Object id);

	/**
	 * Attach the specified {@code attachment} to this message.
	 * 
	 * @param attachment
	 *            the attachment which is normally the message data object.
	 * @return the previous attachment, or {@code null} if there was no
	 *         attachment.
	 */
	public Object attach(Object attachment);

	/**
	 * Get the current attachment.
	 * 
	 * @return the current attachment
	 */
	public Object attachment();

	/**
	 * Detach the current attachment off the message.
	 * 
	 * @return the attachment that is detached
	 */
	public Object detach();
}
