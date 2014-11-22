/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.dbeaver.ui.search.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProcessListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.search.IObjectSearchContainer;
import org.jkiss.dbeaver.ui.search.IObjectSearchPage;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorTree;
import org.jkiss.dbeaver.ui.views.navigator.database.load.TreeLoadNode;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

public class SearchDataPage extends DialogPage implements IObjectSearchPage {

    static final Log log = LogFactory.getLog(SearchDataPage.class);

    private static final String PROP_MASK = "search-view.mask"; //$NON-NLS-1$
    private static final String PROP_CASE_SENSITIVE = "search-view.case-sensitive"; //$NON-NLS-1$
    private static final String PROP_MAX_RESULT = "search-view.max-results"; //$NON-NLS-1$
    private static final String PROP_MATCH_INDEX = "search-view.match-index"; //$NON-NLS-1$
    private static final String PROP_HISTORY = "search-view.history"; //$NON-NLS-1$
    private static final String PROP_OBJECT_TYPE = "search-view.object-type"; //$NON-NLS-1$
    private static final String PROP_SOURCES = "search-view.object-source"; //$NON-NLS-1$

    private IObjectSearchContainer container;
    private Table typesTable;
    private Combo searchText;
    private DatabaseNavigatorTree dataSourceTree;

    private String searchString;
    private boolean caseSensitive;
    private int maxResults;
    private int matchTypeIndex;
    private Set<DBSObjectType> checkedTypes = new HashSet<DBSObjectType>();
    private Set<String> searchHistory = new LinkedHashSet<String>();
    private Set<String> savedTypeNames = new HashSet<String>();
    private List<DBNNode> sourceNodes = new ArrayList<DBNNode>();

    public SearchDataPage() {
		super("Database objects search");
    }

