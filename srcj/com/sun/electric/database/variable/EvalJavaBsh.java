/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: EvalJavaBsh.java
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

package com.sun.electric.database.variable;

import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Stack;
import java.util.Iterator;

/**
 * Used for evaluating Java expressions in Variables
 * It is meant to be invoked from the Variable context;
 * these methods should not be used from other contexts, and
 * thus are declared protected.
 *
 * @author  gainsley
 */
public class EvalJavaBsh
{

    // ------------------------ private data ------------------------------------

	/** the singleton object of this class. */		public static EvalJavaBsh evalJavaBsh = new EvalJavaBsh();

    /** The bean shell interpreter eval method */   private static Method evalMethod;
    /** The bean shell interpreter source method */ private static Method sourceMethod;
    /** The bean shell interpreter set method */    private static Method setMethod;
    /** The bean shell TargetError getTarget method */ private static Method getTargetMethod;
    /** The bean shell interpreter class */         private static Class interpreterClass = null;
    /** The bean shell TargetError class */         private static Class targetErrorClass;

    /** For replacing @variable */					private static final Pattern atPat = Pattern.compile("@(\\w+)");
    /** For replacing @variable */					private static final Pattern pPat = Pattern.compile("(P|PAR)\\(\"(\\w+)\"\\)");

    /** The bean shell interpreter object */        private Object envObject;
    /** Context stack for recursive eval calls */   private Stack contextStack = new Stack();
    /** Info stack for recursive eval calls */      private Stack infoStack = new Stack();

    /** turn on Bsh verbose debug stmts */          private static boolean debug = false;


    public static class IgnorableException extends Exception {
        public IgnorableException() { super(); }
        public IgnorableException(String message) { super(message); }
        public IgnorableException(String message, Throwable cause) { super(message, cause); }
        public IgnorableException(Throwable cause) { super(cause); }
    }

    // ------------------------ private and protected methods -------------------

    /** the contructor */
    private EvalJavaBsh()
    {
        envObject = null;

        initBSH();

        // if interpreter class is null, we cannot create a new bean shell object
        if (interpreterClass == null) return;

        // create the BSH object
        try
        {
            envObject = interpreterClass.newInstance();
        } catch (Exception e)
        {
            System.out.println("Can't create an instance of the Bean Shell: " + e.getMessage());
            envObject = null;
            return;
        }

        setVariable("evalJavaBsh", this);
        doEval("Object P(String par) { return evalJavaBsh.P(par); }");
        doEval("Object PAR(String par) { return evalJavaBsh.PAR(par); }");

        // the following is for running scripts
        doEval("import com.sun.electric.tool.user.MenuCommands;");
        doEval("import com.sun.electric.database.hierarchy.*;");
        doEval("import com.sun.electric.database.prototype.*;");
        doEval("import com.sun.electric.database.topology.*;");
        doEval("import com.sun.electric.database.variable.ElectricObject;");
        doEval("import com.sun.electric.database.variable.FlagSet;");
        doEval("import com.sun.electric.database.variable.TextDescriptor;");
        doEval("import com.sun.electric.database.variable.VarContext;");
        doEval("import com.sun.electric.database.variable.Variable;");

        // do not import variable.EvalJavaBsh, because calling EvalJavaBsh.runScript
        // will spawn jobs in an unexpected order
        doEval("import com.sun.electric.tool.io.*;");
    }

    /** Get the interpreter so other tools may add methods to it. There is only
     * one interpreter, so be careful that separate tools do not conflict in
     * terms of namespace.  I recommend when adding objects or methods to the
     * Interpreter you prepend the object or method names with the Tool name.
     */
//	  public static Interpreter getInterpreter() { return env; }

    /**
     * See what the current context of eval is.
     * @return a VarContext.
     */
    public synchronized VarContext getCurrentContext() { return (VarContext)contextStack.peek(); }

    /**
     * See what the current info of eval is.
     * @return an Object.
     */
    public synchronized Object getCurrentInfo() { return infoStack.peek(); }

