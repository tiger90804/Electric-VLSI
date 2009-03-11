/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Environment.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.database;

import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.TechFactory;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable class to represent Database environment
 */
public class Environment {
    public final Setting.RootGroup toolSettings;
    public final TechPool techPool;
    public final Map<Setting,Object> settingValues;

    public Environment(IdManager idManager) {
        toolSettings = new Setting.RootGroup();
        toolSettings.lock();
        techPool = idManager.getInitialTechPool();
        settingValues = Collections.emptyMap();
        check();
    }

    private Environment(Setting.RootGroup toolSettings, TechPool techPool, Map<Setting,Object> settingValues) {
        this.toolSettings = toolSettings;
        this.techPool = techPool;
        this.settingValues = settingValues;
        check();
    }

    /** Returns map from Setting to its value in this Snapshot */
    public Map<Setting,Object> getSettings() {
        return settingValues;
    }

    public void activate() {
        techPool.activate();
        Technology.updateCurrents(techPool);
        for (Map.Entry<Setting,Object> e: settingValues.entrySet()) {
            Setting setting = e.getKey();
            Object value = e.getValue();
            setting.set(value);
        }
    }

    public boolean isActive() {
        if (!techPool.isActive())
            return false;
        for (Map.Entry<Setting,Object> e: settingValues.entrySet()) {
            Setting setting = e.getKey();
            Object value = e.getValue();
            if (setting.getValue() != value)
                return false;
        }
        return true;
    }

    private Environment with(Setting.RootGroup toolSettings, TechPool techPool, Map<Setting,Object> settingValues) {
        if (this.techPool == techPool && this.toolSettings == toolSettings && this.settingValues.equals(settingValues))
            return this;
        Map<Setting,Object> newSettingValues = Collections.unmodifiableMap(new HashMap<Setting,Object>(settingValues));
        return new Environment(toolSettings, techPool, newSettingValues);
    }

    public Environment withToolSettings(Setting.RootGroup toolSettings) {
        HashMap<Setting,Object> newSettingValues = new HashMap<Setting,Object>(settingValues);
        for (Setting setting: this.toolSettings.getSettings())
            newSettingValues.remove(setting);
        for (Setting setting: toolSettings.getSettings())
            newSettingValues.put(setting, setting.getFactoryValue());
        return with(toolSettings, this.techPool, newSettingValues);
    }

    public Environment addTech(Technology tech) {
        if (techPool.getTech(tech.getId()) != null)
            throw new IllegalArgumentException();
        TechPool newTechPool = techPool.withTech(tech);
        HashMap<Setting,Object> newSettingValues = new HashMap<Setting,Object>(settingValues);
        for (Setting setting: tech.getProjectSettings().getSettings())
            newSettingValues.put(setting, setting.getFactoryValue());
        for (Map.Entry<TechFactory.Param,Object> e: tech.getCurrentState().paramValues.entrySet()) {
            TechFactory.Param param = e.getKey();
            newSettingValues.put(tech.getSetting(param), e.getValue());
        }
        return with(toolSettings, newTechPool, newSettingValues);
    }

    public Environment withSettingChanges(Setting.SettingChangeBatch changeBatch) {

        // Look for tech param changes
        Map<TechFactory.Param,Object> techParams = techPool.getTechParams();
        boolean changed = false;
        for (Map.Entry<TechFactory.Param,Object> e: techParams.entrySet()) {
            TechFactory.Param param = e.getKey();
            Object oldValue = e.getValue();
            String xmlPath = param.xmlPath;
            if (!changeBatch.changesForSettings.containsKey(xmlPath)) continue;
            Object newValue = changeBatch.changesForSettings.get(xmlPath);
            if (newValue == null)
                newValue = param.factoryValue;
            if (newValue.equals(oldValue)) continue;
            changed = true;
            techParams.put(param, newValue);
        }
        TechPool newTechPool = techPool.withTechParams(techParams);
        assert (newTechPool != techPool) == changed;

        // Gather by xmlPath
        HashMap<String,Object> valuesByXmlPath = new HashMap<String,Object>();
        for (Map.Entry<Setting,Object> e: settingValues.entrySet()) {
            Setting oldSetting = e.getKey();
            String xmlPath = oldSetting.getXmlPath();
            Object value = e.getValue();
            if (changeBatch.changesForSettings.containsKey(xmlPath))
                value = changeBatch.changesForSettings.get(xmlPath);
            valuesByXmlPath.put(xmlPath, value);
        }
        for (Map.Entry<TechFactory.Param,Object> e: newTechPool.getTechParams().entrySet()) {
            TechFactory.Param param = e.getKey();
            valuesByXmlPath.put(param.xmlPath, e.getValue());
        }

        // Prepare new Setting Values
        HashMap<Setting,Object> newSettingValues = new HashMap<Setting,Object>();
        for (Setting setting: toolSettings.getSettings()) {
            Object value = valuesByXmlPath.get(setting.getXmlPath());
            Object factoryValue = setting.getFactoryValue();
            if (value == null || value.getClass() != factoryValue.getClass() || value.equals(factoryValue))
                value = factoryValue;
            newSettingValues.put(setting, value);
        }
        for (Technology tech: techPool.values()) {
            for (Setting setting: tech.getProjectSettings().getSettings()) {
                Object value = valuesByXmlPath.get(setting.getXmlPath());
                Object factoryValue = setting.getFactoryValue();
                if (value == null || value.getClass() != factoryValue.getClass() || value.equals(factoryValue))
                    value = factoryValue;
                newSettingValues.put(setting, value);
            }
        }
        return with(toolSettings, newTechPool, newSettingValues);
    }

