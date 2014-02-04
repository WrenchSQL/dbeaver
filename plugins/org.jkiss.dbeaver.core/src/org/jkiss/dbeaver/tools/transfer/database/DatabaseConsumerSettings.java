/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 * eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.tools.transfer.database;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferPipe;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DatabaseConsumerSettings
 */
public class DatabaseConsumerSettings implements IDataTransferSettings {

    static final Log log = LogFactory.getLog(DatabaseConsumerSettings.class);

    private DBNDatabaseNode containerNode;
    private Map<DBSDataContainer, DatabaseMappingContainer> dataMappings = new LinkedHashMap<DBSDataContainer, DatabaseMappingContainer>();
    private boolean openNewConnections = true;
    private boolean useTransactions = true;
    private int commitAfterRows = 10000;
    private boolean openTableOnFinish = true;

    public DatabaseConsumerSettings()
    {
    }

    public DBSObjectContainer getContainer()
    {
        if (containerNode == null) {
            return null;
        }
        return DBUtils.getAdapter(DBSObjectContainer.class, containerNode.getObject());
    }

    public DBNDatabaseNode getContainerNode()
    {
        return containerNode;
    }

    public void setContainerNode(DBNDatabaseNode containerNode)
    {
        this.containerNode = containerNode;
    }

    public Map<DBSDataContainer, DatabaseMappingContainer> getDataMappings()
    {
        return dataMappings;
    }

    public DatabaseMappingContainer getDataMapping(DBSDataContainer dataContainer)
    {
        return dataMappings.get(dataContainer);
    }

    public boolean isCompleted(Collection<DataTransferPipe> pipes)
    {
        for (DataTransferPipe pipe : pipes) {
            if (pipe.getProducer() != null) {
                DBSDataContainer sourceObject = (DBSDataContainer)pipe.getProducer().getSourceObject();
                DatabaseMappingContainer containerMapping = dataMappings.get(sourceObject);
                if (containerMapping == null ||
                    containerMapping.getMappingType() == DatabaseMappingType.unspecified ||
                    !containerMapping.isCompleted())
                {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isOpenTableOnFinish()
    {
        return openTableOnFinish;
    }

    public void setOpenTableOnFinish(boolean openTableOnFinish)
    {
        this.openTableOnFinish = openTableOnFinish;
    }

    public boolean isOpenNewConnections()
    {
        return openNewConnections;
    }

    public void setOpenNewConnections(boolean openNewConnections)
    {
        this.openNewConnections = openNewConnections;
    }

    public boolean isUseTransactions()
    {
        return useTransactions;
    }

    public void setUseTransactions(boolean useTransactions)
    {
        this.useTransactions = useTransactions;
    }

    public int getCommitAfterRows()
    {
        return commitAfterRows;
    }

    public void setCommitAfterRows(int commitAfterRows)
    {
        this.commitAfterRows = commitAfterRows;
    }

    DBPDataSource getTargetDataSource(DatabaseMappingObject attrMapping)
    {
        DBSObjectContainer container = getContainer();
        if (container != null) {
            return container.getDataSource();
        } else if (attrMapping.getTarget() != null) {
            return attrMapping.getTarget().getDataSource();
        } else {
            return null;
        }
    }

    @Override
    public void loadSettings(IRunnableContext runnableContext, IDialogSettings dialogSettings)
    {
        final String containerPath = dialogSettings.get("container");
        if (!CommonUtils.isEmpty(containerPath)) {
            try {
                RuntimeUtils.run(runnableContext, true, true, new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try {
                            DBNNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByPath(
                                monitor,
                                containerPath);
                            if (node instanceof DBNDatabaseNode) {
                                containerNode = (DBNDatabaseNode) node;
                            }
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                log.error("Error getting container node", e.getTargetException());
            } catch (InterruptedException e) {
                // skip
            }
        }
        if (dialogSettings.get("openNewConnections") != null) {
            openNewConnections = dialogSettings.getBoolean("openNewConnections");
        }
        if (dialogSettings.get("useTransactions") != null) {
            useTransactions = dialogSettings.getBoolean("useTransactions");
        }
        if (dialogSettings.get("commitAfterRows") != null) {
            commitAfterRows = dialogSettings.getInt("commitAfterRows");
        }
        if (dialogSettings.get("openTableOnFinish") != null) {
            openTableOnFinish = dialogSettings.getBoolean("openTableOnFinish");
        }
    }

    @Override
    public void saveSettings(IDialogSettings dialogSettings)
    {
        if (containerNode != null) {
            dialogSettings.put("container", containerNode.getNodeItemPath());
        }
        dialogSettings.put("openNewConnections", openNewConnections);
        dialogSettings.put("useTransactions", useTransactions);
        dialogSettings.put("commitAfterRows", commitAfterRows);
        dialogSettings.put("openTableOnFinish", openTableOnFinish);
    }

    public String getContainerFullName()
    {
        DBSObjectContainer container = getContainer();
        return container == null ? null :
            container instanceof DBPDataSource ? DBUtils.getObjectFullName(container) :
            DBUtils.getObjectFullName(container) + " [" + container.getDataSource().getContainer().getName() + "]";
    }

}
