/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: KeyStrokePair.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.tool.Client;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;

import javax.swing.KeyStroke;

/**
 * User: gainsley
 * Date: Apr 6, 2004
 * Time: 12:31:27 PM
 */
public class KeyStrokePair {

    /** prefix KeyStroke */                 private KeyStroke prefixStroke;
    /** primary KeyStroke */                private KeyStroke stroke;

    /** cache of defined KeyStrokePairs */  private static HashMap<KeyStrokePair,KeyStrokePair> cache = new HashMap<KeyStrokePair,KeyStrokePair>();
    /** separator for toString() */         private static final String sep = ", ";
    /** list of special keyStrokes */       private static int[] specialKeyStrokes = {
        KeyEvent.VK_AMPERSAND,

        KeyEvent.VK_INSERT,
        KeyEvent.VK_HOME,
        KeyEvent.VK_PAGE_UP,
        KeyEvent.VK_DELETE,
        KeyEvent.VK_END,
        KeyEvent.VK_PAGE_DOWN,

        KeyEvent.VK_MINUS,
        KeyEvent.VK_EQUALS,
        KeyEvent.VK_BACK_SPACE,

        KeyEvent.VK_OPEN_BRACKET,
        KeyEvent.VK_CLOSE_BRACKET,
        KeyEvent.VK_SLASH,
        KeyEvent.VK_BACK_SLASH,

        KeyEvent.VK_SEMICOLON,
        KeyEvent.VK_QUOTE,
        KeyEvent.VK_ENTER,

        KeyEvent.VK_PERIOD,
        KeyEvent.VK_COMMA,
        KeyEvent.VK_SLASH,
        '>',
        '<',

        KeyEvent.VK_BACK_QUOTE,

        KeyEvent.VK_LEFT,
        KeyEvent.VK_RIGHT,
        KeyEvent.VK_UP,
        KeyEvent.VK_DOWN
    };

    private KeyStrokePair() {}

    /**
     * Factory method to get a new KeyStrokePair.  KeyStrokePairs are unique,
     * therefore two objects of the same pair of key strokes will be the same object.
     * Which means == is the same as .equals().
     * @param prefixStroke the prefix stroke
     * @param stroke the primary stroke
     * @return a new KeyStrokePair
     */
    public static KeyStrokePair getKeyStrokePair(KeyStroke prefixStroke, KeyStroke stroke) {
        return getCachedKeyStrokePair(prefixStroke, stroke);
    }

    /**
     * Get a KeyStrokePair from a String representation.  The string consists
     * of two KeyStroke strings separated by a separator (currently ", ").
     * The KeyStroke strings are generated by keyStrokeToString.
     * @param keyString the string to be converted to a key stroke pair
     * @return a KeyStrokePair. May return null if invalid keyString
     */
    public static KeyStrokePair getKeyStrokePair(String keyString) {
        KeyStroke prefixStroke = null;
        KeyStroke stroke = null;
        // otherwise, split by separator
        String [] strokes = keyString.split(sep);
        if (strokes.length == 1) {
            // only one stroke
            stroke = stringToKeyStroke(strokes[0]);
            return getCachedKeyStrokePair(prefixStroke, stroke);
        }
        if (strokes.length == 2) {
            prefixStroke = stringToKeyStroke(strokes[0]);
            stroke = stringToKeyStroke(strokes[1]);
            return getCachedKeyStrokePair(prefixStroke, stroke);
        }
        return null;
    }



    // --------------------- String to Key and Back Conversions -------------------

    /**
     * Convert this KeyStrokePair to a string.
     * @return a string representing this KeyStrokePair.
     */
    public String toString() {
        if (stroke == null) return "";
        if (prefixStroke == null) return keyStrokeToString(stroke);
        return keyStrokeToString(prefixStroke)+ sep + keyStrokeToString(stroke);
    }

    public static String getStringFromKeyStroke(KeyStroke key)
    {
      String id = "";
        if (key.getKeyCode() == KeyEvent.VK_UNDEFINED ) // not recognized? like >
            id = String.valueOf(key.getKeyChar());
        else
            id = KeyEvent.getKeyText(key.getKeyCode());
        return id;
    }

    /**
     * Converts KeyStroke to String that can be parsed by
     * KeyStroke.getKeyStroke(String s).  For some reason the
     * KeyStroke class has a method to parse a KeyStroke String
     * identifier, but not one to make one.
     */
    public static String keyStrokeToString(KeyStroke key) {
        if (key == null) return "";
        String mods = KeyEvent.getKeyModifiersText(key.getModifiers());
        String id = getStringFromKeyStroke(key);
        // change key to lower case, unless the shift modifier was pressed
        //if ((key.getModifiers() & InputEvent.SHIFT_DOWN_MASK) == 0) id = id.toLowerCase();
        if (mods.equals("")) return id;

        mods = mods.replace('+', ' ');
        mods = mods.toLowerCase();
        return mods + " " + id;
    }

