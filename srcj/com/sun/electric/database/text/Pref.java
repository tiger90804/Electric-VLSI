/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Pref.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.text;

import com.sun.electric.Main;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;

import com.sun.electric.tool.user.ActivityLogger;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXParseException;

/**
 * This class manages appearance options.
 * There are two types of options: <I>appearance</I> and <I>meaning</I>.
 * Appearance options affect the way that the design is presented to the user.
 * Examples are grid dot spacing, layer stipple patterns, etc.
 * Meaning options affect the way that a design is produced (for fabrication,
 * simulation, and other outputs).  Examples are CIF layer names, technology
 * options, etc.)
 * Meaning options are managed by Setting class now.
 * <P>
 * Some Prefs can be used in server Jobs, other are client only.
 * Server Prefs are created by special factory methods during Electric initialization.
 * They can't be created later.
 * <P>
 * All options are saved in a machine-specific way by the Java Preferences class.
 * In addition, "meaning" options are stored in libraries.  When the libraries
 * are read back into Electric, the stored meaning options are checked against
 * the current meaning options, the two are reconciled.
 * <P>
 * Where are these options stored?  It varies with each operating system.
 * <UL>
 * <LI><B>Windows:</B>
 *   In the registry.
 *   Look in: HKEY_CURRENT_USER / Software / JavaSoft / Prefs / com / sun / electric.
 * </LI>
 * <LI><B>UNIX/Linux:</B>
 *   In your home directory.
 *   Look in: ~/.java/.userPrefs/com/sun/electric
 * </LI>
 * <LI><B>Macintosh System 10:</B>
 *   In your home directory, under Library/Preferences.
 *   Look at: ~/Library/Preferences/com.sun.electric.plist
 * </LI>
 * </UL>
 */
public class Pref
{
    public static class Group {
        private final boolean isTechGroup;
        private boolean lockCreation;
        Preferences preferences;
        private final TreeMap<String,Pref> prefs = new TreeMap<String,Pref>();

        public Group(Preferences preferences, boolean isTechGroup) {
            this.preferences = preferences;
            this.isTechGroup = isTechGroup;
        }
        public String absolutePath() { return preferences.absolutePath(); }

        public Collection<Pref> getPrefs() {
            return Collections.unmodifiableCollection(prefs.values());
        }

        public void lockCreation() {
            lockCreation = true;
        }

        private void setCachedObjsFromPreferences() {
            for(Pref pref : prefs.values())
                pref.setCachedObjFromPreferences();
        }

        private void putValue(String key, Object value) {
            if (preferences == null) return;
            if (value instanceof Boolean) {
                preferences.putBoolean(key, ((Boolean)value).booleanValue());
            } else if (value instanceof Integer) {
                preferences.putInt(key, ((Integer)value).intValue());
            } else if (value instanceof Long) {
                preferences.putLong(key, ((Long)value).longValue());
            } else if (value instanceof Double) {
                preferences.putDouble(key, ((Double)value).doubleValue());
            } else {
                assert value instanceof String;
                preferences.put(key, (String)value);
            }
            if (doFlushing)
                flushOptions(preferences);
            else
                queueForFlushing.add(preferences);
        }
        
        private Object getValue(String key, Object def) {
            if (preferences == null) return def;
            Object value = def;
            if (def instanceof Boolean) {
                boolean defV = ((Boolean)def).booleanValue();
                boolean v = preferences.getBoolean(key, defV);
                if (v != defV)
                    value = Boolean.valueOf(v);
            } else if (def instanceof Integer) {
                int defV = ((Integer)def).intValue();
                int v = preferences.getInt(key, defV);
                if (v != defV)
                    value = Integer.valueOf(v);
            } else if (def instanceof Long) {
                long defV = ((Long)def).longValue();
                long v = preferences.getLong(key, defV);
                if (v != defV)
                    value = Long.valueOf(v);
            } else if (def instanceof Double) {
                double defV = ((Double)def).doubleValue();
                double v = preferences.getDouble(key, defV);
                if (v != defV)
                    value = Double.valueOf(v);
            } else {
                assert def instanceof String;
                value = preferences.get(key, (String)def);
            }
            return value;
        }
        
