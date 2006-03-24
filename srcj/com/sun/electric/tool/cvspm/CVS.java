/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CVSMenu.java
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


package com.sun.electric.tool.cvspm;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Exec;
import com.sun.electric.tool.user.dialogs.ModalCommandDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.DELIB;
import com.sun.electric.tool.Job;

import javax.swing.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Mar 10, 2006
 * Time: 11:47:01 AM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Only one CVS command can be working at a time.  While CVS is running,
 * the GUI will be tied up.  This is to
 */
public class CVS {

    public static boolean isEnabled() { return true; }

    public static void checkoutFromRepository() {
        // get list of modules in repository
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        runModalCVSCommand("-n checkout -c", "Getting modules in repository...", User.getWorkingDirectory(), out);

        // this must come after the runModal command
        LineNumberReader result = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray())));

        List<String> modules = new ArrayList<String>();
        for (;;) {
            String line = null;
            try {
                line = result.readLine();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                return;
            }
            if (line == null) break;
            line = line.trim();
            if (line.equals("")) continue;

            String[] parts = line.split("\\s");
            modules.add(parts[0]);
        }
        if (modules.size() == 0) {
            System.out.println("No modules in CVS!");
            return;
        }
        Object ret = JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Choose Module to Checkout",
                "Checkout Module...", JOptionPane.QUESTION_MESSAGE, null, modules.toArray(), modules.toArray()[0]);
        if (ret == null) {
            // user cancelled
            return;
        }
        String module = (String)ret;
        // choose directory to checkout to
        String directory = OpenFile.chooseDirectory("Choose directory in which to checkout module "+module);
        if (directory == null) {
            // user cancelled
            return;
        }
        // checkout module to current working directory
        String cmd = "checkout "+module;
        runModalCVSCommand(cmd, "Checking out '"+module+"' to "+directory, directory, System.out);
        System.out.println("Checked out '"+module+"' to '"+directory+"'");
    }

    // ------------------------------------------------------------------------

    /**
     * This will run a CVS command in-thread; i.e. the current thread will
     * block until the CVS command completes.
     * @param cmd the command to run
     * @param comment the message to display on the dialog
     * @param workingDir the directory in which to run the CVS command
     * (null for current working directory). I recommend you specify
     * this as the current library dir.
     * @param out where the result of the command gets printed. May be
     * a ByteArrayOutputStream for storing it, or just System.out for
     * printing it.
     */
    public static void runCVSCommand(String cmd, String comment, String workingDir, OutputStream out) {
        String specifyRepository = "";
        if (!getRepository().equals("")) specifyRepository = " -d"+getRepository();
        String run = getCVSProgram() + specifyRepository +" "+cmd;

        System.out.println(comment+": cvs "+cmd);
        Exec e = new Exec(run, null, new File(workingDir), out, out);
        e.run();
    }

    /**
     * Run this command if you have quotes ("") in your command that delimit a single
     * argument. Normally exec breaks up the string command by whitespace, ignoring
     * quotes. This command will preprocess the command to ensure it thinks of the
     * string in quotes as one argument.
     * @param cmd the command to run
     * @param comment the message to display on the dialog
     * @param workingDir the directory in which to run the CVS command
     * (null for current working directory). I recommend you specify
     * this as the current library dir.
     * @param out where the result of the command gets printed. May be
     * a ByteArrayOutputStream for storing it, or just System.out for
     * printing it.
     */
    static void runCVSCommandWithQuotes(String cmd, String comment, String workingDir, OutputStream out) {
        String specifyRepository = "";
        if (!getRepository().equals("")) specifyRepository = " -d "+getRepository();

        cmd = getCVSProgram() + specifyRepository + " " + cmd;

        // break command into separate arguments
        List<String> execparts = new ArrayList<String>();
        String [] quoteParts = cmd.split("\"");
        for (int i=0; i<quoteParts.length; i++) {
            if (i % 2 == 0) {
                // evens are not enclosed in quotes
                String [] parts = quoteParts[i].trim().split("\\s+");
                for (int j=0; j<parts.length; j++)
                    execparts.add(parts[j]);
            } else {
                // odds are enclosed in quotes
                execparts.add(quoteParts[i]);
            }

        }
        String [] exec = new String[execparts.size()];
        for (int i=0; i<exec.length; i++) {
            exec[i] = execparts.get(i);
            // debug
            //System.out.println(i+": "+exec[i]);
        }

        System.out.println(comment+": "+cmd);
        Exec e = new Exec(exec, null, new File(workingDir), out, out);
        e.run();
    }

    /**
     * This will run a CVS command in a separate Thread and block the GUI until the command
     * completes, or until the user hits 'cancel', which will try to
     * terminate the external command.  This method returns after
     * the cvs command has completed.
     * @param cmd the command to run
     * @param comment the message to display on the dialog
     * @param workingDir the directory in which to run the CVS command
     * (null for current working directory). I recommend you specify
     * this as the current library dir.
     * @param out where the result of the command gets printed. May be
     * a ByteArrayOutputStream for storing it, or just System.out for
     * printing it.
     */
    public static void runModalCVSCommand(String cmd, String comment, String workingDir, OutputStream out) {
        String run = getCVSProgram() + " -d"+getRepository()+" "+cmd;

        Exec e = new Exec(run, null, new File(workingDir), out, out);
        // add a listener to get rid of the modal dialog when the command finishes
        String message = "Running: "+run;
        JFrame frame = TopLevel.getCurrentJFrame();
        ModalCommandDialog dialog = new ModalCommandDialog(frame, true, e, message, comment);
        dialog.setVisible(true);
    }

    // -------------------------- Utility Commands --------------------------

    public static void testModal() {
        runModalCVSCommand("-n history -c -a", "testing command", User.getWorkingDirectory(), System.out);
    }

    /**
     * Get the file for the given Cell, assuming the library is
     * in DELIB format.  If not, returns null.
     * @param cell
     * @return
     */
    public static File getCellFile(Cell cell) {
        if (isDELIB(cell.getLibrary())) {
            String relativeFile = DELIB.getCellFile(cell.backup());
            URL libFile = cell.getLibrary().getLibFile();
            return new File(libFile.getFile(), relativeFile);
        }
        return null;
    }

    public static boolean isDELIB(Library lib) {
        URL libFile = lib.getLibFile();
        FileType type = OpenFile.getOpenFileType(libFile.getFile(), FileType.JELIB);
        return (type == FileType.DELIB);
    }

    /**
     * Returns true if this file has is being maintained from a
     * CVS respository, returns false otherwise.
     */
    public static boolean isFileInCVS(File fd) {
        // get directory file is in
        if (fd == null) return false;
        File parent = fd.getParentFile();
        File CVSDIR = new File(parent, "CVS");
        if (!CVSDIR.exists()) return false;
        File entries = new File(CVSDIR, "Entries");
        if (!entries.exists()) return false;
        // make sure file is mentioned in Entries file
        String filename = fd.getName();
        try {
            FileReader fr = new FileReader(entries);
            LineNumberReader reader = new LineNumberReader(fr);
            for (;;) {
                String line = reader.readLine();
                if (line == null) break;
                if (line.equals("")) continue;
                String parts[] = line.split("/");
                if (parts.length >= 2 && parts[1].equals(filename)) return true;
            }

        } catch (IOException e) {
        }
        return false;
    }

    /**
     * Used by commands that require the library to be in sync with the disk.
     * @param lib
     * @param dialog true to pop up a dialog to tell the user, false to not do so.
     * @return true if not modified, false if modified
     */
    public static boolean assertNotModified(Library lib, String cmd, boolean dialog) {
        if (lib.isChanged()) {
            if (dialog) {
                Job.getUserInterface().showErrorMessage("Library "+lib.getName()+" must be saved to run CVS "+cmd, "CVS "+cmd);
            } else {
                System.out.println("Library "+lib.getName()+" must be saved to run CVS "+cmd);
            }
            return false;
        }
        return true;
    }

    /**
     * Used by commands that require the library to be in sync with the disk.
     * @param cell
     * @param dialog true to pop up a dialog to tell the user, false to not do so.
     * @return true if not modified, false if modified
     */
    public static boolean assertNotModified(Cell cell, String cmd, boolean dialog) {
        if (cell.isModified(true)) {
            if (dialog) {
                Job.getUserInterface().showErrorMessage("Cell "+cell.getName()+" must be saved to run CVS "+cmd, "CVS "+cmd);
            } else {
                System.out.println("Cell "+cell.getName()+" must be saved to run CVS "+cmd);
            }
            return false;
        }
        return true;
    }

    /**
     * Returns true if the library is in CVS, otherwise generates an error message.
     * @param lib the library to check
     * @param cmd the CVS command (for error message display)
     * @param dialog true to show a modal dialog, false to write error to message window
     * @return true if it is in cvs, false otherwise
     */
    public static boolean assertInCVS(Library lib, String cmd, boolean dialog) {
        File libFile = new File(lib.getLibFile().getPath());
        if (!CVS.isFileInCVS(libFile)) {
            if (libFile.getPath().indexOf("com/sun/electric/lib/spiceparts") != -1) return false;
            String message = "Library "+lib.getName()+" is not part of CVS repository.\n" +
                        "Use 'CVS Add' to add to current repository.";
            if (dialog) {
                Job.getUserInterface().showErrorMessage(message, "CVS "+cmd+" Failed");
            } else {
                System.out.println(message+" CVS "+cmd+" Failed");
            }
            return false;
        }
        return true;
    }

    /**
     * Returns true if the Cell is in CVS, otherwise generates an error message.
     * @param cell the cell to check
     * @param cmd the CVS command (for error message display)
     * @param dialog true to show a modal dialog, false to write error to message window
     * @return true if it is in cvs, false otherwise
     */
    public static boolean assertInCVS(Cell cell, String cmd, boolean dialog) {
        File cellFile = getCellFile(cell);
        if (cellFile == null) {
            String message = "Cell "+cell.libDescribe()+" is not part of CVS repository.\n" +
                        "Use 'CVS Add' to add to current repository.";
            if (dialog) {
                Job.getUserInterface().showErrorMessage(message, "CVS "+cmd+" Failed");
            } else {
                System.out.println(message+" CVS "+cmd+" Failed");
            }
            return false;
        }
        return true;
    }

    /**
     * Get a command directory in which to run a CVS command on the given
     * libraries and cells.  This just picks the parent dir of
     * the first library found.
     * @param libs
     * @param cells
     * @return
     */
    static String getUseDir(List<Library> libs, List<Cell> cells) {
        if (libs != null) {
            for (Library lib : libs) {
                if (lib.isHidden()) continue;
                if (!lib.isFromDisk()) continue;
                File libFile = new File(lib.getLibFile().getPath());
                return libFile.getParent();
            }
        }
        if (cells != null) {
            for (Cell cell : cells) {
                Library lib = cell.getLibrary();
                if (lib.isHidden()) continue;
                if (!lib.isFromDisk()) continue;
                File libFile = new File(lib.getLibFile().getPath());
                return libFile.getParent();
            }
        }
        return User.getWorkingDirectory();
    }

    /**
     * Get a String of filenames for the associated libraries, to pass as the
     * 'files' argument to a CVS command.  Any files in 'useDir' will
     * be relative names, otherwise they will be absolute file names.
     * @param libs
     * @return
     */
    static StringBuffer getLibraryFiles(List<Library> libs, String useDir) {
        StringBuffer libsBuf = new StringBuffer();
        if (libs == null) return libsBuf;
        for (Library lib : libs) {
            String file = lib.getLibFile().getPath();
            if (file.startsWith(useDir)) {
                file = file.substring(useDir.length()+1, file.length());
            }
            libsBuf.append(file+" ");
        }
        return libsBuf;
    }

    /**
     * Get a String of filenames for the associated cells, to pass as the
     * 'files' argument to a CVS command.  Any files in 'useDir' will
     * be relative names, otherwise they will be absolute file names.
     * @param cells
     * @return
     */
    static StringBuffer getCellFiles(List<Cell> cells, String useDir) {
        StringBuffer cellsBuf = new StringBuffer();
        if (cells == null) return cellsBuf;
        for (Cell cell : cells) {
            String file = getCellFile(cell).getPath();
            if (file.startsWith(useDir)) {
                file = file.substring(useDir.length()+1, file.length());
            }
            cellsBuf.append(file+" ");
        }
        return cellsBuf;
    }

    /**
     * Get the Cell for the given path. The path is to be of the format
     * .../libraryName.delib/cellname/cellname.view. Returns null if
     * not of the correct format, if the library cannot be found, or
     * if the cell cannot be found.
     * @param path
     * @return
     */
    static Cell getCellFromPath(String path) {
        int delibExt = path.toLowerCase().indexOf(".delib"+File.separator);
        if (delibExt == -1) return null;

        // get the library
        String libpath = path.substring(0, delibExt);
        File libFile = new File(libpath);
        String libName = libFile.getName();
        Library lib = Library.findLibrary(libName);
        if (lib == null) return null;

        // get cell file
        File file = new File(path);
        String cellFile = file.getName();
        int ext = cellFile.indexOf('.');
        if (ext == -1) return null;
        String cellName = cellFile.substring(0, ext);
        String view = cellFile.substring(ext+1);
        View realView = View.findView(view);
        if (realView == null) return null;
        Cell cell = lib.findNodeProto(cellName+"{"+view+"}");
        return cell;
    }

    /**
     * Reloading libraries has the side affect that any EditWindows
     * containing cells that were reloaded now point to old, unlinked
     * cells instead of the new ones. This method checks for this state
     * and fixes it.
     */
    public static void fixStaleCellReferences(List<Library> libs) {
        for (Library lib : libs) {
            fixStaleCellReferences(lib);
        }
    }
    /**
     * Reloading libraries has the side affect that any EditWindows
     * containing cells that were reloaded now point to old, unlinked
     * cells instead of the new ones. This method checks for this state
     * and fixes it.
     */
    public static void fixStaleCellReferences(Library reloadedLib) {
        for (Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); ) {
            WindowFrame frame = it.next();
            if (frame.getContent() instanceof EditWindow) {
                EditWindow wnd = (EditWindow)frame.getContent();
                Cell cell = wnd.getCell();
                if (!cell.isLinked()) {
                    Library newLib = Library.findLibrary(cell.getLibrary().getName());
                    if (newLib == null) return;
                    Cell newCell = newLib.findNodeProto(cell.noLibDescribe());
                    if (newCell == null) return;
                    wnd.setCell(newCell, VarContext.globalContext);
                }
            }
        }
    }

    // ------------------- Preferences --------------------

    /**
     * Get the repository.  In the future, there may be some
     * dialog to let the user choose between multiple respositories.
     * @return
     */
    private static String getRepository() {
        return getCVSRepository();
    }

/*
    private static Pref cacheCVSEnabled = Pref.makeBooleanPref("CVS Enabled", User.getUserTool().prefs, false);
    public static boolean getCVSEnabled() { return cacheCVSEnabled.getBoolean(); }
    public static void setCVSEnabled(boolean b) { cacheCVSEnabled.setBoolean(b); }
*/

    private static Pref cacheCVSProgram = Pref.makeStringPref("CVS Program", User.getUserTool().prefs, "cvs");
    public static String getCVSProgram() { return cacheCVSProgram.getString(); }
    public static void setCVSProgram(String s) { cacheCVSProgram.setString(s); }

    private static Pref cacheCVSRepository = Pref.makeStringPref("CVS Repository", User.getUserTool().prefs, "");
    public static String getCVSRepository() { return cacheCVSRepository.getString(); }
    public static void setCVSRepository(String s) { cacheCVSRepository.setString(s); }

    private static Pref cacheCVSLastCommitMessage = Pref.makeStringPref("CVS Last Commit Message", User.getUserTool().prefs, "");
    public static String getCVSLastCommitMessage() { return cacheCVSLastCommitMessage.getString(); }
    public static void setCVSLastCommitMessage(String s) { cacheCVSLastCommitMessage.setString(s); }
}