    public static KeyStroke stringToKeyStroke(String str) {
        // change NUMPAD-# to NUMPAD#
        str = str.replaceAll("NumPad\\-", "NUMPAD");

        // Hack: getKeyStroke does not understand mac's Command key.
        // Hack tested on Leopard
        if (str.length() > 0 && Client.isOSMac())
        {
            // Checking if the first character is a MaCOSX special one.
            char ctl = 8963; // ctl
            char command = 8984; // command
            char shift = 8679;
            boolean hasCtl = false, hasCmd = false, hasShift = false;

            for (int i = 0; i < str.length(); i++)
            {
                char c = str.charAt(i);
                if (!hasCtl && c == ctl)
                    hasCtl = true;
                if (!hasCmd && c == command)
                    hasCmd = true;
                if (!hasShift && c == shift)
                    hasShift = true;
                if (hasCtl && hasCmd && hasShift)
                    break;
            }
            if (hasCtl)
                str = str.replace(ctl, (char)32);
            if (hasCmd)
                str = str.replace(command, (char)32);
            if (hasShift)
                str = str.replace(shift, (char)32);
            str = str.trim();
            KeyStroke key = KeyStroke.getKeyStroke(str);

            if (key != null)
            {
                int val = key.getModifiers();
                if (hasCtl)
                    val |= InputEvent.CTRL_DOWN_MASK;
                if (hasShift)
                    val |= InputEvent.SHIFT_DOWN_MASK;
                if (hasCmd)
                    val |= InputEvent.META_DOWN_MASK;
                key = KeyStroke.getKeyStroke(key.getKeyCode(), val);
                return key;
            }
        }


        // Hack: getKeyStroke does not understand mac's Command key.
        // Hack tested on Tiger
        if (str.matches(".*?command.*")) {
            // must be a mac, get mac command key
            str = str.replaceAll("command", "");
            str = str.trim();
            KeyStroke key = KeyStroke.getKeyStroke(str);
            int command_mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
            if (key != null) {
                key = KeyStroke.getKeyStroke(key.getKeyCode(), key.getModifiers() | command_mask);
                return key;
            }
            return null;
        }
        KeyStroke key = KeyStroke.getKeyStroke(str);
        if (key == null) // Doesn't seem to handle properly special keyEvent
        {
            for (int i=0; i<specialKeyStrokes.length; i++)
            {
            	KeyStroke k = KeyStroke.getKeyStroke(specialKeyStrokes[i], 0);
                if (str.equals(keyStrokeToString(k))) { key = k;   break; }
            	k = KeyStroke.getKeyStroke(specialKeyStrokes[i], InputEvent.SHIFT_DOWN_MASK);
                if (str.equals(keyStrokeToString(k))) { key = k;   break; }
            	k = KeyStroke.getKeyStroke(specialKeyStrokes[i], InputEvent.CTRL_DOWN_MASK);
                if (str.equals(keyStrokeToString(k))) { key = k;   break; }
            	k = KeyStroke.getKeyStroke(specialKeyStrokes[i], InputEvent.META_DOWN_MASK);
                if (str.equals(keyStrokeToString(k))) { key = k;   break; }
            	k = KeyStroke.getKeyStroke(specialKeyStrokes[i], InputEvent.ALT_DOWN_MASK);
                if (str.equals(keyStrokeToString(k))) { key = k;   break; }
            }
        }
        return key;
    }

    // ----------------------------- Other methods --------------------------------

    public KeyStroke getPrefixStroke() { return prefixStroke; }

    public KeyStroke getStroke() { return stroke; }

    /**
     * Returns a numeric value for this object that is likely to be unique.
     * Uses hash codes of prefixStroke and stroke (which are derived from fields
     * of those KeyStrokes.  see KeyStroke.hashCode().
     * @return an int that represents this object
     */
    public int hashCode() {
        int prefixCode = (prefixStroke == null) ? 0 : prefixStroke.hashCode();
        int strokeCode = (stroke == null) ? 0 : stroke.hashCode();
        //System.out.println("Generated hash code: "+ (int)((prefixCode + 1) * strokeCode));
        return (prefixCode + 1) * strokeCode;
    }

    private static KeyStrokePair getCachedKeyStrokePair(KeyStroke prefixStroke, KeyStroke stroke) {
        if (prefixStroke == null && stroke == null) return null;        // don't allow null, null
        KeyStrokePair k = new KeyStrokePair();
        k.prefixStroke = prefixStroke;
        k.stroke = stroke;
        // this looks funny but works cause hashCode is constructed from object
        // contents, not address of object.
        if (!cache.containsKey(k)) {
            cache.put(k, k);
        } else {
            k = cache.get(k);
        }
        return k;
    }

}