        private void remove(String key) {
            if (preferences == null) return;
            preferences.remove(key);
            if (doFlushing)
                flushOptions(preferences);
            else
                queueForFlushing.add(preferences);
        }
    }

    public static Group groupForPackage(Class classFromPackage) {
        return groupForPackage(classFromPackage, false);
    }

    public static Group groupForPackage(Class classFromPackage, boolean techGroup) {
        Preferences prefs = Preferences.userNodeForPackage(classFromPackage);
        if (techGroup)
            return new Group(prefs, true);
        synchronized(allGroups) {
            for (Group group: allGroups)
                if (group.preferences == prefs)
                    return group;
            Group newGroup = new Group(prefs, false);
            allGroups.add(newGroup);
            return newGroup;
        }
    }

	private   final String      name;
	private   final Group       group;
    private   final boolean     serverAccessible;
	private   Object            cachedObj;
	private   Object      factoryObj;

	private static final ArrayList<Group> allGroups = new ArrayList<Group>();
	private static boolean doFlushing = true;
	private static Set<Preferences> queueForFlushing;
    private static boolean lockCreation;
    private static final HashSet<Pref> reportedAccess = new HashSet<Pref>();

	/**
	 * The constructor for the Pref.
	 * @param name the name of this Pref.
	 */
	protected Pref(Group group, String name, boolean serverAccessible, Object factoryObj) {
        if (group.lockCreation)
            throw new IllegalStateException("Pref "+group.absolutePath()+"/"+name+" was created from improper place");
        this.name = name;
        this.group = group;
        this.serverAccessible = serverAccessible;
        cachedObj = this.factoryObj = factoryObj;
        synchronized (group.prefs) {
            assert !group.prefs.containsKey(name);
            group.prefs.put(name, this);
        }
        setCachedObjFromPreferences();
//        if (lockCreation && serverAccessible && Job.getDebug()) {
//            try {
//                throw new IllegalStateException("Pref "+group.absolutePath()+"/"+name+" was created from improper place");
//            } catch (IllegalStateException e) {
//                ActivityLogger.logException(e);
//            }
//        }
    }

    /**
     * Currently Setting can be created only at initialization phase.
     * This method forbids further cration of Settings.
     */
    public static void lockCreation() {
        lockCreation = true;
    }

    /**
     * Method used in regressions so it has to be public.
     * @param fileName
     */
    public static void importPrefs(String fileName, EDatabase database)
    {
        if (fileName == null) return;

        // import preferences
        importPrefs(TextUtils.makeURLToFile(fileName), database);
    }

    public static void importPrefs(URL fileURL, EDatabase database)
    {
        if (fileURL == null) return;

        // import preferences
        try
		{
            URLConnection urlCon = fileURL.openConnection();
			InputStream inputStream = urlCon.getInputStream();
			System.out.println("Importing preferences...");

			// reset all preferences to factory values
            try {
                clearPrefs(Preferences.userNodeForPackage(Main.class));
    		} catch (BackingStoreException e) {
            	System.out.println("Error resetting Electric preferences");
                e.printStackTrace();
    		}

			// import preferences
			Preferences.importPreferences(inputStream);
			inputStream.close();

			// recache all prefs
			delayPrefFlushing();
            synchronized (allGroups) {
                for (Group group: allGroups)
                    group.setCachedObjsFromPreferences();
                for (Technology tech: database.getTechnologies()) {
                    for (Group group: tech.getTechnologyAllPreferences())
                        group.setCachedObjsFromPreferences();
                }
                database.getEnvironment().saveToPreferences();
            }
			resumePrefFlushing();

			// recache technology color information
            Technology.cacheTransparentLayerColors();
		} catch (InvalidPreferencesFormatException e)
		{
			String message = "Invalid preferences format";
			if (e.getCause() instanceof SAXParseException)
			{
				SAXParseException se = (SAXParseException)e.getCause();
				message += " (line " + se.getLineNumber() + ")";
			}
			System.out.println(message + ": "+e.getMessage());
			return;
		} catch (IOException e)
		{
			System.out.println("Error reading preferences file");
            e.printStackTrace();
			return;
		}

        Job.getExtendedUserInterface().restoreSavedBindings(false);
        Job.getUserInterface().repaintAllWindows();
        System.out.println("...preferences imported from " + fileURL.getFile());
	}