    public Environment deepClone() {
        TechPool newTechPool = techPool.deepClone();
        HashMap<String,Object> oldSettingsByXmlPath = new HashMap<String,Object>();
        for (Setting setting: toolSettings.getSettings())
            oldSettingsByXmlPath.put(setting.getXmlPath(), setting);
        for (Technology tech: techPool.values()) {
            for (Setting setting: tech.getProjectSettings().getSettings())
                oldSettingsByXmlPath.put(setting.getXmlPath(), setting);
        }
        HashMap<Setting,Object> newSettingValues = new HashMap<Setting,Object>();
        for (Setting setting: toolSettings.getSettings())
            newSettingValues.put(setting, settingValues.get(setting));
        for (Technology tech: techPool.values()) {
            for (Setting setting: tech.getProjectSettings().getSettings()) {
                Object oldSetting = oldSettingsByXmlPath.get(setting.getXmlPath());
                Object value = settingValues.get(oldSetting);
                Object factoryValue = setting.getFactoryValue();
                if (value == null || value.getClass() != factoryValue.getClass())
                    value = factoryValue;
                newSettingValues.put(setting, value);
            }
        }
        return new Environment(toolSettings, newTechPool, Collections.unmodifiableMap(newSettingValues));
    }

    /**
     * Writes this Environment to IdWriter
     * @param writer IdWriter
     * @param old old Environment
     * @throws java.io.IOException
     */
    public void writeDiff(IdWriter writer, Environment old) throws IOException {
        boolean changed = this != old;
        writer.writeBoolean(changed);
        if (!changed)
            return;
        boolean techPoolChanged = techPool != old.techPool;
        writer.writeBoolean(techPoolChanged);
        if (techPoolChanged)
            techPool.write(writer);
        boolean toolSettingsChanged = toolSettings != old.toolSettings;
        writer.writeBoolean(toolSettingsChanged);
        if (toolSettingsChanged)
            toolSettings.write(writer);
        for (Map.Entry<Setting,Object> e: settingValues.entrySet()) {
            Setting setting = e.getKey();
            Object value = e.getValue();
            Object oldValue = old.settingValues.get(setting);
            if (oldValue == null)
                oldValue = setting.getFactoryValue();
            if (value.equals(oldValue)) continue;
            writer.writeString(setting.getXmlPath());
            Variable.writeObject(writer, value);
        }
        writer.writeString("");
    }

    public static Environment readEnvironment(IdReader reader, Environment old) throws IOException {
        boolean changed = reader.readBoolean();
        if (!changed) return old;
        TechPool techPool = old.techPool;
        boolean techPoolChanged = reader.readBoolean();
        if (techPoolChanged)
            techPool = TechPool.read(reader);
        Setting.RootGroup toolSettings = old.toolSettings;
        boolean toolSettingsChanged = reader.readBoolean();
        if (toolSettingsChanged)
            toolSettings = Setting.read(reader);
        HashMap<String,Setting> settingsByXmlPath = new HashMap<String,Setting>();
        for (Setting setting: toolSettings.getSettings())
            settingsByXmlPath.put(setting.getXmlPath(), setting);
        for (Technology tech: techPool.values()) {
            for (Setting setting: tech.getProjectSettings().getSettings())
                settingsByXmlPath.put(setting.getXmlPath(), setting);
        }
        HashMap<Setting,Object> settingValues = new HashMap<Setting,Object>();
        for (Setting setting: settingsByXmlPath.values()) {
            Object value = old.settingValues.get(setting);
            if (value == null)
                value = setting.getFactoryValue();
            settingValues.put(setting, value);
        }
        for (;;) {
            String xmlPath = reader.readString();
            if (xmlPath.length() == 0) break;
            Object value = Variable.readObject(reader);
            Setting setting = settingsByXmlPath.get(xmlPath);
            settingValues.put(setting, value);
        }
        return new Environment(toolSettings, techPool, Collections.unmodifiableMap(settingValues));
    }

    public void check() {
        if (!toolSettings.isLocked())
            throw new IllegalArgumentException("Tool Settings are not locked");
        HashMap<String,Object> xmlPaths = new HashMap<String,Object>();
        checkSettings(toolSettings, settingValues, xmlPaths);
        for (Technology tech: techPool.values()) {
            Setting.RootGroup techSettings = tech.getProjectSettingsRoot();
            checkSettings(techSettings, settingValues, xmlPaths);
        }
        if (xmlPaths.size() != settingValues.size())
            throw new IllegalArgumentException("Setting count");
        for (Map.Entry<TechFactory.Param,Object> e: techPool.getTechParams().entrySet()) {
            TechFactory.Param param = e.getKey();
            Object value = xmlPaths.get(param.xmlPath);
            if (!value.equals(e.getValue()))
                throw new IllegalArgumentException("TechParam mismatch");
        }
    }

    private static void checkSettings(Setting.RootGroup settings, Map<Setting,Object> settingValues, HashMap<String,Object> xmlPaths) {
        for (Setting setting: settings.getSettings()) {
            String xmlPath = setting.getXmlPath();
            if (xmlPath.length() == 0)
                throw new IllegalArgumentException("Empty xmlPath");
            Object value = settingValues.get(setting);
            if (value.getClass() != setting.getFactoryValue().getClass())
                throw new IllegalArgumentException("Type mismatch " + setting);
            Object oldValue = xmlPaths.put(xmlPath, value);
            if (oldValue != null)
                throw new IllegalArgumentException("Dupilcate xmlPath " + xmlPath);
        }
    }
}