    /**
     * Replaces @var calls to P("var")
     * Replaces P("var") calls to P("ATTR_var")
     * Replaces PAR("var") calls to PAR("ATTR_var")
     * @param expr the expression
     * @return replaced expression
     */
    protected static String replace(String expr) {
        StringBuffer sb = new StringBuffer();
        Matcher atMat = atPat.matcher(expr);
        while(atMat.find()) {
            atMat.appendReplacement(sb, "P(\""+atMat.group(1)+"\")");
        }
        atMat.appendTail(sb);

        expr = sb.toString();
        sb = new StringBuffer();
        Matcher pMat = pPat.matcher(expr);
        while(pMat.find()) {
            if (pMat.group(2).startsWith("ATTR_"))
                pMat.appendReplacement(sb, pMat.group(0));
            else
                pMat.appendReplacement(sb, pMat.group(1)+"(\"ATTR_"+pMat.group(2)+"\")");
        }
        pMat.appendTail(sb);

        return sb.toString();
    }

    /** Evaluate Object as if it were a String containing java code.
     * Note that this function may call itself recursively.
     * @param obj the object to be evaluated (toString() must apply).
     * @param context the context in which the object will be evaluated.
     * @param info used to pass additional info from Electric to the interpreter, if needed.
     * @return the evaluated object.
     */
    public synchronized Object eval(Object obj, VarContext context, Object info) {
        String expr = replace(obj.toString());  // change @var calls to P(var)
        if (context == null) context = VarContext.globalContext;
        // check for infinite recursion
        for (int i=0; i<contextStack.size(); i++) {
            VarContext vc = (VarContext)contextStack.get(i);
            Object inf = infoStack.get(i);
            if ((vc == context) && (inf == info)) return "Eval recursion error";
        }
        contextStack.push(context);             // push context
        infoStack.push(info);                   // push info
        Object ret = doEval(expr);              // ask bsh to eval
        contextStack.pop();                     // pop context
        infoStack.pop();                        // pop info
        //System.out.println("BSH: "+expr.toString()+" --> "+ret);
        if (ret instanceof Number) {
            // get rid of lots of decimal places on floats and doubles
            ret = Variable.format((Number)ret, 3);
        }
        return ret;
    }

    //------------------Methods that may be called through Interpreter--------------

    /** Lookup variable for evaluation
     * @return an evaluated object
     */
    public Object P(String name) throws IgnorableException {
        VarContext context = (VarContext)contextStack.peek();
        Object val = context.lookupVarEval(name);
        if (val == null) throw new IgnorableException("Lookup of "+name+" not found");
        return val;
    }

    public Object PAR(String name) throws IgnorableException {
        VarContext context = (VarContext)contextStack.peek();
        Object val = context.lookupVarFarEval(name);
        if (val == null) throw new IgnorableException("Far lookup of "+name+" not found");
        return val;
    }

    //---------------------------Running Scripts-------------------------------------

    /** Run a Java Bean Shell script */
    public static void runScript(String script) {
        runScriptJob job = new runScriptJob(script);
    }

    private static class runScriptJob extends Job
	{
        String script;
        EvalJavaBsh evaluator;

        protected runScriptJob(String script) {
            super("JavaBsh script: "+script, User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.script = script;
            evaluator = new EvalJavaBsh();
            this.startJob();
        }

        public boolean doIt() {
            evaluator.doSource(script);
			return true;
       }
    }

    // ****************************** REFLECTION FOR ACCESSING THE BEAN SHELL ******************************

