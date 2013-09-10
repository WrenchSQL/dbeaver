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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TriggerDepCache;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TriggerEvent;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TriggerGranularity;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TriggerTime;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TriggerValid;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.ext.db2.model.source.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.model.source.DB2SourceType;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 Table Trigger
 * 
 * @author Denis Forveille
 * 
 */
public class DB2Trigger extends DB2SchemaObject implements DBSTrigger, DB2SourceObject, DBPRefreshableObject {

   private final DB2TriggerDepCache triggerDepCache = new DB2TriggerDepCache();

   private DB2Table                 table;

   private String                   owner;
   private DB2OwnerType             ownerType;
   private DB2TriggerTime           time;
   private DB2TriggerEvent          event;
   private Boolean                  eventUpdate;
   private Boolean                  eventDelete;
   private Boolean                  eventInsert;
   private DB2TriggerGranularity    granularity;
   private DB2TriggerValid          valid;
   private Timestamp                createTime;
   private String                   qualifier;
   private String                   funcPath;
   private String                   text;
   private Timestamp                lastRegenTime;
   private String                   collationSchema;
   private String                   collationName;
   private String                   collationSchemaOrderBy;
   private String                   collationNameOrderBy;
   private Boolean                  secure;
   private Timestamp                alterTime;
   private Integer                  libId;
   private String                   precompileOptions;
   private String                   compileOptions;
   private String                   remarks;

   // -----------------------
   // Constructors
   // -----------------------

