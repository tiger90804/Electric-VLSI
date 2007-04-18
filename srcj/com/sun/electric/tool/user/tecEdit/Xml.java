/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Xml.java
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;

/**
 * This class generates Xml description from information in Info classes.
 */
public class Xml {
    private static final int INDENT_WIDTH = 4;
    
    private final PrintStream out;
    private final String techName;
    private final GeneralInfo gi;
    private final LayerInfo[] lList;
    private final NodeInfo[] nList;
    private final ArcInfo[] aList;
    private int indent;
    private boolean indentEmitted;
    
    /** Creates a new instance of Xml */
    private Xml(PrintStream out, String techName, GeneralInfo gi, LayerInfo[] lList, NodeInfo[] nList, ArcInfo[] aList) {
        this.out = out;
        this.techName = techName;
        this.gi = gi;
        this.lList = lList;
        this.nList = nList;
        this.aList = aList;
    }
    
    /**
     * Dump technology information to Java
     * @param fileName name of file to write
     * @param newTechName new technology name
     * @param gi general technology information
     * @param lList information about layers
     * @param nList information about primitive nodes
     * @param aList information about primitive arcs.
     */
    static void writeXml(String fileName, String newTechName, GeneralInfo gi, LayerInfo[] lList, NodeInfo[] nList, ArcInfo[] aList) {
        try {
            PrintStream buffWriter = new PrintStream(new FileOutputStream(fileName));
            writeXml(buffWriter, newTechName, gi, lList, nList, aList);
            buffWriter.close();
            System.out.println("Wrote " + fileName);
        } catch (IOException e) {
            System.out.println("Error creating " + fileName);
        }
    }
    
    private static void writeXml(PrintStream out, String newTechName, GeneralInfo gi, LayerInfo[] lList, NodeInfo[] nList, ArcInfo[] aList) {
        Xml xml = new Xml(out, newTechName, gi, lList, nList, aList);
        xml.writeTechnology();
    }
    
    private void writeTechnology() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
        
        header();
        
        pl("");
        out.println("<!--");
		pl(" *");
		pl(" * Electric(tm) VLSI Design System");
		pl(" *");
		pl(" * File: " + techName + ".xml");
		pl(" * " + techName + " technology description");
		pl(" * Generated automatically from a library");
		pl(" *");
		pl(" * Copyright (c) " + cal.get(Calendar.YEAR) + " Sun Microsystems and Static Free Software");
		pl(" *");
		pl(" * Electric(tm) is free software; you can redistribute it and/or modify");
		pl(" * it under the terms of the GNU General Public License as published by");
		pl(" * the Free Software Foundation; either version 2 of the License, or");
		pl(" * (at your option) any later version.");
		pl(" *");
		pl(" * Electric(tm) is distributed in the hope that it will be useful,");
		pl(" * but WITHOUT ANY WARRANTY; without even the implied warranty of");
		pl(" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
		pl(" * GNU General Public License for more details.");
		pl(" *");
		pl(" * You should have received a copy of the GNU General Public License");
		pl(" * along with Electric(tm); see the file COPYING.  If not, write to");
		pl(" * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,");
		pl(" * Boston, Mass 02111-1307, USA.");
		pl(" */");
        out.println("-->");
        l();
        
        b("technology"); a("name", techName); l();
        a("xmlns", "http://electric.sun.com/Technology"); l();
        a("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"); l();
        a("xsi:schemaLocation", "http://electric.sun.com/Technology ../../technology/Technology.xsd"); cl();
        l();
        
        if (gi.shortName != null) {
            b("shortName"); c(); p(gi.shortName); el("shortName");
        }
        
        bcpel("description", gi.description);
        b("numMetals"); a("min", gi.defaultNumMetals); a("max", gi.defaultNumMetals); a("default", gi.defaultNumMetals); el();
        b("scale"); a("value", gi.scale); a("relevant", gi.scaleRelevant); el();
        b("defaultFoundry"); a("value", gi.defaultFoundry); el();
        b("minResistance"); a("value", gi.minRes); el();
        b("minCapacitance"); a("value", gi.minCap); el();
//        printlnAttribute("  gateLengthSubtraction", gi.gateShrinkage);
//        printlnAttribute("  gateInclusion", gi.includeGateInResistance);
//        printlnAttribute("  groundNetInclusion", gi.includeGround);
        l();
        
        if (gi.transparentColors != null) {
            comment("Transparent layers");
            for (int i = 0; i < gi.transparentColors.length; i++) {
                Color color = gi.transparentColors[i];
                b("transparentLayer"); a("transparent", i + 1); cl();
                bcpel("r", color.getRed());
                bcpel("g", color.getGreen());
                bcpel("b", color.getBlue());
                el("transparentLayer");
            }
            l();
        }
        
        comment("**************************************** LAYERS ****************************************");
        for (LayerInfo li: lList) {
            writeXml(li);
            l();
        }
        
		comment("******************** ARCS ********************");
        for (ArcInfo ai: aList) {
            writeXml(ai);
            l();
        }
        
		comment("******************** NODES ********************");
        for (NodeInfo ni: nList) {
            writeXml(ni);
            l();
        }
        
        writeSpiceHeaderXml(1, gi.spiceLevel1Header);
        writeSpiceHeaderXml(2, gi.spiceLevel2Header);
        writeSpiceHeaderXml(3, gi.spiceLevel3Header);
        
        writeDesignRulesXml(gi.defaultFoundry, lList, gi.conDist, gi.unConDist);
        
        el("technology");
     }
    
