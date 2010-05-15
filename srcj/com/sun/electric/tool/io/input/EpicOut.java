/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EpicOut.java
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.Environment;
import com.sun.electric.database.geometry.btree.BTree;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.UserInterfaceExec;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.ScalarSignal;
import com.sun.electric.tool.simulation.ScalarSample;
import com.sun.electric.tool.user.ActivityLogger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.simulation.AnalogAnalysis;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.user.ActivityLogger;

import com.sun.electric.tool.simulation.*;
import com.sun.electric.database.geometry.btree.*;
import com.sun.electric.database.geometry.btree.unboxed.*;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class EpicOut {

/**
 * Class for reading and displaying waveforms from Epic output using
 * the new on-disk index.
 */
public static class EpicOutProcess extends Simulate implements Runnable
{
    private Thread readerProcess;
    private PipedInputStream pis;
    private PipedOutputStream pos;
    private PipedInputStream epis;
    private PipedOutputStream epos;
    private DataInputStream stdOut;
    private ArrayList<String> strings = new ArrayList<String>();
    private final Environment launcherEnvironment;
    private final UserInterfaceExec userInterface;
    EpicAnalysis epicAnalysis = null;

    EpicOutProcess() {
        launcherEnvironment = Environment.getThreadEnvironment();
        userInterface = new UserInterfaceExec();
    }

    /**
     * Method to read an Spice output file.
     */
    protected void readSimulationOutput(Stimuli sd, URL fileURL, Cell cell)
        throws IOException
    {
        // show progress reading .spo file
        startProgressDialog("EPIC output", fileURL.getFile());

        // read the actual signal data from the .spo file
        boolean eof = false;

        pos = new PipedOutputStream();
        pis = new PipedInputStream(pos);
        epos = new PipedOutputStream();
        epis = new PipedInputStream(epos);

        char separator = '.';
        sd.setSeparatorChar(separator);
        this.epicAnalysis = new EpicAnalysis(sd);

        readerProcess = new EpicReader(pos, epos, fileURL.getFile());
        
        try {
            stdOut = new DataInputStream(pis);
            (new Thread(this, "EpicReaderErrors")).start();
            sd = readEpicFile(sd);
            sd.setCell(cell);
        } catch (EOFException e) {
            eof = true;
        }

        if (readerProcess != null) {
            try {
                readerProcess.join();
            } catch (InterruptedException e) {}
        }

        if (eof) {
            //Job.getUserInterface().showErrorMessage("EpicReaderProcess exited");
        }

        // stop progress dialog
        stopProgressDialog();

        // free memory
        strings = null;
        stdOut.close();
        stdOut = null;
        readerProcess = null;
    }




    private static class SigInfo {
        private final int minV, maxV;
        private final int start, len;

        private SigInfo(int minV, int maxV, int start, int len) {
            this.minV = minV;
            this.maxV = maxV;
            this.start = start;
            this.len = len;
        }
    }

    private Stimuli readEpicFile(Stimuli sd) throws IOException {
        EpicAnalysis an = this.epicAnalysis;
        int numSignals = 0;
        ContextBuilder contextBuilder = new ContextBuilder();
        ArrayList<ContextBuilder> contextStack = new ArrayList<ContextBuilder>();
        contextStack.add(contextBuilder);
        int contextStackDepth = 1;
        final boolean DEBUG = false;
        for (;;) {
            byte b = stdOut.readByte();
            if (b == 'F') break;
            switch (b) {
                case 'V':
                case 'I':
                    int sigNum = stdOut.readInt();
                    String name = readString();
                    if (DEBUG) printDebug(contextStackDepth, (char)b, name);
                    contextBuilder.strings.add(name);
                    byte type = b == 'V' ? EpicAnalysis.VOLTAGE_TYPE: EpicAnalysis.CURRENT_TYPE;
                    contextBuilder.contexts.add(EpicAnalysis.getContext(type));
                    break;
                case 'D':
                    String down = readString();
                    if (DEBUG) printDebug(contextStackDepth, (char)b, down);
                    contextBuilder.strings.add(down);

                    if (contextStackDepth >= contextStack.size())
                        contextStack.add(new ContextBuilder());
                    contextBuilder = contextStack.get(contextStackDepth++);
                    break;
                case 'U':
                    if (DEBUG) printDebug(contextStackDepth, (char)b, null);
                    EpicAnalysis.Context newContext = an.getContext(contextBuilder.strings, contextBuilder.contexts);
                    contextBuilder.clear();

                    contextStackDepth--;
                    contextBuilder = contextStack.get(contextStackDepth - 1);

                    contextBuilder.contexts.add(newContext);
                    break;
                default:
                    assert false;
            }
            assert contextBuilder == contextStack.get(contextStackDepth - 1);
        }
        assert contextStackDepth == 1;
        an.setRootContext(an.getContext(contextBuilder.strings, contextBuilder.contexts));

        an.setTimeResolution(stdOut.readDouble());
        an.setVoltageResolution(stdOut.readDouble());
        an.setCurrentResolution(stdOut.readDouble());
        an.setMaxTime(stdOut.readDouble());
        List<AnalogSignal> signals = an.getSignals();
        assert numSignals == signals.size();
        an.waveStarts = new int[numSignals];
        an.waveLengths = new int[numSignals];

        ArrayList<SigInfo> sigInfoByEpicIndex = new ArrayList<SigInfo>();
        int start = 0;
        for (;;) {
            int sigNum = stdOut.readInt();
            if (sigNum == -1) break;
            while (sigNum >= sigInfoByEpicIndex.size())
                sigInfoByEpicIndex.add(null);
            int minV = stdOut.readInt();
            int maxV = stdOut.readInt();
            int len = stdOut.readInt();
            assert sigInfoByEpicIndex.get(sigNum) == null;
            sigInfoByEpicIndex.set(sigNum, new SigInfo(minV, maxV, start, len));
            assert len >= 0;
            start += len;
        }

        /*
        for (int i = 0; i < numSignals; i++) {
            EpicAnalysis.EpicSignal s = (EpicAnalysis.EpicSignal)signals.get(i);
            SigInfo si = sigInfoByEpicIndex.get(s.sigNum);
            s.setBounds(si.minV, si.maxV);
            an.waveStarts[i] = si.start;
            an.waveLengths[i] = si.len;
        }
        */
        an.setWaveFile(new File(stdOut.readUTF()));

        return sd;
    }

    private void printDebug(int level, char cmd, String arg) {
        StringBuilder sb = new StringBuilder();
        while (level-- > 0)
            sb.append(' ');
        sb.append(cmd);
        if (arg != null) {
            sb.append(' ');
            sb.append(arg);
        }
        System.out.println(sb);
    }

    private String readString() throws IOException {
        int stringIndex = stdOut.readInt();
        if (stringIndex == -1) return null;
        if (stringIndex == strings.size()) {
            String s = stdOut.readUTF();
            strings.add(s);
        }
        return strings.get(stringIndex);
    }

    /**
     * This methods implements Runnable interface for thread which polls stdErr of EpicReaderProcess
     * and redirects it to System.out and progress indicator.
     */
    public void run() {
        Environment.setThreadEnvironment(launcherEnvironment);
        Job.setUserInterface(userInterface);
        
        final String progressKey = "**PROGRESS ";
        BufferedReader stdErr = new BufferedReader(new InputStreamReader(epis));
        try {
            // read from stream
            String line = null;
            while ((line = stdErr.readLine()) != null) {
                if (line.startsWith(progressKey)) {
                    line = line.substring(progressKey.length());
                    if (line.startsWith("!"))  {
                        setProgressValue(0);
                        setProgressNote(line.substring(1));
                    } else {
                        setProgressValue(TextUtils.atoi(line));
                    }
                    continue;
                }
                System.out.println("EpicReader: " + line);
            }
            stdErr.close();
        } catch (java.io.IOException e) {
            if (e.toString().indexOf("Write end dead")!=-1) return;
            ActivityLogger.logException(e);
        }
    }

    /**
     * Class which is used to restore Contexts from flat list of Signals.
     */
    private static class ContextBuilder {
        ArrayList<String> strings = new ArrayList<String>();
        ArrayList<EpicAnalysis.Context> contexts = new ArrayList<EpicAnalysis.Context>();

        void clear() { strings.clear(); contexts.clear(); }
    }

    /** Time resolution from input file. */                     private double timeResolution;
    /** Voltage resolution from input file. */                  private double voltageResolution;
    /** Current resolution from input file. */                  private double currentResolution;

    /**
     * This class is a launched in external JVM to read Epic output file.
     * It doesn't import other Electric modules.
     * Waveforms are stored in temporary file.
     * Signal names are passed to std out using the following syntax:
     * StdOut :== signames resolutions signalInfo* signalInfoEOF fileName
     * signames :== ( up | down | signal )* 'F' 
     * up :== 'U'
     * down :== 'D' stringRef
     * signal :== ( 'V' | 'I' ) sigEpicNum stringRef
     * sigEpicNum ::== INT
     * stringRef= INT [ UTF ]
     * resolutions :== timeResolution voltageResolution currentResolution timeMax
     * timeResolution :== DOUBLE
     * voltageResolution :== DOUBLE
     * currentResolution :== DOUBLE
     * timeMax :== DOUBLE
     * signalInfo :== sigEpicNum minV maxV packedLength
     * minV :== INT
     * maxV :== INT
     * packedLength :== INT
     * signalInfoEOF :== -1
     * fileName :== UDF
     *
     * Inititially current context is empty.
     * 'D' pushes a string to it.
     * 'U' pops a string.
     * 'V' and 'I' use the current context.
     *
     * stringRef  starts with a number. If this number is a new number then it is followed by definition of this string,
     * otherwise it is reference of previously defined string.
     * Number of signalInfo is equal to number of defined signals.
     * fileName is a name of temporary file on local machine with packed waveform data.
     * length in signalInfo is a number of bytes occupied in the file by this signal.
     */
    private class EpicReader extends Thread {
        /** Input stream with Epic data. */                         private InputStream inputStream;
        /** File length of inputStream. */                          private long fileLength;
        /** Number of bytes read from stream to buffer. */          private long byteCount;
        /** Buffer for parsing. */                                  private byte[] buf = new byte[65536];
        /** Count of valid bytes in buffer. */                      private int bufL;
        /** Count of parsed bytes in buffer. */                     private int bufP;
        /** Count of parsed lines. */                               private int lineNum;
        /** String builder to build input line. */                  private StringBuilder builder = new StringBuilder();
        /** Pattern used to split input line into pieces. */        private Pattern whiteSpace = Pattern.compile("[ \t]+");
        /** Last value of progress indicater (percents(.*/          private byte lastProgress;

        /** ContextRoot */                                          private EpicReaderContext rootCtx = new EpicReaderContext();
        /** A map from Strings to Integer ids. */                   private HashMap<String,Integer> stringIds = new HashMap<String,Integer>();
        /** Sparce list to access signals by their Epic indices. */ private ArrayList<EpicReaderSignal> signalsByEpicIndex = new ArrayList<EpicReaderSignal>();
        /** Current time (in integer units). */                     private int curTime = 0;
        /** A stack of signal context pieces. */                    private ArrayList<String> contextStack = new ArrayList<String>();
        /** Count of timepoints for statistics. */                  private int timesC = 0;
        /** Count of signal events for statistics. */               private int eventsC = 0;
    
        /* DataOutputStream view of standard output. */             DataOutputStream stdOut;

        /** Epic format we are able to read. */                     private static final String VERSION_STRING50 = ";! output_format 5.0";
        /** Epic format we are able to read. */                     private static final String VERSION_STRING53 = ";! output_format 5.3";
        /** Epic separator char. */                                 private static final char separator = '.';

        private PrintStream err;
        private String urlName;
    
        /** Private constructor. */
        private EpicReader(OutputStream os, OutputStream eos, String urlName) throws IOException {
            this.err = new PrintStream(eos);
            this.stdOut = new DataOutputStream(new PrintStream(os));
            this.urlName = urlName;
            this.start();
        }

        public void run() {
            BTree.clearStats();
            try {
            URL fileURL = new URL("file:"+urlName);
            long startTime = System.currentTimeMillis();
            URLConnection urlCon = fileURL.openConnection();
            urlCon.setConnectTimeout(10000);
            urlCon.setReadTimeout(1000);
            String contentLength = urlCon.getHeaderField("content-length");
            fileLength = -1;
            try {
                fileLength = Long.parseLong(contentLength);
            } catch (Exception e) {}
            inputStream = urlCon.getInputStream();
            byteCount = 0;
            String firstLine = getLine();
            if (firstLine == null || !(firstLine.equals(VERSION_STRING50) || firstLine.equals(VERSION_STRING53))) {
                message("Unknown Epic Version: " + firstLine);
            }
            for (;;) {
                if (bufP >= bufL && readBuf()) break;
                int startLine = bufP;
                if (parseNumLineFast()) continue;
                bufP = startLine;
                String line = getLine();
                assert bufP <= bufL;
                if (line == null) break;
                parseNumLine(line);
            }
            rootCtx.writeSigs(this);
            /*
              writeContext("");
            */
            inputStream.close();
        
            long stopTime = System.currentTimeMillis();
            int numSignals = 0;
            for (EpicReaderSignal s: signalsByEpicIndex) {
                if (s == null) continue;
                numSignals++;
            }
            err.println((stopTime - startTime)/1000.0 + " sec to read " + byteCount + " bytes " + numSignals + " signals (max " + (signalsByEpicIndex.size() - 1) + " ) "+ stringIds.size() + " strings " +
                               timesC + " timepoints " +  eventsC + " events from " + urlName);
            BTree.dumpStats(err);
            writeOut();
            } catch (IOException e) { throw new RuntimeException(e); }
        }

        /**
         * Parses line from Epic file.
         */
        private void parseNumLine(String line) throws IOException {
            if (line.length() == 0) return;
            char ch = line.charAt(0);
            if (ch == '.') {
                String[] split = whiteSpace.split(line);
                if (split[0].equals(".index") && split.length == 4) {
                    byte type;
                    if (split[3].equals("v"))
                        type = 'V';
                    else if (split[3].equals("i"))
                        type = 'I';
                    else {
                        message("Unknown waveform type: " + line);
                        return;
                    }
                    String name = split[1];
                    int sigNum = atoi(split[2]);
                    while (signalsByEpicIndex.size() <= sigNum)
                        signalsByEpicIndex.add(null);
                
                    // name the signal
                    if (name.startsWith("v(") && name.endsWith(")")) {
                        name = name.substring(2, name.length() - 1);
                    } else if (name.startsWith("i(") && name.endsWith(")")) {
                        name = name.substring(2, name.length() - 1);
                    } else if (name.startsWith("i1(") && name.endsWith(")")) {
                        name = name.substring(3, name.length() - 1);
                    }
                    rootCtx.addSig(name, separator, type, sigNum);

                    EpicReaderSignal s = signalsByEpicIndex.get(sigNum);
                    if (s == null) {
                        String context  = name.indexOf(separator)==-1 ? ""   : name.substring(0, name.lastIndexOf(separator));
                        String basename = name.indexOf(separator)==-1 ? name : name.substring(name.lastIndexOf(separator)+1);
                        s = new EpicReaderSignal(sigNum, basename, context);
                        signalsByEpicIndex.set(sigNum, s);
                    }

                    /*
                      int lastSlashPos = name.lastIndexOf(separator);
                      String contextName = "";
                      if (lastSlashPos > 0) {
                      contextName = name.substring(0, lastSlashPos + 1);
                      }
                      name = name.substring(lastSlashPos + 1);
                      if (type == 'I') name = "i(" + name + ")";
                      writeContext(contextName);
                      stdOut.writeByte(type);
                      stdOut.writeInt(sigNum);
                      writeString(name);
                    **/
                } else if (split[0].equals(".vdd") && split.length == 2) {
                } else if (split[0].equals(".time_resolution") && split.length == 2) {
                    timeResolution = atof(split[1]) * 1e-9;
                } else if (split[0].equals(".current_resolution") && split.length == 2) {
                    currentResolution = atof(split[1]);
                } else if (split[0].equals(".voltage_resolution") && split.length == 2) {
                    voltageResolution = atof(split[1]);
                } else if (split[0].equals(".simulation_time") && split.length == 2) {
                } else if (split[0].equals(".high_threshold") && split.length == 2) {
                } else if (split[0].equals(".low_threshold") && split.length == 2) {
                } else if (split[0].equals(".nnodes") && split.length == 2) {
                } else if (split[0].equals(".nelems") && split.length == 2) {
                } else if (split[0].equals(".extra_nodes") && split.length == 2) {
                } else if (split[0].equals(".bus_notation") && split.length == 4) {
                } else if (split[0].equals(".hier_separator") && split.length == 2) {
                } else if (split[0].equals(".case") && split.length == 2) {
                } else {
                    message("Unrecognized Epic line: " + line);
                }
            } else if (ch >= '0' && ch <= '9') {
                String[] split = whiteSpace.split(line);
                int num = atoi(split[0]);
                if (split.length  > 1) {
                    putValue(num, atoi(split[1]));
                } else {
                    putTime(num);
                }
            } else if (ch == ';' || Character.isSpaceChar(ch)) {
            } else {
                message("Unrecognized Epic line: " + line);
            }
        }
    
        /**
         * Writes new context as diff to previous context.
         * @param s string with new context.
         */
        private void writeContext(String s) throws IOException {
            int matchSeps = 0;
            int pos = 0;
            matchLoop:
            while (matchSeps < contextStack.size()) {
                String si = contextStack.get(matchSeps);
                if (pos < s.length() && s.charAt(pos) == 'x')
                    pos++;
                if (pos + si.length() >= s.length() || s.charAt(pos + si.length()) != separator)
                    break;
                for (int k = 0; k < si.length(); k++)
                    if (s.charAt(pos + k) != si.charAt(k))
                        break matchLoop;
                matchSeps++;
                pos += si.length() + 1;
            }
            while (contextStack.size() > matchSeps) {
                stdOut.writeByte('U');
                contextStack.remove(contextStack.size() - 1);
            }
            assert contextStack.size() == matchSeps;
            while (pos < s.length()) {
                int indexOfSep = s.indexOf(separator, pos);
                assert indexOfSep >= pos;
                stdOut.writeByte('D');
                if (pos < indexOfSep && s.charAt(pos) == 'x')
                    pos++;
                String si = s.substring(pos, indexOfSep);
                writeString(si);
                contextStack.add(si);
                pos = indexOfSep + 1;
            }
            assert pos == s.length();
        }
    
        /**
         * Writes string to stdOut.
         * It writes its chronological number.
         * It writes string itself, if it is a new string.
         * @param s string to write.
         */
        void writeString(String s) throws IOException {
            if (s == null) {
                stdOut.writeInt(-1);
                return;
            }
            Integer i = stringIds.get(s);
            if (i != null) {
                stdOut.writeInt(i.intValue());
                return;
            }
            stdOut.writeInt(stringIds.size());
            i = new Integer(stringIds.size());
            s = new String(s); // To avoid long char array of substrings
            stringIds.put(s, i);
            stdOut.writeUTF(s);
        }
    
        /**
         * Fast routine to parse a line from buffer.
         * It either recognizes a simple line or rejects.
         * @return true if this method recognized a line.
         */
        private boolean parseNumLineFast() {
            final int MAX_DIGITS = 9;
            if (bufP + (MAX_DIGITS*2 + 4) >= bufL) return false;
            int ch = buf[bufP++];
            if (ch < '0' || ch > '9') return false;
            int num1 = ch - '0';
            ch = buf[bufP++];
            for (int lim = bufP + (MAX_DIGITS - 1); '0' <= ch && ch <= '9' && bufP < lim; ch = buf[bufP++])
                num1 = num1*10 + (ch - '0');
            boolean twoNumbers = false;
            int num2 = 0;
            if (ch == ' ') {
                ch = buf[bufP++];
                boolean sign = false;
                if (ch == '-') {
                    sign = true;
                    ch = buf[bufP++];
                }
                if (ch < '0' || ch > '9') return false;
                num2 = ch - '0';
                ch = buf[bufP++];
                for (int lim = bufP + (MAX_DIGITS - 1); '0' <= ch && ch <= '9' && bufP < lim; ch = buf[bufP++])
                    num2 = num2*10 + (ch - '0');
                if (sign) num2 = -num2;
                twoNumbers = true;
            }
            if (ch == '\n') {
            } else if (ch == '\r') {
                if (buf[bufP] == '\n')
                    bufP++;
            } else {
                return false;
            }
            if (twoNumbers)
                putValue(num1, num2);
            else
                putTime(num1);
            lineNum++;
            return true;
        }
    
        /**
         * Sets new current time.
         * This current time will be used in PutBValue method.
         * @param time time as int value.
         */
        private void putTime(int time) {
            timesC++;
            curTime = Math.max(curTime, time);
        }
    
        /**
         * Puts event into packed waveforms.
         * @param sigNum Epic signal index of signal.
         * @param value new value of signal.
         */
        private void putValue(int sigNum, int value) {
            EpicReaderSignal s = signalsByEpicIndex.get(sigNum);
            if (s == null) {
                message("Signal " + sigNum + " not defined");
                return;
            }
            s.putEvent(curTime, value);
            eventsC++;
        }
    
        /**
         * Gets new line from buffer.
         * @return String line read.
         */
        private String getLine() throws IOException {
            builder.setLength(0);
            for (;;) {
                while (bufP < bufL) {
                    int ch = buf[bufP++] & 0xff;
                    if (ch == '\n') {
                        return builder.toString();
                    }
                    if (ch == '\r') {
                        if (bufP == bufL) readBuf();
                        if (bufP < bufL && buf[bufP] == '\n')
                            bufP++;
                        return builder.toString();
                    }
                    builder.append((char)ch);
                }
                if (readBuf()) {
                    if (builder.length() == 0) return null;
                    lineNum++;
                    return builder.toString();
                }
            }
        }
    
        /**
         * ASCII to double.
         * @param s ASCII string. 
         * @return double value of string.
         */
        private double atof(String s) {
            double value = 0;
            try {
                value = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                message("Bad float format: " + s);
            }
            return value;
        }
    
        /**
         * ASCII to int.
         * @param s ASCII string. 
         * @return int value of string.
         */
        private int atoi(String s) {
            int value = 0;
            try {
                value = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                message("Bad integer format: " + s);
            }
            return value;
        }
    
        /**
         * Reads buffer from Epic file.
         */
        private boolean readBuf() throws IOException {
            assert bufP == bufL;
            bufP = bufL = 0;
            bufL = inputStream.read(buf, 0, buf.length);
            if (bufL <= 0) {
                bufL = 0;
                return true;
            }
            byteCount += bufL;
            showProgress(fileLength != 0 ? byteCount/(double)fileLength : 0);
            return false;
        }
    
        /**
         * Writes resolutions, signalInfo and fileName to stdOut.
         */
        private void writeOut() throws IOException {
            File tempFile = null;
            BufferedOutputStream waveStream = null;
            boolean ok = false;
            try {
                long startTime = System.currentTimeMillis();
                tempFile = File.createTempFile("elec", ".epic");
                waveStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            
                showProgressNote("Writing " + tempFile);
                stdOut.writeByte('F');

                stdOut.writeDouble(timeResolution);
                stdOut.writeDouble(voltageResolution);
                stdOut.writeDouble(currentResolution);
                stdOut.writeDouble(curTime*timeResolution);
                stdOut.writeInt(-1);
                waveStream.close();
            
                stdOut.writeUTF(tempFile.toString());
                stdOut.close();
                ok = true;
                long stopTime = System.currentTimeMillis();
                err.println((stopTime - startTime)/1000.0 + " sec to write " + tempFile.length() + " bytes to " + tempFile);
            } finally {
                if (ok)
                    return;
                if (waveStream != null)
                    waveStream.close();
                if (tempFile != null)
                    tempFile.delete();
            }
        }
    
        /**
         * Puts progress mark into stdErr.
         */
        private void showProgress(double ratio) {
            byte progress = (byte)(ratio*100);
            if (progress == lastProgress) return;
            err.println("**PROGRESS " + progress);
            lastProgress = progress;
        }
    
        /**
         * Puts progress not mark into stdErr.
         */
        private void showProgressNote(String note) {
            err.println("**PROGRESS !" + note);
            lastProgress = 0;
        }

        /**
         * Puts message int stdErr.
         */
        private void message(String s) {
            err.println(s + " in line " + (lineNum + 1));
        }
    }

    class EpicReaderContext {
        private ArrayList<EpicReaderSig> signals = new ArrayList<EpicReaderSig>();
        private LinkedHashMap<String,EpicReaderContext> subs = new LinkedHashMap<String,EpicReaderContext>();

        EpicReaderSig addSig(String path, char separator, byte type, int sigNum) {
            int indexOfSep = path.indexOf(separator);
            if (indexOfSep == -1) {
                if (type == 'I')
                    path = "i(" + path + ")";
                EpicReaderSig sig = new EpicReaderSig(type, path, sigNum);
                signals.add(sig);
                return sig;
            }
            String subName = path.substring(0, indexOfSep);
            if (subName.length() > 0 && subName.charAt(0) == 'x')
                subName = subName.substring(1);
            EpicReaderContext ctx = subs.get(subName);
            if (ctx == null) {
                ctx = new EpicReaderContext();
                subs.put(subName, ctx);
            }
            path = path.substring(indexOfSep + 1);
            return ctx.addSig(path, separator, type, sigNum);
        }

        void writeSigs(EpicReader reader) throws IOException {
            DataOutputStream stdOut = reader.stdOut;
            for (EpicReaderSig sig: signals) {
                stdOut.writeByte(sig.type);
                stdOut.writeInt(sig.sigNum);
                reader.writeString(sig.name);
            }
            for (Map.Entry<String,EpicReaderContext> e: subs.entrySet()) {
                String subName = e.getKey();
                EpicReaderContext sub = e.getValue();
                stdOut.writeByte('D');
                reader.writeString(subName);
                sub.writeSigs(reader);
                stdOut.writeByte('U');
            }
        }
    }

    class EpicReaderSig {
        final byte type;
        final String name;
        final int sigNum;

        EpicReaderSig(byte type, String name, int sigNum) {
            this.type = type;
            this.name = name;
            this.sigNum = sigNum;
        }

    }

    /**
     * This class is a buffer to pack waveform of Epic signal.
     */
    class EpicReaderSignal {
        /** Time of last event.*/               int lastT;
        /** Value of last event. */             int lastV;
        /** Minimal value among events. */      int minV = Integer.MAX_VALUE;
        /** Maximal value among events. */      int maxV = Integer.MIN_VALUE;

        /** Packed waveform. */                 byte[] waveform = new byte[512];
        /** Count of bytes used in waveform. */ int len;

        ScalarSignal signal;
        int sigNum;
        int count = 0;

        public EpicReaderSignal(int sigNum, String name, String context) {
            this.sigNum = sigNum;
            this.signal = new AnalogSignal(epicAnalysis, name, context);
        }

        public Signal getBWaveform() {
            return signal;
        }

        /**
         * Puts event into waveform. 
         * @param t time of event.
         * @param v value of event.
         */
        void putEvent(int t, int v) {
            double value = v * voltageResolution;
            count++;
            signal.addSample(new Double(t*timeResolution), new ScalarSample(value));
        }

        /**
         * Packes unsigned int.
         * @param value value to pack.
         */
        void putUnsigned(int value) {
            if (value < 0xC0) {
                ensureCapacity(1);
                putByte(value);
            } else if (value < 0x3F00) {
                ensureCapacity(2);
                putByte((value + 0xC000) >> 8);
                putByte(value);
            } else {
                ensureCapacity(5);
                putByte(0xFF);
                putByte(value >> 24);
                putByte(value >> 16);
                putByte(value >> 8);
                putByte(value);
            }
        }
    
        /**
         * Packes signed int.
         * @param value value to pack.
         */
        void putSigned(int value) {
            if (-0x60 <= value && value < 0x60) {
                ensureCapacity(1);
                putByte(value + 0x60);
            } else if (-0x1F00 <= value && value < 0x2000) {
                ensureCapacity(2);
                putByte((value + 0xDF00) >> 8);
                putByte(value);
            } else {
                ensureCapacity(5);
                putByte(0xFF);
                putByte(value >> 24);
                putByte(value >> 16);
                putByte(value >> 8);
                putByte(value);
            }
        }
    
        /**
         * Unpackes waveform.
         * @return array with time/value pairs.
         */    
        int[] getWaveform() {
            int count = 0;
            for (int i = 0; i < len; count++) {
                int l;
                int b = waveform[i++] & 0xff;
                if (b < 0xC0)
                    l = 0;
                else if (b < 0xFF)
                    l = 1;
                else
                    l = 4;
                i += l;
                b = waveform[i++] & 0xff;
                if (b < 0xC0)
                    l = 0;
                else if (b < 0xFF)
                    l = 1;
                else
                    l = 4;
                i += l;
            }
            int[] w = new int[count*2];
            count = 0;
            int t = 0;
            int v = 0;
            for (int i = 0; i < len; count++) {
                int l;
                int b = waveform[i++] & 0xff;
                if (b < 0xC0) {
                    l = 0;
                } else if (b < 0xFF) {
                    l = 1;
                    b -= 0xC0;
                } else {
                    l = 4;
                }
                while (l > 0) {
                    b = (b << 8) | waveform[i++] & 0xff; 
                    l--;
                }
                w[count*2] = t = t + b;
            
                b = waveform[i++] & 0xff;
                if (b < 0xC0) {
                    l = 0;
                    b -= 0x60;
                } else if (b < 0xFF) {
                    l = 1;
                    b -= 0xDF;
                } else {
                    l = 4;
                }
                while (l > 0) {
                    b = (b << 8) | waveform[i++] & 0xff; 
                    l--;
                }
                w[count*2 + 1] = v = v + b;
            }
            assert count*2 == w.length;
            return w;
        }
    
        /**
         * Puts byte int packed array.
         * @param value byte to but.
         */
        void putByte(int value) { waveform[len++] = (byte)value; }
    
        /**
         * Ensures that it is possible to add specified number of bytes to packed waveform.
         * @param l number of bytes.
         */
        void ensureCapacity(int l) {
            if (len + l <= waveform.length) return;
            byte[] newWaveform = new byte[waveform.length*3/2];
            System.arraycopy(waveform, 0, newWaveform, 0, waveform.length);
            waveform = newWaveform;
            assert len + l <= waveform.length;
        }
    }

}

/**
 * Class to define a set of simulation data producet by Epic
 * simulators, using the new disk-indexed format.

 * This class differs from Anylisis base class by less memory consumption.
 * Waveforms are stored in a file in packed form. They are loaded to memory by
 * denand. Hierarchical structure is reconstructed from Epic flat names into Context objects.
 * EpicSignals don't store signalContex strings, EpicAnalysis don't have signalNames hash map.
 * Elements of Context are EpicTreeNodes. They partially implements interface javax.swing.tree.TreeNode .
 */
public static class EpicAnalysis extends AnalogAnalysis {
    
    /** Separator in Epic signal names. */              static final char separator = '.';
    
    /** Type of voltage signal. */                      static final byte VOLTAGE_TYPE = 1;
    /** Type of current signal. */                      static final byte CURRENT_TYPE = 2;
    
    /** Fake context which denotes voltage signal. */   static final Context VOLTAGE_CONTEXT = new Context(VOLTAGE_TYPE);
    /** Fake context which denotes current signal. */   static final Context CURRENT_CONTEXT = new Context(CURRENT_TYPE);
    
    /** File name of File with waveforms. */            private File waveFileName;
    /** Opened file with waveforms. */                  private RandomAccessFile waveFile;
    /** Offsets of packed waveforms in the file. */     int[] waveStarts;
    /** Lengths of packed waveforms in the file. */     int[] waveLengths;
   
    /** Time resolution of integer time unit. */        private double timeResolution;
    /** Voltage resolution of integer voltage unit. */  private double voltageResolution;
    /** Current resolution of integer current unit. */  private double currentResolution;
    /** Simulation time. */                             private double maxTime;
    
    /** Unmodifieable view of signals list. */          private List<AnalogSignal> signalsUnmodifiable;
    /** a set of voltage signals. */                    private BitSet voltageSignals = new BitSet(); 
    /** Top-most context. */                            private Context rootContext;
    /** Hash of all contexts. */                        private Context[] contextHash = new Context[1];
    /** Count of contexts in the hash. */               private int numContexts = 0;

    /**
     * Package-private constructor.
     * @param sd Stimuli.
     */
    EpicAnalysis(Stimuli sd) {
        super(sd, AnalogAnalysis.ANALYSIS_TRANS, false);
        signalsUnmodifiable = Collections.unmodifiableList(super.getSignals());
    }
    
    /**
     * Set time resolution of this EpicAnalysis.
     * @param timeResolution time resolution in nanoseconds.
     */
    void setTimeResolution(double timeResolution) { this.timeResolution = timeResolution; }
    
    /**
     * Set voltage resolution of this EpicAnalysys.
     * @param voltageResolution voltage resolution in volts.
     */
    void setVoltageResolution(double voltageResolution) { this.voltageResolution = voltageResolution; }
    
    /**
     * Set current resolution of this EpicAnalysys.
     * @param currentResolution in amperes ( milliamperes? ).
     */
    void setCurrentResolution(double currentResolution) { this.currentResolution = currentResolution; }
    
    double getValueResolution(int signalIndex) {
        return voltageSignals.get(signalIndex) ? voltageResolution : currentResolution;
    }
    
    /**
     * Set simulation time of this EpicAnalysis.
     * @param maxTime simulation time in nanoseconds.
     */
    void setMaxTime(double maxTime) { this.maxTime = maxTime; }
    
    /**
     * Set root context of this EpicAnalysys.
     * @param context root context.
     */
    void setRootContext(Context context) { rootContext = context; }
    
    /**
     * Set waveform file of this EpicAnalysis.
     * @param waveFileName File object with name of waveform file.
     */
    void setWaveFile(File waveFileName) throws FileNotFoundException {
        this.waveFileName = waveFileName;
        waveFileName.deleteOnExit();
        waveFile = new RandomAccessFile(waveFileName, "r");
    }
    
    /**
     * Free allocated resources before closing.
     */
    @Override
        public void finished() {
        try {
            waveFile.close();
            waveFileName.delete();
        } catch (IOException e) {
        }
    }
    
	/**
	 * Method to quickly return the signal that corresponds to a given Network name.
     * This method overrides the m,ethod from Analysis class.
     * It doesn't use signalNames hash map.
	 * @param netName the Network name to find.
	 * @return the Signal that corresponds with the Network.
	 * Returns null if none can be found.
	 */
    @Override
        public AnalogSignal findSignalForNetworkQuickly(String netName) {
        AnalogSignal old = super.findSignalForNetworkQuickly(netName);
        
        String lookupName = TextUtils.canonicString(netName);
        int index = searchName(lookupName);
        if (index < 0) {
            assert old == null;
            return null;
        }
        AnalogSignal sig = signalsUnmodifiable.get(index);
        return sig;
    }

    public List<AnalogSignal> getSignalsFromExtractedNet(Signal ws) {
        ArrayList<AnalogSignal> ret = new ArrayList<AnalogSignal>();
        ret.add((AnalogSignal)ws);
        return ret;
    }


    /**
     * Public method to build tree of EpicTreeNodes.
     * Root of the tree us EpicRootTreeNode objects.
     * Deeper nodes are EpicTreeNode objects.
     * @param analysis name of root DefaultMutableTreeNode.
     * @return root EpicRootTreeNode of the tree.
     */
	public DefaultMutableTreeNode getSignalsForExplorer(String analysis) {
		DefaultMutableTreeNode signalsExplorerTree = new EpicRootTreeNode(this, analysis);
        return signalsExplorerTree;
    }

   
    /**
     * Returns EpicSignal by its TreePath.
     * @param treePath specified TreePath.
     * @return EpicSignal or null.
     */
    public static AnalogSignal getSignal(TreePath treePath) {
        Object[] path = treePath.getPath();
        int i = 0;
        while (i < path.length && !(path[i] instanceof EpicRootTreeNode))
            i++;
        if (i >= path.length) return null;
        EpicAnalysis an = ((EpicRootTreeNode)path[i]).an;
        i++;
        int index = 0;
        for (; i < path.length; i++) {
            EpicTreeNode tn = (EpicTreeNode)path[i];
            index += tn.nodeOffset;
            if (tn.isLeaf())
                return (AnalogSignal)an.signalsUnmodifiable.get(index);
        }
        return null;
    }
    
	/**
	 * Method to get the list of signals in this Simulation Data object.
     * List is unmodifieable.
	 * @return a List of signals.
	 */
    @Override
        public List<AnalogSignal> getSignals() { return signalsUnmodifiable; }

    /**
     * This methods overrides Analysis.nameSignal.
     * It doesn't use Analisys.signalNames to use less memory.
     */
    @Override
        public void nameSignal(AnalogSignal ws, String sigName) {}
    
    /**
     * Finds signal index of AnalogSignal with given full name.
     * @param name full name.
     * @return signal index of AnalogSignal in unmodifiable list of signals or -1.
     */
    int searchName(String name) {
        Context context = rootContext;
        for (int pos = 0, index = 0;;) { 
            int indexOfSep = name.indexOf(separator, pos);
            if (indexOfSep < 0) {
                EpicTreeNode sig = context.sigs.get(name.substring(pos));
                return sig != null ? index + sig.nodeOffset : -1;
            }
            EpicTreeNode sub = context.subs.get(name.substring(pos, indexOfSep));
            if (sub == null) return -1;
            context = sub.context;
            pos = indexOfSep + 1;
            index += sub.nodeOffset;
        }
    }
        
    /**
     * Makes fullName or signalContext of signal with specified index.
     * @param index signal index.
     * @param full true to request full name.
     */
    String makeName(int index, boolean full) {
        StringBuilder sb = new StringBuilder();
        Context context = rootContext;
        if (context == null) return null;
        if (index < 0 || index >= context.treeSize)
            throw new IndexOutOfBoundsException();
        for (;;) {
            int localIndex = context.localSearch(index);
            EpicTreeNode tn = context.nodes[localIndex];
            Context subContext = tn.context;
            if (subContext.isLeaf()) {
                if (full)
                    sb.append(tn.name);
                else if (sb.length() == 0)
                    return null;
                else
                    sb.setLength(sb.length() - 1);
                return sb.toString();
            }
            sb.append(tn.name);
            sb.append(separator);
            index -= tn.nodeOffset;
            context = subContext;
        }
    }
    
    /**
     * Method to get Fake context of AnalogSignal.
     * @param byte physical type of AnalogSignal.
     * @return Fake context.
     */ 
    static Context getContext(byte type) {
        switch (type) {
            case VOLTAGE_TYPE:
                return VOLTAGE_CONTEXT;
            case CURRENT_TYPE:
                return CURRENT_CONTEXT;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Method to get Context by list of names and list of contexts.
     * @param strings list of names.
     * @param contexts list of contexts.
     */
    Context getContext(ArrayList<String> strings, ArrayList<Context> contexts) {
        assert strings.size() == contexts.size();
        int hashCode = Context.hashValue(strings, contexts);
        
        int i = hashCode & 0x7FFFFFFF;
        i %= contextHash.length;
        for (int j = 1; contextHash[i] != null; j += 2) {
            Context c = contextHash[i];
            if (c.hashCode == hashCode && c.equals(strings, contexts)) return c;
            
            i += j;
            if (i >= contextHash.length) i -= contextHash.length;
        }
        
        // Need to create new context.
        if (numContexts*2 <= contextHash.length - 3) {
            // create a new CellUsage, if enough space in the hash
            Context c = new Context(strings, contexts, hashCode);
            contextHash[i] = c;
            numContexts++;
            return c;
        }
        // enlarge hash if not
        rehash();
        return getContext(strings, contexts);
    }
    
    /**
     * Rehash the context hash.
     * @throws IndexOutOfBoundsException on hash overflow.
     */
    private void rehash() {
        int newSize = numContexts*2 + 3;
        if (newSize < 0) throw new IndexOutOfBoundsException();
        Context[] newHash = new Context[GenMath.primeSince(newSize)];
        for (int k = 0; k < contextHash.length; k++) {
            Context c = contextHash[k];
            if (c == null) continue;
            int i = c.hashCode & 0x7FFFFFFF;
            i %= newHash.length;
            for (int j = 1; newHash[i] != null; j += 2) {
                i += j;
                if (i >= newHash.length) i -= newHash.length;
            }
            newHash[i] = c;
        }
        contextHash = newHash;
    }

    HashMap<Integer, Signal> loadWaveformCache =
        new HashMap<Integer, Signal>();

    protected Signal[] loadWaveforms(AnalogSignal signal) { return new Signal[] { signal }; }

    


    
    /************************* EpicRootTreeNode *********************************************
     * This class is for root node of EpicTreeNode tree.
     * It contains children from rootContext of specified EpicAnalysis.
     */
    private static class EpicRootTreeNode extends DefaultMutableTreeNode {
        /** EpicAnalysis which owns the tree. */    private EpicAnalysis an;
        
        /**
         * Private constructor.
         * @param an specified EpicAnalysy.
         * @paran name name of bwq EpicRootTreeNode
         */
        private EpicRootTreeNode(EpicAnalysis an, String name) {
            super(name);
            this.an = an;
            Vector<EpicTreeNode> children = new Vector<EpicTreeNode>();

            //    		// make a list of signal names with "#" in them
            //    		Map<String,DefaultMutableTreeNode> sharpMap = new HashMap<String,DefaultMutableTreeNode>();
            //            for (EpicTreeNode tn: an.rootContext.sortedNodes)
            //    		{
            //    			String sigName = tn.name;
            //    			int hashPos = sigName.indexOf('#');
            //    			if (hashPos <= 0) continue;
            //    			String sharpName = sigName.substring(0, hashPos);
            //    			DefaultMutableTreeNode parent = sharpMap.get(sharpName);
            //    			if (parent == null)
            //    			{
            //    				parent = new DefaultMutableTreeNode(sharpName + "#");
            //    				sharpMap.put(sharpName, parent);
            //    				this.add(parent);
            //    			}
            //    		}
            //
            //    		// add all signals to the tree
            //            for (EpicTreeNode tn: an.rootContext.sortedNodes)
            //    		{
            //    			String nodeName = null;
            //    			String sigName = tn.name;
            //    			int hashPos = sigName.indexOf('#');
            //    			if (hashPos > 0)
            //    			{
            //    				// force a branch with the proper name
            //    				nodeName = sigName.substring(0, hashPos);
            //    			} else
            //    			{
            //    				// if this is the pure name of a hash set, force a branch
            //    				String pureSharpName = sigName;
            //    				if (sharpMap.get(pureSharpName) != null)
            //    					nodeName = pureSharpName;
            //    			}
            //    			if (nodeName != null)
            //    			{
            //    				DefaultMutableTreeNode parent = sharpMap.get(nodeName);
            //    				parent.add(new DefaultMutableTreeNode(tn));
            //    			} else
            //    			{
            //                    children.add(tn);
            //    			}
            //    		}

            for (EpicTreeNode tn: an.rootContext.sortedNodes)
                children.add(tn);
            this.children = children;
        }

        /**
         * Returns the index of the specified child in this node's child array.
         * If the specified node is not a child of this node, returns
         * <code>-1</code>.  This method performs a linear search and is O(n)
         * where n is the number of children.
         *
         * @param	aChild	the TreeNode to search for among this node's children
         * @exception	IllegalArgumentException	if <code>aChild</code>
         *							is null
         * @return	an int giving the index of the node in this node's child
         *          array, or <code>-1</code> if the specified node is a not
         *          a child of this node
         */
        public int getIndex(TreeNode aChild) {
            try {
                EpicTreeNode tn = (EpicTreeNode)aChild;
                if (getChildAt(tn.sortedIndex) == tn)
                    return tn.sortedIndex;
            } catch (Exception e) {
                if (aChild == null)
                    throw new IllegalArgumentException("argument is null");
            }
            return -1;
        }
    }
    
    /************************* Context *********************************************
     * This class denotes a group of siignals. It may have a few instance in signal tree.
     */
    static class Context {
        /**
         * Type of context.
         * it is nonzero for leaf context and it is zero for non-leaf contexts.
         */
        private final byte type;
        /**
         * Hash code of this Context
         */
        private final int hashCode;
        /**
         * Nodes of this Context in chronological order.
         */
        private final EpicTreeNode[] nodes;
        /**
         * Nodes of this Context sorted by type and name.
         */
        private final EpicTreeNode[] sortedNodes;
        /**
         * Flat number of signals in tree whose root is this context.
         */
        private final int treeSize;
        
        /**
         * Map from name of subcontext to EpicTreeNode.
         */
        private final HashMap<String,EpicTreeNode> subs = new HashMap<String,EpicTreeNode>();
        /**
         * Map from name of leaf npde to EpicTreeNode.
         */
        private final HashMap<String,EpicTreeNode> sigs = new HashMap<String,EpicTreeNode>();
 
        /**
         * Constructor of leaf fake context.
         */
        private Context(byte type) {
            assert type != 0;
            this.type = type;
            assert isLeaf();
            hashCode = type;
            nodes = null;
            sortedNodes = null;
            treeSize = 1;
        }
        
        /**
         * Constructor of real context.
         * @param names list of names.
         * @param contexts list of contexts.
         * @param hashCode precalculated hash code of new Context.
         */
        private Context(ArrayList<String> names, ArrayList<Context> contexts, int hashCode) {
            assert names.size() == contexts.size();
            type = 0;
            this.hashCode = hashCode;
            nodes = new EpicTreeNode[names.size()];
            int treeSize = 0;
            for (int i = 0; i < nodes.length; i++) {
                String name = names.get(i);
                Context subContext = contexts.get(i);
                EpicTreeNode tn = new EpicTreeNode(i, name, subContext,treeSize);
                nodes[i] = tn;
                String canonicName = TextUtils.canonicString(name);
                if (subContext.isLeaf())
                    sigs.put(canonicName, tn);
                else
                    subs.put(canonicName, tn);
                treeSize += subContext.treeSize;
            }
            sortedNodes = nodes.clone();
            Arrays.sort(sortedNodes, TREE_NODE_ORDER);
            for (int i = 0; i < sortedNodes.length; i++)
                sortedNodes[i].sortedIndex = i;
            this.treeSize = treeSize;
        }
        
        /**
         * Returns true if this context is fake leaf context.
         * @return true if this context is fake leaf context.
         */
        boolean isLeaf() { return type != 0; }
        
        /**
         * Returns true if contenst of this Context is equal to specified lists.
         * @returns true if contenst of this Context is equal to specified lists.
         */
        private boolean equals(ArrayList<String> names, ArrayList<Context> contexts) {
            int len = nodes.length;
            if (names.size() != len || contexts.size() != len) return false;
            for (int i = 0; i < len; i++) {
                EpicTreeNode tn = nodes[i];
                if (names.get(i) != tn.name || contexts.get(i) != tn.context) return false;
            }
            return true;
        }
        
        /**
         * Returns hash code of this Context.
         * @return hash code of this Context.
         */
        public int hashCode() { return hashCode; }
        
        /**
         * Private method to calculate hash code.
         * @param names list of names.
         * @param contexts list of contexts.
         * @return hash code
         */
        private static int hashValue(ArrayList<String> names, ArrayList<Context> contexts) {
            assert names.size() == contexts.size();
            int hash = 0;
            for (int i = 0; i < names.size(); i++)
                hash = hash * 19 + names.get(i).hashCode()^contexts.get(i).hashCode();
            return hash;
        }
        
        /**
         * Searches an EpicTreeNode in this context where signal with specified flat offset lives.
         * @param offset flat offset of AnalogSignal.
         * @return index of EpicTreeNode in this Context or -1.
         */
        private int localSearch(int offset) {
            assert offset >= 0 && offset < treeSize;
            assert nodes.length > 0;
            int l = 1;
            int h = nodes.length - 1;
            while (l <= h) {
                int m = (l + h) >> 1;
                if (nodes[m].nodeOffset <= offset)
                    l = m + 1;
                else
                    h = m - 1;
            }
            return h;
        }
    }
    
    /************************* EpicTreeNode *********************************************
     * This class denotes an element of a Context.
     * It partially implements TreeNode (without getParent).
     */
    public static class EpicTreeNode implements TreeNode {
        //        private final int chronIndex;
        private final String name;
        private final Context context;
        private final int nodeOffset;
        private int sortedIndex;

        /**
         * Private constructor.
         */
        private EpicTreeNode(int chronIndex, String name, Context context, int nodeOffset) {
            //            this.chronIndex = chronIndex;
            this.name = name;
            this.context = context;
            this.nodeOffset = nodeOffset;
        }
        
        /**
         * Returns the child <code>TreeNode</code> at index
         * <code>childIndex</code>.
         */
        public TreeNode getChildAt(int childIndex) {
            try {
                return context.sortedNodes[childIndex];
            } catch (NullPointerException e) {
                throw new ArrayIndexOutOfBoundsException("node has no children");
            }
        }
        
        /**
         * Returns the number of children <code>TreeNode</code>s the receiver
         * contains.
         */
        public int getChildCount() {
            return isLeaf() ? 0 : context.sortedNodes.length;
        }
        
        /**
         * Returns the parent <code>TreeNode</code> of the receiver.
         */
        public TreeNode getParent() {
            throw new UnsupportedOperationException();
        }
        
        /**
         * Returns the index of <code>node</code> in the receivers children.
         * If the receiver does not contain <code>node</code>, -1 will be
         * returned.
         */
        public int getIndex(TreeNode node) {
            try {
                EpicTreeNode tn = (EpicTreeNode)node;
                if (context.nodes[tn.sortedIndex] == tn)
                    return tn.sortedIndex;
            } catch (Exception e) {
                if (node == null)
                    throw new IllegalArgumentException("argument is null");
            }
            return -1;
        }
        
        /**
         * Returns true if the receiver allows children.
         */
        public boolean getAllowsChildren() { return !isLeaf(); }
        
        /**
         * Returns true if the receiver is a leaf.
         */
        public boolean isLeaf() { return context.type != 0; }
        
        /**
         * Returns the children of the receiver as an <code>Enumeration</code>.
         */
        public Enumeration children() {
            if (isLeaf())
                return DefaultMutableTreeNode.EMPTY_ENUMERATION;
            return new Enumeration<EpicTreeNode>() {
                int count = 0;
                
                public boolean hasMoreElements() {
                    return count < context.nodes.length;
                }
                
                public EpicTreeNode nextElement() {
                    if (count < context.nodes.length)
                        return context.nodes[count++];
                    throw new NoSuchElementException("Vector Enumeration");
                }
            };
        }
        
        public String toString() { return name; }
    }
    
    /**
     * Comparator which compares EpicTreeNodes by type and by name.
     */
    private static Comparator<EpicTreeNode> TREE_NODE_ORDER = new Comparator<EpicTreeNode>() {
      
        public int compare(EpicTreeNode tn1, EpicTreeNode tn2) {
            int cmp = tn1.context.type - tn2.context.type;
            if (cmp != 0) return cmp;
            return TextUtils.STRING_NUMBER_ORDER.compare(tn1.name, tn2.name);
        }
    };
    
}

}