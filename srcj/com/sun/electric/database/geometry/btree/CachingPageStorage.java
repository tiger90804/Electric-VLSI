/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BTree.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.database.geometry.btree;

import java.io.*;
import java.util.*;

/**
 *  PageStorage with a cache.
 */
public abstract class CachingPageStorage extends PageStorage {

    public CachingPageStorage(int pageSize) {
        super(pageSize);
    }

    /**
     *  Creates space in the cache for pageid, but only actually reads
     *  the bytes if readBytes is true.  If the page was not already
     *  in the cache and readBytes is false, subsequent calls to
     *  setDirty()/flush() will overwrite data previously on the page.
     */
    public abstract CachedPage getPage(int pageid, boolean readBytes);

    /** A page which is currently in the cache. */
    public abstract class CachedPage {
        public abstract byte[] getBuf();
        public abstract int    getPageId();
        public abstract void touch();
        public abstract void setDirty();
        public abstract void flush();
        public abstract boolean isDirty();
    }
}