    private static void clearPrefs(Preferences topNode) throws BackingStoreException {
        topNode.clear();
        for (String child: topNode.childrenNames())
            clearPrefs(topNode.node(child));
    }

	/**
	 * Method to export the preferences to an XML file. This function is public due to the regressions.
	 * @param fileName the file to write.
	 */
    public static void exportPrefs(String fileName)
    {
        if (fileName == null) return;

        // save preferences there
        try
		{
        	// dump the preferences as a giant XML string (unformatted)
			Preferences root = Preferences.userNodeForPackage(Main.class);
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			root.exportSubtree(bs);
			String xmlDump = bs.toString();

			// remove the DTD statement (causes trouble)
			int sunPos = xmlDump.indexOf("java.sun.com");
			String insertDTD = "";
			if (sunPos >= 0)
			{
				int openPos = xmlDump.lastIndexOf('<', sunPos);
				int closePos = xmlDump.indexOf('>', sunPos);
				if (openPos >= 0 && closePos >= 0)
				{
					insertDTD = xmlDump.substring(openPos, closePos+1);
					xmlDump = xmlDump.substring(0, openPos) + xmlDump.substring(closePos+1);
				}
			}

			// reformat the XML
			StreamSource source = new StreamSource(new StringReader(xmlDump));
	    	TransformerFactory factory = TransformerFactory.newInstance();
	    	factory.setAttribute("indent-number", new Integer(2));
	    	Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			StreamResult result = new StreamResult(new OutputStreamWriter(bos, "utf-8"));
			transformer.transform(source, result);

			// add the removed DTD line back into the XML
			String xmlFormatted = bos.toString();
			int closePos = xmlFormatted.indexOf('>');
			if (closePos >= 0)
				xmlFormatted = xmlFormatted.substring(0, closePos+1) + "\n" + insertDTD + xmlFormatted.substring(closePos+1);

			// save the XML to disk
			PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
			printWriter.print(xmlFormatted);
			printWriter.close();
		} catch (Exception e)
		{
            if (Job.getDebug())
                e.printStackTrace();
			System.out.println("Error exporting Preferences");
			return;
		}

		System.out.println("Preferences saved to " + fileName);
	}

	/**
	 * Factory methods to create a boolean Pref objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Pref makeBooleanPref(String name, Group group, boolean factory) {
		return new Pref(group, name, false, Boolean.valueOf(factory));
	}

	/**
	 * Factory methods to create an integer Pref objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeIntPref(String name, Group group, int factory) {
		return new Pref(group, name, false, Integer.valueOf(factory));
	}

	/**
	 * Factory methods to create a long Pref objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeLongPref(String name, Group group, long factory) {
		return new Pref(group, name, false, Long.valueOf(factory));
	}

	/**
	 * Factory methods to create a double Pref objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeDoublePref(String name, Group group, double factory) {
		return new Pref(group, name, false, Double.valueOf(factory));
	}

	/**
	 * Factory methods to create a string Pref objects.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeStringPref(String name, Group group, String factory) {
        if (factory == null)
            throw new NullPointerException();
		return new Pref(group, name, false, factory);
	}

	/**
	 * Factory methods to create a boolean Pref objects.
     * The Pref is accessible from server Jobs.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
    public static Pref makeBooleanServerPref(String name, Group group, boolean factory) {
		return new Pref(group, name, true, Boolean.valueOf(factory));
	}

	/**
	 * Factory methods to create an integer Pref objects.
     * The Pref is accessible from server Jobs.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeIntServerPref(String name, Group group, int factory) {
		return new Pref(group, name, true, Integer.valueOf(factory));
	}

	/**
	 * Factory methods to create a long Pref objects.
     * The Pref is accessible from server Jobs.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeLongServerPref(String name, Group group, long factory) {
		return new Pref(group, name, true, Long.valueOf(factory));
	}

	/**
	 * Factory methods to create a double Pref objects.
     * The Pref is accessible from server Jobs.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeDoubleServerPref(String name, Group group, double factory) {
		return new Pref(group, name, true, Double.valueOf(factory));
	}

	/**
	 * Factory methods to create a string Pref objects.
     * The Pref is accessible from server Jobs.
	 * @param name the name of this Pref.
	 * @param group group of preferences to which a new Pref belongs
	 * @param factory the "factory" default value (if nothing is stored).
	 */
	public static Pref makeStringServerPref(String name, Group group, String factory) {
        if (factory == null)
            throw new NullPointerException();
		return new Pref(group, name, true, factory);
	}