   public DB2Trigger(DB2Schema schema, DB2Table table, ResultSet dbResult) {
      super(schema, JDBCUtils.safeGetString(dbResult, "TRIGNAME"), true);

      this.table = table;

      this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
      this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
      this.time = CommonUtils.valueOf(DB2TriggerTime.class, JDBCUtils.safeGetString(dbResult, "TRIGTIME"));
      this.event = CommonUtils.valueOf(DB2TriggerEvent.class, JDBCUtils.safeGetString(dbResult, "TRIGEVENT"));
      this.eventUpdate = JDBCUtils.safeGetBoolean(dbResult, "EVENTUPDATE", DB2YesNo.Y.name());
      this.eventDelete = JDBCUtils.safeGetBoolean(dbResult, "EVENTDELETE", DB2YesNo.Y.name());
      this.eventInsert = JDBCUtils.safeGetBoolean(dbResult, "EVENTINSERT", DB2YesNo.Y.name());
      this.granularity = CommonUtils.valueOf(DB2TriggerGranularity.class, JDBCUtils.safeGetString(dbResult, "GRANULARITY"));
      this.valid = CommonUtils.valueOf(DB2TriggerValid.class, JDBCUtils.safeGetString(dbResult, "VALID"));
      this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
      this.qualifier = JDBCUtils.safeGetString(dbResult, "QUALIFIER");
      this.funcPath = JDBCUtils.safeGetString(dbResult, "FUNC_PATH");
      this.text = JDBCUtils.safeGetString(dbResult, "TEXT");
      this.lastRegenTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REGEN_TIME");
      this.collationSchema = JDBCUtils.safeGetString(dbResult, "COLLATIONSCHEMA");
      this.collationName = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME");
      this.collationSchemaOrderBy = JDBCUtils.safeGetString(dbResult, "COLLATIONSCHEMA_ORDERBY");
      this.collationNameOrderBy = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME_ORDERBY");
      this.secure = JDBCUtils.safeGetBoolean(dbResult, "SECURE", DB2YesNo.Y.name());
      this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, "ALTER_TIME");
      this.libId = JDBCUtils.safeGetInteger(dbResult, "LIB_ID");
      this.precompileOptions = JDBCUtils.safeGetString(dbResult, "PRECOMPILE_OPTIONS");
      this.compileOptions = JDBCUtils.safeGetString(dbResult, "COMPILE_OPTIONS");
      this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");
   }

   @Override
   public DBSObjectState getObjectState() {
      return valid.getState();
   }

   @Override
   public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
      // TODO DF: what to do here?
   }

   // -----------------
   // Source
   // -----------------

   @Override
   public DB2SourceType getSourceType() {
      return DB2SourceType.TRIGGER;
   }

   @Override
   public String getSourceDeclaration(DBRProgressMonitor monitor) throws DBException {
      return this.text;
   }

   @Override
   public void setSourceDeclaration(String source) {
      this.text = source;
   }

   @Override
   public IDatabasePersistAction[] getCompileActions() {
      // TODO Auto-generated method stub
      return null;
   }

   // -----------------
   // Association
   // -----------------

   @Association
   public Collection<DB2TriggerDep> getTriggerDeps(DBRProgressMonitor monitor) throws DBException {
      return triggerDepCache.getObjects(monitor, this);
   }

   @Override
   public boolean refreshObject(DBRProgressMonitor monitor) throws DBException {
      triggerDepCache.clearCache();
      return true;
   }

   // -----------------
   // Properties
   // -----------------

   @Override
   @Property(viewable = true, editable = false, order = 1)
   public String getName() {
      return super.getName();
   }

   @Property(viewable = true, editable = false, order = 2, id = "Schema")
   public DB2Schema getSchema() {
      return super.getParentObject();
   }

   @Override
   @Property(viewable = true, order = 3)
   public DB2Table getTable() {
      return table;
   }

   @Property(viewable = true, order = 4)
   public DB2TriggerValid getValid() {
      return valid;
   }

   @Property(viewable = true, order = 5)
   public String getEventDescription() {
      return event.getDescription();
   }

   @Property(viewable = true, order = 6)
   public String getTimeDescription() {
      return time.getDescription();
   }

   @Property(viewable = true, order = 7)
   public Boolean getEventUpdate() {
      return eventUpdate;
   }

   @Property(viewable = true, order = 8)
   public Boolean getEventDelete() {
      return eventDelete;
   }

   @Property(viewable = true, order = 9)
   public Boolean getEventInsert() {
      return eventInsert;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
   public String getOwner() {
      return owner;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
   public DB2OwnerType getOwnerType() {
      return ownerType;
   }

   @Property(viewable = true)
   public String getGranularityDescription() {
      return granularity.getDescription();
   }

   @Property(viewable = false)
   public String getQualifier() {
      return qualifier;
   }

   @Property(viewable = false)
   public String getFuncPath() {
      return funcPath;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
   public Timestamp getCreateTime() {
      return createTime;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
   public Timestamp getLastRegenTime() {
      return lastRegenTime;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
   public Timestamp getAlterTime() {
      return alterTime;
   }

   @Property(viewable = false, category = DB2Constants.CAT_COLLATION)
   public String getCollationSchema() {
      return collationSchema;
   }

   @Property(viewable = false, category = DB2Constants.CAT_COLLATION)
   public String getCollationName() {
      return collationName;
   }

   @Property(viewable = false, category = DB2Constants.CAT_COLLATION)
   public String getCollationSchemaOrderBy() {
      return collationSchemaOrderBy;
   }

   @Property(viewable = false, category = DB2Constants.CAT_COLLATION)
   public String getCollationNameOrderBy() {
      return collationNameOrderBy;
   }

   @Property(viewable = false)
   public Boolean getSecure() {
      return secure;
   }

   @Property(viewable = false)
   public Integer getLibId() {
      return libId;
   }

   @Property(viewable = false, category = DB2Constants.CAT_COMPILER)
   public String getPrecompileOptions() {
      return precompileOptions;
   }

   @Property(viewable = false, category = DB2Constants.CAT_COMPILER)
   public String getCompileOptions() {
      return compileOptions;
   }

   @Override
   @Property(viewable = false)
   public String getDescription() {
      return remarks;
   }

}
