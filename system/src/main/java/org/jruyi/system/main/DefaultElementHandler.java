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
package org.jruyi.system.main;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Default base class for element handlers.
 * 
 * @see IElementHandler
 * @see XmlParser
 */
public class DefaultElementHandler implements IElementHandler {

	/**
	 * An empty implementation.
	 * 
	 * @param attributes
	 *            the attributes attached to the element handled by this
	 *            handler.
	 * @throws SAXException
	 *             any SAX exception, possibly wrapping another exception.
	 */
	@Override
	public void start(Attributes attributes) throws SAXException {
	}

	/**
	 * An empty implementation.
	 * 
	 * @throws SAXException
	 *             any SAX exception, possibly wrapping another exception.
	 */
	@Override
	public void setText(String text) throws SAXException {
	}

	/**
	 * An empty implementation.
	 * 
	 * @throws SAXException
	 *             any SAX exception, possibly wrapping another exception.
	 */
	@Override
	public void end() throws SAXException {
	}
}