    /**
	 * Method to get the boolean value on this Pref object.
	 * The object must have been created as "boolean".
	 * @return the boolean value on this Pref object.
	 */
	public boolean getBoolean() { return ((Boolean)getValue()).booleanValue(); }

	/**
	 * Method to get the integer value on this Pref object.
	 * The object must have been created as "integer".
	 * @return the integer value on this Pref object.
	 */
	public int getInt() { return ((Integer)getValue()).intValue(); }

	/**
	 * Method to get the long value on this Pref object.
	 * The object must have been created as "long".
	 * @return the long value on this Pref object.
	 */
	public long getLong() { return ((Long)getValue()).longValue(); }

	/**
	 * Method to get the double value on this Pref object.
	 * The object must have been created as "double".
	 * @return the double value on this Pref object.
	 */
	public double getDouble() { return ((Double)getValue()).doubleValue(); }

	/**
	 * Method to get the string value on this Pref object.
	 * The object must have been created as "string".
	 * @return the string value on this Pref object.
	 */
	public String getString() { return (String)getValue(); }

	/**
	 * Method to get the factory-default value of this Pref object.
	 * @return the factory-default value of this Pref object.
	 */
	public Object getFactoryValue() { return factoryObj; }

	/**
	 * Method to get the factory-default boolean value of this Pref object.
	 * @return the factory-default boolean value of this Pref object.
	 */
	public boolean getBooleanFactoryValue() { return ((Boolean)factoryObj).booleanValue(); }

	/**
	 * Method to get the factory-default integer value of this Pref object.
	 * @return the factory-default integer value of this Pref object.
	 */
	public int getIntFactoryValue() { return ((Integer)factoryObj).intValue(); }

	/**
	 * Method to get the factory-default long value of this Pref object.
	 * @return the factory-default long value of this Pref object.
	 */
	public long getLongFactoryValue() { return ((Long)factoryObj).longValue(); }

	/**
	 * Method to get the factory-default double value of this Pref object.
	 * @return the factory-default double value of this Pref object.
	 */
	public double getDoubleFactoryValue() { return ((Double)factoryObj).doubleValue(); }

	/**
	 * Method to get the factory-default String value of this Pref object.
	 * @return the factory-default String value of this Pref object.
	 */
	public String getStringFactoryValue() { return (String)factoryObj; }

	/**
	 * Method to get the name of this Pref object.
	 * @return the name of this Pref object.
	 */
	public String getPrefName() { return name; }

   /**
     * Method to get the pref name of this Pref object.
     * @return the name of this Setting object.
     */
    public String getPrefPath() { return group.absolutePath() + "/" + name; }

	/**
	 * Method to get the value of this Pref object as an Object.
	 * The proper way to get the current value is to use one of the type-specific
	 * methods such as getInt(), getBoolean(), etc.
	 * @return the Object value of this Pref object.
	 */
	public Object getValue() {
        if (Job.getDebug() && !serverAccessible && lockCreation && Job.inServerThread() && !reportedAccess.contains(this)) {
            String msg = getPrefName() + " is accessed from " + Job.getRunningJob();
//            ActivityLogger.logMessage(msg);
//            System.out.println(msg);
            reportedAccess.add(this);
        }
        return cachedObj;
    }

    private void setValue(Object value) {
        assert value.getClass() == factoryObj.getClass();
        cachedObj = value.equals(factoryObj) ? factoryObj : value;
        group.putValue(name, cachedObj);
    }