    private void initBSH()
    {
        // if already initialized, return
        if (interpreterClass != null) return;

        // find the BSH classes
        try
        {
            interpreterClass = Class.forName("bsh.Interpreter");
            targetErrorClass = Class.forName("bsh.TargetError");
        } catch (ClassNotFoundException e)
        {
            System.out.println("Can't find the Bean Shell: " + e.getMessage());
            interpreterClass = null;
            return;
        }

        // find the necessary methods on the BSH class
        try
        {
            evalMethod = interpreterClass.getMethod("eval", new Class[] {String.class});
            sourceMethod = interpreterClass.getMethod("source", new Class[] {String.class});
            setMethod = interpreterClass.getMethod("set", new Class[] {String.class, Object.class});
            getTargetMethod = targetErrorClass.getMethod("getTarget", null);
        } catch (NoSuchMethodException e)
        {
            System.out.println("Can't find methods in the Bean Shell: " + e.getMessage());
            interpreterClass = null;
            return;
        }
    }

    /**
     * Set a variable in the Java Bean Shell
     * @param name the name of the variable
     * @param value the value to set the variable to
     */
    public void setVariable(String name, Object value)
    {
        try {
            if (envObject != null) {
                setMethod.invoke(envObject, new Object[] {name, value});
            }
        } catch (Exception e) {
            handleInvokeException(e, "Bean shell error setting " + name + " to "+ value + ": ");
        }
    }


    // -------------------------- Private Methods -----------------------------

    /**
     * Evaluate a string containing Java Bean Shell code.
     * @param line the string to evaluate
     * @return an object representing the evaluated string, or null on error.
     */
    private Object doEval(String line)
    {
        Object returnVal = null;
        try {
            if (envObject != null) {
                returnVal = evalMethod.invoke(envObject, new Object[] {line});
            }
        } catch (Exception e) {
            handleInvokeException(e, "Bean shell error evaluating "+line);
        }
        return returnVal;
    }

    // source a Java Bean Shell script file
    private void doSource(String file)
    {
        try {
            if (envObject != null) {
                sourceMethod.invoke(envObject, new Object[] {file});
            }
        } catch (Exception e) {
            handleInvokeException(e, "Bean shell error sourcing '" + file +"'");
        }
    }

    private static Throwable doGetTarget(Object ex)
    {
        Throwable returnVal = null;
        if (interpreterClass != null) {
            try {
                returnVal = (Throwable)getTargetMethod.invoke(ex, new Object[] {});
            } catch (Exception e) {
                handleInvokeException(e, "Bean shell error getting exception target");
            }
        }
        return returnVal;
    }

    /**
     * Handle exceptions thrown by attempting to invoke a reflected method or constructor.
     * @param e The exception thrown by the invoked method or constructor.
     * @param description a description of the event to be printed with the error message.
     */
    private static void handleInvokeException(Exception e, String description) {

        if (e instanceof InvocationTargetException) {
            // This wraps an exception thrown by the method invoked.
            Throwable t = e.getCause();
            if (t != null)
                handleBshError((Exception)t, description);
        }
        else if (e instanceof IllegalArgumentException) {
            System.out.println(description+": "+e.getMessage());
            if (debug) e.printStackTrace(System.out);
        }
        else if (e instanceof IllegalAccessException) {
            System.out.println(description+": "+e.getMessage());
            if (debug) e.printStackTrace(System.out);
        }
        else {
            System.out.println("Unhandled Exception: ");
            System.out.println(description+": "+e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    /**
     * Handle Bean Shell evaluation errors.  Sends it to system.out.
     * @param e the TargetError exception thrown.
     * @param description a description of the event that caused the error to be thrown.
     */
    private static void handleBshError(Exception e, String description)
    {
        if (targetErrorClass.isInstance(e)) {
            // The Bean Shell had an error
            Throwable t = doGetTarget(e);
            if (t != null) {
                if (t instanceof IgnorableException) {
                    if (debug) {
                        System.out.println("IngorableException: "+description+": "+t.getMessage());
                        // e.printStackTrace(System.out);
                    }
                } else {
                    System.out.println(description+": "+t.getMessage());
                    if (debug) e.printStackTrace(System.out);
                }
            }
        } else {
            System.out.println("Unhandled Bsh Exception: "+description+": "+e.getMessage());
            e.printStackTrace();
        }
    }

}
