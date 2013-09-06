
// WorkflowDashboardMain.java --
//
// WorkflowDashboardMain.java is part of the WorkflowDashboard plugin.
//
// Copyright (c) 2005-2010 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.WorkflowDashboard.client;

import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.gwt.client.ComponentBase;
import com.electriccloud.commander.client.domain.*;
import com.electriccloud.commander.client.requests.*;
import com.electriccloud.commander.client.responses.*;
import com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

import ecplugins.WorkflowDashboard.client.events.WorkflowProcessedEvent;
import ecplugins.WorkflowDashboard.client.events.WorkflowProcessedEventHandler;

import com.google.gwt.event.shared.SimpleEventBus;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;


/**
* Basic component that is meant to be cloned and then customized to perform a
* real function.
*/
public class WorkflowDashboardMain
    extends ComponentBase implements WorkflowProcessedEventHandler
{

    private static WorkflowDashboardMainUiBinder uiBinder = GWT.create(
    		WorkflowDashboardMainUiBinder.class);
    interface WorkflowDashboardMainUiBinder extends UiBinder<Widget, WorkflowDashboardMain> {}

    interface Style extends CssResource {
        String evenRow();
        String oddRow();
        String headerRow();
        String vPanel();
        String important();
        String link();
        String h2();
        String topPanel();
        String mouseCursor();
        String projectHeader();
        String historyHeader();
        String lastRunHeader();
        String procedureHeader();
        String descriptionHeader();
    }


    //@UiField Anchor configureLink;
    @UiField FlexTable dashboardPanel;
    @UiField Label refreshLink;
    @UiField Style style;
    @UiField Label dashboardTitle;
    @UiField Label errorMessage;
    @UiField Anchor configureLink;

    private WorkflowDashboardMain iThis;
    
    private FindObjectsResponse iResponse;
    
    final int DEFAULT_NUMBER_OF_JOBS=10;
    final int DEFAULT_SUCCESS_THRESHOLD=75;
    final int DEFAULT_ERROR_THRESHOLD=25;
    final String DEFAULT_PROCEDURE_COLUMN_NAME="Workflow";
    final String DEFAULT_PROJECT_COLUMN_NAME="Project";
    final int EXPAND_IMAGE = 0;
    final int COLLAPSE_IMAGE = 1;

    int m_numberOfjobs=DEFAULT_NUMBER_OF_JOBS;
    int m_errorThreshold = DEFAULT_ERROR_THRESHOLD;
    int m_successThreshold = DEFAULT_SUCCESS_THRESHOLD;

    int totalWfProcessed = 0;

    private SimpleEventBus m_eventBus;
    final private HashMap m_hmWorkflowStats=new HashMap();
    int REFRESH_INTERVAL = 30000;

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

        iThis = this;
        m_eventBus = new SimpleEventBus();
        WorkflowProcessedEvent.register(m_eventBus,this);

        DashboardResources.INSTANCE.css().ensureInjected();
        DashboardResources.RESOURCES.DashboardStyles().ensureInjected();

        // instantiate the main widget, holding the dashboard
        Widget mainLaunch = uiBinder.createAndBindUi(this);
        iResponse = null;

        //m_hmWorkflowStats = new HashMap();

        createConfigureLink();
        processPropertySheet2();

        return mainLaunch;
        
    }
    
    protected void refreshCounter(final int refreshRate) {
        Timer t = new Timer() {
        	int countDown = refreshRate;
        	public void run() {
				refreshLink.setText(("Refreshing in: " + countDown + " seconds"));
				countDown = countDown -1 ;
                if (countDown == 0){
                 	countDown=refreshRate;
                 	processPropertySheetOnRefresh();
                 	//Window.Location.reload();
                	 }
				}
		    };	
		    t.scheduleRepeating(1000);
    }

    @Override public void onWorkflowProcessed(WorkflowProcessedEvent event)

    {
        WorkflowStats eventStats = event.getResult();
        String key = eventStats.getWfKey();

        WorkflowStats currentWfStats = null;

        if(!m_hmWorkflowStats.containsKey(key)){
            currentWfStats = eventStats;
            m_hmWorkflowStats.put(eventStats.getWfKey(),eventStats);
        }
        else{
            currentWfStats = (WorkflowStats)m_hmWorkflowStats.get(key);

            //update stats

            int totalWorkflows = currentWfStats.getTotalWorkflows();
            totalWorkflows++;
            currentWfStats.setTotalWorkflows(totalWorkflows);

            if(eventStats.getNumberOfWorkflowsCompletedWithError()>0){
                int totalWfCompletedWithError = currentWfStats.getNumberOfWorkflowsCompletedWithError();
                totalWfCompletedWithError++;
                currentWfStats.setNumberOfWorkflowsCompletedWithError(totalWfCompletedWithError);
            }

            if(eventStats.getNumberOfWorkflowsCompletedWithSuccess()>0){
                int totalWfCompletedWithSuccess = currentWfStats.getNumberOfWorkflowsCompletedWithSuccess();
                totalWfCompletedWithSuccess++;
                currentWfStats.setNumberOfWorkflowsCompletedWithSuccess(totalWfCompletedWithSuccess);
            }

            if(eventStats.getNumberOfWorkflowsWaitingForManualIntervention()>0){
                int totalWfAwaitingManualIntervention = currentWfStats.getNumberOfWorkflowsWaitingForManualIntervention();
                totalWfAwaitingManualIntervention++;
                currentWfStats.setNumberOfWorkflowsWaitingForManualIntervention(totalWfAwaitingManualIntervention);
            }

            long totalCycleTime = currentWfStats.getTotalCycleTimeMs();
            long currentCycleTime = eventStats.getTotalCycleTimeMs();


            totalCycleTime+=currentCycleTime;

            currentWfStats.setTotalCycleTimeMs(totalCycleTime);
            //Window.alert(currentWfStats.getAverageCycleTimeAsString());
            //update average cycle time
            /*String totalCycleTime = currentWfStats.getCycleTime();
            DateTimeFormat dtf = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.ISO_8601);
            Date d = dtf.parse(totalCycleTime);
            
            String eventCycleTime=eventStats.getCycleTime();
            Date d2=dtf.parse(eventCycleTime);
            
           long totalDate = d.getTime()+d2.getTime();*/
            

            

        }

        //Window.alert(currentWfStats.getDump());
        currentWfStats.getAvgCycleTimeContainer().setText(currentWfStats.getAverageCycleTimeAsString());
        currentWfStats.getHtmlStatusContainer().setHTML(generateIndicatorImage(m_successThreshold,m_errorThreshold,currentWfStats));

    }

    private void createConfigureLink(){
        HashMap<String,String> paramMap=new HashMap<String,String>();
        String href="/commander/pages/ActiveWorkflowDashboard/ConfigureDashboard_run?redirectTo="+CommanderUrlBuilder.createRedirectUrl().buildString();
        paramMap.put("redirectTo", CommanderUrlBuilder.createRedirectUrl().buildString());
        CommanderUrlBuilder url = CommanderUrlBuilder.createUrl("pages/WorkflowDashboard/ConfigureDashboard_run");
        url.setParameters(paramMap);
        href=url.buildString();
        configureLink.setHref(href);

    }
    
    private Anchor generateWorkflowCellImage(final Workflow wf,final WorkflowData wfData){
        
        Image img=null;
        //first check if workflow is completed
        if(wf.isCompleted()){
            if(wfData.getCompletedWithSuccess()==true){
                img=new Image(DashboardResources.RESOURCES.buildSuccessIcon());    
            }else{
               img = new Image(DashboardResources.RESOURCES.buildFailedIcon()); 
            }
        }
        else{
            

            if(wfData.getIsManualTransition()){
                img = new Image(DashboardResources.RESOURCES.workflowManualTransitionIcon());
            }else{
                if(wfData.getHasActiveJob()){
                    img=new Image(DashboardResources.RESOURCES.runningSuccessIcon());
                }
                else{
                    if(wfData.getHasActiveWorkflow()){
                        img=new Image(DashboardResources.RESOURCES.runningWorkflowIcon());
                    }
                }

            }
        }
        
        String description = wfData.getDescription();
        if (description.isEmpty()) {
          img.setTitle("Active State: "+wf.getActiveState());
        } else {
            img.setTitle("Active State: "+wf.getActiveState() + "\n"+ wfData.getDescription());
        }
        Anchor clickable = new Anchor();
        clickable.addStyleName(DashboardResources.RESOURCES.DashboardStyles().no_underline());
        clickable.setHref(createWorkflowUrl(wf));
        Element linkElement = clickable.getElement();
        linkElement.appendChild(img.getElement());
        Element clickableElement=clickable.getElement();
        clickableElement.appendChild(img.getElement());
        return clickable;
    }

    private String generateIndicatorImage(int successThreshold,int errorThreshold, WorkflowStats wfStats){
        //int completedWithSuccess=wfStats.getNumberOfWorkflowsCompletedWithSuccess();
        //int completedWithError=wfStats.getNumberOfWorkflowsCompletedWithError();
        //Window.alert("Completed with success "+String.valueOf(wfStats.getNumberOfWorkflowsCompletedWithSuccess()));
        //Window.alert("Completed with error "+String.valueOf(wfStats.getNumberOfWorkflowsCompletedWithError()));
        //Window.alert("success rate "+String.valueOf(wfStats.getSuccessRate()));
        Image img=null;
        HTML htmlStatusIndicator = new HTML();

        if(wfStats.getSuccessRate()>=successThreshold){
            img = new Image(DashboardResources.RESOURCES.GreenLightIcon());
            htmlStatusIndicator.getElement().appendChild(img.getElement());
        }else{
            if(wfStats.getSuccessRate()<=errorThreshold){
                img = new Image(DashboardResources.RESOURCES.RedLightIcon());
                htmlStatusIndicator.getElement().appendChild(img.getElement());
            }
            else{

                if(wfStats.getSuccessRate()>errorThreshold && wfStats.getSuccessRate()<successThreshold){

                    img = new Image(DashboardResources.RESOURCES.YellowLightIcon());
                    htmlStatusIndicator.getElement().appendChild(img.getElement());
                }
            }
        }
        return htmlStatusIndicator.getHTML();
    }

    private void processCurrentWorkflow(final Workflow wf, final WorkflowData wfData, final FlexTable ft, final int currentRowItem, final WorkflowStats wfStats1, final HTML htmlStatusIndicator,
                                        final Label avgCycleTimeContainer, final int successThreshold, final int errorThreshold){



        final WorkflowStats wfStats = new WorkflowStats();
        wfStats.setProjectName(wf.getProjectName());
        wfStats.setWorkflowName(wf.getWorkflowDefinitionName());
        //Window.alert(wf.getCreateTime());

        // wfData.setDescription("this is text");
        GetPropertyRequest propReqDesc = getRequestFactory().createGetPropertyRequest();
        propReqDesc.setWorkflowName(wf.getName());
        propReqDesc.setStateName(wf.getStartingState());
        propReqDesc.setProjectName(wf.getProjectName());
        propReqDesc.setPropertyName("ec_workflow_description");
        propReqDesc.setCallback(new PropertyCallback() {
            @Override
            public void handleResponse(Property response) {

            	wfData.setDescription(response.getValue());

            }
            @Override
            public void handleError(CommanderError error) {
            	 // nothing for now
            }
        });
        doRequest(propReqDesc);
        
        
        
        //m_eventBus.(new WorkflowProcessedEvent(true));
        if(wf.isCompleted()){
            //check workflow result
            GetPropertyRequest propReq = getRequestFactory().createGetPropertyRequest();
            propReq.setWorkflowName(wf.getName());
            propReq.setStateName(wf.getStartingState());
            propReq.setProjectName(wf.getProjectName());
            propReq.setPropertyName("result");
            propReq.setCallback(new PropertyCallback() {
                @Override
                public void handleResponse(Property response) {



                    //To change body of implemented methods use File | Settings | File Templates.
                    wfData.setCompletedWithSuccess(true);
                    if(response.getValue().toLowerCase().equals("error")){
                        wfData.setCompletedWithSuccess(false);
                        int completedWithError = wfStats.getNumberOfWorkflowsCompletedWithError();
                        completedWithError++;
                        wfStats.setNumberOfWorkflowsCompletedWithError((completedWithError));
                    }
                    else{

                        int completedWithSuccess = wfStats.getNumberOfWorkflowsCompletedWithSuccess();
                        completedWithSuccess++;
                        wfStats.setNumberOfWorkflowsCompletedWithSuccess(completedWithSuccess);
                        //Window.alert(String.valueOf(completedWithSuccess));
                    }


                    //if(wfData.getCompletedWithSuccess()){
                    //    Window.alert("adding cell for completed with success workflow "+wf.getName());
                    //}else{
                    //    Window.alert("adding cell for completed with failure workflow "+wf.getName());

                    //}
                    ft.setWidget(0, currentRowItem, generateWorkflowCellImage(wf, wfData));
                    wfStats.setHtmlStatusContainer(htmlStatusIndicator);
                    wfStats.setAvgCycleTimeContainer(avgCycleTimeContainer);
                    //long cycletime = wfStats.calculateCycleTime(wf.getCreateTime(),wf.getModifyTime());
                    wfStats.setTotalCycleTimeMs(wf.getElapsedTime());
                    m_eventBus.fireEvent(new WorkflowProcessedEvent(wfStats));

                    //htmlStatusIndicator.setHTML(generateIndicatorImage(successThreshold,warningThreshold,errorThreshold,wfStats));
                }

                @Override
                public void handleError(CommanderError error) {
                    //To change body of implemented methods use File | Settings | File Templates.
                    //assume result property does not exist.  Display as completed with success.
                    wfData.setCompletedWithSuccess(true);
                    int completedWithSuccess = wfStats.getNumberOfWorkflowsCompletedWithSuccess();
                    completedWithSuccess++;
                    wfStats.setNumberOfWorkflowsCompletedWithSuccess(completedWithSuccess);
                    ft.setWidget(0, currentRowItem, generateWorkflowCellImage(wf, wfData));
                    wfStats.setAvgCycleTimeContainer(avgCycleTimeContainer);
                    wfStats.setHtmlStatusContainer(htmlStatusIndicator);
                    //long cycletime = wfStats.calculateCycleTime(wf.getCreateTime(),wf.getModifyTime());
                    wfStats.setTotalCycleTimeMs(wf.getElapsedTime());
                    m_eventBus.fireEvent(new WorkflowProcessedEvent(wfStats));
                    //htmlStatusIndicator.setHTML(generateIndicatorImage(successThreshold,warningThreshold,errorThreshold,wfStats));


                }
            });
            doRequest(propReq);
        }
        else{
        
            State s = null;
            com.electriccloud.commander.client.requests.
            GetStateRequest wftrans = getRequestFactory().createGetStateRequest();
            wftrans.setProjectName(wf.getProjectName());
            wftrans.setWorkflowName(wf.getName());
            wftrans.setStateName(wf.getActiveState());
            wftrans.setCallback(new StateCallback() {
                @Override
                public void handleResponse(State response) {
                    //To change body of implemented methods use File | Settings | File Templates.
                    if ((response.getSubjob() == null || response.getSubjob() == "") && (response.getSubworkflow() == null || response.getSubworkflow() == "")) {
                        wfData.setIsManualTransition(true);
                        ft.setWidget(0, currentRowItem, generateWorkflowCellImage(wf, wfData));
                        int manualTransitions = wfStats.getNumberOfWorkflowsWaitingForManualIntervention();
                        manualTransitions++;
                        wfStats.setNumberOfWorkflowsWaitingForManualIntervention(manualTransitions);
                        wfStats.setAvgCycleTimeContainer(avgCycleTimeContainer);
                        wfStats.setHtmlStatusContainer(htmlStatusIndicator);

                        //long cycletime = wfStats.calculateCycleTime(wf.getCreateTime(),wf.getModifyTime());
                        wfStats.setTotalCycleTimeMs(wf.getElapsedTime());
                        m_eventBus.fireEvent(new WorkflowProcessedEvent(wfStats));

                        //htmlStatusIndicator.setHTML(generateIndicatorImage(successThreshold,warningThreshold,errorThreshold,wfStats));
                    } else {
                        if (!response.getSubjob().equals("")) {
                            FindObjectsRequest jobRequest = getRequestFactory().createFindObjectsRequest(ObjectType.job);
                            jobRequest.addFilter(new FindObjectsFilter.EqualsFilter("jobName", response.getSubjob()));
                            jobRequest.setCallback(new FindObjectsResponseCallback() {
                                @Override
                                public void handleResponse(FindObjectsResponse response) {
                                    //To change body of implemented methods use File | Settings | File Templates.
                                    if (response.getJobs().isEmpty() == false) {
                                        Job currentJob = response.getJobs().get(0);
                                        if (currentJob.getStatus() == JobStatus.completed) {
                                            //must be manual transition
                                            wfData.setIsManualTransition(true);
                                        } else {
                                            wfData.setHasActiveJob(true);
                                            wfData.setIsManualTransition(false);
                                        }
                                        int numberOfActiveWorkflows=wfStats.getNumberOfActiveWorkflows();
                                        numberOfActiveWorkflows++;
                                        wfStats.setNumberOfActiveWorkflows(numberOfActiveWorkflows);

                                        ft.setWidget(0, currentRowItem, generateWorkflowCellImage(wf, wfData));
                                        wfStats.setAvgCycleTimeContainer(avgCycleTimeContainer);
                                        wfStats.setHtmlStatusContainer(htmlStatusIndicator);
                                        //long cycletime = wfStats.calculateCycleTime(wf.getCreateTime(),wf.getModifyTime());

                                        wfStats.setTotalCycleTimeMs(wf.getElapsedTime());
                                        m_eventBus.fireEvent(new WorkflowProcessedEvent(wfStats));
                                        //htmlStatusIndicator.setHTML(generateIndicatorImage(successThreshold,warningThreshold,errorThreshold,wfStats));
                                    }
                                }

                                @Override
                                public void handleError(CommanderError error) {
                                    //To change body of implemented methods use File | Settings | File Templates.
                                    Window.alert("Error: " + error.getMessage());
                                }
                            });
                            doRequest(jobRequest);
                        } else {
                            if (!response.getSubworkflow().equals("")) {
                                FindObjectsRequest wfRequest = getRequestFactory().createFindObjectsRequest(ObjectType.workflow);
                                wfRequest.addFilter(new FindObjectsFilter.EqualsFilter("workflowName", response.getSubworkflow()));
                                wfRequest.setCallback(new FindObjectsResponseCallback() {
                                    @Override
                                    public void handleResponse(FindObjectsResponse response) {
                                        //To change body of implemented methods use File | Settings | File Templates.
                                        if (response.getWorkflows().isEmpty() == false) {
                                            Workflow currentWorkflow = response.getWorkflows().get(0);
                                            if (currentWorkflow.isCompleted()) {
                                                //must be manual transition
                                                wfData.setIsManualTransition(true);
                                            } else {
                                                wfData.setHasActiveWorkflow(true);
                                                wfData.setIsManualTransition(false);
                                            }
                                            ft.setWidget(0, currentRowItem, generateWorkflowCellImage(wf, wfData));
                                            //htmlStatusIndicator.setHTML(generateIndicatorImage(75,31,30,wfStats));
                                            wfStats.setAvgCycleTimeContainer(avgCycleTimeContainer);
                                            wfStats.setHtmlStatusContainer(htmlStatusIndicator);
                                            //long cycletime = wfStats.calculateCycleTime(wf.getCreateTime(),wf.getModifyTime());
                                            wfStats.setTotalCycleTimeMs(wf.getElapsedTime());
                                            m_eventBus.fireEvent(new WorkflowProcessedEvent(wfStats));

                                        }
                                    }

                                    @Override
                                    public void handleError(CommanderError error) {
                                        //To change body of implemented methods use File | Settings | File Templates.
                                        Window.alert("Error: " + error.getMessage());
                                    }
                                });
                                doRequest(wfRequest);

                            }
                        }
                    }

                }

                @Override
                public void handleError(CommanderError error) {
                    //To change body of implemented methods use File | Settings | File Templates.
                    Window.alert("Error: " + error.getMessage());
                }
            });
            doRequest(new ChainedCallback() {
                @Override
                public void onComplete() {
                    //To change body of implemented methods use File | Settings | File Templates.
                    //ft.setWidget(0, currentRowItem, generateWorkflowCellImage(wf, wfData));
                }
            }, wftrans);
        }
    }
    private void populateTableRow(final String projectName,
                                  final String procedureName,
                                  final Label lblField, 
                                  final FlexTable ftJobs,
                                  final HTML htmlLastRun,
                                  final HTML htmlStatusIndicator,
                                  final Label avgCycleTimeContainer,
                                  final int successThreshold,
                                  final int errorThreshold){
        FindObjectsRequest findObjectsRequest = this.getRequestFactory().createFindObjectsRequest(ObjectType.workflow);
        findObjectsRequest.addFilter(new FindObjectsFilter.EqualsFilter("projectName", projectName));
        findObjectsRequest.addFilter(new FindObjectsFilter.EqualsFilter("workflowDefinitionName", procedureName));
        findObjectsRequest.addSort("modifyTime", Order.descending);
        findObjectsRequest.addSelect("elapsedTime",false);
        findObjectsRequest.setNumObjects(m_numberOfjobs);


        findObjectsRequest.setCallback(new FindObjectsResponseCallback(){
            @Override public void handleResponse(FindObjectsResponse response)
            {
                iResponse = response;

	            List<Workflow> workflows  = response.getWorkflows();
                lblField.setText(procedureName);
	            if(workflows.isEmpty()){
	            	ftJobs.clear();
	            	//Window.alert("no jobs for "+procedureName+" requested "+m_numberOfjobs+" jobs from project "+projectName);
                    ftJobs.setWidget(0,0,new Label("no workflows found.") );
                    return;
	            }
            	int currentRowItem = m_numberOfjobs+3; //number of jobs + first 3 columns for project,procedure, stats
                int successCount=0;

                int completedWithSuccessCount=0;

                Workflow lastWorkflow = workflows.get(0);

	            //populate last run column
                String lastWorkflowRunText="<B>Name:</B> "+lastWorkflow.getName();
                lastWorkflowRunText+="<BR /><B>Started By:</B> "+lastWorkflow.getLaunchedByUser();
                lastWorkflowRunText+="<BR /><B>Current State:</B>"+lastWorkflow.getActiveState();
                //lastJobRunText+="<BR /><B>Started At:</B> "+lastRunDate;
                htmlLastRun.setHTML(lastWorkflowRunText);
                //lblLastRun.setWordWrap(true);
                for(final Workflow currentWorkflow:workflows){

                    WorkflowData wfData = new WorkflowData();
                    //Window.alert(String.valueOf(wfData.getCompletedWithSuccess()));
                    //SimpleEventBus eventBus = new SimpleEventBus();
                    //WorkflowProcessedEvent.register(eventBus,this);
                    WorkflowStats wfStats = new WorkflowStats();
                    processCurrentWorkflow(currentWorkflow,wfData,ftJobs,currentRowItem, wfStats, htmlStatusIndicator,avgCycleTimeContainer, successThreshold, errorThreshold);
                    if(wfData.getCompletedWithSuccess()==true){
                        completedWithSuccessCount++;
                    }
                    
                    //Window.alert("Completed with success: "+String.valueOf(wfData.getCompletedWithSuccess()));
                    //Window.alert("Is manual transition: "+String.valueOf(wfData.getIsManualTransition()));
                    currentRowItem--;
	            }
                //if currentRowItem > 3 generate other cells
	            while(currentRowItem>3)
	            {
	                Image img = new Image();
                    img=new Image(DashboardResources.RESOURCES.buildNoDataIcon());
	            	ftJobs.setWidget(0, currentRowItem, img);
	            	currentRowItem--;
	            	
	            }

                //Image img = new Image();
                //Window.alert(String.valueOf(completedWithSuccessCount));
                //double successRate=((double)completedWithSuccessCount/(double)workflows.size())*100;
                //img=generateSuccessImageUrl(completedWithSuccessCount,workflows.size());
                //hpStatus.add(img);

                //hpStatus.add(new Label(Integer.toString((int)successRate) + "%"));
                //hRatio.setName("ratio");
                //hRatio.setValue(String.valueOf(successRate));
			};

            

			@Override
			public void handleError(CommanderError error) {
				// TODO Auto-generated method stub
				Window.alert(error.getMessage());
			}
        });
        doRequest(findObjectsRequest);
    }
    
    
    Image generateSuccessImageUrl(double successCount,double totalNumberOfJobs)
    {
    	double successPercentage = (successCount/totalNumberOfJobs) * 100;

    	Image img = null;
        if(successPercentage < 5){

            img=new Image(DashboardResources.RESOURCES.successRatio000Icon());
    	}
    	if(successPercentage > 5 && successPercentage <= 15){
            img=new Image(DashboardResources.RESOURCES.successRatio010Icon());
    	}
    	if(successPercentage > 15 && successPercentage <= 22){
            img=new Image(DashboardResources.RESOURCES.successRatio020Icon());
    	}
    	if(successPercentage > 22 && successPercentage <= 28){
            img=new Image(DashboardResources.RESOURCES.successRatio025Icon());
    	}
        if(successPercentage > 28 && successPercentage <= 35){
            img=new Image(DashboardResources.RESOURCES.successRatio030Icon());
        }
    	if(successPercentage > 35 && successPercentage <= 45){
            img=new Image(DashboardResources.RESOURCES.successRatio040Icon());
    	}
    	if(successPercentage > 45 && successPercentage <= 55){
            img=new Image(DashboardResources.RESOURCES.successRatio050Icon());
    	}
    	if(successPercentage > 55 && successPercentage <= 65){
            img=new Image(DashboardResources.RESOURCES.successRatio060Icon());
    	}
    	if(successPercentage > 65 && successPercentage <= 72){
            img=new Image(DashboardResources.RESOURCES.successRatio070Icon());
    	}
        if(successPercentage > 72 && successPercentage <= 78){
            img=new Image(DashboardResources.RESOURCES.successRatio075Icon());
        }
        if(successPercentage > 78 && successPercentage <= 85){
            img=new Image(DashboardResources.RESOURCES.successRatio080Icon());
        }
        if(successPercentage > 85 && successPercentage <= 95){
            img=new Image(DashboardResources.RESOURCES.successRatio090Icon());
        }
    	if(successPercentage > 95){
            img=new Image(DashboardResources.RESOURCES.successRatio100Icon());
    	}
    
    	return img;
    }

    private void processDashboardConfig(PropertySheet ps, Label dashboardTitle, Label projectColumnHeader, Label procedureColumnHeader){
        for (Entry<String, Property> entry : ps.getProperties().entrySet())
        {
            Property prop = entry.getValue();
            if (prop.isStringProperty())
            {
                //no-op properties at the root have no meaning
                Property currentProp = entry.getValue();
                String currentValue = currentProp.getValue();
                if(prop.getName().toLowerCase()=="history"){
                    m_numberOfjobs=Integer.parseInt(currentValue);
                }
                
                if(prop.getName()=="refresh"){
                	REFRESH_INTERVAL=Integer.parseInt(currentValue);
                	refreshCounter(REFRESH_INTERVAL);
                }

                if(prop.getName().toLowerCase()=="workflowcolumnheader"){
                    procedureColumnHeader.setText(currentValue);
                }

                if(prop.getName().toLowerCase()=="projectcolumnheader"){
                    projectColumnHeader.setText(currentValue);
                }

                if(prop.getName().toLowerCase()=="dashboardtitle"){
                    dashboardTitle.setText(currentValue);
                }

                if(prop.getName().toLowerCase()=="successthreshold"){
                    m_successThreshold=Integer.parseInt(currentValue);
                }

                if(prop.getName().toLowerCase()=="errorthreshold"){
                    m_errorThreshold=Integer.parseInt(currentValue);
                }

            }
        }
        
    }


    private Widget addIdField(String id)
    {
        Hidden idField = new Hidden();
        idField.setName("id");
        idField.setValue(id);
        return idField;
    }


    private String createWorkflowUrl(Workflow currentWorkflow){
        CommanderUrlBuilder urlBuilder = CommanderUrlBuilder.createLinkUrl("workflowDetails","projects",currentWorkflow.getProjectName(),"workflows",currentWorkflow.getName());
        return urlBuilder.buildString();
    }


    private void imgClickHandler(ClickEvent event, HorizontalPanel hpProjectRoot, Image[] imgArray){
        HTMLTable.Cell selectedCell = dashboardPanel.getCellForEvent(event);
        int selectedRow=selectedCell.getRowIndex();
        boolean bHide=false;
        Widget hId = dashboardPanel.getWidget(selectedRow,0);
        Widget hExpanded = dashboardPanel.getWidget(selectedRow,2);
            if(((Hidden)hExpanded).getValue()=="false"){
                imgArray[EXPAND_IMAGE].setVisible(false);
                imgArray[COLLAPSE_IMAGE].setVisible(true);
                ((Hidden)hExpanded).setValue("true");
                bHide=true;
            }
            else{
                imgArray[EXPAND_IMAGE].setVisible(true);
                imgArray[COLLAPSE_IMAGE].setVisible(false);
                ((Hidden)hExpanded).setValue("false");
                bHide=false;
            }


        hpProjectRoot.setVisible(false);
        hpProjectRoot.setVisible(true);

        //get the name of the product to hide data for
        Hidden idElement=(Hidden)dashboardPanel.getWidget(selectedRow,0);
        String selectedProduct=idElement.getValue();
        int numTests=0;
        double totalSuccess = 0;
        for(int i=1;i<dashboardPanel.getRowCount();i++){
            if(i!=selectedRow){

                hId = dashboardPanel.getWidget(i,0);
                Widget ratio = dashboardPanel.getWidget(i,1);
                if(((Hidden)hId).getValue().contains(selectedProduct)){
                    double currentSuccessRate=0;

                    numTests++;
                    try{
                        currentSuccessRate=Double.parseDouble(((Hidden)ratio).getValue());
                    }
                    catch(Exception e){
                        numTests--;
                    }
                    totalSuccess+=currentSuccessRate;
                    dashboardPanel.getRowFormatter().setVisible(i,bHide);
                }
            }
        }

        if(bHide==false){
            double overallSuccess=totalSuccess/numTests;
            HorizontalPanel hp = new HorizontalPanel();
            Image img = new Image();
            Label lbl = new Label();
            lbl.setText(String.valueOf((int)overallSuccess)+"%");
            img=generateSuccessImageUrl(totalSuccess/100,numTests);
            hp.add(img);
            hp.add(lbl);
            dashboardPanel.setWidget(selectedRow,5,hp);
        }
        else{
            dashboardPanel.clearCell(selectedRow,5);
        }
        dashboardPanel.getRowFormatter().setVisible(selectedRow,false);
        dashboardPanel.getRowFormatter().setVisible(selectedRow,true);


    }

    private void processPropertySheet2()
    {
        m_numberOfjobs=DEFAULT_NUMBER_OF_JOBS;
        final String propertyPath="/myUser/userSettings/myWorkflowsDashboard";
        GetPropertiesRequest req = getRequestFactory().createGetPropertiesRequest();
        req.setPath(propertyPath);

        req.setRecurse(true);

        req.setCallback(new PropertySheetCallback() {

            @Override public void handleError(CommanderError error){
                errorMessage.setText("Could not read data from "+propertyPath+".\n\nPlease configure the dashboard properties.");
                errorMessage.setVisible(true);
            }
            
            @Override public void handleResponse(PropertySheet response)
            {
                String procedureColumnName=DEFAULT_PROCEDURE_COLUMN_NAME;
                String projectColumnName=DEFAULT_PROJECT_COLUMN_NAME;
                Hidden rowId = new Hidden();
                rowId.setName("show");
                rowId.setValue("true");
                

                Label lblProjectHeader = new Label(DEFAULT_PROJECT_COLUMN_NAME);
                lblProjectHeader.addStyleName(style.projectHeader());

                Label lblProcedureHeader = new Label(DEFAULT_PROCEDURE_COLUMN_NAME);
                lblProcedureHeader.setStyleName(style.procedureHeader());
                
                Label lblLastRunHeader = new Label("Last run details");
                lblLastRunHeader.setStyleName(style.lastRunHeader());

                
                Label lblDescription = new Label("Description");
                Label lblStatus=new Label("Risk");

                processDashboardConfig(response, dashboardTitle, lblProjectHeader, lblProcedureHeader);

                dashboardPanel.setWidget(0, 0, rowId); //stores project name
                dashboardPanel.setWidget(0,1,null);  //used for storing success ratio in rows
                
                dashboardPanel.setWidget(0,2,null);  //used for expanded/collapsed state
                dashboardPanel.setWidget(0, 3, lblProjectHeader); //project column
                dashboardPanel.setWidget(0, 4, lblProcedureHeader); //procedure column
                //dashboardPanel.setWidget(0,5,new Label("Success rate")); //success ratio

                Label lblHistory = new Label("Workflows (Old -> New)");
                dashboardPanel.setWidget(0,5,lblHistory);//procedure history
                lblHistory.setStyleName(style.historyHeader());
                dashboardPanel.setWidget(0, 6, lblLastRunHeader);
                dashboardPanel.getRowFormatter().addStyleName(0, style.headerRow());
                dashboardPanel.setWidget(0,7,lblStatus);
                dashboardPanel.setWidget(0,8,new Label("Avg Cycle Time"));
                for (Entry<String, Property> entry : response.getProperties().entrySet())
                {
                    Property prop = entry.getValue();
                    if (prop.isStringProperty())
                    {
                        //no-op properties at the root have no meaning
                    }
                    else
                    {
                        //found property sheet at top level, it is a project definition
                        int row = dashboardPanel.getRowCount();

                        final String projectName = entry.getKey();

                        final HorizontalPanel hpProjectRoot = new HorizontalPanel();
                        final Image[] treeCtrlArray = new Image[2];

                        treeCtrlArray[EXPAND_IMAGE]=new Image(DashboardResources.RESOURCES.expandTreeIcon());
                        treeCtrlArray[COLLAPSE_IMAGE]=new Image(DashboardResources.RESOURCES.collapseTreeIcon());
                        treeCtrlArray[EXPAND_IMAGE].addClickHandler(new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent event) {
                                imgClickHandler(event, hpProjectRoot,treeCtrlArray);
                            }
                        });
                        treeCtrlArray[COLLAPSE_IMAGE].addClickHandler(new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent event) {
                                imgClickHandler(event, hpProjectRoot, treeCtrlArray);
                            }
                        });

                        Label lblRoot = new Label(projectName);
                        hpProjectRoot.add(treeCtrlArray[EXPAND_IMAGE]);
                        hpProjectRoot.add(treeCtrlArray[COLLAPSE_IMAGE]);
                        hpProjectRoot.add(lblRoot);
                        Hidden hExpanded = new Hidden();

                        //default to items expanded so hide expanded image
                        treeCtrlArray[EXPAND_IMAGE].setVisible(false);
                        hExpanded.setName("expanded");
                        hExpanded.setValue("true");

                        dashboardPanel.setWidget(row,0,addIdField(projectName));
                        dashboardPanel.setWidget(row,1,new Hidden());
                        dashboardPanel.setWidget(row,2,hExpanded);
                        dashboardPanel.setWidget(row, 3, hpProjectRoot);
                        dashboardPanel.getRowFormatter().addStyleName(row, style.important());
                        
                        //process project details
                        for (Entry<String, Property> projectSheetEntry : prop.getPropertySheet().getProperties().entrySet())
                        {
                            row = dashboardPanel.getRowCount();
                            dashboardPanel.setWidget(row,0,addIdField(projectName+"-"+String.valueOf(row)));
                            Property currentProp = projectSheetEntry.getValue();
                            final String procedureName = currentProp.getValue();
                            final Label lbl = new Label();
                            //final HorizontalPanel hp = new HorizontalPanel();
                            final FlexTable ftJobs = new FlexTable();
                            final HTML htmlLastRun = new HTML();
                            final HTML htmlStatusIndicator = new HTML();
                            final Label lblAvgCycleTime = new Label();
                            final Hidden hRatio = new Hidden();
                            dashboardPanel.setWidget(row, 1, hRatio);
                            dashboardPanel.setWidget(row,4,lbl);
                            //dashboardPanel.setWidget(row,5,hp);
                            dashboardPanel.setWidget(row,5,ftJobs);
                            dashboardPanel.setWidget(row,6,htmlLastRun);
                            dashboardPanel.setWidget(row,7,htmlStatusIndicator);
                            dashboardPanel.setWidget(row,8,lblAvgCycleTime);
                            if((row%2)==0){
                                dashboardPanel.getRowFormatter().addStyleName(row, style.evenRow());
                            }
                            else{
                              dashboardPanel.getRowFormatter().addStyleName(row, style.oddRow());

                            }
                            GetWorkflowRequest wfRequest = getRequestFactory().createGetWorkflowRequest();
                            wfRequest.setWorkflowName(procedureName);
                            wfRequest.setProjectName(projectName);
                            wfRequest.setCallback(new WorkflowCallback() {
                                @Override
                                public void handleResponse(Workflow response) {
                                    //To change body of implemented methods use File | Settings | File Templates.
                                    populateTableRow(projectName, procedureName, lbl, ftJobs, htmlLastRun,htmlStatusIndicator,lblAvgCycleTime,m_successThreshold,m_errorThreshold);
                                }

                                @Override
                                public void handleError(CommanderError error) {
                                    //Ignore and populate table row
                                    //lblProcedureDescription.setText(error.getMessage());
                                    //Window.alert("Could not get procedure data: "+errorMessage);
                                    populateTableRow(projectName, procedureName, lbl, ftJobs, htmlLastRun,htmlStatusIndicator,lblAvgCycleTime,m_successThreshold,m_errorThreshold);
                                }
                            });

                            doRequest(wfRequest);
                                    //dashboardPanel.
                                    //Window.alert("processing table row for "+procedureName);
                                    //populateTableRow(projectName, procedureName, lbl, hp, ftJobs, hRatio, htmlLastRun);
                        }
                    }
                }
            }
        });

        doRequest(req);
    }

    public void processPropertySheetOnRefresh()
    {
        final String propertyPath="/myUser/userSettings/myWorkflowsDashboard";
        GetPropertiesRequest req = getRequestFactory().createGetPropertiesRequest();
        req.setPath(propertyPath);

        req.setRecurse(true);

        req.setCallback(new PropertySheetCallback() {

            @Override public void handleError(CommanderError error){
                errorMessage.setText("Could not read data from "+propertyPath+".\n\nPlease configure the dashboard properties.");
                errorMessage.setVisible(true);
            }
            
            @Override public void handleResponse(PropertySheet response)
            {
                dashboardPanel.getRowFormatter().addStyleName(0, style.headerRow());
                int row = 1;
                for (Entry<String, Property> entry : response.getProperties().entrySet())
                {
                    Property prop = entry.getValue();
                    if (prop.isStringProperty())
                    {
                        //no-op properties at the root have no meaning
                    }
                    else
                    {
                        //found property sheet at top level, it is a project definition

                        final String projectName = entry.getKey();
                        row++;
                        //process project details
                        for (Entry<String, Property> projectSheetEntry : prop.getPropertySheet().getProperties().entrySet())
                        {
                            Property currentProp = projectSheetEntry.getValue();
                            final String procedureName = currentProp.getValue();
                            final Label lbl = new Label();



                            final FlexTable ftJobs = (FlexTable) dashboardPanel.getWidget(row,5);
                            final HTML htmlLastRun =  (HTML) dashboardPanel.getWidget(row,6);

                            final HTML htmlStatusIndicator = (HTML) dashboardPanel.getWidget(row,7);
                            final Label lblAvgCycleTime = new Label();
                            getLog().debug("Project Name is " + projectName);
                 			if (ftJobs.toString().contains("no workflows found."))
	               			   {
	               				ftJobs.clear();
	               				if (dashboardPanel.getHTML(row, 6).toString().contains("Started By"))
	               					{
	                  				dashboardPanel.clearCell(row, 6);
	                  				dashboardPanel.clearCell(row, 7);
	               					}
	               			   }
                            GetWorkflowRequest wfRequest = getRequestFactory().createGetWorkflowRequest();
                            wfRequest.setWorkflowName(procedureName);
                            wfRequest.setProjectName(projectName);
                            wfRequest.setCallback(new WorkflowCallback() {
                                @Override
                                public void handleResponse(Workflow response) {
                                    //To change body of implemented methods use File | Settings | File Templates.
                                    populateTableRow(projectName, procedureName, lbl, ftJobs, htmlLastRun,htmlStatusIndicator,lblAvgCycleTime,m_successThreshold,m_errorThreshold);
                                }

                                @Override
                                public void handleError(CommanderError error) {
                                    //Ignore and populate table row
                                    //lblProcedureDescription.setText(error.getMessage());
                                    //Window.alert("Could not get procedure data: "+errorMessage);
                                    populateTableRow(projectName, procedureName, lbl, ftJobs, htmlLastRun,htmlStatusIndicator,lblAvgCycleTime,m_successThreshold,m_errorThreshold);
                                }
                            });
                            getLog().debug("Done");

                            doRequest(wfRequest);
                            row++;
                                    //dashboardPanel.
                                    //Window.alert("processing table row for "+procedureName);
                                    //populateTableRow(projectName, procedureName, lbl, hp, ftJobs, hRatio, htmlLastRun);
                        }
                    }
                }
            }
        });

        doRequest(req);
    }


}