    public void patchDoubleFactoryValue(double newFactoryValue) {
        if (newFactoryValue == getDoubleFactoryValue()) return;
        if (cachedObj == factoryObj) {
//            group.remove(name);
            cachedObj = factoryObj = Double.valueOf(newFactoryValue);
        } else {
            factoryObj = Double.valueOf(newFactoryValue);
        }
    }

    private void setCachedObjFromPreferences() {
        cachedObj = group.getValue(name, factoryObj);
    }

	public static class PrefChangeBatch implements Serializable
	{
        private HashMap<String,HashMap<String,Object>> changesForNodes = new HashMap<String,HashMap<String,Object>>();

        private void add(Pref pref, Object newValue) {
            String nodeName = pref.group.absolutePath();
            HashMap<String,Object> changesForTheNode = changesForNodes.get(nodeName);
            if (changesForTheNode == null) {
                changesForTheNode = new HashMap<String,Object>();
                changesForNodes.put(nodeName, changesForTheNode);
            }
            changesForTheNode.put(pref.name, newValue);
        }
	}

	/**
	 * Method to start accumulation of Pref changes.
	 * All changes to preferences after this call are gathered,
	 * and not actually implemented.
	 * Call "getPrefChanges()" to get the gathered changes, and call
	 * "implementPrefChanges()" to actually make the changes.
	 */
	public static void gatherPrefChanges()
	{
	}

	/**
	 * Method to get the accumulated Pref changes.
	 * In order to make preference changes on the server,
	 * it is necessary to gather them on the client, and send
	 * the changes to the server for actual change.
	 * This method runs on the client and gets a serializable
	 * object that can be sent to the server.
	 * @return a collection of changes to preferences that have
	 * been made since the call to "gatherPrefChanges()".
	 * Call "implementPrefChanges()" with the returned collection
	 * to actually make the changes.
	 */
	public static PrefChangeBatch getPrefChanges()
	{
		return null;
	}

	/**
	 * Method to make a collection of preference changes.
	 * In order to make preference changes on the server,
	 * it is necessary to gather them on the client, and send
	 * the changes to the server for actual change.
	 * This method runs on the server.
	 * @param obj the collection of preference changes.
	 */
	public static void implementPrefChanges(PrefChangeBatch obj)
	{
	}

    /**
	 * Method to delay the saving of preferences to disk.
	 * Since individual saving is time-consuming, batches of preference
	 * changes are wrapped with this, and "resumePrefFlushing()".
	 */
	public static void delayPrefFlushing()
	{
		doFlushing = false;
		queueForFlushing = new HashSet<Preferences>();
	}

	/**
	 * Method to resume the saving of preferences to disk.
	 * Since individual saving is time-consuming, batches of preference
	 * changes are wrapped with this, and "resumePrefFlushing()".
	 * Besides resuming saving, this method also saves anything queued
	 * while saving was delayed.
	 */
	public static void resumePrefFlushing()
	{
		doFlushing = true;
		for(Preferences p : queueForFlushing)
			flushOptions(p);
	}

	/**
	 * Method to immediately flush all Electric preferences to disk.
	 */
	public static void flushAll()
	{
        flushOptions(Preferences.userNodeForPackage(Main.class));
	}

	/**
	 * Method to set a new boolean value on this Pref object.
	 * @param v the new boolean value of this Pref object.
	 */
	public void setBoolean(boolean v)
	{
        checkModify();
		boolean cachedBool = ((Boolean)cachedObj).booleanValue();
		if (v != cachedBool)
			setValue(Boolean.valueOf(v));
	}

	/**
	 * Method to set a new integer value on this Pref object.
	 * @param v the new integer value of this Pref object.
	 */
	public void setInt(int v)
	{
        checkModify();
		int cachedInt = ((Integer)cachedObj).intValue();
		if (v != cachedInt)
			setValue(Integer.valueOf(v));
	}

	/**
	 * Method to set a new long value on this Pref object.
	 * @param v the new long value of this Pref object.
	 */
	public void setLong(long v)
	{
        checkModify();
		long cachedLong = ((Long)cachedObj).longValue();
		if (v != cachedLong)
			setValue(Long.valueOf(v));
	}

