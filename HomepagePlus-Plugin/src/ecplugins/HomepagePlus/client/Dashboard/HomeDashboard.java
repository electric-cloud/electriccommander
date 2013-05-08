//
// Copyright (c) 2005-2012 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.HomepagePlus.client.Dashboard;
import java.util.HashMap;
import java.util.List;
import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.gwt.client.ComponentBase;
import com.electriccloud.commander.gwt.client.ComponentBaseFactory;
import com.electriccloud.commander.client.domain.ObjectType;
import com.electriccloud.commander.client.domain.Order;
import com.electriccloud.commander.client.domain.Property;
import com.electriccloud.commander.client.domain.Workflow;
import com.electriccloud.commander.client.requests.FindObjectsFilter;
import com.electriccloud.commander.client.requests.FindObjectsRequest;
import com.electriccloud.commander.client.requests.GetPropertyRequest;
import com.electriccloud.commander.client.responses.CommanderError;
import com.electriccloud.commander.client.responses.DefaultFindObjectsResponseCallback;
import com.electriccloud.commander.client.responses.FindObjectsResponse;
import com.electriccloud.commander.client.responses.PropertyCallback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import ecplugins.HomepagePlus.client.Dashboard.ui.ShowWidgetCallback;

/**
 * This widget component constructs the "Active Tasks" panel/dashboard listing 
 * active task workflows.
 * Each row in the panel except for the header row instantiates a WorkflowPanel   
 * 
 */
