/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTrigger;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSActionTiming;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;

import java.sql.ResultSet;

/**
 * GenericProcedure
 */
public class PostgreTrigger extends AbstractTrigger implements PostgreScriptObject
{
    static final Log log = Log.getLog(PostgreTrigger.class);

    private PostgreSchema schema;
    private PostgreTable table;
    private String body;
    private String charsetClient;
    private String sqlMode;

    public PostgreTrigger(
        PostgreSchema schema,
        PostgreTable table,
        ResultSet dbResult)
    {
        this.schema = schema;
        this.table = table;

        setName(JDBCUtils.safeGetString(dbResult, "Trigger"));
        setManipulationType(DBSManipulationType.getByName(JDBCUtils.safeGetString(dbResult, "Event")));
        setActionTiming(DBSActionTiming.getByName(JDBCUtils.safeGetString(dbResult, "Timing")));
        this.body = JDBCUtils.safeGetString(dbResult, "Statement");
        this.charsetClient = JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_TRIGGER_CHARACTER_SET_CLIENT);
        this.sqlMode = JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_TRIGGER_SQL_MODE);
    }

    public String getBody()
    {
        return body;
    }

    @Override
    @Property(viewable = true, order = 4)
    public PostgreTable getTable()
    {
        return table;
    }

    @Property(order = 5)
    public String getCharsetClient()
    {
        return charsetClient;
    }

    @Property(order = 6)
    public String getSqlMode()
    {
        return sqlMode;
    }

    @Override
    public PostgreSchema getParentObject()
    {
        return schema;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return schema.getDataSource();
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        return getBody();
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException
    {
        body = sourceText;
    }
}
