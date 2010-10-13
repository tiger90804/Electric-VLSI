/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Configuration.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.util.config;

/**
 * @author fschmidt
 *
 */
public abstract class Configuration {
    
    private static Configuration instance = null;
    
    protected Configuration() {      
    }
    
    public static void setInstance(Configuration config) {
        if (config == null)
            throw new NullPointerException();
//        if (instance != null)
//            throw new IllegalStateException();
        instance = config;
    }
    
    public static Object lookup(String name) {
        return getInstance().lookupImpl(name);
    } 
    
    private static Configuration getInstance() {
        if (instance == null) {
            instance = new ClasspathConfig();
        }
        return instance;
    }
    
    protected abstract Object lookupImpl(String name);
}