    private void writeXml(LayerInfo li) {
        EGraphics desc = li.desc;
        String funString = null;
        if (li.funExtra != 0) {
            int funExtra = li.funExtra;
            if ((funExtra&Layer.Function.DEPLETION) != 0 || (funExtra&Layer.Function.ENHANCEMENT) != 0) {
                funString = Layer.Function.getExtraConstantName(funExtra&(Layer.Function.DEPLETION|Layer.Function.ENHANCEMENT));
                funExtra &= ~(Layer.Function.DEPLETION|Layer.Function.ENHANCEMENT);
                if (funExtra != 0)
                    funString += "_" + Layer.Function.getExtraConstantName(funExtra);
            } else {
                funString = Layer.Function.getExtraConstantName(funExtra);
            }
        }
        b("layer"); a("name", li.name); a("fun", li.fun.name()); a("extraFun", funString); cl();
        
        if (desc.getTransparentLayer() > 0) {
            b("transparentColor"); a("transparent", desc.getTransparentLayer()); el();
        } else {
            Color color = desc.getColor();
            b("opaqueColor"); a("r", color.getRed()); a("g", color.getGreen()); a("b", color.getBlue()); el();
        }
        
        bcpel("patternedOnDisplay", desc.isPatternedOnDisplay());
        bcpel("patternedOnPrinter", desc.isPatternedOnDisplay());
        
        int [] pattern = li.desc.getPattern();
//         boolean hasPattern = false;
//         for(int j=0; j<16; j++) if (pattern[j] != 0) hasPattern = true;
        if (true/*hasPattern*/) {
            for(int j=0; j<16; j++) {
                String p = "";
                for(int k=0; k<16; k++)
                    p += (pattern[j] & (1 << (15-k))) != 0 ? 'X' : ' ';
                bcpel("pattern", p);
            }
        }
        
        if (li.desc.getOutlined() != null)
            bcpel("outlined", desc.getOutlined().getConstName());
        
        // write the 3D information
        if (li.thick3d != 0 || li.height3d != 0) {
            b("display3D"); a("thick", li.thick3d); a("height", li.height3d); el();
        }
        
        if (li.cif != null && li.cif.length() > 0) {
            b("cifLayer"); a("cif", li.cif); el();
        }
        
        // write the SPICE information
        if (li.spiRes != 0 || li.spiCap != 0 || li.spiECap != 0) {
            b("parasitics"); a("resistance", li.spiRes); a("capacitance", li.spiCap); a("edgeCapacitance", li.spiECap); el();
        }
        el("layer");
    }
    
    private void writeXml(ArcInfo ai) {
        b("arcProto"); a("name", ai.name); a("fun", ai.func.getConstantName()); cl();
        bcpel("widthOffset", ai.widthOffset);
        bcpel("defaultWidth", ai.maxWidth);
        bcpel("extended", !ai.noExtend);
        bcpel("fixedAngle", ai.fixAng);
        bcpel("wipable", ai.wipes);
        bcpel("angleIncrement", ai.angInc);
        bcpel("antennaRatio", ai.antennaRatio);
        
        for (ArcInfo.LayerDetails al: ai.arcDetails) {
            String style = al.style == Poly.Type.FILLED ? "FILLED" : "CLOSED";
            b("arcLayer"); a(" layer", al.layer.name); a("widthOffset", al.width); a("style", style); el();
        }
        el("arcProto");
    }
    
