/**
 *
 * Copyright (C) 2012-2013 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.archinnov.achilles.type;

import java.util.LinkedHashMap;

public class TypedMap extends LinkedHashMap<String, Object> {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public <T> T getTyped(String key) {
		return (T) super.get(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T getTypedOr(String key, T defaultValue) {
		if (super.containsKey(key)) {
			return (T) super.get(key);
		} else {
			return defaultValue;
		}
	}
}
