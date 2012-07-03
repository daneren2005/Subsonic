/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.androidapp.util;

import java.lang.ref.SoftReference;
import java.util.concurrent.TimeUnit;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class TimeLimitedCache<T> {

    private SoftReference<T> value;
    private final long ttlMillis;
    private long expires;

    public TimeLimitedCache(long ttl, TimeUnit timeUnit) {
        this.ttlMillis = TimeUnit.MILLISECONDS.convert(ttl, timeUnit);
    }

    public T get() {
        return System.currentTimeMillis() < expires ? value.get() : null;
    }

    public void set(T value) {
        set(value, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public void set(T value, long ttl, TimeUnit timeUnit) {
        this.value = new SoftReference<T>(value);
        expires = System.currentTimeMillis() + timeUnit.toMillis(ttl);
    }

    public void clear() {
        expires = 0L;
        value = null;
    }
}