    private void writeXml(NodeInfo ni) {
        b("primitiveNode"); a("name", ni.name); a("fun", ni.func.name()); cl();
        bcpel("defaultWidth", ni.xSize);
        bcpel("defaultHeight", ni.ySize);
        if (ni.so != null) {
            double lx = ni.so.getLowXOffset();
            double hx = ni.so.getHighXOffset();
            double ly = ni.so.getLowYOffset();
            double hy = ni.so.getHighYOffset();
            b("sizeOffset"); a("lx", lx); a("hx", hx); a("ly", ly); a("hy", hy); el();
        }
        if (ni.arcsShrink)
            bel("shrinkArcs");
        
        for(int j=0; j<ni.nodeLayers.length; j++) {
            NodeInfo.LayerDetails nl = ni.nodeLayers[j];
            Integer portNum = nl.portIndex != 0 ? Integer.valueOf(nl.portIndex) : null;
            b("nodeLayer"); a("layer", nl.layer.name); a("style", nl.style.name()); a(" portNum", portNum); cl();
            switch (nl.representation) {
                case Technology.NodeLayer.BOX:
                    b("box"); el();
                    break;
                case Technology.NodeLayer.MINBOX:
                    b("minbox"); el();
                    break;
                case Technology.NodeLayer.POINTS:
                    b("points"); el();
                    break;
                case Technology.NodeLayer.MULTICUTBOX:
                    b("multicutbox"); a("sizex", nl.multiXS); a("sizey", nl.multiYS); a("sep1d", nl.multiSep);  a("sep2d", nl.multiSep2D);el();
                    break;
                default:
                    b("?????"); el();
                    break;
            }
            for (Technology.TechPoint tp: nl.values) {
                double xm = tp.getX().getMultiplier();
                double xa = tp.getX().getAdder();
                double ym = tp.getY().getMultiplier();
                double ya = tp.getY().getAdder();
                b("techPoint"); a("xm", xm); a("xa", xa); a("ym", ym); a("ya", ya); el();
            }
            el("nodeLayer");
        }
//            int portNum = nList[i].nodeLayers[j].portIndex;
//            buffWriter.print("\t\t\t\tnew Technology.NodeLayer(" +
//                    nList[i].nodeLayers[j].layer.javaName + "_lay, " + portNum + ", Poly.Type." +
//                    nList[i].nodeLayers[j].style.getConstantName() + ",");
//            switch (nList[i].nodeLayers[j].representation) {
//                case Technology.NodeLayer.BOX:
//                    buffWriter.print(" Technology.NodeLayer.BOX,");     break;
//                case Technology.NodeLayer.MINBOX:
//                    buffWriter.print(" Technology.NodeLayer.MINBOX,");  break;
//                case Technology.NodeLayer.POINTS:
//                    buffWriter.print(" Technology.NodeLayer.POINTS,");  break;
//                default:
//                    buffWriter.print(" Technology.NodeLayer.????,");    break;
//            }
//            buffWriter.println(" new Technology.TechPoint [] {");
//            int totLayers = nList[i].nodeLayers[j].values.length;
//            for(int k=0; k<totLayers; k++) {
//                Technology.TechPoint tp = nList[i].nodeLayers[j].values[k];
//                buffWriter.print("\t\t\t\t\tnew Technology.TechPoint(" +
//                        getEdgeLabel(tp, false) + ", " + getEdgeLabel(tp, true) + ")");
//                if (k < totLayers-1) buffWriter.println(","); else
//                    buffWriter.print("}");
//            }
//            if (nList[i].specialType == PrimitiveNode.SERPTRANS) {
//                buffWriter.print(", " + nList[i].nodeLayers[j].lWidth + ", " + nList[i].nodeLayers[j].rWidth + ", " +
//                        nList[i].nodeLayers[j].extendB + ", " + nList[i].nodeLayers[j].extendT);
//            }
//            buffWriter.print(")");
//            if (j+1 < tot) buffWriter.print(",");
//            buffWriter.println();
//        }
//        buffWriter.println("\t\t\t});");
        for (int j = 0; j < ni.nodePortDetails.length; j++) {
            NodeInfo.PortDetails pd = ni.nodePortDetails[j];
            b("primitivePort"); a("name", pd.name); cl();
            b("portAngle"); a("primary", pd.angle); a("range", pd.range); el();
            bcpel("portTopology", pd.netIndex);
            for (int k = 0; k < 2; k++) {
                double xm = pd.values[k].getX().getMultiplier();
                double xa = pd.values[k].getX().getAdder();
                double ym = pd.values[k].getY().getMultiplier();
                double ya = pd.values[k].getY().getAdder();
                b("techPoint"); a("xm", xm); a("xa", xa); a("ym", ym); a("ya", ya); el();
            }
            for (ArcInfo a: pd.connections)
                bcpel("portArc", a.name);
            el("primitivePort");
        }
        switch (ni.specialType) {
            case PrimitiveNode.POLYGONAL:
                bel("polygonal");
                break;
            case PrimitiveNode.SERPTRANS:
                b("serpTrans"); cl();
                for (int i = 0; i < 6; i++) {
                    bcpel("specialValue", ni.specialValues[i]);
                }
                el("serpTrans");
                break;
        }

        el("primitiveNode");
    }
    
