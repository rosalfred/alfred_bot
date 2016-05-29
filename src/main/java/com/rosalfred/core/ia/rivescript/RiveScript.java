/*
    com.rivescript.RiveScript - The Official Java RiveScript Interpreter
    Copyright (C) 2010  Noah Petherbridge

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.rosalfred.core.ia.rivescript;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rivescript.Client;
import com.rivescript.ClientManager;
import com.rivescript.Topic;
import com.rivescript.TopicManager;
import com.rivescript.Trigger;
import com.rivescript.Util;
import com.rosalfred.core.ia.rivescript.lang.Java;

/**
 * A RiveScript interpreter written in Java.<p>
 *
 * SYNOPSIS<p>
 *
 * import com.rivescript.RiveScript;<p>
 *
 * // Create a new interpreter<br>
 * RiveScript rs = new RiveScript();<p>
 *
 * // Load a directory full of replies in *.rive files<br>
 * rs.loadDirectory("./replies");<p>
 *
 * // Sort replies<br>
 * rs.sortReplies();<p>
 *
 * // Get a reply for the user<br>
 * String reply = rs.reply("user", "Hello bot!");
 */

public class RiveScript {

    public static final String TYPE_PYTHON = "python";
    public static final String TYPE_PERL = "perl";
    public static final String TYPE_JAVA = "java";

    // Private class variables.
    private boolean        debug = false;        // Debug mode
    private int            depth = 50;           // Recursion depth limit
    private String         error = "";           // Last error text
    private static Random  rand  = new Random(); // A random number generator

    // Module version
    /**
     * The version of the RiveScript Java library.
     */
    public static final double VERSION        = 0.02;

    // Constant RiveScript command symbols.
    private static final double RS_VERSION    = 2.0; // This implements RiveScript 2.0
    private static final String CMD_DEFINE    = "!";
    private static final String CMD_TRIGGER   = "+";
    private static final String CMD_PREVIOUS  = "%";
    private static final String CMD_REPLY     = "-";
    private static final String CMD_CONTINUE  = "^";
    private static final String CMD_REDIRECT  = "@";
    private static final String CMD_CONDITION = "*";
    private static final String CMD_LABEL     = ">";
    private static final String CMD_ENDLABEL  = "<";

    // The topic data structure, and the "thats" data structure.
    private TopicManager topics = new TopicManager();

    // Bot's users' data structure.
    private ClientManager clients = new ClientManager();

    // Object handlers
    private HashMap<String, ObjectHandler<BotReply>> handlers =
            new HashMap<String, ObjectHandler<BotReply>>();
    protected HashMap<String, String> objects  =
            new HashMap<String, String>(); // name->language mappers

    // Simpler internal data structures.
    //private ArrayList<String> vTopics =
    //        new ArrayList<String>(); // vector containing topic list (for quicker lookups)
    private HashMap<String, String>         globals  =
            new HashMap<String, String>();         // ! global
    private HashMap<String, String>         vars     =
            new HashMap<String, String>();         // ! var
    private HashMap<String, Vector<String>> arrays   =
            new HashMap<String, Vector<String>>(); // ! array
    private HashMap<String, String>         subs     =
            new HashMap<String, String>();         // ! sub
    private String[]                        subs_s   =
            null;                                  // sorted subs
    private HashMap<String, String>         person   =
            new HashMap<String, String>();         // ! person
    private String[]                        person_s =
            null;                                  // sorted persons


    private String currentUser = null;

    /*-------------------------*/
    /*-- Constructor Methods --*/
    /*-------------------------*/

    /**
     * Create a new RiveScript interpreter object, specifying the debug mode.
     *
     * @param debug Enable debug mode (a *lot* of stuff is printed to the terminal)
     */
    public RiveScript (boolean debug) {
        this.debug = debug;

        // Set static debug modes.
        Topic.setDebug(this.debug);
    }

    /**
     * Create a new RiveScript interpreter object.
     */
    public RiveScript () {
        this(false);
    }

    /*-------------------*/
    /*-- Error Methods --*/
    /*-------------------*/

    /**
     * Return the text of the last error message given.
     */
    public String error () {
        return this.error;
    }

    /**
     * Set the error message.
     *
     * @param message The new error message to set.
     */
    protected boolean error(final String message) {
        this.error = message;
        return false;
    }

    protected HashMap<String, ObjectHandler<BotReply>> getHandlers() {
        return this.handlers;
    }

    /*---------------------*/
    /*-- Loading Methods --*/
    /*---------------------*/

    /**
     * Load a directory full of RiveScript documents, specifying a custom
     * list of valid file extensions.
     *
     * @param path The path to the directory containing RiveScript documents.
     * @param exts A string array containing file extensions to look for.
     */
    public boolean loadDirectory (final String path, final String[] exts) {
        say("Load directory: " + path);

        // Get a directory handle.
        final File dh = new File(path);

        // Search it for files.
        for (int i = 0; i < exts.length; i++) {
            // Search the directory for files of this type.
            this.say("Searching for files of type: " + exts[i]);
            final String type = exts[i];
            final List<File> filesRive = this.getFiles(dh, type);

            // No results?
            if (filesRive == null) {
                return error("Couldn't read any files from directory " + path);
            }

            // Parse each file.
            for (File file : filesRive) {
                if (type.endsWith(TYPE_JAVA)) {
                    loadFileJava(file.getAbsolutePath());
                } else {
                    loadFileRive(file.getAbsolutePath());
                }
            }
        }

        return true;
    }

