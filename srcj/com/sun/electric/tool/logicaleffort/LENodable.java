package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.technology.technologies.Schematics;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Aug 27, 2004
 * Time: 3:57:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class LENodable {

    /** Type is a typesafe enum class that describes the type of Instance this is */
    protected static class Type {
        private final String name;
        private Type(String name) { this.name = name; }
        public String toString() { return name; }

        /** NotSizeable */  protected static final Type STATICGATE = new Type("Static Gate");
        /** NotSizeable */  protected static final Type LOAD = new Type("Load");
        /** NotSizeable */  protected static final Type WIRE = new Type("Wire");
        /** LeGate */       protected static final Type LEGATE = new Type("LE Gate");
        /** LeKeeper */     protected static final Type LEKEEPER = new Type("LE Keeper");
        /** NotSizeable */  protected static final Type TRANSISTOR = new Type("Transistor");
        /** NotSizeable */  protected static final Type CAPACITOR = new Type("Capacitor");
        /** Cached cell */  protected static final Type CACHEDCELL = new Type("Cached Cell");
    }

    // --------- Definition fields ----------
    /** list of pins */                     private List pins; // do not point to networks
    /** nodable */                          private Nodable no;
    /** gate type */                        private Type type;
    /** output JNetwork */                  private JNetwork outputJNet;
    /** mfactor variable */                 private Variable mfactorVar;
    /** su variable */                      private Variable suVar;
    /** parallel group # variable */        private Variable parallelGroupVar;

    // --------- instance fields ------------
    protected VarContext context;
    protected LENetwork outputNetwork;        // global network
    protected float mfactor;
    protected float su;
    protected float leX;
    protected int parallelGroup;

    /**
     * Create a new LEInstance tied to the Nodable.
     * If the type represents a singular instance, i.e. whose properties
     * do not change based on it's location in the hierarchy, then
     * leContext is the singular (one and only) context.
     * Otherwise, leContext is merely the first of many possible contexts.
     * @param no the Nodable
     */
    public LENodable(Nodable no, Type type, Variable mfactorVar, Variable suVar, Variable parallelGroupVar) {
        this.no = no;
        this.type = type;
        pins = new ArrayList();
        this.outputJNet = null;
        this.mfactorVar = mfactorVar;
        this.suVar = suVar;
        this.parallelGroupVar = parallelGroupVar;
        this.context = VarContext.globalContext;
    }

    /**
     * Add a port to this LEInstance
     * @param name the name of the port
     * @param dir the direction of the port
     * @param le the logical effort of the port
     */
    protected void addPort(String name, LEPin.Dir dir, float le, JNetwork jnet) {
        LEPin pin = new LEPin(name, dir, le, jnet, this);
        pins.add(pin);
    }

    /** Set the output network */
    protected void setOutputJNet(JNetwork jnet) { outputJNet = jnet; }

    /** Get the output network */
    protected JNetwork getOutputJNet() { return outputJNet; }

    /** Get the nodable */
    protected Nodable getNodable() { return no; }

    /** Get the type */
    protected Type getType() { return type; }

    /** Get the pins */
    protected List getPins() { return pins; }

    /** Return true if this is a sizeable gate */
    protected boolean isLeGate() {
        if (type == Type.LEKEEPER || type == Type.LEGATE) return true;
        return false;
    }

    /** True if this is a gate */
    protected boolean isGate() {
        if (type == Type.LEGATE || type == Type.LEKEEPER || type == Type.STATICGATE)
            return true;
        return false;
    }

    /**
     * Set the only context of this LENodable. This is used when the nodable is cacheable,
     * and remains the same throughout all contexts above the passed context.
     * Returns false if provided context is not enough to evaluate all variables properly.
     * @param context the context
     * @param outputNetwork the global network loading the output
     * @param mfactor the parent's mfactor
     * @param su the parent's step-up
     */
    protected boolean setOnlyContext(VarContext context, LENetwork outputNetwork, float mfactor, float su, LENetlister2.NetlisterConstants constants) {
        boolean evalOk = instantiate(this, context, outputNetwork, mfactor, su, constants);
        //print();
        return evalOk;
    }

    /**
     * Factory method to create a copy of this Nodable with the context-relevant info
     * evaluated.
     * @param context the context
     * @param outputNetwork the global network loading the output
     * @param mfactor the parent's mfactor
     * @param su the parent's step-up
     */
    protected LENodable createUniqueInstance(VarContext context, LENetwork outputNetwork, float mfactor, float su, LENetlister2.NetlisterConstants constants) {
        LENodable instance = new LENodable(no, type, mfactorVar, suVar, parallelGroupVar);
        // copy pins
        for (Iterator it = pins.iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next();
            instance.addPort(pin.getName(), pin.getDir(), pin.getLE(), pin.getJNetwork());
        }
        instantiate(instance, context, outputNetwork, mfactor, su, constants);
        return instance;
    }

    /**
     * Fill-in the given LENodable with evalutated instance-specific info for the given context.
     * Returns false if there are variables that cannot be evaluated in the given context
     * @param instance the LENodable to fill-in
     * @param context the context
     * @param outputNetwork the global network loading the output
     * @param mfactor the parent's mfactor
     * @param su the parent's step-up
     */
    private boolean instantiate(LENodable instance, VarContext context, LENetwork outputNetwork, float mfactor, float su, LENetlister2.NetlisterConstants constants) {
        instance.outputNetwork = outputNetwork;
        boolean evalOk = true;

        // default values
        instance.leX = getLeX(context, constants);
        if (instance.leX == -1f) evalOk = false;
        instance.mfactor = mfactor;
        instance.su = su;
        instance.parallelGroup = 0;
        instance.context = context;

        // evaluate variables in context, if any
        if (parallelGroupVar != null) {
            Object retVal = context.evalVar(parallelGroupVar);
            if (retVal == null) evalOk = false;
            else instance.parallelGroup = VarContext.objectToInt(retVal, instance.parallelGroup);
        }
        if (suVar != null) {
            Object retVal = context.evalVar(suVar);
            if (retVal == null) evalOk = false;
            else {
                float localsu = VarContext.objectToFloat(retVal, -1f);
                instance.su = (localsu == -1f) ? instance.su : localsu;
            }
        }
        if (mfactorVar != null) {
            Object retVal = context.evalVar(mfactorVar);
            if (retVal == null) evalOk = false;
            else instance.mfactor *= VarContext.objectToFloat(retVal, 1f);
        }
        return evalOk;
    }

    /**
     * Get the leX size for the given LENodable
     * Returns -1 if any evaluations failed.
     * @param context the VarContext
     * @return the leX size, or -1 if eval failed
     */
    private float getLeX(VarContext context, LENetlister2.NetlisterConstants constants) {
        float leX = (float)0.0;

        Variable var = null;
        Object retVal = null;
        if (type == LENodable.Type.WIRE) {
            // Note that if inst is an LEWIRE, it will have no 'le' attributes.
            // we therefore assign pins to have default 'le' values of one.
            // This creates an instance which has Type LEWIRE, but has
            // boolean leGate set to false; it will not be sized
            var = no.getVar("ATTR_L");
            retVal = context.evalVar(var);
            if (retVal == null) return -1f;
            float len = VarContext.objectToFloat(retVal, 0.0f);

            var = no.getVar(Schematics.ATTR_WIDTH);
            retVal = context.evalVar(var);
            if (retVal == null) return -1f;
            float width = VarContext.objectToFloat(retVal, 3.0f);

            leX = (float)(0.95f*len + 0.05f*len*(width/3.0f))*constants.wireRatio;  // equivalent lambda of gate
            leX = leX/9.0f;                         // drive strength X=1 is 9 lambda of gate
        }
        else if (type == LENodable.Type.TRANSISTOR) {
            var = no.getVar(Schematics.ATTR_WIDTH);
            if (var == null) {
                System.out.println("Error: transistor "+no+" has no width in Cell "+no.getParent());
                //ErrorLogger.ErrorLog log = errorLogger.logError("Error: transistor "+no+" has no width in Cell "+info.getCell(), info.getCell(), 0);
                //log.addGeom(ni.getNodeInst(), true, no.getParent(), context);
                return -1f;
            }
            retVal = context.evalVar(var);
            if (retVal == null) return -1f;
            float width = VarContext.objectToFloat(retVal, (float)3.0);

            var = no.getVar(Schematics.ATTR_LENGTH);
            if (var == null) {
                System.out.println("Error: transistor "+no+" has no length in Cell "+no.getParent());
                //ErrorLogger.ErrorLog log = errorLogger.logError("Error: transistor "+ni+" has no length in Cell "+info.getCell(), info.getCell(), 0);
                //log.addGeom(ni.getNodeInst(), true, info.getCell(), info.getContext());
                return -1f;
            }
            retVal = context.evalVar(var);
            if (retVal == null) return -1f;
            float length = VarContext.objectToFloat(retVal, (float)2.0);
            // not exactly correct because assumes all cap is area cap, which it isn't
            leX = (float)(width*length/2.0f);
            leX = leX/9.0f;
        }
        else if (type == Type.CAPACITOR) {
            var = no.getVar(Schematics.SCHEM_CAPACITANCE);
            if (var == null) {
                System.out.println("Error: capacitor "+no+" has no capacitance in Cell "+no.getParent());
                //ErrorLogger.ErrorLog log = errorLogger.logError("Error: capacitor "+no+" has no capacitance in Cell "+info.getCell(), info.getCell(), 0);
                //log.addGeom(ni.getNodeInst(), true, no.getParent(), context);
                return -1f;
            }
            retVal = context.evalVar(var);
            if (retVal == null) return -1f;
            float cap = VarContext.objectToFloat(retVal, (float)0.0);
            leX = (float)(cap/constants.gateCap/1e-15/9.0f);
        }
        return leX;
    }

    // -----------------------------------------------------------------

    protected String getName() {
        if (context == null) return no.getName();
        return context.push(getNodable()).getInstPath(".");
    }

    protected void print() {
        System.out.println(getType().toString()+": "+getName());
        System.out.println("    Size    \t= "+leX);
        System.out.println("    Step-up \t= "+su);
        System.out.println("    Parallel Group\t= "+parallelGroup);
        System.out.println("    M Factor\t= "+mfactor);
    }

    protected void printPins() {
        for (Iterator it = pins.iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next();
            System.out.println("Pin "+pin.getName()+", le="+pin.getLE()+", dir="+pin.getDir()+" on network "+pin.getJNetwork());
        }
    }

    protected float printLoadInfo(LEPin pin, float alpha) {
        StringBuffer buf = new StringBuffer();
        buf.append(getType().toString());
        buf.append("\tSize="+TextUtils.formatDouble(leX, 2));
        buf.append("\tLE="+TextUtils.formatDouble(pin.getLE(), 2));
        buf.append("\tM="+TextUtils.formatDouble(mfactor, 2));
        float load;
        if (pin.getDir() == LEPin.Dir.OUTPUT) {
            load = (float)(leX*pin.getLE()*mfactor*alpha);
            buf.append("\tAlpha="+alpha);
            buf.append("\tLoad="+TextUtils.formatDouble(load, 2));
        } else {
            load = (float)(leX*pin.getLE()*mfactor);
            buf.append("\tLoad="+TextUtils.formatDouble(load, 2));
        }
        buf.append("\t"+getName());
        System.out.println(buf.toString());
        return load;
    }
}
