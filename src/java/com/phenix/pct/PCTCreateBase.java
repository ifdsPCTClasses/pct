/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.phenix.pct;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Environment.Variable;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for creating Progress databases
 * 
 * @author <a href="mailto:g.querret+PCT@gmail.com">Gilles QUERRET </a>
 */
public class PCTCreateBase extends PCT {
    private static final int DEFAULT_BLOCK_SIZE = 8;
    private static final int DB_NAME_MAX_LENGTH = 11;

    private String dbName = null;
    private String codepage = null;
    private File destDir = null;
    private File structFile = null;
    private int blockSize = DEFAULT_BLOCK_SIZE;
    private boolean noInit = false;
    private boolean overwrite = false;
    private String schema = null;
    private Path propath = null;
    private int[] blocks = {0, 1024, 2048, 0, 4096, 0, 0, 0, 8192};
    private int wordRule = -1;
    private List<SchemaHolder> holders = null;

    /**
     * Structure file (.st)
     * 
     * @param structFile File
     */
    public void setStructFile(File structFile) {
        this.structFile = structFile;
    }

    /**
     * Database name
     * 
     * @param dbName String
     */
    public void setDBName(String dbName) {
        this.dbName = dbName;
    }

    /**
     * If database shouldn't be initialized
     * 
     * @param noInit "true|false|on|off|yes|no"
     */
    public void setNoInit(boolean noInit) {
        this.noInit = noInit;
    }

    /**
     * No schema
     * 
     * @param noSchema "true|false|on|off|yes|no"
     */
    public void setNoSchema(boolean noSchema) {
        this.log(Messages.getString("PCTCreateBase.0")); //$NON-NLS-1$
    }

    /**
     * Block size
     * 
     * @param blockSize int
     */
    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    /**
     * In which directory create the database
     * 
     * @param destDir File
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Overwrite database if existent
     * 
     * @param overwrite "true|false|on|off|yes|no"
     */
    public void setOverwrite(boolean overwrite) {
        log(Messages.getString("PCTCreateBase.1")); //$NON-NLS-1$
        this.overwrite = overwrite;
    }

    /**
     * Load schema after creating database. Multiple schemas can be loaded : seperate them with
     * commas e.g. dump1.df,dump2.df,dump3.df
     * 
     * @param schemaFile String
     */
    public void setSchemaFile(String schemaFile) {
        this.schema = schemaFile;
    }

    /**
     * Set the propath to be used when running the procedure
     * 
     * @param propath an Ant Path object containing the propath
     */
    public void setPropath(Path propath) {
        createPropath().append(propath);
    }

    /**
     * Creates a new Path instance
     * 
     * @return Path
     */
    public Path createPropath() {
        if (this.propath == null) {
            this.propath = new Path(this.getProject());
        }

        return this.propath;
    }

    /**
     * Set the desired database codepage (copy from $DLC/prolang/codepage/emptyX)
     * 
     * @param codepage Subdirectory name from prolang directory where to find the empty database
     */
    public void setCodepage(String codepage) {
        this.codepage = codepage;
    }

    /**
     * Set the word file rule number applied to this database
     * 
     * @param wordRule Integer (0-255)
     */
    public void setWordRules(int wordRule) {
        if ((wordRule < 0) || (wordRule > 255))
            throw new BuildException("wordRule value should be between 0 and 255");
        this.wordRule = wordRule;
    }

    /**
     * Adds an Oracle schema holder
     * 
     * @param holder Instance of OracleHolder
     */
    public void addOracleHolder(OracleHolder holder) {
        if (this.holders == null) {
            this.holders = new ArrayList<SchemaHolder>();
        }
        this.holders.add(holder);
    }

    /**
     * Adds an SQL Server schema holder
     * 
     * @param holder Instance of MSSHolder
     */
    public void addMSSHolder(MSSHolder holder) {
        if (this.holders == null) {
            this.holders = new ArrayList<SchemaHolder>();
        }
        this.holders.add(holder);
    }