    protected List<File> getFiles(final File directory, final String type) {
    	List<File> result = new ArrayList<File>();
    	final List<File> directories = new ArrayList<File>();

    	final File[] filesRive = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept (final File f) {
            	boolean result = !f.isDirectory();

            	if (!result) {
            		directories.add(f);
            	}

                return result && f.getName().endsWith(type);
            }
        });

    	if (filesRive.length > 0) {
    		result.addAll(Arrays.asList(filesRive));
    	}

    	for (File file : directories) {
    		result.addAll(this.getFiles(file, type));
		}

    	return result;
    }

    /**
     * Load a directory full of RiveScript documents (.rive files).
     *
     * @param path The path to the directory containing RiveScript documents.
     */
    public boolean loadDirectory (final String path) {
        final String[] exts = { ".rive", ".rs", "." + TYPE_JAVA };
        return this.loadDirectory(path, exts);
    }

    /**
     * Load a single RiveScript document.
     *
     * @param file Path to a RiveScript document.
     */
    public boolean loadFileRive (final String file) {
        this.say("Load file: " + file);

        // Create a file handle.
        final File fh = new File(file);

        // Run some sanity checks on the file handle.
        if (!fh.exists()) {
            return error(file + ": file not found.");
        }
        if (!fh.isFile()) {
            return error(file + ": not a regular file.");
        }
        if (!fh.canRead()) {
            return error(file + ": can't read from file.");
        }

        String[] code;
        try {
            code = loadFile(file, fh);
        } catch (FileNotFoundException e) {
            // How did this happen? We checked it earlier.
            return error(file + ": file not found exception.");
        } catch (IOException e) {
            trace(e);
            return error(file + ": IOException while reading.");
        }

        // Send the code to the parser.
        return parse (file, code);
    }

    /**
     * Load a single RiveScript document.
     *
     * @param file Path to a RiveScript document.
     */
    public boolean loadFileJava (String file) {
        say("Load file: " + file);

        // Create a file handle.
        File fh = new File(file);

        // Run some sanity checks on the file handle.
        if (fh.exists() == false) {
            return error(file + ": file not found.");
        }
        if (fh.isFile() == false) {
            return error(file + ": not a regular file.");
        }
        if (fh.canRead() == false) {
            return error(file + ": can't read from file.");
        }

        String[] code;
        try {
            code = loadFile(file, fh);
        } catch (FileNotFoundException e) {
            // How did this happen? We checked it earlier.
            return error(file + ": file not found exception.");
        } catch (IOException e) {
            trace(e);
            return error(file + ": IOException while reading.");
        }

        String filePath = fh.getAbsolutePath().replaceAll("."+TYPE_JAVA, "");

        // Send the code to the parser.
        Java java = (Java) this.handlers.get(TYPE_JAVA);
        java.onLoad(filePath, code);

        // Map the name to the language.
        if (!this.objects.containsKey(java.getLastCompiledClassName())) {
            this.objects.put(java.getLastCompiledClassName(), TYPE_JAVA);
        }

        return true;
    }

    /**
     * @param file
     * @param fh
     * @return
     * @throws IOException
     */
    private String[] loadFile(String file, File fh) throws IOException {
        // Slurp the file's contents.
        Vector<String> lines = new Vector<String>();

        FileInputStream fis = new FileInputStream(fh);

        // Using buffered input stream for fast reading.
        DataInputStream dis = new DataInputStream(fis);
        BufferedReader  br  = new BufferedReader(new InputStreamReader(dis));

        // Read all the lines.
        String line;
        while ((line = br.readLine()) != null) {
            lines.add(line);
        }

        // Dispose of the resources we don't need anymore.
        dis.close();

        // Convert the vector into a string array.
        String[] code = Util.Sv2s (lines);
        return code;
    }

    /**
     * Stream some RiveScript code directly into the interpreter (as a single string
     * containing newlines in it).
     *
     * @param code A string containing all the RiveScript code.
     */
    public boolean stream (String code) {
        // Split the given code up into lines.
        String[] lines = code.split("\n");

        // Send the lines to the parser.
        return parse("(streamed)", lines);
    }

    /**
     * Stream some RiveScript code directly into the interpreter (as a string array,
     * one line per item).
     *
     * @param code A string array containing all the lines of code.
     */
    public boolean stream (String[] code) {
        // The coder has already broken the lines for us!
        return parse("(streamed)", code);
    }

    /*---------------------------*/
    /*-- Configuration Methods --*/
    /*---------------------------*/

    /**
     * Add a handler for a programming language to be used with RiveScript object calls.
     *
     * @param name    The name of the programming language.
     * @param handler An instance of a class that implements an ObjectHandler.
     */
    public void setHandler (String name, ObjectHandler<BotReply> handler) {
        this.handlers.put(name, handler);
    }

    /**
     * Set a global variable for the interpreter (equivalent to ! global).
     * Set the value to null to delete the variable.<p>
     *
     * There are two special globals that require certain data types:<p>
     *
     * debug is boolean-like and its value must be a string value containing
     * "true", "yes", "1", "false", "no" or "0".<p>
     *
     * depth is integer-like and its value must be a quoted integer like "50".
     * The "depth" variable controls how many levels deep RiveScript will go when
     * following reply redirections.<p>
     *
     * Returns true on success, false on error.
     *
     * @param name  The variable name.
     * @param value The variable's value.
     */
    public boolean setGlobal (String name, String value) {
        boolean delete = false;
        if (value == null || value == "<undef>") {
            delete = true;
        }

        // Special globals
        if (name.equals("debug")) {
            // Debug is a boolean.
            if (value.equals("true") ||
                    value.equals("1") ||
                    value.equals("yes")) {
                this.debug = true;
            }
            else if (value.equals("false") ||
                    value.equals("0") ||
                    value.equals("no") ||
                    delete) {
                this.debug = false;
            }
            else {
                return error("Global variable \"debug\" needs a boolean value");
            }
        }
        else if (name.equals("depth")) {
            // Depth is an integer.
            try {
                this.depth = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return error("Global variable \"depth\" needs an integer value");
            }
        }

        // It's a user-defined global. OK.
        if (delete) {
            globals.remove(name);
        }
        else {
            globals.put(name, value);
        }

        return true;
    }

    /**
     * Set a bot variable for the interpreter (equivalent to ! var). A bot
     * variable is data about the chatbot, like its name or favorite color.<p>
     *
     * A null value will delete the variable.
     *
     * @param name  The variable name.
     * @param value The variable's value.
     */
    public boolean setVariable (String name, String value) {
        if (value == null || value == "<undef>") {
            vars.remove(name);
        }
        else {
            vars.put(name, value);
        }

        return true;
    }

    /**
     * Set a substitution pattern (equivalent to ! sub). The user's input (and
     * the bot's reply, in %Previous) get substituted using these rules.<p>
     *
     * A null value for the output will delete the substitution.
     *
     * @param pattern The pattern to match in the message.
     * @param output  The text to replace it with (must be lowercase, no special characters).
     */
    public boolean setSubstitution (String pattern, String output) {
        if (output == null || output == "<undef>") {
            subs.remove(pattern);
        }
        else {
            subs.put(pattern, output);
        }

        return true;
    }

    /**
     * Set a person substitution pattern (equivalent to ! person). Person
     * substitutions swap first- and second-person pronouns, so the bot can
     * safely echo the user without sounding too mechanical.<p>
     *
     * A null value for the output will delete the substitution.
     *
     * @param pattern The pattern to match in the message.
     * @param output  The text to replace it with (must be lowercase, no special characters).
     */
    public boolean setPersonSubstitution (String pattern, String output) {
        if (output == null || output == "<undef>") {
            person.remove(pattern);
        }
        else {
            person.put(pattern, output);
        }

        return true;
    }

    /**
     * Set a variable for one of the bot's users. A null value will delete a
     * variable.
     *
     * @param user  The user's ID.
     * @param name  The name of the variable to set.
     * @param value The value to set.
     */
    public boolean setUservar (String user, String name, String value) {
        if (value == null || value == "<undef>") {
            clients.client(user).delete(name);
        }
        else {
            clients.client(user).set(name, value);
        }

        return true;
    }

    /**
     * Set -all- user vars for a user. This will replace the internal hash for
     * the user. So your hash should at least contain a key/value pair for the
     * user's current "topic". This could be useful if you used getUservars to
     * store their entire profile somewhere and want to restore it later.
     *
     * @param user  The user's ID.
     * @param data  The full hash of the user's data.
     */
    public boolean setUservars (String user, HashMap<String, String> data) {
        // TODO: this should be handled more sanely. ;)
        clients.client(user).setData(data);
        return true;
    }

    public ClientManager getUservars() {
        return this.clients;
    }

    public void setUservars(ClientManager clients) {
        this.clients = clients;
    }

    /**
     * Get a list of all the user IDs the bot knows about.
     */
    public String[] getUsers () {
        // Get the user list from the clients object.
        return clients.listClients();
    }

    public String getCurrentUser() {
    	return this.currentUser;
    }

    /**
     * Retrieve a listing of all the uservars for a user as a HashMap.
     * Returns null if the user doesn't exist.
     *
     * @param user The user ID to get the vars for.
     */
    public HashMap<String, String> getUservars (String user) {
        if (clients.clientExists(user)) {
            return clients.client(user).getData();
        }
        else {
            return null;
        }
    }

    /**
     * Retrieve a single variable from a user's profile.
     *
     * Returns null if the user doesn't exist. Returns the string "undefined"
     * if the variable doesn't exist.
     *
     * @param user The user ID to get data from.
     * @param name The name of the variable to get.
     */
    public String getUservar (String user, String name) {
        if (clients.clientExists(user)) {
            return clients.client(user).get(name);
        }
        else {
            return null;
        }
    }

    /*---------------------*/
    /*-- Parsing Methods --*/
    /*---------------------*/

    /**
     * Parse RiveScript code and load it into internal memory.
     *
     * @param filename A file name to associate with this code (for error reporting)
     * @param code     A string array of all the code to parse.
     */
    private boolean parse (String filename, String[] code) {
        // Track some state variables for this parsing round.
        String topic            = "random"; // Default topic = random
        int lineno              = 0;
        boolean comment         = false; // In a multi-line comment
        boolean inobj           = false; // In an object
        String objName          = "";    // Name of the current object
        String objLang          = "";    // Programming language of the object
        Vector<String> objBuff  = null;  // Buffer for the current object
        String onTrig           = "";    // Trigger we're on
        //String lastcmd          = "";    // Last command code
        String isThat           = "";    // Is a %Previous trigger

        // The given "code" is an array of lines, so jump right in.
        for (int i = 0; i < code.length; i++) {
            lineno++; // Increment the line counter.
            String line = code[i];
            say("Line: " + line);

            // Trim the line of whitespaces.
            line = line.trim();

            // Are we inside an object?
            if (inobj) {
                if (line.startsWith("<object") || line.startsWith("< object")) { // TODO regexp
                    // End of the object. Did we have a handler?
                    if (handlers.containsKey(objLang)) {
                        // Yes, call the handler's onLoad function.
                        handlers.get(objLang).onLoad(objName, Util.Sv2s(objBuff));

                        // Map the name to the language.
                        objects.put(objName, objLang);
                    }

                    objName = "";
                    objLang = "";
                    objBuff = null;
                    inobj   = false;
                    continue;
                }

                // Collect the code.
                objBuff.add(line);
                continue;
            }

            // Look for comments.
            if (line.startsWith("/*")) {
                // Beginning a multi-line comment.
                if (line.indexOf("*/") > -1) {
                    // It ends on the same line.
                    continue;
                }
                comment = true;
            }
            else if (line.startsWith("/")) {
                // A single line comment.
                continue;
            }
            else if (line.indexOf("*/") > -1) {
                // End a multi-line comment.
                comment = false;
                continue;
            }
            if (comment) {
                continue;
            }

            // Skip any blank lines.
            if (line.length() < 2) {
                continue;
            }

            // Separate the command from the rest of the line.
            String cmd = line.substring(0,1);
            line = line.substring(1).trim();
            say("\tCmd: " + cmd);

            // Ignore inline comments.
            if (line.indexOf(" // ") > -1) {
                String[] split = line.split(" // ");
                line = split[0];
            }

            // Reset the %Previous if this is a new +Trigger.
            if (cmd.equals(CMD_TRIGGER)) {
                isThat = "";
            }

            // Do a look-ahead to see ^Continue and %Previous.
            for (int j = (i + 1); j < code.length; j++) {
                // Peek ahead.
                String peek = code[j].trim();

                // Skip blank.
                if (peek.length() == 0) {
                    continue;
                }

                // Get the command.
                String peekCmd = peek.substring(0,1);
                peek           = peek.substring(1).trim();

                // Only continue if the lookahead line has any data.
                if (peek.length() > 0) {
                    // The lookahead command has to be a % or a ^
                    if (peekCmd.equals(CMD_CONTINUE) == false &&
                            peekCmd.equals(CMD_PREVIOUS) == false) {
                        break;
                    }

                    // If the current command is a +, see if the following is a %.
                    if (cmd.equals(CMD_TRIGGER)) {
                        if (peekCmd.equals(CMD_PREVIOUS)) {
                            // It has a %Previous!
                            isThat = peek;
                            break;
                        }
                        else {
                            isThat = "";
                        }
                    }

                    // If the current command is a ! and the next command(s) are
                    // ^, we'll tack each extension on as a "line break".
                    if (cmd.equals(CMD_DEFINE)) {
                        if (peekCmd.equals(CMD_CONTINUE)) {
                            line += "<crlf>" + peek;
                        }
                    }

                    // If the current command is not a ^ and the line after is
                    // not a %, but the line after IS a ^, then tack it onto the
                    // end of the current line.
                    if (cmd.equals(CMD_CONTINUE) == false &&
                            cmd.equals(CMD_PREVIOUS) == false &&
                            cmd.equals(CMD_DEFINE) == false) {
                        if (peekCmd.equals(CMD_CONTINUE)) {
                            line += peek;
                        }
                        else {
                            break;
                        }
                    }
                }
            }

            // Start handling command types.
            if (cmd.equals(CMD_DEFINE)) {
                say("\t! DEFINE");
                String[] whatis = line.split("\\s*=\\s*", 2);
                String[] left   = whatis[0].split("\\s+", 2);
                String type     = left[0];
                String var      = "";
                String value    = "";
                boolean delete  = false;
                if (left.length == 2)	{
                    var = left[1].trim().toLowerCase();
                }
                if (whatis.length == 2) {
                    value = whatis[1].trim();
                }

                // Remove line breaks unless this is an array.
                if (!type.equals("array")) {
                    value = value.replaceAll("<crlf>", "");
                }

                // Version is the only type that doesn't have a var.
                if (type.equals("version")) {
                    say("\tUsing RiveScript version " + value);

                    // Convert the value into a double, catch exceptions.
                    double version = 0;
                    try {
                        version = Double.valueOf(value).doubleValue();
                    } catch (NumberFormatException e) {
                        cry("RiveScript version \"" + value +
                                "\" not a valid floating number",
                                filename, lineno);
                        continue;
                    }

                    if (version > RS_VERSION) {
                        cry("We can't parse RiveScript v" + value +
                                " documents",
                                filename, lineno);
                        return false;
                    }

                    continue;
                }
                else {
                    // All the other types require a variable and value.
                    if (var.equals("")) {
                        cry("Missing a " + type +
                                " variable name",
                                filename, lineno);
                        continue;
                    }
                    if (value.equals("")) {
                        cry("Missing a " + type + " value", filename, lineno);
                        continue;
                    }
                    if (value.equals("<undef>")) {
                        // Deleting its value.
                        delete = true;
                    }
                }

                // Handle the variable set types.
                if (type.equals("global")) {
                    // Is it a special global? (debug or depth or etc).
                    say("\tSet global " + var + " = " + value);
                    this.setGlobal(var, value);
                }
                else if (type.equals("var")) {
                    // Set a bot variable.
                    say("\tSet bot variable " + var + " = " + value);
                    this.setVariable(var, value);
                }
                else if (type.equals("array")) {
                    // Set an array.
                    say("\tSet array " + var);

                    // Deleting it?
                    if (delete) {
                        arrays.remove(var);
                        continue;
                    }

                    // Did the array have multiple lines?
                    String[] parts = value.split("<crlf>");
                    Vector<String> items = new Vector<String>();
                    for (int a = 0; a < parts.length; a++) {
                        // Split at pipes or spaces?
                        String[] pieces;
                        if (parts[a].indexOf("|") > -1) {
                            pieces = parts[a].split("\\|");
                        }
                        else {
                            pieces = parts[a].split("\\s+");
                        }

                        // Add the pieces to the final array.
                        for (int b = 0; b < pieces.length; b++) {
                            items.add(pieces[b]);
                        }
                    }

                    // Store this array.
                    arrays.put(var, items);
                }
                else if (type.equals("sub")) {
                    // Set a substitution.
                    say("\tSubstitution " + var + " => " + value);
                    this.setSubstitution(var, value);
                }
                else if (type.equals("person")) {
                    // Set a person substitution.
                    say("\tPerson substitution " + var + " => " + value);
                    this.setPersonSubstitution(var, value);
                }
                else {
                    cry("Unknown definition type \"" + type +
                            "\"",
                            filename, lineno);
                    continue;
                }
            }
            else if (cmd.equals(CMD_LABEL)) {
                // > LABEL
                say("\t> LABEL");
                String label[] = line.split("\\s+");
                String type    = "";
                String name    = "";
                if (label.length >= 1) {
                    type = label[0].trim().toLowerCase();
                }
                if (label.length >= 2) {
                    name = label[1].trim();
                }

                // Handle the label types.
                if (type.equals("begin")) {
                    // The BEGIN statement.
                    say("\tFound the BEGIN Statement.");

                    // A BEGIN is just a special topic.
                    type = "topic";
                    name = "__begin__";
                }
                if (type.equals("topic")) {
                    // Starting a new topic.
                    say("\tSet topic to " + name);
                    onTrig = "";
                    topic = name;

                    // Does this topic include or inherit another one?
                    if (label.length >= 3) {
                        final int mode_includes = 1;
                        final int mode_inherits = 2;
                        int mode = 0;
                        for (int a = 2; a < label.length; a++) {
                            if (label[a].toLowerCase().equals("includes")) {
                                mode = mode_includes;
                            }
                            else if (label[a].toLowerCase().equals("inherits")) {
                                mode = mode_inherits;
                            }
                            else if (mode > 0) {
                                // This topic is either inherited or included.
                                if (mode == mode_includes) {
                                    topics.topic(topic).includes(label[a]);
                                }
                                else if (mode == mode_inherits) {
                                    topics.topic(topic).inherits(label[a]);
                                }
                            }
                        }
                    }
                }
                if (type.equals("object")) {
                    // If a field was provided, it should be the programming language.
                    String lang = "";
                    if (label.length >= 3) {
                        lang = label[2].toLowerCase();
                    }

                    // Only try to parse a language we support.
                    onTrig = "";
                    if (lang.length() == 0) {
                        cry("Trying to parse unknown programming language (assuming it's JavaScript)",
                                filename, lineno);
                        lang = "javascript"; // Assume it's JavaScript
                    }
                    if (!handlers.containsKey(lang)) {
                        // We don't have a handler for this language.
                        say("We can't handle " + lang + " object code!");
                        continue;
                    }

                    // Start collecting its code!
                    objName = name;
                    objLang = lang;
                    objBuff = new Vector<String>();
                    inobj   = true;
                }
            }
            else if (cmd.equals(CMD_ENDLABEL)) {
                // < ENDLABEL
                say("\t< ENDLABEL");
                String type = line.trim().toLowerCase();

                if (type.equals("begin") || type.equals("topic")) {
                    say("\t\tEnd topic label.");
                    topic = "random";
                }
                else if (type.equals("object")) {
                    say("\t\tEnd object label.");
                    inobj = false;
                }
                else {
                    cry("Unknown end topic type \"" + type + "\"",
                            filename, lineno);
                }
            }
            else if (cmd.equals(CMD_TRIGGER)) {
                // + TRIGGER
                say("\t+ TRIGGER: " + line);

                if (isThat.length() > 0) {
                    // This trigger had a %Previous. To prevent conflict, tag the
                    // trigger with the "that" text.
                    onTrig = line + "{previous}" + isThat;
                    topics.topic(topic).trigger(line).hasPrevious(true);
                    topics.topic(topic).addPrevious(line, isThat);
                }
                else {
                    // Set the current trigger to this.
                    onTrig = line;
                }
            }
            else if (cmd.equals(CMD_REPLY)) {
                // - REPLY
                say("\t- REPLY: " + line);

                // This can't come before a trigger!
                if (onTrig.length() == 0) {
                    cry("Reply found before trigger", filename, lineno);
                    continue;
                }

                // Add the reply to the trigger.
                topics.topic(topic).trigger(onTrig).addReply(line);
            }
            else if (cmd.equals(CMD_PREVIOUS)) {
                // % PREVIOUS
                // This was handled above.
            }
            else if (cmd.equals(CMD_CONTINUE)) {
                // ^ CONTINUE
                // This was handled above.
            }
            else if (cmd.equals(CMD_REDIRECT)) {
                // @ REDIRECT
                say("\t@ REDIRECT: " + line);

                // This can't come before a trigger!
                if (onTrig.length() == 0) {
                    cry("Redirect found before trigger", filename, lineno);
                    continue;
                }

                // Add the redirect to the trigger.
                // TODO: this extends RiveScript, not compat w/ Perl yet
                topics.topic(topic).trigger(onTrig).addRedirect(line);
            }
            else if (cmd.equals(CMD_CONDITION)) {
                // * CONDITION
                say("\t* CONDITION: " + line);

                // This can't come before a trigger!
                if (onTrig.length() == 0) {
                    cry("Redirect found before trigger", filename, lineno);
                    continue;
                }

                // Add the condition to the trigger.
                topics.topic(topic).trigger(onTrig).addCondition(line);
            }
            else {
                cry("Unrecognized command \"" + cmd + "\"", filename, lineno);
            }
        }

        return true;
    }

    /*---------------------*/
    /*-- Sorting Methods --*/
    /*---------------------*/

    /**
     * After loading replies into memory, call this method to (re)initialize
     * internal sort buffers. This is necessary for accurate trigger matching.
     */
    public void sortReplies () {
        // We need to make sort buffers under each topic.
        String[] topics = this.topics.listTopics();
        say("There are " + topics.length + " topics to sort replies for.");

        // Tell the topic manager to sort its topics' replies.
        this.topics.sortReplies();

        // Sort the substitutions.
        subs_s = Util.sortByLength (Util.SSh2s(subs));
        person_s = Util.sortByLength (Util.SSh2s(person));
    }

    /*---------------------*/
    /*-- Reply Methods   --*/
    /*---------------------*/

    /**
     * Get a reply from the RiveScript interpreter.
     *
     * @param username A unique user ID for the user chatting with the bot.
     * @param message  The user's message to the bot.
     */
    public BotReply reply (String username, String message) {
        say("Get reply to [" + username + "] " + message);

        this.currentUser = username;

        // Format their message first.
        message = formatMessage (message);

        // This will hold the final reply.
        BotReply reply = new BotReply("");

        // If the BEGIN statement exists, consult it first.
        if (topics.exists("__begin__")) {
            BotReply begin = this.reply (username, "request", true, 0);

            // OK to continue?
            if (begin.getReply().indexOf("{ok}") > -1) {
                // Get a reply then.
                reply = this.reply (username, message, false, 0);
                begin.setReply(begin.getReply().replaceAll(
                        "\\{ok\\}", reply.getReply()));
                reply.setReply(begin.getReply());
            }

            // Run final substitutions.
            reply = processTags (username,
                    clients.client(username),
                    message,
                    reply,
                    new ArrayList<String>(),
                    new ArrayList<String>(),
                    0);
        }
        else {
            // No BEGIN, just continue.
            reply = this.reply (username, message, false, 0);
        }

        // Save their chat history.
        clients.client(username).addInput(message);
        clients.client(username).addReply(reply.getReply()); //TODO replace by BotReply?

        // Return their reply.
        return reply;
    }

    /**
     * Internal method for getting a reply.
     *
     * @param user    The username of the calling user.
     * @param message The (formatted!) message sent by the user.
     * @param begin   Whether the context is that we're in the BEGIN statement or not.
     * @param step    The recursion depth that we're at so far.
     */
    private BotReply reply (String user, String message, boolean begin, int step) {
        BotReply result = new BotReply();

        /*-----------------------*/
        /*-- Collect User Info --*/
        /*-----------------------*/

        String topic            = "random";             // Default topic = random
        ArrayList<String> stars    = new ArrayList<String>(); // Wildcard matches
        ArrayList<String> botstars = new ArrayList<String>(); // Wildcards in %Previous
        String reply            = "";                   // The eventual reply
        Client profile;                                 // The user's profile object

        // Get the user's profile.
        profile = clients.client(user);

        // Update their topic.
        topic = profile.get("topic");

        // Avoid letting the user fall into a missing topic.
        if (topics.exists(topic) == false) {
            cry("User " + user + " was in a missing topic named \"" + topic + "\"!");
            topic = "random";
            profile.set("topic", "random");
        }

        // Avoid deep recursion.
        if (step > depth) {
            reply = "ERR: Deep Recursion Detected!";
            cry(reply);
            result.setReply(reply);
            return result;
        }

        // Are we in the BEGIN statement?
        if (begin) {
            // This implies the begin topic.
            topic = "__begin__";
        }

        /*------------------*/
        /*-- Find a Reply --*/
        /*------------------*/

        // Create a pointer for the matched data.
        Trigger matched = null;
        boolean foundMatch     = false;
        String  matchedTrigger = "";

        // See if there are any %previous's in this topic, or any topic related to it. This
        // should only be done the first time -- not during a recursive redirection.
        if (step == 0) {
            say("Looking for a %Previous");
            String[] allTopics = { topic };
            //		if (this.topics.topic(topic).includes() || this.topics.topic(topic).inherits()) {
            // We need to walk the topic tree.
            allTopics = this.topics.getTopicTree(topic, 0);
            //		}
            for (int i = 0; i < allTopics.length; i++) {
                // Does this topic have a %Previous anywhere?
                say("Seeing if " + allTopics[i] + " has a %Previous");
                if (this.topics.topic(allTopics[i]).hasPrevious()) {
                    say("Topic " + allTopics[i] + " has at least one %Previous");

                    // Get them.
                    String[] previous = this.topics.topic(allTopics[i]).listPrevious();
                    for (int j = 0; j < previous.length; j++) {
                        say("Candidate: " + previous[j]);

                        // Try to match the bot's last reply against this.
                        String lastReply = formatMessage(profile.getReply(1));
                        String regexp    = triggerRegexp(user, profile, previous[j]);
                        say("Compare " + lastReply + " <=> " + previous[j] + " (" + regexp + ")");

                        // Does it match?
                        Pattern re = Pattern.compile("^" + regexp + "$");
                        Matcher m  = re.matcher(lastReply);
                        while (m.find() == true) {
                            say("OMFG the lastReply matches!");

                            // Harvest the botstars.
                            for (int s = 1; s <= m.groupCount(); s++) {
                                say("Add botstar: " + m.group(s));
                                botstars.add(m.group(s));
                            }

                            // Now see if the user matched this trigger too!
                            String[] candidates = this.topics.topic(allTopics[i]).listPreviousTriggers(previous[j]);
                            for (int k = 0; k < candidates.length; k++) {
                                say("Does the user's message match " + candidates[k] + "?");
                                String humanside = triggerRegexp(user, profile, candidates[k]);
                                say("Compare " + message + " <=> " + candidates[k] + " (" + humanside + ")");

                                Pattern reH = Pattern.compile("^" + humanside + "$");
                                Matcher mH  = reH.matcher(message);
                                while (mH.find() == true) {
                                    say("It's a match!!!");

                                    // Make sure it's all valid.
                                    String realTrigger = candidates[k] + "{previous}" + previous[j];
                                    if (this.topics.topic(allTopics[i]).triggerExists(realTrigger)) {
                                        // Seems to be! Collect the stars.
                                        for (int s = 1; s <= mH.groupCount(); s++) {
                                            say("Add star: " + mH.group(s));
                                            stars.add(mH.group(s));
                                        }

                                        foundMatch = true;
                                        matchedTrigger = candidates[k];
                                        matched = this.topics.topic(allTopics[i]).trigger(realTrigger);
                                    }

                                    break;
                                }

                                if (foundMatch) {
                                    break;
                                }
                            }
                            if (foundMatch) {
                                break;
                            }
                        }
                    }
                }
            }
        }

        // Search their topic for a match to their trigger.
        if (foundMatch == false) {
            // Go through the sort buffer for their topic.
            String[] triggers = topics.topic(topic).listTriggers();
            for (int a = 0; a < triggers.length; a++) {
                String trigger = triggers[a];

                // Prepare the trigger for the regular expression engine.
                String regexp = triggerRegexp(user, profile, trigger);
                say("Try to match \"" + message + "\" against \"" + trigger + "\" (" + regexp + ")");

                // Is it a match?
                Pattern re = Pattern.compile("^" + regexp + "$");
                Matcher m  = re.matcher(message);
                if (m.find() == true) {
                    say("The trigger matches! Star count: " + m.groupCount());

                    // Harvest the stars.
                    int starcount = m.groupCount();
                    for (int s = 1; s <= starcount; s++) {
                        say("Add star: " + m.group(s));
                        stars.add(m.group(s));
                    }

                    // We found a match, but what if the trigger we matched belongs to
                    // an inherited topic? Check for that.
                    if (this.topics.topic(topic).triggerExists(trigger)) {
                        // No, the trigger does belong to us.
                        matched = this.topics.topic(topic).trigger(trigger);
                    }
                    else {
                        say("Trigger doesn't exist under this topic, trying to find it!");
                        matched = this.topics.findTriggerByInheritance(topic, trigger, 0);
                    }

                    foundMatch = true;
                    matchedTrigger = trigger;
                    break;
                }
            }
        }

        // Store what trigger they matched on (matchedTrigger can be blank if they didn't match).
        profile.set("__lastmatch__", matchedTrigger);

        // Did they match anything?
        if (foundMatch) {
            say("They were successfully matched to a trigger!");

            /*---------------------------------*/
            /*-- Process Their Matched Reply --*/
            /*---------------------------------*/

            // Make a dummy once loop so we can break out anytime.
            for (int n = 0; n < 1; n++) {
                // Exists?
                if (matched == null) {
                    cry("Unknown error: they matched trigger " + matchedTrigger + ", but it doesn't exist?");
                    foundMatch = false;
                    break;
                }

                // Get the trigger object.
                Trigger trigger = matched;
                say("The trigger matched belongs to topic " + trigger.topic());

                //See if there are any hard redirects.
                if (matched.listRedirects() != null && matched.listRedirects().length > 0) {
                    say("Redirecting us to " + matched.listRedirects()[0]);
                    result = processTags(user, profile, message,
                            new BotReply(matched.listRedirects()[0]),
                            stars, botstars, step);
                    say("Pretend user said: " + result.getReply());
                    reply = reply(user, result.getReply(), false, step=(step+1)).getReply();
                    break;
                }

                // Check for conditions.
                String[] conditions = trigger.listConditions();
                if (conditions.length > 0) {
                    say("This trigger has some conditions!");

                    // See if any conditions are true.
                    boolean truth = false;
                    for (int c = 0; c < conditions.length; c++) {
                        // Separate the condition from the potential reply.
                        String[] halves = conditions[c].split("\\s*=>\\s*");
                        String condition = halves[0].trim();
                        String potreply  = halves[1].trim();

                        // Split up the condition.
                        Pattern reCond = Pattern.compile("^(.+?)\\s+(==|eq|\\!=|ne|<>|<|<=|>|>=)\\s+(.+?)$");
                        Matcher mCond  = reCond.matcher(condition);
                        while (mCond.find()) {
                            String left  = mCond.group(1).trim();
                            String eq    = mCond.group(2).trim();
                            String right = mCond.group(3).trim();

                            // Process tags on both halves.
                            left = processTags(user, profile, message, left, stars, botstars, step+1);
                            right = processTags(user, profile, message, right, stars, botstars, step+1);
                            say("Compare: " + left + " " + eq + " " + right);

                            // Defaults
                            if (left.length() == 0) {
                                left = "undefined";
                            }
                            if (right.length() == 0) {
                                right = "undefined";
                            }

                            // Validate the expression.
                            if (eq.equals("eq") || eq.equals("ne") || eq.equals("==") || eq.equals("!=") || eq.equals("<>")) {
                                // String equality comparing.
                                if ((eq.equals("eq") || eq.equals("==")) && left.equals(right)) {
                                    truth = true;
                                    break;
                                }
                                else if ((eq.equals("ne") || eq.equals("!=") || eq.equals("<>")) && !left.equals(right)) {
                                    truth = true;
                                    break;
                                }
                            }

                            // Numeric comparing.
                            int lt = 0;
                            int rt = 0;

                            // Turn the two sides into numbers.
                            try {
                                lt = Integer.parseInt(left);
                                rt = Integer.parseInt(right);
                            } catch (NumberFormatException e) {
                                // Oh well!
                                break;
                            }

                            // Run the remaining equality checks.
                            if (eq.equals("==") || eq.equals("!=") || eq.equals("<>")) {
                                // Equality checks.
                                if (eq.equals("==") && lt == rt) {
                                    truth = true;
                                    break;
                                }
                                else if ((eq.equals("!=") || eq.equals("<>")) && lt != rt) {
                                    truth = true;
                                    break;
                                }
                            }
                            else if (eq.equals("<") && lt < rt) {
                                truth = true;
                                break;
                            }
                            else if (eq.equals("<=") && lt <= rt) {
                                truth = true;
                                break;
                            }
                            else if (eq.equals(">") && lt > rt) {
                                truth = true;
                                break;
                            }
                            else if (eq.equals(">=") && lt >= rt) {
                                truth = true;
                                break;
                            }
                        }

                        // True condition?
                        if (truth) {
                            reply = potreply;
                            break;
                        }
                    }
                }

                // Break if we got a reply from the conditions.
                if (reply.length() > 0) {
                    break;
                }

                // Return one of the replies at random. We lump any redirects in as well.
                String[] redirects = trigger.listRedirects();
                String[] replies   = trigger.listReplies();

                // Take into account their weights.
                Vector<Integer> bucket = new Vector<Integer>();
                Pattern reWeight = Pattern.compile("\\{weight=(\\d+?)\\}");

                // Look at weights on redirects.
                for (int i = 0; i < redirects.length; i++) {
                    if (redirects[i].indexOf("{weight=") > -1) {
                        Matcher mWeight = reWeight.matcher(redirects[i]);
                        while (mWeight.find()) {
                            int weight = Integer.parseInt(mWeight.group(1));

                            // Add to the bucket this many times.
                            if (weight > 1) {
                                for (int j = 0; j < weight; j++) {
                                    say("Trigger has a redirect (weight " + weight + "): " + redirects[i]);
                                    bucket.add(i);
                                }
                            }
                            else {
                                say("Trigger has a redirect (weight " + weight + "): " + redirects[i]);
                                bucket.add(i);
                            }

                            // Only one weight is supported.
                            break;
                        }
                    }
                    else {
                        say("Trigger has a redirect: " + redirects[i]);
                        bucket.add(i);
                    }
                }

                // Look at weights on replies.
                for (int i = 0; i < replies.length; i++) {
                    if (replies[i].indexOf("{weight=") > -1) {
                        Matcher mWeight = reWeight.matcher(replies[i]);
                        while (mWeight.find()) {
                            int weight = Integer.parseInt(mWeight.group(1));

                            // Add to the bucket this many times.
                            if (weight > 1) {
                                for (int j = 0; j < weight; j++) {
                                    say("Trigger has a reply (weight " + weight + "): " + replies[i]);
                                    bucket.add(redirects.length + i);
                                }
                            }
                            else {
                                say("Trigger has a reply (weight " + weight + "): " + replies[i]);
                                bucket.add(redirects.length + i);
                            }

                            // Only one weight is supported.
                            break;
                        }
                    }
                    else {
                        say("Trigger has a reply: " + replies[i]);
                        bucket.add(redirects.length + i);
                    }
                }

                // Pull a random value out.
                int[] choices = Util.Iv2s(bucket);
                if (choices.length > 0) {
                    int choice = choices [ rand.nextInt(choices.length) ];
                    say("Possible choices: " + choices.length + "; chosen: " + choice);
                    if (choice < redirects.length) {
                        // The choice was a redirect!
                        String redirect = redirects[choice].replaceAll("\\{weight=\\d+\\}","");
                        say("Chosen a redirect to " + redirect + "!");
                        reply = reply(user, redirect, begin, step+1).getReply();
                    }
                    else {
                        // The choice was a reply!
                        choice -= redirects.length;
                        if (choice < replies.length) {
                            say("Chosen a reply: " + replies[choice]);
                            reply = replies[choice];
                        }
                    }
                }
            }
        }

        // Still no reply?
        if (!foundMatch) {
            reply = "ERR: No Reply Matched";
        }
        else if (reply.length() == 0) {
            reply = "ERR: No Reply Found";
        }

        say("Final reply: " + reply);

        // Special tag processing for the BEGIN statement.
        if (begin) {
            // The BEGIN block may have {topic} or <set> tags and that's all.
            // <set> tag
            if (reply.indexOf("<set") > -1) {
                Pattern reSet = Pattern.compile("<set (.+?)=(.+?)>");
                Matcher mSet  = reSet.matcher(reply);
                while (mSet.find()) {
                    String tag   = mSet.group(0);
                    String var   = mSet.group(1);
                    String value = mSet.group(2);

                    // Set the uservar.
                    profile.set(var, value);
                    reply = reply.replace(tag, "");
                }
            }

            // {topic} tag
            if (reply.indexOf("{topic=") > -1) {
                Pattern reTopic = Pattern.compile("\\{topic=(.+?)\\}");
                Matcher mTopic  = reTopic.matcher(reply);
                while (mTopic.find()) {
                    String tag = mTopic.group(0);
                    topic      = mTopic.group(1);
                    say("Set user's topic to: " + topic);
                    profile.set("topic", topic);
                    reply = reply.replace(tag, "");
                }
            }

            result.setReply(reply);
        }
        else {
            // Process tags.
            result.setReply(reply);
            result = processTags (user, profile, message, result, stars, botstars, step);
        }

        return result;
    }

    /**
     * Formats a trigger for the regular expression engine.
     *
     * @param user    The user ID of the caller.
     * @param trigger The raw trigger text.
     */
    private String triggerRegexp (String user, Client profile, String trigger) {
        // If the trigger is simply '*', it needs to become (.*?) so it catches the empty string.
        String regexp = trigger.replaceAll("^\\*$", "<zerowidthstar>");

        // Simple regexps are simple.
        regexp = regexp.replaceAll("\\*", "(.+?)");             // *  ->  (.+?)
        regexp = regexp.replaceAll("#",   "(\\\\d+?)");         // #  ->  (\d+?)
        regexp = regexp.replaceAll("_",   "(\\\\w+?)");     // _  ->  ([A-Za-z ]+?)
        regexp = regexp.replaceAll("\\{weight=\\d+\\}", "");    // Remove {weight} tags
        regexp = regexp.replaceAll("<zerowidthstar>", "(.*?)"); // *  ->  (.*?)

        // Handle optionals.
        if (regexp.indexOf("[") > -1) {
            Pattern reOpts = Pattern.compile("\\s*\\[(.+?)\\]\\s*");
            Matcher mOpts  = reOpts.matcher(regexp);
            while (mOpts.find() == true) {
                String optional = mOpts.group(0);
                String contents = mOpts.group(1);

                // Split them at the pipes.
                String[] parts = contents.split("\\|");

                // Construct a regexp part.
                StringBuffer re = new StringBuffer();
                for (int i = 0; i < parts.length; i++) {
                    // We want: \s*part\s*
                    re.append("\\s*" + parts[i] + "\\s*");
                    if (i < parts.length - 1) {
                        re.append("|");
                    }
                }
                String pipes = re.toString();

                // If this optional had a star or anything in it, e.g. [*],
                // make it non-matching.
                pipes = pipes.replaceAll("\\(.+?\\)", "(?:.+?)");
                pipes = pipes.replaceAll("\\(\\d+?\\)", "(?:\\\\d+?");
                pipes = pipes.replaceAll("\\(\\w+?\\)", "(?:\\\\w+?)");

                // Put the new text in.
                pipes = "(?:" + pipes + "|\\s*)";
                regexp = regexp.replace(optional, pipes);
            }
        }

        // Make \w more accurate for our purposes.
        regexp = regexp.replaceAll("\\\\w", "[a-z ]");

        // Filter in arrays.
        if (regexp.indexOf("@") > -1) {
            // Match the array's name.
            Pattern reArray = Pattern.compile("\\@(.+?)\\b");
            Matcher mArray  = reArray.matcher(regexp);
            while (mArray.find() == true) {
                String array = mArray.group(0);
                String name  = mArray.group(1);

                // Do we have an array by this name?
                if (arrays.containsKey(name)) {
                    String[] values = Util.Sv2s(arrays.get(name));
                    StringBuffer joined = new StringBuffer();

                    // Join the array.
                    for (int i = 0; i < values.length; i++) {
                        joined.append(values[i]);
                        if (i < values.length - 1) {
                            joined.append("|");
                        }
                    }

                    // Final contents...
                    String rep = "(?:" + joined.toString() + ")";
                    regexp = regexp.replace(array, rep);
                }
                else {
                    // No array by this name.
                    regexp = regexp.replace(array, "");
                }
            }
        }

        // Filter in bot variables.
        if (regexp.indexOf("<bot") > -1) {
            Pattern reBot = Pattern.compile("<bot (.+?)>");
            Matcher mBot  = reBot.matcher(regexp);
            while (mBot.find()) {
                String tag = mBot.group(0);
                String var = mBot.group(1);
                String value = vars.get(var).toLowerCase().replace("[^a-z0-9 ]+","");

                // Have this?
                if (vars.containsKey(var)) {
                    regexp = regexp.replace(tag, value);
                }
                else {
                    regexp = regexp.replace(tag, "undefined");
                }
            }
        }

        // Filter in user variables.
        if (regexp.indexOf("<get") > -1) {
            Pattern reGet = Pattern.compile("<get (.+?)>");
            Matcher mGet  = reGet.matcher(regexp);
            while (mGet.find()) {
                String tag = mGet.group(0);
                String var = mGet.group(1);
                String value = profile.get(var).toLowerCase().replaceAll("[^a-z0-9 ]+","");

                // Have this?
                regexp = regexp.replace(tag, value);
            }
        }

        // Input and reply tags.
        regexp = regexp.replaceAll("<input>", "<input1>");
        regexp = regexp.replaceAll("<reply>", "<reply1>");
        if (regexp.indexOf("<input") > -1) {
            Pattern reInput = Pattern.compile("<input([0-9])>");
            Matcher mInput  = reInput.matcher(regexp);
            while (mInput.find()) {
                String tag   = mInput.group(0);
                int    index = Integer.parseInt(mInput.group(1));
                String text  = profile.getInput(index).toLowerCase().replaceAll("[^a-z0-9 ]+","");
                regexp       = regexp.replace(tag, text);
            }
        }
        if (regexp.indexOf("<reply") > -1) {
            Pattern reReply = Pattern.compile("<reply([0-9])>");
            Matcher mReply  = reReply.matcher(regexp);
            while (mReply.find()) {
                String tag   = mReply.group(0);
                int    index = Integer.parseInt(mReply.group(1));
                String text  = profile.getReply(index).toLowerCase().replaceAll("[^a-z0-9 ]+","");
                regexp       = regexp.replace(tag, text);
            }
        }

        return regexp;
    }

    private String processTags(String user,
            Client profile,
            String message,
            String reply,
            ArrayList<String> vstars,
            ArrayList<String> vbotstars,
            int step) {
        return this.processTags(user, profile, message, new BotReply(reply),
                vstars, vbotstars, step).getReply();
    }

    /**
     * Process reply tags.
     *
     * @param user     The name of the end user.
     * @param profile  The RiveScript client object holding the user's profile
     * @param message  The message sent by the user.
     * @param reply    The bot's original reply including tags.
     * @param stars    The vector of wildcards the user's message matched.
     * @param botstars The vector of wildcards in any %Previous.
     * @param step     The current recursion depth limit.
     */
    private BotReply processTags (String user,
            Client profile,
            String message,
            BotReply botReply,
            ArrayList<String> vstars,
            ArrayList<String> vbotstars,
            int step) {

        String reply = botReply.getReply();

        Vector<String> starsList = new Vector<String>(vstars);
        Vector<String> botstarsList = new Vector<String>(vbotstars);

        // Pad the stars.
        starsList.add(0, "");
        botstarsList.add(0, "");

        // Set a default first star.
        if (starsList.size() == 1) {
            starsList.add("undefined");
        }
        if (botstarsList.size() == 1) {
            botstarsList.add("undefined");
        }

        // Convert the stars into simple arrays.
        String[] stars    = Util.Sv2s(starsList);
        String[] botstars = Util.Sv2s(botstarsList);

        // Shortcut tags.
        reply = reply.replaceAll("<person>",    "{person}<star>{/person}");
        reply = reply.replaceAll("<@>",         "{@<star>}");
        reply = reply.replaceAll("<formal>",    "{formal}<star>{/formal}");
        reply = reply.replaceAll("<sentence>",  "{sentence}<star>{/sentence}");
        reply = reply.replaceAll("<uppercase>", "{uppercase}<star>{/uppercase}");
        reply = reply.replaceAll("<lowercase>", "{lowercase}<star>{/lowercase}");

        // Quick tags.
        reply = reply.replaceAll("\\{weight=\\d+\\}", ""); // Remove {weight}s
        reply = reply.replaceAll("<input>", "<input1>");
        reply = reply.replaceAll("<reply>", "<reply1>");
        reply = reply.replaceAll("<id>", user);
        reply = reply.replaceAll("\\\\s", " ");
        reply = reply.replaceAll("\\\\n", "\n");
        reply = reply.replaceAll("\\\\", "\\");
        reply = reply.replaceAll("\\#", "#");

        // Stars
        reply = reply.replaceAll("<star>", stars[1]);
        reply = reply.replaceAll("<botstar>", botstars[1]);
        for (int i = 1; i < stars.length; i++) {
            reply = reply.replaceAll("<star" + i + ">", stars[i]);
        }
        for (int i = 1; i < botstars.length; i++) {
            reply = reply.replaceAll("<botstar" + i + ">", botstars[i]);
        }
        reply = reply.replaceAll("<(star|botstar)\\d+>", "");

        // Input and reply tags.
        if (reply.indexOf("<input") > -1) {
            Pattern reInput = Pattern.compile("<input([0-9])>");
            Matcher mInput  = reInput.matcher(reply);
            while (mInput.find()) {
                String tag   = mInput.group(0);
                int    index = Integer.parseInt(mInput.group(1));
                String text  = profile.getInput(index).toLowerCase().replaceAll("[^a-z0-9 ]+","");
                reply        = reply.replace(tag, text);
            }
        }
        if (reply.indexOf("<reply") > -1) {
            Pattern reReply = Pattern.compile("<reply([0-9])>");
            Matcher mReply  = reReply.matcher(reply);
            while (mReply.find()) {
                String tag   = mReply.group(0);
                int    index = Integer.parseInt(mReply.group(1));
                String text  = profile.getReply(index).toLowerCase().replaceAll("[^a-z0-9 ]+","");
                reply        = reply.replace(tag, text);
            }
        }

        // {random} tag
        if (reply.indexOf("{random}") > -1) {
            Pattern reRandom = Pattern.compile("\\{random\\}(.+?)\\{\\/random\\}");
            Matcher mRandom  = reRandom.matcher(reply);
            while (mRandom.find()) {
                String tag          = mRandom.group(0);
                String[] candidates = mRandom.group(1).split("\\|");
                String chosen = candidates [ rand.nextInt(candidates.length) ];
                reply = reply.replace(tag, chosen);
            }
        }

        // <bot> tag
        if (reply.indexOf("<bot") > -1) {
            Pattern reBot = Pattern.compile("<bot (.+?)>");
            Matcher mBot  = reBot.matcher(reply);
            while (mBot.find()) {
                String tag = mBot.group(0);
                String var = mBot.group(1);

                // Have this?
                if (vars.containsKey(var)) {
                    reply = reply.replace(tag, vars.get(var));
                }
                else {
                    reply = reply.replace(tag, "undefined");
                }
            }
        }

        // <env> tag
        if (reply.indexOf("<env") > -1) {
            Pattern reEnv = Pattern.compile("<env (.+?)>");
            Matcher mEnv  = reEnv.matcher(reply);
            while (mEnv.find()) {
                String tag = mEnv.group(0);
                String var = mEnv.group(1);

                // Have this?
                if (globals.containsKey(var)) {
                    reply = reply.replace(tag, globals.get(var));
                }
                else {
                    reply = reply.replace(tag, "undefined");
                }
            }
        }

        // {!stream} tag
        if (reply.indexOf("{!") > -1) {
            Pattern reStream = Pattern.compile("\\{\\!(.+?)\\}");
            Matcher mStream  = reStream.matcher(reply);
            while (mStream.find()) {
                String tag = mStream.group(0);
                String code = mStream.group(1);
                say("Stream new code in: " + code);

                // Stream it.
                this.stream(code);
                reply = reply.replace(tag, "");
            }
        }

        // {person}
        if (reply.indexOf("{person}") > -1) {
            Pattern rePerson = Pattern.compile("\\{person\\}(.+?)\\{\\/person\\}");
            Matcher mPerson  = rePerson.matcher(reply);
            while (mPerson.find()) {
                String tag  = mPerson.group(0);
                String text = mPerson.group(1);

                // Run person substitutions.
                say("Run person substitutions: before: " + text);
                text = Util.substitute(person_s, person, text);
                say("After: " + text);
                reply = reply.replace(tag, text);
            }
        }

        // {formal,uppercase,lowercase,sentence} tags
        if (reply.indexOf("{formal}") > -1 || reply.indexOf("{sentence}") > -1 ||
                reply.indexOf("{uppercase}") > -1 || reply.indexOf("{lowercase}") > -1) {
            String[] tags = { "formal", "sentence", "uppercase", "lowercase" };
            for (int i = 0; i < tags.length; i++) {
                Pattern reTag = Pattern.compile("\\{" + tags[i] + "\\}(.+?)\\{\\/" + tags[i] + "\\}");
                Matcher mTag  = reTag.matcher(reply);
                while (mTag.find()) {
                    String tag  = mTag.group(0);
                    String text = mTag.group(1);

                    // String transform.
                    text = stringTransform(tags[i], text);
                    reply = reply.replace(tag, text);
                }
            }
        }

        // <set> tag
        if (reply.indexOf("<set") > -1) {
            Pattern reSet = Pattern.compile("<set (.+?)=(.+?)>");
            Matcher mSet  = reSet.matcher(reply);
            while (mSet.find()) {
                String tag   = mSet.group(0);
                String var   = mSet.group(1);
                String value = mSet.group(2);

                // Set the uservar.
                profile.set(var, value);
                reply = reply.replace(tag, "");
                say("Set user var " + var + "=" + value);
            }
        }

        // <add, sub, mult, div> tags
        if (reply.indexOf("<add") > -1 || reply.indexOf("<sub") > -1 ||
                reply.indexOf("<mult") > -1 || reply.indexOf("<div") > -1) {
            String[] tags = { "add", "sub", "mult", "div" };
            for (int i = 0; i < tags.length; i++) {
                Pattern reTag = Pattern.compile("<" + tags[i] + " (.+?)=(.+?)>");
                Matcher mTag  = reTag.matcher(reply);
                while (mTag.find()) {
                    String tag   = mTag.group(0);
                    String var   = mTag.group(1);
                    String value = mTag.group(2);

                    // Get the user var.
                    String curvalue = profile.get(var);
                    int current = 0;
                    if (!curvalue.equals("undefined")) {
                        // Convert it to a int.
                        try {
                            current = Integer.parseInt(curvalue);
                        } catch (NumberFormatException e) {
                            // Current value isn't a number!
                            reply = reply.replace(tag, "[ERR: Can't \"" + tags[i] + "\" non-numeric variable " + var + "]");
                            continue;
                        }
                    }

                    // Value must be a number too.
                    int modifier = 0;
                    try {
                        modifier = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        reply = reply.replace(tag, "[ERR: Can't \"" + tags[i] + "\" non-numeric value " + value + "]");
                        continue;
                    }

                    // Run the operation.
                    if (tags[i].equals("add")) {
                        current += modifier;
                    }
                    else if (tags[i].equals("sub")) {
                        current -= modifier;
                    }
                    else if (tags[i].equals("mult")) {
                        current *= modifier;
                    }
                    else {
                        // Don't divide by zero.
                        if (modifier == 0) {
                            reply = reply.replace(tag, "[ERR: Can't divide by zero!]");
                            continue;
                        }
                        current /= modifier;
                    }

                    // Store the new value.
                    profile.set(var, Integer.toString(current));
                    reply = reply.replace(tag, "");
                }
            }
        }

        // <get> tag
        if (reply.indexOf("<get") > -1) {
            Pattern reGet = Pattern.compile("<get (.+?)>");
            Matcher mGet  = reGet.matcher(reply);
            while (mGet.find()) {
                String tag = mGet.group(0);
                String var = mGet.group(1);

                // Get the user var.
                reply = reply.replace(tag, profile.get(var));
            }
        }

        // {topic} tag
        if (reply.indexOf("{topic=") > -1) {
            Pattern reTopic = Pattern.compile("\\{topic=(.+?)\\}");
            Matcher mTopic  = reTopic.matcher(reply);
            while (mTopic.find()) {
                String tag   = mTopic.group(0);
                String topic = mTopic.group(1);
                say("Set user's topic to: " + topic);
                profile.set("topic", topic);
                reply = reply.replace(tag, "");
            }
        }

        // {@redirect} tag
        if (reply.indexOf("{@") > -1) {
            Pattern reRed = Pattern.compile("\\{@(.+?)\\}");
            Matcher mRed  = reRed.matcher(reply);
            while (mRed.find()) {
                String tag    = mRed.group(0);
                String target = mRed.group(1).trim();

                // Do the reply redirect.
                botReply = this.reply(user, target, false, step+1);
                reply = reply.replace(tag, botReply.getReply());
            }
        }



        // <call> tag
        if (reply.indexOf("<call>") > -1) {
            Pattern reCall = Pattern.compile("<call>(.+?)<\\/call>");
            Matcher mCall  = reCall.matcher(reply);
            while (mCall.find()) {
                String tag = mCall.group(0);
                String data = mCall.group(1);
                String[] parts = data.split(" ");
                String name = parts[0];
                Vector<String> args = new Vector<String>();
                for (int i = 1; i < parts.length; i++) {
                    args.add(parts[i]);
                }

                // See if we know of this object.
                if (objects.containsKey(name)) {
                    // What language handles it?
                    String lang = objects.get(name);
                    botReply = handlers.get(lang).onCall(
                            name, user, Util.Sv2s(args));
                    if (botReply.getReply() != null) {
                        reply = reply.replace(tag, botReply.getReply());
                    } else {
                        reply = reply.replace(tag, "[ERR: Handler found but reply is null]");
                    }
                }
                else {
                    reply = reply.replace(tag, "[ERR: Object Not Found]");
                }
            }
        }

        botReply.setReply(reply);

        return botReply;
    }

    /**
     * Reformats a string in a certain way: formal, uppercase, lowercase, sentence.
     *
     * @param format The format you want the string in.
     * @param text   The text to format.
     */
    private String stringTransform (String format, String text) {
        if (format.equals("uppercase")) {
            return text.toUpperCase();
        }
        else if (format.equals("lowercase")) {
            return text.toLowerCase();
        }
        else if (format.equals("formal")) {
            // Capitalize Each First Letter
            String[] words = text.split(" ");
            say("wc: " + words.length);
            for (int i = 0; i < words.length; i++) {
                say("word: " + words[i]);
                String[] letters = words[i].split("");
                say("cc: " + letters.length);
                if (letters.length > 1) {
                    say("letter 1: " + letters[1]);
                    letters[1] = letters[1].toUpperCase();
                    say("new letter 1: " + letters[1]);
                    words[i] = Util.join(letters, "");
                    say("new word: " + words[i]);
                }
            }
            return Util.join(words, " ");
        }
        else if (format.equals("sentence")) {
            // Uppercase the first letter of the first word.
            String[] letters = text.split("");
            if (letters.length > 1) {
                letters[1] = letters[1].toUpperCase();
            }
            return Util.join(letters, "");
        }
        else {
            return "[ERR: Unknown String Transform " + format + "]";
        }
    }

    /**
     * Format the user's message to begin reply matching. Lowercases it, runs substitutions,
     * and neutralizes what's left.
     *
     * @param message The input message to format.
     */
    private String formatMessage (String message) {
        // Lowercase it first.
        message = message.toLowerCase();

        // Run substitutions.
        message = Util.substitute(subs_s, subs, message);

        // Sanitize what's left.
        message = message.replaceAll("\\<>", "");
        message = Normalizer.normalize(message, Normalizer.Form.NFD);
        message = message.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return message;
    }

    /*-----------------------*/
    /*-- Developer Methods --*/
    /*-----------------------*/

    /**
     * DEVELOPER: Dump the trigger sort buffers to the terminal.
     */
    public void dumpSorted() {
        String[] topics = this.topics.listTopics();
        for (int t = 0; t < topics.length; t++) {
            String topic = topics[t];
            String[] triggers = this.topics.topic(topic).listTriggers();

            // Dump.
            System.out.println("Topic: " + topic);
            for (int i = 0; i < triggers.length; i++) {
                System.out.println("       " + triggers[i]);
            }
        }
    }

    /**
     * DEVELOPER: Dump the entire topic/trigger/reply structure to the terminal.
     */
    public void dumpTopics () {
        // Dump the topic list.
        System.out.println("{");
        String[] topicList = topics.listTopics();
        for (int t = 0; t < topicList.length; t++) {
            String topic = topicList[t];
            String extra = "";

            // Includes? Inherits?
            String[] includes = topics.topic(topic).includes();
            String[] inherits = topics.topic(topic).inherits();
            if (includes.length > 0) {
                extra = "includes ";
                for (int i = 0; i < includes.length; i++) {
                    extra += includes[i] + " ";
                }
            }
            if (inherits.length > 0) {
                extra += "inherits ";
                for (int i = 0; i < inherits.length; i++) {
                    extra += inherits[i] + " ";
                }
            }
            System.out.println("  '" + topic + "' " + extra + " => {");

            // Dump the trigger list.
            String[] trigList = topics.topic(topic).listTriggers();
            for (int i = 0; i < trigList.length; i++) {
                String trig = trigList[i];
                System.out.println("    '" + trig + "' => {");

                // Dump the replies.
                String[] reply = topics.topic(topic).trigger(trig).listReplies();
                if (reply.length > 0) {
                    System.out.println("      'reply' => [");
                    for (int r = 0; r < reply.length; r++) {
                        System.out.println("        '" + reply[r] + "',");
                    }
                    System.out.println("      ],");
                }

                // Dump the conditions.
                String[] cond = topics.topic(topic).trigger(trig).listConditions();
                if (cond.length > 0) {
                    System.out.println("      'condition' => [");
                    for (int r = 0; r < cond.length; r++) {
                        System.out.println("        '" + cond[r] + "',");
                    }
                    System.out.println("      ],");
                }

                // Dump the redirects.
                String[] red = topics.topic(topic).trigger(trig).listRedirects();
                if (red.length > 0) {
                    System.out.println("      'redirect' => [");
                    for (int r = 0; r < red.length; r++) {
                        System.out.println("        '" + red[r] + "',");
                    }
                    System.out.println("      ],");
                }

                System.out.println("    },");
            }

            System.out.println("  },");
        }
    }

    /*-------------------*/
    /*-- Debug Methods --*/
    /*-------------------*/

    /**
     * Print a line of debug text to the terminal.
     *
     * @param line The line of text to print.
     */
    protected void say (String line) {
        if (this.debug) {
            System.out.println("[RS] " + line);
        }
    }

    /**
     * Print a line of warning text to the terminal.
     *
     * @param line The line of warning text.
     */
    private void cry (String line) {
        System.out.println("<RS> " + line);
    }

    /**
     * Print a line of warning text including a file name and line number.
     *
     * @param text The warning text.
     * @param file The file name.
     * @param line The line number.
     */
    private void cry (String text, String file, int line) {
        System.out.println("<RS> " + text + " at " + file + " line " + line + ".");
    }

    /**
     * Print a stack trace to the terminal when debug mode is on.
     *
     * @param e The IOException object.
     */
    private void trace (IOException e) {
        if (this.debug) {
            e.printStackTrace();
        }
    }
}
