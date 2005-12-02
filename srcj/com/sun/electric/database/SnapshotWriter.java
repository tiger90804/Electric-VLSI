/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SnapshotWriter.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.prototype.NodeProtoId;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Tool;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Class to write trace of Snapshots to DataOutput byte sequence.
 */
public class SnapshotWriter {
    
    public DataOutputStream out;
    private HashMap<Variable.Key,Integer> varKeys = new HashMap<Variable.Key,Integer>();
    private HashMap<TextDescriptor,Integer> textDescriptors = new HashMap<TextDescriptor,Integer>();
    private HashMap<Tool,Integer> tools = new HashMap<Tool,Integer>();
    private HashMap<Technology,Integer> techs = new HashMap<Technology,Integer>();
    private HashMap<ArcProto,Integer> arcProtos = new HashMap<ArcProto,Integer>();
    private HashMap<PrimitiveNode,Integer> primNodes = new HashMap<PrimitiveNode,Integer>();
    private HashMap<Orientation,Integer> orients = new HashMap<Orientation,Integer>();
   
    /** Creates a new instance of SnapshotWriter */
    SnapshotWriter(DataOutputStream out) {
        this.out = out;
    }

    /**
     * Writes variable key.
     * @param key variable key to write.
     */
    public void writeVariableKey(Variable.Key key) throws IOException {
        Integer i = varKeys.get(key);
        if (i != null) {
            out.writeInt(i.intValue());
        } else {
            i = new Integer(varKeys.size());
            varKeys.put(key, i);
            out.writeInt(i.intValue());
            
            out.writeUTF((key.toString()));
        }
    }

    /**
     * Writes TextDescriptor.
     * @param td TextDescriptor to write.
     */
    public void writeTextDescriptor(TextDescriptor td) throws IOException {
        if (td == null) {
            out.writeInt(-1);
            return;
        }
        Integer i = textDescriptors.get(td);
        if (i != null) {
            out.writeInt(i.intValue());
        } else {
            i = new Integer(textDescriptors.size());
            textDescriptors.put(td, i);
            out.writeInt(i.intValue());
            
            out.writeLong(td.lowLevelGet());
            out.writeInt(td.getColorIndex());
            out.writeBoolean(td.isDisplay());
            out.writeBoolean(td.isJava());
            int face = td.getFace();
            String fontName = face != 0 ? TextDescriptor.ActiveFont.findActiveFont(face).getName() : "";
            out.writeUTF(fontName);
        }
    }
    
    /**
     * Writes Tool.
     * @param tool Tool to write.
     */
    public void writeTool(Tool tool) throws IOException {
        Integer i = tools.get(tool);
        if (i != null) {
            out.writeInt(i.intValue());
        } else {
            i = new Integer(tools.size());
            tools.put(tool, i);
            out.writeInt(i.intValue());
            out.writeUTF(tool.getName());
        }
    }
    
    /**
     * Writes Technology.
     * @param tech Technology to write.
     */
    public void writeTechnology(Technology tech) throws IOException {
        Integer i = techs.get(tech);
        if (i != null) {
            out.writeInt(i.intValue());
        } else {
            i = new Integer(techs.size());
            techs.put(tech, i);
            out.writeInt(i.intValue());
            out.writeUTF(tech.getTechName());
        }
    }
    
    /**
     * Writes ArcProto.
     * @param ap ArcProto to write.
     */
    public void writeArcProto(ArcProto ap) throws IOException {
        Integer i = arcProtos.get(ap);
        if (i != null) {
            out.writeInt(i.intValue());
        } else {
            i = new Integer(arcProtos.size());
            arcProtos.put(ap, i);
            out.writeInt(i.intValue());
            writeTechnology(ap.getTechnology());
            out.writeUTF(ap.getName());
        }
    }

    /**
     * Writes LibId.
     * @param libId LibId to write.
     */
    public void writeLibId(LibId libId) throws IOException {
        out.writeInt(libId.libIndex);
    }
    
    /**
     * Writes NodeProtoId.
     * @param nodeProtoId NodeProtoId to write.
     */
    public void writeNodeProtoId(NodeProtoId nodeProtoId) throws IOException {
        if (nodeProtoId instanceof CellId) {
            out.writeInt(((CellId)nodeProtoId).cellIndex);
            return;
        }
        PrimitiveNode pn = (PrimitiveNode)nodeProtoId;
        Integer i = primNodes.get(pn);
        if (i != null) {
            out.writeInt(i.intValue());
        } else {
            i = new Integer(~primNodes.size());
            primNodes.put(pn, i);
            out.writeInt(i.intValue());
            writeTechnology(pn.getTechnology());
            out.writeUTF(pn.getName());
        }
    }
    
    /**
     * Writes PortProtoId.
     * @param portProtoId PortProtoId to write.
     */
    public void writePortProtoId(PortProtoId portProtoId) throws IOException {
        writeNodeProtoId(portProtoId.getParentId());
        out.writeInt(portProtoId.getChronIndex());
    }
    
    /**
     * Writes Name key.
     * @param nameKey name key to write.
     */
    public void writeNameKey(Name nameKey) throws IOException {
        out.writeUTF(nameKey.toString());
    }
    
    /**
     * Writes Orientation.
     * @param orient Orientation.
     */
    public void writeOrientation(Orientation orient) throws IOException {
        Integer i = orients.get(orient);
        if (i != null) {
            out.writeInt(i.intValue());
        } else {
            i = new Integer(orients.size());
            orients.put(orient, i);
            out.writeInt(i.intValue());
            
            out.writeShort(orient.getAngle());
            out.writeBoolean(orient.isXMirrored());
            out.writeBoolean(orient.isYMirrored());
        }
    }
    
    /**
     * Writes EPoint.
     * @param p EPoint.
     */
    public void writePoint(EPoint p) throws IOException {
        out.writeDouble(p.getX());
        out.writeDouble(p.getY());
    }
}