	@Override
	public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite searchGroup = new Composite(parent, SWT.NONE);
        searchGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        searchGroup.setLayout(new GridLayout(3, false));
        setControl(searchGroup);
        UIUtils.createControlLabel(searchGroup, CoreMessages.dialog_search_objects_label_object_name);
        searchText = new Combo(searchGroup, SWT.DROP_DOWN);
        searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (searchString != null) {
            searchText.setText(searchString);
        }
        for (String history : searchHistory) {
            searchText.add(history);
        }
        searchText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                searchString = searchText.getText();
                updateEnablement();
            }
        });

        {
            //new Label(searchGroup, SWT.NONE);
            Composite optionsGroup2 = UIUtils.createPlaceholder(searchGroup, 5, 5);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 3;
            optionsGroup2.setLayoutData(gd);

            if (maxResults <= 0) {
                maxResults = 100;
            }

            final Spinner maxResultsSpinner = UIUtils.createLabelSpinner(optionsGroup2, CoreMessages.dialog_search_objects_spinner_max_results, maxResults, 1, 10000);
            maxResultsSpinner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            maxResultsSpinner.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    maxResults = maxResultsSpinner.getSelection();
                }
            });

            final Button caseCheckbox = UIUtils.createCheckbox(optionsGroup2, CoreMessages.dialog_search_objects_case_sensitive, caseSensitive);
            maxResultsSpinner.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
            caseCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    caseSensitive = caseCheckbox.getSelection();
                }
            });

        }

        Composite optionsGroup = new Composite(searchGroup, SWT.NONE);
        GridLayout layout = new GridLayout(2, true);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        optionsGroup.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 3;
        optionsGroup.setLayoutData(gd);

        {
            final DBeaverCore core = DBeaverCore.getInstance();

            Group sourceGroup = UIUtils.createControlGroup(optionsGroup, CoreMessages.dialog_search_objects_group_objects_source, 1, GridData.FILL_BOTH, 0);
            gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            sourceGroup.setLayoutData(gd);
            final DBNProject projectNode = core.getNavigatorModel().getRoot().getProject(core.getProjectRegistry().getActiveProject());
            DBNNode rootNode = projectNode == null ? core.getNavigatorModel().getRoot() : projectNode.getDatabases();
            dataSourceTree = new DatabaseNavigatorTree(sourceGroup, rootNode, SWT.SINGLE);
            dataSourceTree.setLayoutData(new GridData(GridData.FILL_BOTH));

            dataSourceTree.getViewer().addFilter(new ViewerFilter() {
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element)
                {
                    if (element instanceof TreeLoadNode) {
                        return true;
                    }
                    if (element instanceof DBNNode) {
                        if (element instanceof DBNDatabaseFolder) {
                            DBNDatabaseFolder folder = (DBNDatabaseFolder)element;
                            Class<? extends DBSObject> folderItemsClass = folder.getChildrenClass();
                            return folderItemsClass != null && DBSObjectContainer.class.isAssignableFrom(folderItemsClass);
                        }
                        if (element instanceof DBNLocalFolder ||
                            element instanceof DBNProjectDatabases ||
                            element instanceof DBNDataSource ||
                            (element instanceof DBSWrapper && ((DBSWrapper)element).getObject() instanceof DBSObjectContainer))
                        {
                            return true;
                        }
                    }
                    return false;
                }
            });
            dataSourceTree.getViewer().addSelectionChangedListener(
                new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event)
                    {
                        fillObjectTypes();
                        updateEnablement();
                        IStructuredSelection structSel = (IStructuredSelection) event.getSelection();
                        Object object = structSel.isEmpty() ? null : structSel.getFirstElement();
                        if (object instanceof DBNNode) {
                            for (DBNNode node = (DBNNode)object; node != null; node = node.getParentNode()) {
                                if (node instanceof DBNDataSource) {
                                    DBNDataSource dsNode = (DBNDataSource) node;
                                    dsNode.initializeNode(null, new DBRProcessListener() {
                                        @Override
                                        public void onProcessFinish(IStatus status)
                                        {
                                            if (status.isOK()) {
                                                Display.getDefault().asyncExec(new Runnable() {
                                                    @Override
                                                    public void run()
                                                    {
                                                        if (!dataSourceTree.isDisposed()) {
                                                            fillObjectTypes();
                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                                    break;
                                }
                            }
                        }
                    }
                }
            );
        }

        {
            Group typesGroup = UIUtils.createControlGroup(optionsGroup, CoreMessages.dialog_search_objects_group_object_types, 1, GridData.FILL_BOTH, 0);
            gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 300;
            typesGroup.setLayoutData(gd);
            typesTable = new Table(typesGroup, SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
            typesTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    //checkedTypes.clear();
                    for (TableItem item : typesTable.getItems()) {
                        DBSObjectType objectType = (DBSObjectType) item.getData();
                        if (item.getChecked()) {
                            checkedTypes.add(objectType);
                        } else {
                            checkedTypes.remove(objectType);
                        }
                    }
                    updateEnablement();
                }
            });
            typesTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDoubleClick(MouseEvent e) {
                    TableItem tableItem = typesTable.getSelection()[0];
                    tableItem.setChecked(!tableItem.getChecked());
                }
            });
            typesTable.setLayoutData(new GridData(GridData.FILL_BOTH));

            UIUtils.createTableColumn(typesTable, SWT.LEFT, CoreMessages.dialog_search_objects_column_type);
            UIUtils.createTableColumn(typesTable, SWT.LEFT, CoreMessages.dialog_search_objects_column_description);
        }

        if (!sourceNodes.isEmpty()) {
            dataSourceTree.getViewer().setSelection(
                new StructuredSelection(sourceNodes));
        } else {
            updateEnablement();
        }
    }

    private DBNNode getSelectedNode()
    {
        IStructuredSelection selection = (IStructuredSelection) dataSourceTree.getViewer().getSelection();
        if (!selection.isEmpty()) {
            return (DBNNode) selection.getFirstElement();
        }
        return null;
    }

    private DBPDataSource getSelectedDataSource()
    {
        DBNNode node = getSelectedNode();
        if (node instanceof DBSWrapper) {
            DBSObject object = ((DBSWrapper)node).getObject();
            if (object != null && object.getDataSource() != null) {
                return object.getDataSource();
            }
        }
        return null;
    }

    private DBSStructureAssistant getSelectedStructureAssistant()
    {
        return DBUtils.getAdapter(DBSStructureAssistant.class, getSelectedDataSource());
    }

    private void fillObjectTypes()
    {
        DBSStructureAssistant assistant = getSelectedStructureAssistant();
        typesTable.removeAll();
        if (assistant == null) {
            // No structure assistant - no object types
        } else {
            for (DBSObjectType objectType : assistant.getSupportedObjectTypes()) {
                TableItem item = new TableItem(typesTable, SWT.NONE);
                item.setText(objectType.getTypeName());
                if (objectType.getImage() != null) {
                    item.setImage(0, objectType.getImage());
                }
                if (!CommonUtils.isEmpty(objectType.getDescription())) {
                    item.setText(1, objectType.getDescription());
                }
                item.setData(objectType);
                if (checkedTypes.contains(objectType)) {
                    item.setChecked(true);
                } else if (savedTypeNames.contains(objectType.getTypeClass().getName())) {
                    item.setChecked(true);
                    checkedTypes.add(objectType);
                    savedTypeNames.remove(objectType.getTypeClass().getName());
                }
            }
        }
        for (TableColumn column : typesTable.getColumns()) {
            column.pack();
        }
        updateEnablement();
    }

    private void updateEnablement()
    {
        boolean enabled = false;
        if (getSelectedDataSource() != null) {
            enabled = !checkedTypes.isEmpty();
        }
        if (CommonUtils.isEmpty(searchString)) {
            enabled = false;
        }

        container.setSearchEnabled(enabled);
    }

    @Override
    public void setSearchContainer(IObjectSearchContainer container)
    {
        this.container = container;
    }

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);
        if (visible) {
            updateEnablement();
        }
    }

    @Override
    public SearchDataQuery createQuery() throws DBException
    {
        DBNNode selectedNode = getSelectedNode();
        DBSObjectContainer parentObject = null;
        if (selectedNode instanceof DBSWrapper && ((DBSWrapper)selectedNode).getObject() instanceof DBSObjectContainer) {
            parentObject = (DBSObjectContainer) ((DBSWrapper)selectedNode).getObject();
        }

        DBPDataSource dataSource = getSelectedDataSource();
        DBSStructureAssistant assistant = getSelectedStructureAssistant();
        if (dataSource == null || assistant == null) {
            throw new IllegalStateException("No active datasource");
        }
        List<DBSObjectType> objectTypes = new ArrayList<DBSObjectType>();
        for (TableItem item : typesTable.getItems()) {
            if (item.getChecked()) {
                objectTypes.add((DBSObjectType) item.getData());
            }
        }
        String dataSearchString = searchString;

        // Save search query
        if (!searchHistory.contains(dataSearchString)) {
            searchHistory.add(dataSearchString);
            searchText.add(dataSearchString);
        }

        SearchDataParams params = new SearchDataParams();
        params.setParentObject(parentObject);
        params.setSearchString(dataSearchString);
        params.setCaseSensitive(caseSensitive);
        params.setMaxResults(maxResults);
        return SearchDataQuery.createQuery(dataSource, params);

    }

    @Override
    public void loadState(IPreferenceStore store)
    {
        searchString = store.getString(PROP_MASK);
        caseSensitive = store.getBoolean(PROP_CASE_SENSITIVE);
        maxResults = store.getInt(PROP_MAX_RESULT);
        matchTypeIndex = store.getInt(PROP_MATCH_INDEX);
        for (int i = 0; ;i++) {
            String history = store.getString(PROP_HISTORY + "." + i); //$NON-NLS-1$
            if (CommonUtils.isEmpty(history)) {
                break;
            }
            searchHistory.add(history);
        }
        {
            final String sources = store.getString(PROP_SOURCES);
            if (!CommonUtils.isEmpty(sources)) {
                try {
                    DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                        @Override
                        public void run(DBRProgressMonitor monitor)
                        {
                            StringTokenizer st = new StringTokenizer(sources, "|"); //$NON-NLS-1$
                            while (st.hasMoreTokens()) {
                                String nodePath = st.nextToken();
                                try {
                                    DBNNode node = DBNModel.getInstance().getNodeByPath(monitor, nodePath);
                                    if (node != null) {
                                        sourceNodes.add(node);
                                    }
                                } catch (DBException e) {
                                    log.error(e);
                                }
                            }
                        }
                    });
                } catch (InvocationTargetException e) {
                    log.error(e.getTargetException());
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        {
            String type = store.getString(PROP_OBJECT_TYPE);
            if (!CommonUtils.isEmpty(type)) {
                StringTokenizer st = new StringTokenizer(type, "|"); //$NON-NLS-1$
                while (st.hasMoreTokens()) {
                    savedTypeNames.add(st.nextToken());
                }
            }
        }
    }

    @Override
    public void saveState(IPreferenceStore store)
    {
        store.setValue(PROP_MASK, searchString);
        store.setValue(PROP_CASE_SENSITIVE, caseSensitive);
        store.setValue(PROP_MAX_RESULT, maxResults);
        store.setValue(PROP_MATCH_INDEX, matchTypeIndex);
        {
            // Object sources
            StringBuilder sourcesString = new StringBuilder();
            IStructuredSelection ss = (IStructuredSelection) dataSourceTree.getViewer().getSelection();
            for (Iterator<?> iter = ss.iterator(); iter.hasNext(); ) {
                DBNNode node = (DBNNode) iter.next();
                if (sourcesString.length() > 0) {
                    sourcesString.append("|"); //$NON-NLS-1$
                }
                sourcesString.append(node.getNodeItemPath());
            }
            store.setValue(PROP_SOURCES, sourcesString.toString());
        }

        {
            // Search history
            int historyIndex = 0;
            for (String history : searchHistory) {
                if (historyIndex >= 20) {
                    break;
                }
                store.setValue(PROP_HISTORY + "." + historyIndex, history); //$NON-NLS-1$
                historyIndex++;
            }
        }
        {
            // Object types
            StringBuilder typesString = new StringBuilder();
            for (DBSObjectType type : checkedTypes) {
                if (typesString.length() > 0) {
                    typesString.append("|"); //$NON-NLS-1$
                }
                typesString.append(type.getTypeClass().getName());
            }
            store.setValue(PROP_OBJECT_TYPE, typesString.toString());
        }
    }

}
