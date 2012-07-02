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
package org.jruyi.io.channel;

import org.jruyi.timeoutadmin.ITimeoutNotifier;

public interface IChannelAdmin {

	public void onRegisterRequired(ISelectableChannel channel);

	public void onConnectRequired(ISelectableChannel channel);

	public void onReadRequired(ISelectableChannel channel);

	public void onWriteRequired(ISelectableChannel channel);

	public ITimeoutNotifier createTimeoutNotifier(ISelectableChannel channel);
}