    /**
     * Adds an ODBC schema holder
     * 
     * @param holder Instance of ODBCHolder
     */
    public void addODBCHolder(ODBCHolder holder) {
        if (this.holders == null) {
            this.holders = new ArrayList<SchemaHolder>();
        }
        this.holders.add(holder);
    }

    /**
     * Do the work
     * 
     * @throws BuildException Something went wrong
     */
    public void execute() throws BuildException {
        ExecTask exec = null;

        checkDlcHome();
        // TODO : rediriger la sortie standard
        // Checking there is at least an init or a structure creation
        if ((structFile == null) && noInit) {
            throw new BuildException(Messages.getString("PCTCreateBase.2")); //$NON-NLS-1$
        }

        // Checking dbName is defined
        if (dbName == null) {
            throw new BuildException(Messages.getString("PCTCreateBase.3")); //$NON-NLS-1$
        }

        // If schema holders defined, then no Progress schema can be loaded
        if ((holders != null) && (holders.size() > 0)) {
            if ((schema != null) && (schema.trim().length() > 0)) {
                throw new BuildException("On peut pas !!!");
            }
            // noInit also cannot be set to true
            if (noInit) {
                throw new BuildException("on peut pas non plus !!");
            }
        }

        // Update destDir if not defined
        if (destDir == null) {
            destDir = getProject().getBaseDir();
        }

        // Checking length of the database name
        if (dbName.length() > DB_NAME_MAX_LENGTH) {
            throw new BuildException(Messages.getString("PCTCreateBase.4")); //$NON-NLS-1$
        }

        // Checks if DB already exists
        File db = new File(destDir, dbName + ".db"); //$NON-NLS-1$

        if (db.exists()) {
            if (overwrite) {
                // TODO : revoir l'effacement de l'ancienne base
                Delete del = new Delete();
                del.bindToOwner(this);
                del.setOwningTarget(getOwningTarget());
                del.setTaskName(getTaskName());
                del.setDescription(getDescription());
                del.setFile(db);
                del.execute();
            } else {
                return;
            }
        }

        if (structFile != null) {
            if (!structFile.exists())
                throw new BuildException(MessageFormat.format(
                        Messages.getString("PCTCreateBase.6"), structFile.getAbsolutePath()));
            exec = structCmdLine();
            exec.execute();
        }

        if (!noInit) {
            exec = initCmdLine();
            exec.execute();
        }

        // Word rules are loaded before schema to avoid problems with newly created indexes
        if (wordRule != -1) {
            exec = wordRuleCmdLine();
            exec.execute();
        }

        if (schema != null) {

            String[] v = schema.split(",");
            for (int i = 0; i < v.length; i++) {
                String sc = v[i];
                // Bug #1245992 : use Project#resolveFile(String)
                File f = getProject().resolveFile(sc);
                if (f.isFile() && f.canRead()) {
                    PCTLoadSchema pls = new PCTLoadSchema();
                    pls.bindToOwner(this);
                    pls.setSrcFile(f);
                    pls.setDlcHome(getDlcHome());
                    pls.setDlcBin(getDlcBin());
                    pls.addPropath(propath);
                    pls.setIncludedPL(getIncludedPL());
                    for (Variable var : getEnvironmentVariables()) {
                        pls.addEnv(var);
                    }

                    PCTConnection pc = new PCTConnection();
                    pc.setDbName(dbName);
                    pc.setDbDir(destDir);
                    pc.setSingleUser(true);
                    pls.addDBConnection(pc);
                    pls.execute();
                } else {
                    throw new BuildException(MessageFormat.format(
                            Messages.getString("PCTCreateBase.5"), f));
                }
            }
        }

        if (holders != null) {
            for (SchemaHolder holder : holders) {
                PCTRun run = new PCTRun();
                run.bindToOwner(this);
                run.setDlcHome(getDlcHome());
                run.setDlcBin(getDlcBin());
                run.addPropath(propath);
                run.setIncludedPL(getIncludedPL());
                run.setProcedure(holder.getProcedure());
                run.setParameters(holder.getParameters());

                PCTConnection pc = new PCTConnection();
                pc.setDbName(dbName);
                pc.setDbDir(destDir);
                pc.setSingleUser(true);
                run.addDBConnection(pc);
                run.execute();

                if (holder.getSchemaFile() != null) {
                    PCTLoadSchema pls = new PCTLoadSchema();
                    pls.bindToOwner(this);
                    pls.setSrcFile(holder.getSchemaFile());
                    pls.setDlcHome(getDlcHome());
                    pls.setDlcBin(getDlcBin());
                    pls.addPropath(propath);
                    pls.addDBConnection(pc);
                    pls.execute();

                }
            }
        }
    }