public class HomeDashboard 
extends ComponentBase
{
    /*
     * Set up the binder to the ui-definition, as defined by the <classname>.ui.xml file
     */
    private static HomeDashboard2UiBinder uiBinder = GWT.create(HomeDashboard2UiBinder.class);
    interface HomeDashboard2UiBinder extends UiBinder<Widget, HomeDashboard> {}
    interface Style extends CssResource {
        String evenRow();
        String oddRow();
        String headerRow();
      }
    @UiField FlexTable dashboardPanel;
    @UiField SimplePanel loadingPanel;
    @UiField Style style;

    //TODO: Is this really a proper way of solving the need for a final reference to the class?
    private HomeDashboard iThis;
    
    private FindObjectsResponse iResponse;
    private boolean iUseRemoteAccess;
    private boolean iUseAppServerDeployment;
    
    @Override public Widget doInit() {
        // set a reference to ourselves such that we can use it in anonymous classes
        iThis = this;
        // instantiate the main widget, holding the dashboard
        Widget mainLaunch = uiBinder.createAndBindUi(this);

        iResponse = null;
        iUseRemoteAccess = false;
        iUseAppServerDeployment = false;
        generateWorkflowListing();
        // if we haven't been instructed to make the dashboard visible in 4 secs, 
        // force it visible
        Timer t = new Timer() {
            
            @SuppressWarnings("deprecation")
            @Override public void run()
            {
                // we use the deferred-command mechanism to allow all outstanding
                // events to run before this, which basically means that it's 
                // guaranteed that the css is fully loaded before we show the dashboard  
                DeferredCommand.addCommand(new Command() {

                    @Override public void execute()
                    {
                        loadingPanel.setVisible(false);
                        dashboardPanel.setVisible(true);
                    }
                    
                });
            }
        };
        t.schedule(4000);
        return mainLaunch;
      }

    public FlexTable getFlexTable(){
        return dashboardPanel;
    }
    
    public void generateWorkflowListing(){
        // hash of booleans used to control when the main dashboard should be shown, 
        // instead of the "Loading..." label.
        final HashMap<ShowWidgetCallback, Boolean> showPanelCallbacks = new HashMap<ShowWidgetCallback, Boolean>(); 
        // FindObjectsRequest to query for all active workflows 
        FindObjectsRequest findObjectsRequest = this.getRequestFactory().createFindObjectsRequest(ObjectType.workflow);
        // mcmahon
        //findObjectsRequest.addFilter(new FindObjectsFilter.EqualsFilter("projectName", "jpmdis"));
        //findObjectsRequest.addFilter(new FindObjectsFilter.EqualsFilter("workflowDefinitionName", "jpmDisBasic"));
        //findObjectsRequest.addFilter(new FindObjectsFilter.EqualsFilter("projectName", "Galaxy PDC"));
        //findObjectsRequest.addFilter(new FindObjectsFilter.EqualsFilter("workflowDefinitionName", "GALAXY"));
        //findObjectsRequest.addFilter(new FindObjectsFilter.EqualsFilter("Owner", "foo"));
        findObjectsRequest.setNumObjects(10);
        findObjectsRequest.addFilter(new FindObjectsFilter.NotEqualFilter("Store workflow", "true"));
        findObjectsRequest.addSort("modifyTime", Order.descending);
        findObjectsRequest.setCallback(new DefaultFindObjectsResponseCallback(this){
            @Override public void handleResponse(FindObjectsResponse response)
            {
                iResponse = response;
            }
        });

        this.doRequest(new ChainedCallback() {
            
            @SuppressWarnings("deprecation")
            @Override
            public void onComplete() {
                if(iResponse == null){
                    return;
                }
                dashboardPanel.clear();
                List<Workflow> workflows  = iResponse.getWorkflows();
                // if empty result-set, notify user
                if(workflows.isEmpty()){
                    DeferredCommand.addCommand(new Command() {

                        @Override public void execute()
                        {
                            dashboardPanel.add(new Label("No active tasks"));
                            loadingPanel.setVisible(false);
                            dashboardPanel.setVisible(true);
                        }
                    });
                    return;
                }
                boolean isEvenRow = false;
                // create header-row in the main dashboard
                //Window.alert("mcmahon");
                dashboardPanel.setWidget(0, 0, new Label("Workflow"));
                dashboardPanel.setWidget(0, 1, new Label("Current State"));
                dashboardPanel.setWidget(0, 2, new Label("Available Transitions"));                
                dashboardPanel.setWidget(0, 3, new Label("Reports"));
                dashboardPanel.setWidget(0, 4, new Label("Last Job"));
                dashboardPanel.getRowFormatter().addStyleName(0, style.headerRow());

                // iterate through each workflow and generate a row in the dashboard for each
                for (final Workflow workflow : workflows) {
                    String task = null;
                    // callback that will handle the logic of showing the dashboard when appropriate
                    ShowWidgetCallback showPanelCallback = new ShowWidgetCallback() {
                        @Override public void show()
                        {
                            // register this row as complete
                            showPanelCallbacks.put(this, true);
                            boolean showPanel = true;
                            // loop through the hash to determine if the dashboard should be shown
                            for(ShowWidgetCallback showPanelCallback : showPanelCallbacks.keySet()){
                                showPanel = showPanel && showPanelCallbacks.get(showPanelCallback);
                            }
                            if(showPanel){
                                DeferredCommand.addCommand(new Command() {

                                    @Override public void execute()
                                    {
                                        loadingPanel.setVisible(false);
                                        dashboardPanel.setVisible(true);
                                    }
                                    
                                });                                
                            }
                        }
                        @Override public void hide()
                        {
                            //no-op                
                        }
                    };
                    // add new callback to the hash as non-complete
                    showPanelCallbacks.put(showPanelCallback, false);
                    // Add another row to the workflow-panel
                    //Window.alert("mcmahon: " + task);
                    WorkflowPanel wfp = new WorkflowPanel(iThis, showPanelCallback, workflow, task, iUseRemoteAccess, iUseAppServerDeployment);
                    // set appropiate style
                    dashboardPanel.getRowFormatter().addStyleName(wfp.getRowCount(), (isEvenRow)?(style.evenRow()):(style.oddRow()));
                    isEvenRow = !isEvenRow;
                    dashboardPanel.getColumnFormatter().setWidth(0, "5ex"); // Table shows this width OR width of smallest element
                    dashboardPanel.getColumnFormatter().setWidth(4, "5ex"); // mcmahon
                    setColumnVisible(dashboardPanel, 3, false);
                }                
            }
        }, findObjectsRequest, queryUseRemoteAccessRequest(), queryUseAppServerDeploymentRequest());
    }

    private void setColumnVisible(FlexTable table, int Col, boolean b) {
        for (int i = 0; i < table.getRowCount(); i++) {
        table.getCellFormatter().setVisible(i, Col, b);
   }
}

    private GetPropertyRequest queryUseRemoteAccessRequest(){
        GetPropertyRequest useRemoteAccessRequest = this.getRequestFactory().createGetPropertyRequest();
        useRemoteAccessRequest.setPropertyName(ComponentBaseFactory.getPluginProjectPath()+"/useRemoteAccess");
        useRemoteAccessRequest.setCallback(new PropertyCallback() {
            @Override public void handleResponse(Property response)
            {
                String val = response.getValue();
                if(val.equals("1") || val.equals("true")){
                    iUseRemoteAccess = true;
                }
            }

            @Override public void handleError(CommanderError error)
            {
                // no-op, we don't care if this property doesn't exist
            }
        });
        return useRemoteAccessRequest;
    }

    private GetPropertyRequest queryUseAppServerDeploymentRequest(){
        GetPropertyRequest useAppServerDeploymentRequest = this.getRequestFactory().createGetPropertyRequest();
        useAppServerDeploymentRequest.setPropertyName(ComponentBaseFactory.getPluginProjectPath()+"/useAppServerDeployment");
        useAppServerDeploymentRequest.setCallback(new PropertyCallback() {
            @Override public void handleResponse(Property response)
            {
                String val = response.getValue();
                if(val.equals("1") || val.equals("true")){
                    iUseAppServerDeployment = true;
                }
            }

            @Override public void handleError(CommanderError error)
            {
                // no-op, we don't care if this property doesn't exist
            }
        });
        return useAppServerDeploymentRequest;
    }
    
}
