
// ConfigureDashboard.java --
//
// ConfigureDashboard.java is part of the QADashboard plugin.
//
// Copyright (c) 2005-2010 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.WorkflowDashboard.client;

import com.electriccloud.commander.gwt.client.BrowserContext;
import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.client.domain.*;
import com.electriccloud.commander.client.requests.*;
import com.electriccloud.commander.client.responses.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import com.electriccloud.commander.gwt.client.ComponentBase;
import com.google.gwt.view.client.TreeViewModel;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Basic component that is meant to be cloned and then customized to perform a
 * real function.
 */
public class ConfigureDashboard
        extends ComponentBase
{

    private static ConfigureDashboardMainUiBinder uiBinder = GWT.create(
            ConfigureDashboardMainUiBinder.class);
    interface ConfigureDashboardMainUiBinder extends UiBinder<Widget, ConfigureDashboard> {}

    interface Style extends CssResource {
        String evenRow();
        String oddRow();
        String headerRow();
        String vPanel();
        String important();
        String link();
        String h2();
        String topPanel();
        String panelPadding();
        String labels();
        String textEntry();
    }

    //@UiField Anchor configureLink;
    @UiField FlexTable dashboardPanel;
    @UiField Style style;
    @UiField Tree treeView;
    @UiField Button btnSave;
    @UiField Button btnCancel;
    @UiField TextBox dashboardTitle;
    @UiField TextBox col1Header;
    @UiField TextBox col2Header;
    @UiField TextBox history;
    @UiField TextBox refresh;
    @UiField TextBox successThreshold;
    @UiField TextBox errorThreshold;


    final Image[] m_triStateCheckBoxArray = new Image[3];
    enum tristateCBStyle {SELECTED, PARTIALLY_SELECTED, UNSELECTED};


    //~ Methods ----------------------------------------------------------------

    /**
     * This function is called by SDK infrastructure to initialize the UI parts of
     * this component.
     *
     * @return                 A widget that the infrastructure should place in
     *                         the UI; usually a panel.
     */
    @Override public Widget doInit()
    {

        DashboardResources.RESOURCES.DashboardStyles().ensureInjected();

        Widget mainLaunch = uiBinder.createAndBindUi(this);

        configureSaveHandler("/myUser/userSettings/myWorkflowsDashboard");
        configureCancelHandler();

        renderCurrentProjectAndWorkflowConfiguration("/myUser/userSettings/myWorkflowsDashboard", true);
        return mainLaunch;
    }

    private void returnToCallingPage(){
        String redirectTo = BrowserContext.getInstance().getGetParameter("redirectTo");
        if (redirectTo != null) {
            Window.Location.assign(redirectTo);
        } else {
            History.back();
        }

    }

    private void configureCancelHandler(){
        btnCancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                returnToCallingPage();//To change body of implemented methods use File | Settings | File Templates.
            }
        });
    }

    private void configureSaveHandler(final String propertySheetRoot){
        btnSave.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                //delete existing settings
                DeletePropertyRequest dpReq = getRequestFactory().createDeletePropertyRequest();
                dpReq.setPropertyName(propertySheetRoot);
                dpReq.setCallback(new CommanderObjectCallback() {
                    @Override
                    public void handleResponse(CommanderObject response) {
                        //To change body of implemented methods use File | Settings | File Templates.
                        TreeItem projectRoot = treeView.getItem(0);
                        btnSave.setEnabled(false);
                        //first save general dashboard properties

                        SetPropertyRequest setDashboardTitleProperty = getRequestFactory().createSetPropertyRequest();
                        setDashboardTitleProperty.setPropertyName(propertySheetRoot+"/dashboardTitle");
                        setDashboardTitleProperty.setValue(dashboardTitle.getText());
                        setDashboardTitleProperty.setCallback(new PropertyCallback() {
                            @Override
                            public void handleResponse(Property response) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }

                            @Override
                            public void handleError(CommanderError error) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }
                        });

                        SetPropertyRequest setProjectColumnHeaderProperty = getRequestFactory().createSetPropertyRequest();
                        setProjectColumnHeaderProperty.setPropertyName(propertySheetRoot+"/projectColumnHeader");
                        setProjectColumnHeaderProperty.setValue(col1Header.getText());
                        setProjectColumnHeaderProperty.setCallback(new PropertyCallback() {
                            @Override
                            public void handleResponse(Property response) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }

                            @Override
                            public void handleError(CommanderError error) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }
                        });

                        SetPropertyRequest setWorkflowColumnHeaderProperty = getRequestFactory().createSetPropertyRequest();
                        setWorkflowColumnHeaderProperty.setPropertyName(propertySheetRoot+"/workflowColumnHeader");
                        setWorkflowColumnHeaderProperty.setValue(col2Header.getText());
                        setWorkflowColumnHeaderProperty.setCallback(new PropertyCallback() {
                            @Override
                            public void handleResponse(Property response) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }

                            @Override
                            public void handleError(CommanderError error) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }
                        });

                        SetPropertyRequest setHistoryProperty = getRequestFactory().createSetPropertyRequest();
                        setHistoryProperty.setPropertyName(propertySheetRoot+"/history");
                        setHistoryProperty.setValue(history.getText());
                        setHistoryProperty.setCallback(new PropertyCallback() {
                            @Override
                            public void handleResponse(Property response) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }

                            @Override
                            public void handleError(CommanderError error) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }
                        });

                        SetPropertyRequest setRefreshProperty = getRequestFactory().createSetPropertyRequest();
                        setRefreshProperty.setPropertyName(propertySheetRoot+"/refresh");
                        setRefreshProperty.setValue(refresh.getText());
                        setRefreshProperty.setCallback(new PropertyCallback() {
                            @Override
                            public void handleResponse(Property response) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }

                            @Override
                            public void handleError(CommanderError error) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }
                        });

                        SetPropertyRequest setSuccessThresholdProperty = getRequestFactory().createSetPropertyRequest();
                        setSuccessThresholdProperty.setPropertyName(propertySheetRoot+"/successThreshold");
                        setSuccessThresholdProperty.setValue(successThreshold.getText());
                        setSuccessThresholdProperty.setCallback(new PropertyCallback() {
                            @Override
                            public void handleResponse(Property response) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }

                            @Override
                            public void handleError(CommanderError error) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }
                        });

                        /*SetPropertyRequest setWarningThresholdProperty = getRequestFactory().createSetPropertyRequest();
                        setWarningThresholdProperty.setPropertyName(propertySheetRoot+"/warningThreshold");
                        setWarningThresholdProperty.setValue(warningThreshold.getText());
                        setWarningThresholdProperty.setCallback(new PropertyCallback() {
                            @Override
                            public void handleResponse(Property response) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }

                            @Override
                            public void handleError(CommanderError error) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }
                        });*/

                        SetPropertyRequest setErrorThresholdProperty = getRequestFactory().createSetPropertyRequest();
                        setErrorThresholdProperty.setPropertyName(propertySheetRoot+"/errorThreshold");
                        setErrorThresholdProperty.setValue(errorThreshold.getText());
                        setErrorThresholdProperty.setCallback(new PropertyCallback() {
                            @Override
                            public void handleResponse(Property response) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }

                            @Override
                            public void handleError(CommanderError error) {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }
                        });


                        /*doRequest(new ChainedCallback() {
                            @Override
                            public void onComplete() {
                                //To change body of implemented methods use File | Settings | File Templates.
                            }
                        },setDashboardTitleProperty,setProcedureColumnHeaderProperty,setProjectColumnHeaderProperty,setHistoryProperty,setRefreshProperty);
                        */
                        ArrayList<CommanderRequest<?>> spArray = new ArrayList<CommanderRequest<?>>();
                        for(int i=0;i<projectRoot.getChildCount();i++){
                            //if project doesn't have any children then the project has not been
                            //selected so don't process

                            //if(projectRoot.getChild(i).getChildCount()>1){
                                String projectName = ((Label)projectRoot.getChild(i).getWidget()).getText();
                                for(int j=0;j<projectRoot.getChild(i).getChildCount();j++){
                                    Widget w = projectRoot.getChild(i).getChild(j).getWidget();
                                    if(w.getClass().getName().toLowerCase().contains("horizontalpanel")){
                                        HorizontalPanel hp = (HorizontalPanel)w;
                                        CheckBox cb = (CheckBox)hp.getWidget(0);
                                        Label pn = (Label)hp.getWidget(1);
                                        //"Loading procedures..." has a hidden checkbox that will never be selected so no need
                                        //for extra checks
                                        if(cb.getValue()){
                                            SetPropertyRequest sp = getRequestFactory().createSetPropertyRequest();
                                            sp.setPropertyName(propertySheetRoot+"/"+projectName+"/item"+String.valueOf(j));
                                            sp.setValue(pn.getText());
                                            sp.setCallback(new PropertyCallback() {
                                                @Override
                                                public void handleResponse(Property response) {
                                                    //To change body of implemented methods use File | Settings | File Templates.
                                                }

                                                @Override
                                                public void handleError(CommanderError error) {
                                                    //To change body of implemented methods use File | Settings | File Templates.
                                                    Window.alert("Could not create property "+error.getMessage());
                                                }
                                            });
                                            spArray.add(sp);
                                            //doRequest(sp);
                                        }
                                    }
                                }
                            //}
                        }

                        //have any workflows been selected?
                        if(spArray.isEmpty()){
                            Window.alert("No workflows selected");
                            btnSave.setEnabled(true);
                            return;
                        }


                        //finally, add other fields to the request list
                        spArray.add(setDashboardTitleProperty);
                        spArray.add(setWorkflowColumnHeaderProperty);
                        spArray.add(setProjectColumnHeaderProperty);
                        spArray.add(setHistoryProperty);
                        spArray.add(setRefreshProperty);
                        spArray.add(setSuccessThresholdProperty);
                        //spArray.add(setWarningThresholdProperty);
                        spArray.add(setErrorThresholdProperty);
                        doRequest(new ChainedCallback() {
                            @Override
                            public void onComplete() {
                                //To change body of implemented methods use File | Settings | File Templates.
                                /*String redirectTo = BrowserContext.getInstance().getGetParameter("redirectTo");
                                if (redirectTo != null) {
                                    Window.Location.assign(redirectTo);
                                } else {
                                    History.back();
                                }*/
                                returnToCallingPage();
                            }
                            //},setDashboardTitleProperty,setProcedureColumnHeaderProperty,setProjectColumnHeaderProperty,setHistoryProperty,setRefreshProperty);
                        }, spArray);

                    }

                    @Override
                    public void handleError(CommanderError error) {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }
                });
                doRequest(dpReq);
            }
            //To change body of implemented methods use File | Settings | File Templates.
            //process projects
        });
    }


    private void processDashboardConfig(PropertySheet ps){
        for (Map.Entry<String, Property> entry : ps.getProperties().entrySet())
        {
            Property prop = entry.getValue();
            if (prop.isStringProperty())
            {
                //no-op properties at the root have no meaning
                Property currentProp = entry.getValue();
                String currentValue = currentProp.getValue();
                if(prop.getName().toLowerCase()=="history"){
                    history.setText(currentValue);
                }
                
                if(prop.getName().toLowerCase()=="refresh"){
                    refresh.setText(currentValue);
                }

                if(prop.getName().toLowerCase()=="workflowcolumnheader"){
                    col2Header.setText(currentValue);
                }

                if(prop.getName().toLowerCase()=="projectcolumnheader"){
                    col1Header.setText(currentValue);
                }

                if(prop.getName().toLowerCase()=="dashboardtitle"){
                    dashboardTitle.setText(currentValue);
                }

                if(prop.getName().toLowerCase()=="successthreshold"){
                    successThreshold.setText(currentValue);
                }

                /*if(prop.getName().toLowerCase()=="warningthreshold"){
                    warningThreshold.setText(currentValue);
                } */

                if(prop.getName().toLowerCase()=="errorthreshold"){
                    errorThreshold.setText(currentValue);
                }

            }
        }

    }

    private HorizontalPanel createRow(final String name, Boolean selected){

        HorizontalPanel hp = new HorizontalPanel();
        CheckBox cb = new CheckBox();
        cb.setValue(selected);
        String wfName = name;
        wfName=wfName.trim();
        Label lbl = new Label(wfName);
        hp.add(cb);
        hp.add(lbl);
        //parentItem.addItem(new TreeItem(new Label(proc.getName())));

        return hp;
    }

    private Label projectExistsInTree(String projectName, TreeItem projectRoot){
        Label projectLabel=null;

        try{
            for(int i=0;i<projectRoot.getChildCount();i++){

                Label currentProject = (Label)projectRoot.getChild(i).getWidget();

                if(projectName.equalsIgnoreCase(currentProject.getText())){
                    projectLabel=currentProject;
                    break;
                }
            }
        }catch(Exception e){
            GWT.log("Exception checking if project exists in tree");

        }
        return projectLabel;
    }

    /*
        This procedure assumes that procedureRoot contains a number of TreeItems which contain
        HorizontalPanels on the following format

        cell 0: checkbox
        cell 1: Label containing the name of the procedure as text
     */
    private HorizontalPanel workflowExistsInTree(String workflowName, TreeItem workflowRoot){

        HorizontalPanel workflowPanel=null;

        int workflowsInList=workflowRoot.getChildCount();
        for(int i=0;i<workflowRoot.getChildCount();i++){
            HorizontalPanel hp = null;
            hp=(HorizontalPanel)workflowRoot.getChild(i).getWidget();
            if(hp!=null){
                //Window.alert(String.valueOf(hp.getWidgetCount()));
                if(hp.getWidgetCount()>1){
                    String currentWorkflowName = ((Label)hp.getWidget(1)).getText();
                    if(currentWorkflowName.toLowerCase()==workflowName.toLowerCase()){
                        workflowPanel=hp;
                        break;
                    }
                }
            }
        }

        return workflowPanel;

    }

    private void populateWorkflowList(final String projectName, final TreeItem parentItem, final Property propertySheet){
        GetWorkflowDefinitionsRequest reqWorkflows = getRequestFactory().createGetWorkflowDefinitionsRequest();
        reqWorkflows.setProjectName(projectName);

        reqWorkflows.setCallback(new WorkflowDefinitionListCallback() {
            @Override
            public void handleResponse(List<WorkflowDefinition> response) {
                //To change body of implemented methods use File | Settings | File Templates.
                //parentItem.addItem(new TreeItem(new Label(response.get())));
                //0parentItem.addItem(new TreeItem(new Label()))
                //Window.alert("add item.  processing "+String.valueOf(response.size()));
                boolean projectSelected=false;
                for(WorkflowDefinition wf:response){
                    HorizontalPanel hp = new HorizontalPanel();
                    hp=createRow(wf.getName(),false);
                    parentItem.addItem(hp);
                }

                if(propertySheet!=null){
                    //set projects selected in saved settings property sheet to checked
                    for (Map.Entry<String, Property> projectSheetEntry : propertySheet.getPropertySheet().getProperties().entrySet())
                    {
                        Property currentProp = projectSheetEntry.getValue();
                        String propertyName = currentProp.getName();
                        String propertyValue = currentProp.getValue();

                        HorizontalPanel hp = workflowExistsInTree(propertyValue, parentItem);
                        if(hp!=null){
                            CheckBox cb = (CheckBox)hp.getWidget(0);
                            cb.setValue(true);
                        }
                        //hp = createProcedureRow(propertyValue, true);
                        //parentItem.addItem(hp);
                    }
                }
                //parentItem.setState(true);
            }

            @Override
            public void handleError(CommanderError error) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        doRequest(reqWorkflows);
    }



    public void renderCurrentProjectAndWorkflowConfiguration(String propertySheetPath, boolean recursive){


        treeView.addOpenHandler(new OpenHandler<TreeItem>() {
            @Override
            public void onOpen(OpenEvent<TreeItem> treeItemOpenEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
                TreeItem ti = treeItemOpenEvent.getTarget();
                if(ti.getChildCount()>0){
                    //Label l = (Label)ti.getChild(0).getWidget();
                    if(ti.getChild(0).getWidget().getClass().getName().toLowerCase().contains("horizontalpanel")){

                        HorizontalPanel hp = (HorizontalPanel)ti.getChild(0).getWidget();
                        Label l = (Label)hp.getWidget(1);
                        if(l.getText().equalsIgnoreCase("Loading workflow definitons...")){
                            ti.getChild(0).remove();
                            String project=ti.getText();
                            populateWorkflowList(project,ti,null);
                            //populateProcedureList()
                        }
                    }
                }
            }
        });


        final TreeItem projectRoot = new TreeItem(new Label("Projects"));
        treeView.addItem(projectRoot);

        GetPropertiesRequest currentConfig = getRequestFactory().createGetPropertiesRequest();
        currentConfig.setPath(propertySheetPath);
        currentConfig.setRecurse(recursive);
        currentConfig.setCallback(new PropertySheetCallback() {
            @Override
            public void handleResponse(PropertySheet response) {

                processDashboardConfig(response);

                for (Map.Entry<String, Property> entry : response.getProperties().entrySet()) {
                    Property prop = entry.getValue();
                    if (prop.isStringProperty()) {
                        //skip top level properties, they have already been processed
                    } else {
                        //nested property sheets contain project specific configuration

                        String currentProjectName = entry.getKey();
                        projectRoot.addItem(new Label(currentProjectName));
                        TreeItem currentProjectItem = projectRoot.getChild(projectRoot.getChildCount() - 1);
                        populateWorkflowList(currentProjectName, currentProjectItem, prop);
                    }
                }

            }


            @Override
            public void handleError(CommanderError error) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        doRequest(currentConfig);

        //partial tree built based on config settings, now build rest of tree
        GetProjectsRequest gpReq = getRequestFactory().createGetProjectsRequest();
        gpReq.setCallback(new ProjectListCallback() {
            @Override
            public void handleResponse(List<Project> response) {
                //To change body of implemented methods use File | Settings | File Templates.
                for(Project currentProject:response){
                    if(currentProject.getPluginName().isEmpty()){
                        Label projectLabel=projectExistsInTree(currentProject.getName(),projectRoot);
                        if(projectLabel==null){
                            projectLabel = new Label(currentProject.getName());
                            TreeItem ti = new TreeItem(projectLabel);
                            HorizontalPanel loadingWorkflowsHp = createRow("Loading workflow definitons...",false);
                            //ti.addItem(new Label("Loading procedures..."));
                            //hide checkbox
                            //loadingProceduresHp.getWidget(0).setVisible(false);
                            ti.addItem(loadingWorkflowsHp);

                            projectRoot.addItem(ti);
                        }
                        projectLabel.addClickHandler(new ClickHandler() {

                            @Override
                            public void onClick(ClickEvent event) {
                                //To change body of implemented methods use File | Settings | File Templates.
                                //Window.alert(event.getSource().getClass().getName());

                                final TreeItem parentItem = treeView.getSelectedItem();
                                if(parentItem.getState()==true){
                                    parentItem.setState(false);
                                }
                                else{
                                    parentItem.setState(true);
                                }
                                /*Label projectLabel = ((Label) event.getSource());
                                //if only has "Loading procedures item"
                                if (parentItem.getChildCount() == 1) {
                                    parentItem.getChild(0).remove();
                                    String projectName=((Label)event.getSource()).getText();
                                    populateProcedureList(projectName,parentItem,null);
                                } */


                            }
                        });
                        projectRoot.setState(true);
                    }
                }
            }


            @Override
            public void handleError(CommanderError error) {
                //To change body of implemented methods use File | Settings | File Templates.
                Window.alert("Could not get project list: "+error.getMessage());
            }
        });
        doRequest(gpReq);




    }
}
