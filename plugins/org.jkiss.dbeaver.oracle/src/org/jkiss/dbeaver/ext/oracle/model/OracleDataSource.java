/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.oracle.OracleDataSourceProvider;
import org.jkiss.dbeaver.ext.oracle.model.plan.OraclePlanAnalyser;
import org.jkiss.dbeaver.ext.oracle.oci.OCIUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLConstants;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * GenericDataSource
 */
public class OracleDataSource extends JDBCDataSource
    implements DBSObjectSelector, DBCQueryPlanner, IAdaptable
{
    static final Log log = LogFactory.getLog(OracleDataSource.class);

    //private final static Map<String, OCIClassLoader> ociClassLoadersCache = new HashMap<String, OCIClassLoader>();

    final public SchemaCache schemaCache = new SchemaCache();
    final DataTypeCache dataTypeCache = new DataTypeCache();
    final TablespaceCache tablespaceCache = new TablespaceCache();
    final UserCache userCache = new UserCache();
    final ProfileCache profileCache = new ProfileCache();
    final RoleCache roleCache = new RoleCache();

    private OracleSchema publicSchema;
    private String activeSchemaName;
    private boolean isAdmin;
    private boolean isAdminVisible;
    private String planTableName;

    public OracleDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        super(monitor, container);
    }

    @Override
    protected JDBCConnectionHolder openConnection(DBRProgressMonitor monitor, String purpose) throws DBCException
    {
        // Set tns admin directory
        DBPClientHome clientHome = getContainer().getClientHome();
        if (clientHome != null) {
            System.setProperty("oracle.net.tns_admin", new File(clientHome.getHomePath(), OCIUtils.TNSNAMES_FILE_PATH).getAbsolutePath());
        }

        JDBCConnectionHolder connectionHolder = super.openConnection(monitor, purpose);
/*
        OracleConnection oracleConnection = (OracleConnection)connectionHolder.getConnection();

        try {
            oracleConnection.setClientInfo("ApplicationName", DBeaverCore.getProductTitle() + " - " + purpose);
        } catch (Throwable e) {
            // just ignore
            log.debug(e);
        }
*/

        return connectionHolder;
    }

    @Override
    protected String getConnectionUserName(DBPConnectionInfo connectionInfo)
    {
        final Object role = connectionInfo.getProperties().get(OracleConstants.PROP_INTERNAL_LOGON);
        return role == null ? connectionInfo.getUserName() : connectionInfo.getUserName() + " AS " + role;
    }

    @Override
    protected DBPDataSourceInfo makeInfo(JDBCDatabaseMetaData metaData)
    {
        final JDBCDataSourceInfo info = new JDBCDataSourceInfo(metaData);
        for (String kw : OracleConstants.ADVANCED_KEYWORDS) {
            info.addSQLKeyword(kw);
        }
        return info;
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties()
    {
        return OracleDataSourceProvider.getConnectionsProps();
    }

    public boolean isAdmin()
    {
        return isAdmin;
    }

    public boolean isAdminVisible()
    {
        return isAdmin || isAdminVisible;
    }

    @Association
    public Collection<OracleSchema> getSchemas(DBRProgressMonitor monitor) throws DBException
    {
        return schemaCache.getObjects(monitor, this);
    }

    public OracleSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException
    {
        if (publicSchema != null && publicSchema.getName().equals(name)) {
            return publicSchema;
        }
        return schemaCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<OracleTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException
    {
        return tablespaceCache.getObjects(monitor, this);
    }

    @Association
    public Collection<OracleUser> getUsers(DBRProgressMonitor monitor) throws DBException
    {
        return userCache.getObjects(monitor, this);
    }

    @Association
    public Collection<OracleUserProfile> getProfiles(DBRProgressMonitor monitor) throws DBException
    {
        return profileCache.getObjects(monitor, this);
    }

    @Association
    public Collection<OracleRole> getRoles(DBRProgressMonitor monitor) throws DBException
    {
        return roleCache.getObjects(monitor, this);
    }

    @Association
    public Collection<OracleSynonym> getPublicSynonyms(DBRProgressMonitor monitor) throws DBException
    {
        return publicSchema.getSynonyms(monitor);
    }

    @Association
    public Collection<OracleDBLink> getPublicDatabaseLinks(DBRProgressMonitor monitor) throws DBException
    {
        return publicSchema.getDatabaseLinks(monitor);
    }

    @Association
    public Collection<OracleRecycledObject> getUserRecycledObjects(DBRProgressMonitor monitor) throws DBException
    {
        return publicSchema.getRecycledObjects(monitor);
    }

    @Override
    protected Driver getDriverInstance(DBRProgressMonitor monitor) throws DBException {
        DBSDataSourceContainer container = getContainer();
        DBPDriver driver = container.getDriver();
/*
        boolean ociDriver = OCIUtils.isOciDriver(driver);
        if (ociDriver) {
            String homeId = container.getConnectionInfo().getClientHomeId();
            if (homeId != null) {
                OCIClassLoader ociClassLoader = ociClassLoadersCache.get(homeId);
                if (ociClassLoader == null) {
                    OracleHomeDescriptor homeDescriptor = (OracleHomeDescriptor)driver.getClientHome(homeId);
                    if (homeDescriptor == null) {
                        throw new DBException("Can't load driver from '" + homeId + "'");
                    }
                    ociClassLoader = new OCIClassLoader(homeDescriptor, getClass().getClassLoader());
                    loadOCILibraries(ociClassLoader);
                    ociClassLoadersCache.put(homeId, ociClassLoader);
                }
                String driverClassName = driver.getDriverClassName();
                try {
                    final Class<?> driverClass = ociClassLoader.loadClass(driverClassName);
                    return (Driver) driverClass.newInstance();
                } catch (ClassNotFoundException ex) {
                    throw new DBException("Can't load driver class '" + driverClassName + "'", ex);
                } catch (InstantiationException ex) {
                    throw new DBException("Can't create driver class '" + driverClassName + "'", ex);
                } catch (IllegalAccessException ex) {
                    throw new DBException("Can't create driver class '" + driverClassName + "'", ex);
                }
            }
        }
*/
        return super.getDriverInstance(monitor);
    }

/*
    private void loadOCILibraries(OCIClassLoader classLoader)
    {
        loadOCILibrary(classLoader, "oci");
        loadOCILibrary(classLoader, "ocijdbc11");
    }

    private void loadOCILibrary(OCIClassLoader classLoader, String libName)
    {
        String libPath = classLoader.findLibrary(libName);
        if (libPath != null) {
            System.load(libPath);
        }
    }
*/

    @Override
    public void initialize(DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);

        this.publicSchema = new OracleSchema(this, 1, OracleConstants.USER_PUBLIC);
        {
            final JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Load data source meta info");
            try {
                // Set session settings
                DBPConnectionInfo connectionInfo = getContainer().getConnectionInfo();
                Object sessionLanguage = connectionInfo.getProperties().get(OracleConstants.PROP_SESSION_LANGUAGE);
                if (sessionLanguage != null) {
                    JDBCUtils.executeSQL(
                        session,
                        "ALTER SESSION SET NLS_LANGUAGE='" + sessionLanguage + "'");
                }
                Object sessionTerritory = connectionInfo.getProperties().get(OracleConstants.PROP_SESSION_TERRITORY);
                if (sessionLanguage != null) {
                    JDBCUtils.executeSQL(
                        session,
                        "ALTER SESSION SET NLS_TERRITORY='" + sessionTerritory + "'");
                }

                // Check DBA role
                this.isAdmin = "YES".equals(
                    JDBCUtils.queryString(
                    session,
                    "SELECT 'YES' FROM USER_ROLE_PRIVS WHERE GRANTED_ROLE='DBA'"));
                this.isAdminVisible = isAdmin;
                if (!isAdminVisible) {
                    Object showAdmin = connectionInfo.getProperties().get(OracleConstants.PROP_ALWAYS_SHOW_DBA);
                    if (showAdmin != null) {
                        isAdminVisible = CommonUtils.getBoolean(showAdmin, false);
                    }
                }

                // Get active schema
                this.activeSchemaName = JDBCUtils.queryString(
                    session,
                    "SELECT SYS_CONTEXT( 'USERENV', 'CURRENT_SCHEMA' ) FROM DUAL");
            } catch (SQLException e) {
                //throw new DBException(e);
                log.warn(e);
            }
            finally {
                session.close();
            }
        }
        this.dataTypeCache.getObjects(monitor, this);
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshObject(monitor);

        this.schemaCache.clearCache();
        this.dataTypeCache.clearCache();
        this.tablespaceCache.clearCache();
        this.userCache.clearCache();
        this.profileCache.clearCache();
        this.roleCache.clearCache();
        this.activeSchemaName = null;

        this.initialize(monitor);

        return true;
    }

    @Override
    public Collection<OracleSchema> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getSchemas(monitor);
    }

    @Override
    public OracleSchema getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getSchema(monitor, childName);
    }

    @Override
    public Class<? extends OracleSchema> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return OracleSchema.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        
    }

    @Override
    public boolean supportsObjectSelect()
    {
        return true;
    }

    @Override
    public OracleSchema getSelectedObject()
    {
        return activeSchemaName == null ? null : schemaCache.getCachedObject(activeSchemaName);
    }

    @Override
    public void selectObject(DBRProgressMonitor monitor, DBSObject object)
        throws DBException
    {
        final OracleSchema oldSelectedEntity = getSelectedObject();
        if (!(object instanceof OracleSchema)) {
            throw new IllegalArgumentException("Invalid object type: " + object);
        }
        JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema");
        try {
            JDBCUtils.executeSQL(session, "ALTER SESSION SET CURRENT_SCHEMA=" + object.getName());
        } catch (SQLException e) {
            throw new DBException(e, session.getDataSource());
        }
        finally {
            session.close();
        }
        activeSchemaName = object.getName();

        // Send notifications
        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        if (this.activeSchemaName != null) {
            DBUtils.fireObjectSelect(object, true);
        }
    }

    @Override
    public DBCPlan planQueryExecution(DBCSession session, String query) throws DBCException
    {
        OraclePlanAnalyser plan = new OraclePlanAnalyser(this, query);
        plan.explain((JDBCSession) session);
        return plan;
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return new OracleStructureAssistant(this);
        }
        return null;
    }

    @Override
    public OracleDataSource getDataSource() {
        return this;
    }

    @Override
    public DBPDataKind resolveDataKind(String typeName, int valueType)
    {
        if (typeName != null &&
            (typeName.equals(OracleConstants.TYPE_NAME_XML) || typeName.equals(OracleConstants.TYPE_FQ_XML)))
        {
            return DBPDataKind.LOB;
        }
        DBPDataKind dataKind = OracleDataType.getDataKind(typeName);
        if (dataKind != null) {
            return dataKind;
        }
        return super.resolveDataKind(typeName, valueType);
    }

    @Override
    public Collection<? extends DBSDataType> getDataTypes()
    {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public DBSDataType getDataType(String typeName)
    {
        return dataTypeCache.getCachedObject(typeName);
    }

    @Override
    public DBSDataType resolveDataType(DBRProgressMonitor monitor, String typeFullName) throws DBException
    {
        int divPos = typeFullName.indexOf(SQLConstants.STRUCT_SEPARATOR);
        if (divPos == -1) {
            // Simple type name
            return getDataType(typeFullName);
        } else {
            String schemaName = typeFullName.substring(0, divPos);
            String typeName = typeFullName.substring(divPos + 1);
            OracleSchema schema = getSchema(monitor, schemaName);
            if (schema == null) {
                return null;
            }
            return schema.getDataType(monitor, typeName);
        }
    }

    public String getPlanTableName(JDBCSession session)
        throws SQLException
    {
        String tableName = getContainer().getPreferenceStore().getString(OracleConstants.PREF_EXPLAIN_TABLE_NAME);
        if (!CommonUtils.isEmpty(tableName)) {
            return tableName;
        }
        if (planTableName == null) {
            String[] candidateNames = new String[] {"PLAN_TABLE", "TOAD_PLAN_TABLE"};
            for (String candidate : candidateNames) {
                try {
                    JDBCUtils.executeSQL(session, "SELECT 1 FROM " + candidate);
                } catch (SQLException e) {
                    // No such table
                    continue;
                }
                planTableName = candidate;
                break;
            }
            if (planTableName == null) {
                // Plan table not found - try to create new one
                if (!UIUtils.confirmAction(
                    DBeaverUI.getActiveWorkbenchShell(),
                    "Oracle PLAN_TABLE missing",
                    "PLAN_TABLE not found in current user's session. " +
                        "Do you want DBeaver to create new PLAN_TABLE?"))
                {
                    return null;
                }
                planTableName = createPlanTable(session);
            }
        }
        return planTableName;
    }

    private String createPlanTable(JDBCSession session) throws SQLException
    {
        JDBCUtils.executeSQL(session, OracleConstants.PLAN_TABLE_DEFINITION);
        return "PLAN_TABLE";
    }

    static class SchemaCache extends JDBCObjectCache<OracleDataSource, OracleSchema> {
        SchemaCache()
        {
            setListOrderComparator(DBUtils.<OracleSchema>nameComparator());
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleDataSource owner) throws SQLException
        {
            StringBuilder schemasQuery = new StringBuilder();
            boolean manyObjects = "false".equals(owner.getContainer().getConnectionInfo().getProperties().get(OracleConstants.PROP_CHECK_SCHEMA_CONTENT));
            schemasQuery.append("SELECT U.USERNAME FROM SYS.ALL_USERS U\n");

//                if (owner.isAdmin() && false) {
//                    schemasQuery.append(
//                        "WHERE (U.USER_ID IN (SELECT DISTINCT OWNER# FROM SYS.OBJ$) ");
//                } else {
            schemasQuery.append(
                "WHERE (");
            if (manyObjects) {
                schemasQuery.append("U.USERNAME IS NOT NULL");
            } else {
                schemasQuery.append("U.USERNAME IN (SELECT DISTINCT OWNER FROM SYS.ALL_OBJECTS)");
            }
//                }

            DBSObjectFilter schemaFilters = owner.getContainer().getObjectFilter(OracleSchema.class, null);
            if (schemaFilters != null) {
                JDBCUtils.appendFilterClause(schemasQuery, schemaFilters, "U.USERNAME", false);
            }
            schemasQuery.append(")");
            //if (!CommonUtils.isEmpty(owner.activeSchemaName)) {
                //schemasQuery.append("\nUNION ALL SELECT '").append(owner.activeSchemaName).append("' AS USERNAME FROM DUAL");
            //}
            //schemasQuery.append("\nORDER BY USERNAME");

            JDBCPreparedStatement dbStat = session.prepareStatement(schemasQuery.toString());

            if (schemaFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, schemaFilters);
            }
            return dbStat;
        }

        @Override
        protected OracleSchema fetchObject(JDBCSession session, OracleDataSource owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleSchema(owner, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, OracleDataSource owner, Iterator<OracleSchema> objectIter)
        {
            setListOrderComparator(DBUtils.<OracleSchema>nameComparator());
            // Add predefined types
            if (owner.activeSchemaName != null && getCachedObject(owner.activeSchemaName) == null) {
                cacheObject(
                    new OracleSchema(owner, -1, owner.activeSchemaName));
            }
        }
    }

    static class DataTypeCache extends JDBCObjectCache<OracleDataSource, OracleDataType> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleDataSource owner) throws SQLException
        {
            return session.prepareStatement(
                "SELECT * FROM SYS.ALL_TYPES WHERE OWNER IS NULL ORDER BY TYPE_NAME");
        }
        @Override
        protected OracleDataType fetchObject(JDBCSession session, OracleDataSource owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataType(owner, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, OracleDataSource owner, Iterator<OracleDataType> objectIter)
        {
            // Add predefined types
            for (Map.Entry<String, OracleDataType.TypeDesc> predefinedType : OracleDataType.PREDEFINED_TYPES.entrySet()) {
                if (getCachedObject(predefinedType.getKey()) == null) {
                    cacheObject(
                        new OracleDataType(owner, predefinedType.getKey(), true));
                }
            }
        }
    }

    static class TablespaceCache extends JDBCObjectCache<OracleDataSource, OracleTablespace> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleDataSource owner) throws SQLException
        {
            return session.prepareStatement(
                "SELECT * FROM " + OracleUtils.getAdminViewPrefix(owner) + "TABLESPACES ORDER BY TABLESPACE_NAME");
        }

        @Override
        protected OracleTablespace fetchObject(JDBCSession session, OracleDataSource owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleTablespace(owner, resultSet);
        }
    }

    static class UserCache extends JDBCObjectCache<OracleDataSource, OracleUser> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleDataSource owner) throws SQLException
        {
            return session.prepareStatement(
                "SELECT * FROM " + OracleUtils.getAdminAllViewPrefix(owner) + "USERS ORDER BY USERNAME");
        }

        @Override
        protected OracleUser fetchObject(JDBCSession session, OracleDataSource owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleUser(owner, resultSet);
        }
    }

    static class RoleCache extends JDBCObjectCache<OracleDataSource, OracleRole> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleDataSource owner) throws SQLException
        {
            return session.prepareStatement(
                "SELECT * FROM DBA_ROLES ORDER BY ROLE");
        }

        @Override
        protected OracleRole fetchObject(JDBCSession session, OracleDataSource owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleRole(owner, resultSet);
        }
    }

    static class ProfileCache extends JDBCStructCache<OracleDataSource, OracleUserProfile, OracleUserProfile.ProfileResource> {
        protected ProfileCache()
        {
            super("PROFILE");
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleDataSource owner) throws SQLException
        {
            return session.prepareStatement(
                "SELECT DISTINCT PROFILE FROM DBA_PROFILES ORDER BY PROFILE");
        }

        @Override
        protected OracleUserProfile fetchObject(JDBCSession session, OracleDataSource owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleUserProfile(owner, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(JDBCSession session, OracleDataSource dataSource, OracleUserProfile forObject) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT RESOURCE_NAME,RESOURCE_TYPE,LIMIT FROM DBA_PROFILES " +
                (forObject == null ? "" : "WHERE PROFILE=? ") +
                "ORDER BY RESOURCE_NAME");
            if (forObject != null) {
                dbStat.setString(1, forObject.getName());
            }
            return dbStat;
        }

        @Override
        protected OracleUserProfile.ProfileResource fetchChild(JDBCSession session, OracleDataSource dataSource, OracleUserProfile parent, ResultSet dbResult) throws SQLException, DBException
        {
            return new OracleUserProfile.ProfileResource(parent, dbResult);
        }
    }

}