    private void writeSpiceHeaderXml(int level, String[] spiceHeader) {
        if (spiceHeader == null) return;
        b("spiceHeader"); a("level", level); cl();
        for (String line: spiceHeader) {
            b("spiceLine"); a("line", line); el();
        }
        el("spiceHeader");
        l();
    }
    
    private void writeDesignRulesXml(String foundry, LayerInfo[] lList, double[] conDist, double[] uConDist) {
        b("Foundry"); a("name", foundry); cl();
        
        if (conDist != null && uConDist != null) {
            int layerTotal = lList.length;
            int ruleIndex = 0;
            for (int i1 = 0; i1 < layerTotal; i1++) {
                LayerInfo l1 = lList[i1];
                for (int i2 = i1; i2 < layerTotal; i2++) {
                    LayerInfo l2 = lList[i2];
                    double conSpa = conDist[ruleIndex];
                    double uConSpa = uConDist[ruleIndex];
                    if (conSpa > -1)
                        printDesignRule("C" + ruleIndex, l1, l2, "CONSPA", conSpa);
                    if (uConSpa > -1)
                        printDesignRule("U" + ruleIndex, l1, l2, "UCONSPA", uConSpa);
                    ruleIndex++;
                }
            }
        }
        el("Foundry");
    }
    
    private void printDesignRule(String ruleName, LayerInfo l1, LayerInfo l2, String type, double value) {
        String layerNames = "{" + l1.name + ", " + l2.name + "}";
        b("LayersRule"); a("ruleName", ruleName); a("layerNames", layerNames); a("type", type); a("when", "ALL"); a("value", value); el();
    }
    
    private void header() {
        checkIndent();
        out.print("<?xml"); a("version", "1.0"); a("encoding", "UTF-8"); out.println("?>");
    }

    private void comment(String s) {
        checkIndent();
        out.print("<!-- "); p(s); out.print(" -->"); l();
    }
    
    /**
     * Print attribute.
     */
    private void a(String name, Object value) {
        checkIndent();
        if (value == null) return;
        out.print(" " + name + "=\"");
        p(value.toString());
        out.print("\"");
    }

    private void bcpel(String key, Object v) {
        if (v == null) return;
        b(key); c(); p(v.toString()); el(key);
    }
    
    private void bel(String key) {
        b(key); el();
    }
    
    /**
     * Print text with replacement of special chars.
     */
    private void pl(String s) {
        checkIndent();
        p(s); l();
    }
    
    /**
     * Print text with replacement of special chars.
     */
    private void p(String s) {
        assert indentEmitted;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<':
                    out.print("&lt;");
                    break;
                case '>':
                    out.print("&gt;");
                    break;
                case '&':
                    out.print("&amp;");
                    break;
                case '\'':
                    out.print("&apos;");
                    break;
                case '"':
                    out.print("quot;");
                    break;
                default:
                    out.print(c);
            }
        }
    }

    /**
     * Print element name, and indent.
     */
    private void b(String key) {
        checkIndent();
        out.print('<');
        out.print(key);
        indent += INDENT_WIDTH;
    }
    
    private void cl() {
        assert indentEmitted;
        out.print('>');
        l();
    }
    
    private void c() {
        assert indentEmitted;
        out.print('>');
    }
    
    private void el() {
        e(); l();
    }
    
    private void e() {
        assert indentEmitted;
        out.print("/>");
        indent -= INDENT_WIDTH;
    }
    
    private void el(String key) {
        indent -= INDENT_WIDTH;
        checkIndent();
        out.print("</");
        out.print(key);
        out.print(">");
        l();
    }
    
    private void checkIndent() {
        if (indentEmitted) return;
        for (int i = 0; i < indent; i++)
            out.print(' ');
        indentEmitted = true;
    }
    
    /**
     *  Print new line.
     */
    private void l() {
        out.println();
        indentEmitted = false;
    }
}
