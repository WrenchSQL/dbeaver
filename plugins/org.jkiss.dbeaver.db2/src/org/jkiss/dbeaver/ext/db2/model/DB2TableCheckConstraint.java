/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableCheckConstraintType;
import org.jkiss.dbeaver.ext.db2.model.source.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.model.source.DB2SourceType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

/**
 * DB2 Table Check Constraint
 * 
 * @author Denis Forveille
 */
public class DB2TableCheckConstraint extends JDBCTableConstraint<DB2Table> implements DB2SourceObject {

    private String owner;
    private DB2OwnerType ownerType;
    private Timestamp createTime;
    private String qualifier;
    private DB2TableCheckConstraintType type;
    private String fumcPath;
    private String text;
    private Integer precentValid;
    private String collationSchema;
    private String collationName;
    private String collationSchemaOrderBy;
    private String collationNameOrderBy;

    private List<DB2TableCheckConstraintColumn> columns;

    // -----------------
    // Constructor
    // -----------------

    public DB2TableCheckConstraint(DBRProgressMonitor monitor, DB2Table table, ResultSet dbResult) throws DBException
    {
        super(table, JDBCUtils.safeGetString(dbResult, "CONSTNAME"), null, DBSEntityConstraintType.CHECK, true);

        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.qualifier = JDBCUtils.safeGetString(dbResult, "QUALIFIER");
        this.type = CommonUtils.valueOf(DB2TableCheckConstraintType.class, JDBCUtils.safeGetString(dbResult, "TYPE"));
        this.fumcPath = JDBCUtils.safeGetString(dbResult, "FUNC_PATH");
        this.text = JDBCUtils.safeGetString(dbResult, "TEXT");
        this.precentValid = JDBCUtils.safeGetInteger(dbResult, "PERCENTVALID");
        this.collationSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATIONSCHEMA");
        this.collationName = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME");
        this.collationSchemaOrderBy = JDBCUtils.safeGetString(dbResult, "COLLATIONSCHEMA_ORDERBY");
        this.collationNameOrderBy = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME_ORDERBY");
    }

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
    }

    @Override
    public DB2DataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    // -----------------
    // Columns
    // -----------------

    @Override
    public Collection<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor) throws DBException
    {
        return columns;
    }

    public void setColumns(List<DB2TableCheckConstraintColumn> columns)
    {
        this.columns = columns;
    }

    // -----------------
    // Source
    // -----------------

    @Override
    public DB2Schema getSchema()
    {
        return getTable().getSchema();
    }

    @Override
    public DBSObjectState getObjectState()
    {
        return DBSObjectState.UNKNOWN;
    }

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException
    {
        // TODO Auto-generated method stub
    }

    @Override
    public DB2SourceType getSourceType()
    {
        return DB2SourceType.PROCEDURE; // TODO DF: no real correspondance
    }

    @Override
    public String getSourceDeclaration(DBRProgressMonitor monitor) throws DBException
    {
        return text;
    }

    @Override
    public void setSourceDeclaration(String source)
    {
        // TODO Auto-generated method stub
    }

    @Override
    public IDatabasePersistAction[] getCompileActions()
    {
        // TODO Auto-generated method stub
        return null;
    }

    // -----------------
    // Properties
    // -----------------
    @Override
    @Property(viewable = true, editable = false, order = 2)
    public DB2Table getTable()
    {
        return super.getTable();
    }

    @Override
    @Property(hidden = true)
    public DBSEntityConstraintType getConstraintType()
    {
        return super.getConstraintType();
    }

    @Property(viewable = true, editable = false, order = 3)
    public DB2TableCheckConstraintType getType()
    {
        return type;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, editable = false)
    public String getQualifier()
    {
        return qualifier;
    }

    @Property(viewable = false, editable = false)
    public String getFumcPath()
    {
        return fumcPath;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_STATS)
    public Integer getPrecentValid()
    {
        return precentValid;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationSchema()
    {
        return collationSchema;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationName()
    {
        return collationName;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationSchemaOrderBy()
    {
        return collationSchemaOrderBy;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationNameOrderBy()
    {
        return collationNameOrderBy;
    }

}