    /**
     * Creates the _dbutil procopy emptyX command line
     * 
     * @return An ExecTask, ready to be executed
     */
    private ExecTask initCmdLine() {
        ExecTask exec = new ExecTask(this);

        File srcDir = getDlcHome();
        if (codepage != null) {
            srcDir = new File(srcDir, "prolang"); //$NON-NLS-1$
            srcDir = new File(srcDir, codepage);
        }
        File srcDB = new File(srcDir, "empty" + blockSize); //$NON-NLS-1$

        exec.setExecutable(getExecPath("_dbutil").toString()); //$NON-NLS-1$
        exec.setDir(destDir);
        exec.createArg().setValue("procopy"); //$NON-NLS-1$
        exec.createArg().setValue(srcDB.getAbsolutePath());
        exec.createArg().setValue(dbName);

        Environment.Variable var = new Environment.Variable();
        var.setKey("DLC"); //$NON-NLS-1$
        var.setValue(getDlcHome().toString());
        exec.addEnv(var);

        for (Variable var2 : getEnvironmentVariables()) {
            exec.addEnv(var2);
        }

        return exec;
    }

    /**
     * Creates the _dbutil prostrct create command line
     * 
     * @return An ExecTask, ready to be executed
     */
    private ExecTask structCmdLine() {
        ExecTask exec = new ExecTask(this);

        exec.setExecutable(getExecPath("_dbutil").toString()); //$NON-NLS-1$
        exec.setDir(destDir);
        exec.createArg().setValue("prostrct"); //$NON-NLS-1$
        exec.createArg().setValue("create"); //$NON-NLS-1$
        exec.createArg().setValue(dbName);
        exec.createArg().setValue(structFile.getAbsolutePath());
        exec.createArg().setValue("-blocksize"); //$NON-NLS-1$
        exec.createArg().setValue(Integer.toString(blocks[blockSize]));

        Environment.Variable var = new Environment.Variable();
        var.setKey("DLC"); //$NON-NLS-1$
        var.setValue(getDlcHome().toString());
        exec.addEnv(var);

        for (Variable var2 : getEnvironmentVariables()) {
            exec.addEnv(var2);
        }

        return exec;
    }

    private ExecTask wordRuleCmdLine() {
        ExecTask exec = new ExecTask(this);
        exec.setExecutable(getExecPath("_proutil").toString()); //$NON-NLS-1$
        exec.setDir(destDir);
        exec.createArg().setValue(dbName);
        exec.createArg().setValue("-C"); //$NON-NLS-1$
        exec.createArg().setValue("word-rules"); //$NON-NLS-1$
        exec.createArg().setValue(String.valueOf(wordRule));

        Environment.Variable var = new Environment.Variable();
        var.setKey("DLC"); //$NON-NLS-1$
        var.setValue(getDlcHome().toString());
        exec.addEnv(var);

        for (Variable var2 : getEnvironmentVariables()) {
            exec.addEnv(var2);
        }

        return exec;
    }
}