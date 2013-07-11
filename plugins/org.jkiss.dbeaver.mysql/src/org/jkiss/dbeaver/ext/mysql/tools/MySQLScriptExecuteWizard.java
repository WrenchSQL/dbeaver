/*
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLMessages;
import org.jkiss.dbeaver.ext.mysql.MySQLServerHome;
import org.jkiss.dbeaver.ext.mysql.MySQLUtils;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractScriptExecuteWizard;

import java.io.IOException;
import java.util.List;

class MySQLScriptExecuteWizard extends AbstractScriptExecuteWizard<MySQLCatalog> {

    enum LogLevel {
        Normal,
        Verbose,
        Debug
    }

    private LogLevel logLevel;
    private boolean noBeep;

    private boolean isImport;
    private MySQLScriptExecuteWizardPageSettings mainPage;

    public MySQLScriptExecuteWizard(MySQLCatalog catalog, boolean isImport)
    {
        super(catalog, isImport ? MySQLMessages.tools_script_execute_wizard_db_import : MySQLMessages.tools_script_execute_wizard_execute_script);
        this.isImport = isImport;
        this.logLevel = LogLevel.Normal;
        this.noBeep = true;
        this.mainPage = new MySQLScriptExecuteWizardPageSettings(this);
    }

    public LogLevel getLogLevel()
    {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel)
    {
        this.logLevel = logLevel;
    }

    public boolean isImport()
    {
        return isImport;
    }

    @Override
    public boolean isVerbose()
    {
        return logLevel == LogLevel.Verbose || logLevel == LogLevel.Debug;
    }

    @Override
    public void addPages()
    {
        addPage(mainPage);
        super.addPages();
    }

    @Override
    public void fillProcessParameters(List<String> cmd) throws IOException
    {
        String dumpPath = MySQLUtils.getHomeBinary(getClientHome(), "mysql").getAbsolutePath(); //$NON-NLS-1$
        cmd.add(dumpPath);
        if (logLevel == LogLevel.Debug) {
            cmd.add("--debug-info"); //$NON-NLS-1$
        }
        if (noBeep) {
            cmd.add("--no-beep"); //$NON-NLS-1$
        }
    }

    @Override
    public MySQLServerHome findServerHome(String clientHomeId)
    {
        return MySQLDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    protected List<String> getCommandLine() throws IOException
    {
        return MySQLToolScript.getMySQLToolCommandLine(this);
    }
}
