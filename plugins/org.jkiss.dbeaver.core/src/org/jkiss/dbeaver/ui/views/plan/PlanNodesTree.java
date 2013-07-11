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
package org.jkiss.dbeaver.ui.views.plan;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ObjectViewerRenderer;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * Plan nodes tree
 */
public class PlanNodesTree extends DatabaseObjectListControl<DBCPlanNode> {

    private DBCQueryPlanner planner;
    private String query;

    public PlanNodesTree(Composite parent, int style)
    {
        super(parent, style, CONTENT_PROVIDER);
        setFitWidth(true);
    }

    @Override
    protected ObjectViewerRenderer createRenderer()
    {
        return new PlanTreeRenderer();
    }

    @Override
    protected LoadingJob<Collection<DBCPlanNode>> createLoadService()
    {
        return LoadingUtils.createService(
                new ExplainPlanService(),
                new PlanLoadVisualizer());
    }

    public boolean isInitialized()
    {
        return planner != null;
    }

    public void init(DBCQueryPlanner planner, String query)
    {
        this.planner = planner;
        this.query = query;
    }

    private static ITreeContentProvider CONTENT_PROVIDER = new ITreeContentProvider() {
        @Override
        public Object[] getElements(Object inputElement)
        {
            if (inputElement instanceof Collection) {
                return ((Collection<?>)inputElement).toArray();
            }
            return null;
        }

        @Override
        public Object[] getChildren(Object parentElement)
        {
            if (parentElement instanceof DBCPlanNode) {
                Collection<? extends DBCPlanNode> nestedNodes = ((DBCPlanNode) parentElement).getNested();
                return CommonUtils.isEmpty(nestedNodes) ? new Object[0] : nestedNodes.toArray();
            }
            return null;
        }

        @Override
        public Object getParent(Object element)
        {
            if (element instanceof DBCPlanNode) {
                return ((DBCPlanNode)element).getParent();
            }
            return null;
        }

        @Override
        public boolean hasChildren(Object element)
        {
            return element instanceof DBCPlanNode && !CommonUtils.isEmpty(((DBCPlanNode) element).getNested());
        }

        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }

    };

    private class ExplainPlanService extends DatabaseLoadService<Collection<DBCPlanNode>> {

        protected ExplainPlanService()
        {
            super("Explain plan", planner.getDataSource());
        }

        @Override
        public Collection<DBCPlanNode> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                DBCExecutionContext context = planner.getDataSource().openContext(getProgressMonitor(), DBCExecutionPurpose.UTIL, "Explain '" + query + "'");
                try {
                    DBCPlan plan = planner.planQueryExecution(context, query);
                    return (Collection<DBCPlanNode>) plan.getPlanNodes();
                }
                finally {
                    context.close();
                }
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }
    }

    public class PlanLoadVisualizer extends ObjectsLoadVisualizer {

        @Override
        public void completeLoading(Collection<DBCPlanNode> items)
        {
            super.completeLoading(items);
            final TreeViewer itemsViewer = (TreeViewer) PlanNodesTree.this.getItemsViewer();
            itemsViewer.getControl().setRedraw(false);
            try {
                itemsViewer.expandAll();
            } finally {
                itemsViewer.getControl().setRedraw(true);
            }
        }
    }

    private class PlanTreeRenderer extends ViewerRenderer {
        @Override
        public boolean isHyperlink(Object cellValue)
        {
            return cellValue instanceof DBSObject;
        }

        @Override
        public void navigateHyperlink(Object cellValue)
        {
            if (cellValue instanceof DBSObject) {
                NavigatorHandlerObjectOpen.openEntityEditor((DBSObject) cellValue);
            }
        }

    }

}