    /**
	 * Method to set a new double value on this Pref object.
	 * @param v the new double value of this Pref object.
	 */
	public void setDouble(double v)
	{
        checkModify();
		double cachedDouble = ((Double)cachedObj).doubleValue();

		if (v != cachedDouble)
			setValue(Double.valueOf(v));
	}

	/**
	 * Method to set a new string value on this Pref object.
	 * @param str the new string value of this Pref object.
	 */
	public void setString(String str)
	{
        checkModify();
		String cachedString = (String)cachedObj;
		if (!str.equals(cachedString))
			setValue(str);
	}

	/**
	 * Method to reset Pref value to factory default.
	 */
    public void factoryReset() {
        cachedObj = factoryObj;
        group.remove(name);
    }

    private void checkModify() {
        if (Job.getDebug() && lockCreation && Job.inServerThread()) {
            String msg = getPrefName() + " is modified in " + Job.getRunningJob();
//            ActivityLogger.logMessage(msg);
//            System.out.println(msg);
        }
    }

    private static int numStrings;
    private static int lenStrings;
    private static int numValueStrings = 0;
    private static int lenValueStrings = 0;

    public static void printAllPrefs(PrintStream out, EDatabase database) {
        numValueStrings = lenValueStrings = 0;
        TreeMap<String,Pref> sortedPrefs = new TreeMap<String,Pref>();
        synchronized (allGroups) {
            for (Group group: allGroups) {
                for (Pref pref: group.prefs.values())
                    sortedPrefs.put(pref.group.absolutePath() + "/" + pref.name, pref);
            }
        }
        Preferences rootNode = Preferences.userRoot().node("com/sun/electric");
        numStrings = lenStrings = 0;
        try {
            gatherPrefs(out, 0, rootNode, null);
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        out.println(lenStrings + " chars in " + numStrings + " strings");
        out.println(lenValueStrings + " chars in " + numValueStrings + " value strings");
        printAllSettings(out, database);
        out.println("ELECTRIC USER PREFERENCES");
        int  i = 0;
        for (Pref pref: sortedPrefs.values())
            out.println((i++) + pref.group.absolutePath() + " " + pref.name + " " + pref.cachedObj);
        for (Technology tech: database.getTechnologies()) {
            i = 0;
            for (Pref.Group group: tech.getTechnologyAllPreferences()) {
                for (Pref pref: group.prefs.values())
                    out.println((i++) + pref.group.absolutePath() + " " + tech + " " + pref.name + " " + pref.cachedObj);
            }
        }
    }

    private static void gatherPrefs(PrintStream out, int level, Preferences topNode, List<String> ks) throws BackingStoreException {
        for (int i = 0; i < level; i++) out.print("  ");
        String[] keys = topNode.keys();
        for (int i = 0; i < keys.length; i++) {
            numStrings++;
            lenStrings += keys.length;
            String value = topNode.get(keys[i], null);
            numValueStrings++;
            lenValueStrings += value.length();
        }
        out.println(topNode.name() + " " + keys.length);
        if (topNode.absolutePath().equals("/com/sun/electric/database/hierarchy")) return;
        String[] children = topNode.childrenNames();
        for (int i = 0; i < children.length; i++) {
            String childName = children[i];
            numStrings++;
            lenStrings += children[i].length();
            Preferences childNode = topNode.node(childName);
            gatherPrefs(out, level + 1, childNode, ks);
        }
    }

    private static void printAllSettings(PrintStream out, EDatabase database) {
        Map<Setting,Object> settings = database.getSettings();
        TreeMap<String,Setting> sortedSettings = new TreeMap<String,Setting>();
        for (Setting setting: settings.keySet())
            sortedSettings.put(setting.getXmlPath(), setting);
        out.println("PROJECT SETTINGS");
        int i = 0;
        for (Setting setting: sortedSettings.values())
            out.println((i++) + "\t" + setting.getXmlPath() + " " + settings.get(setting));
    }

    /****************************** private methods ******************************/

	/**
	 * Method to force all Preferences to be saved.
	 */
	private static void flushOptions(Preferences p)
	{
		try
		{
			p.flush();
		} catch (Exception e)
		{
            if (!Job.BATCHMODE) {
			    System.out.println("Failed to save preferences");
            }
		}
	}
}
