package ecplugins.HomepagePlus.client.Dashboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.gwt.client.ComponentBase;
import com.electriccloud.commander.gwt.client.ComponentBaseFactory;
import com.electriccloud.commander.client.domain.Access;
import com.electriccloud.commander.client.domain.AccessPrivileges;
import com.electriccloud.commander.client.domain.FormalParameter;
import com.electriccloud.commander.client.domain.Job;
import com.electriccloud.commander.client.domain.JobStatus;
import com.electriccloud.commander.client.domain.ObjectType;
import com.electriccloud.commander.client.domain.Order;
import com.electriccloud.commander.client.domain.Property;
import com.electriccloud.commander.client.domain.PropertySheet;
import com.electriccloud.commander.client.domain.State;
import com.electriccloud.commander.client.domain.Transition;
import com.electriccloud.commander.client.domain.Trigger;
import com.electriccloud.commander.client.domain.Workflow;
import com.electriccloud.commander.client.requests.CheckAccessRequest;
import com.electriccloud.commander.client.requests.CommanderRequest;
import com.electriccloud.commander.client.requests.DeleteWorkflowRequest;
import com.electriccloud.commander.client.requests.FindObjectsFilter;
import com.electriccloud.commander.client.requests.FindObjectsRequest;
import com.electriccloud.commander.client.requests.GetActualParametersRequest;
import com.electriccloud.commander.client.requests.GetFormalParametersRequest;
import com.electriccloud.commander.client.requests.GetPropertiesRequest;
import com.electriccloud.commander.client.requests.GetPropertyRequest;
import com.electriccloud.commander.client.requests.GetStateRequest;
import com.electriccloud.commander.client.requests.GetTransitionsRequest;
import com.electriccloud.commander.client.requests.GetWorkflowRequest;
import com.electriccloud.commander.client.requests.RunWorkflowRequest;
import com.electriccloud.commander.client.requests.TransitionWorkflowRequest;
import com.electriccloud.commander.client.responses.AccessPrivilegesCallback;
import com.electriccloud.commander.client.responses.CommanderError;
import com.electriccloud.commander.client.responses.CommanderObject;
import com.electriccloud.commander.client.responses.CommanderObjectCallback;
import com.electriccloud.commander.client.responses.DefaultFindObjectsResponseCallback;
import com.electriccloud.commander.client.responses.DefaultStateCallback;
import com.electriccloud.commander.client.responses.DefaultTransitionListCallback;
import com.electriccloud.commander.client.responses.FindObjectsResponse;
import com.electriccloud.commander.client.responses.FindObjectsResponseCallback;
import com.electriccloud.commander.client.responses.FormalParameterListCallback;
import com.electriccloud.commander.client.responses.PropertyCallback;
import com.electriccloud.commander.client.responses.PropertySheetCallback;
import com.electriccloud.commander.client.responses.StateCallback;
import com.electriccloud.commander.client.responses.WorkflowCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import ecplugins.HomepagePlus.client.Dashboard.ui.ShowWidgetCallback;

/**
 * This WorkflowPanel will fill up a row in the given dashboard with data from a given workflow, 
 * and handle all the logic and actions associated with that.
 * 
 */
public class WorkflowPanel{

    private Anchor iTaskLabel;
    private String iTaskLabelPopupText;
    private VerticalPanel iReportsPanel;
    private Anchor iStateLabel;
    private Anchor iLastActionLabel;
    private VerticalPanel iTransitionPanel;

    private HomeDashboard iHomeDashboard;
    private int iWorkflowDashboardRow;
    private Workflow iWorkflow;
    private String iTask = null;
    private String iUserName = null;

    private State iActiveStateResponse = null;
    private FindObjectsResponse iLastStateResponse = null;
    private List<Transition> iTransitionListResponse = null;
    private PropertySheet iDeployedResourcesResponse = null;

    private boolean iUseRemoteAccess;
    private boolean iUseAppServerDeployment;
    
    private int iMouseX = 0;
    private int iMouseY = 0;

    public WorkflowPanel(HomeDashboard aHomeDashboard, ShowWidgetCallback aShowWidgetCallback, Workflow aWorkflow, 
                         String aTask, boolean aUseRemoteAccess, boolean aUseAppServerDeployment){
        super();

        iHomeDashboard = aHomeDashboard;
        FlexTable ft = iHomeDashboard.getFlexTable();
        iWorkflowDashboardRow = ft.getRowCount();
        iWorkflow = aWorkflow;
        iTask = aTask;
        iUseRemoteAccess = aUseRemoteAccess;
        iUseAppServerDeployment = aUseAppServerDeployment;
        
        // Column 1: the task label
        iTaskLabel = new Anchor();
        iTaskLabel.setHTML(""); // TODO: is this needed
        iHomeDashboard.getFlexTable().setWidget(iWorkflowDashboardRow, 0, iTaskLabel);

        // Column 2: the workflow/state label
        iStateLabel = new Anchor();
        iHomeDashboard.getFlexTable().setWidget(iWorkflowDashboardRow, 1, iStateLabel);
        
        // Column 3: the transitions panel
        iTransitionPanel = new VerticalPanel();
        iHomeDashboard.getFlexTable().setWidget(iWorkflowDashboardRow, 2, iTransitionPanel);

        // Column 4: the reports panel
        iReportsPanel = new VerticalPanel();
        iHomeDashboard.getFlexTable().setWidget(iWorkflowDashboardRow, 3, iReportsPanel);

        // Column 5: the last action label
        iLastActionLabel = new Anchor();
        iHomeDashboard.getFlexTable().setWidget(iWorkflowDashboardRow, 4, iLastActionLabel);
        
        updatePanel(aShowWidgetCallback);
    }
    
    public int getRowCount(){
        return iWorkflowDashboardRow;
    }

    public boolean useAppServerDeployment(){
        return iUseAppServerDeployment;
    }

    @SuppressWarnings("rawtypes")
    private void addToListIfNotNull(Collection<CommanderRequest> commanderRequests, CommanderRequest aRequest){
        if(aRequest != null){
            commanderRequests.add(aRequest);
        }
    }
    /*
     * Main function responsible to fill in all the column cells with appropriate data
     * Triggers multiple requests in a batch towards the Commander server 
     */
    private void updatePanel(final ShowWidgetCallback aShowWidgetCallback){

        // mcmahon TODO: consider using "label" instead of anchor
        iTaskLabel.setHref("/commander/link/workflowDetails/projects/"+iWorkflow.getProjectName()+"/workflows/"+iWorkflow.getName());
        String[] wfNameParts = iWorkflow.getName().split("_",3);
        String wfNameToShow;
        if(wfNameParts.length==3 && wfNameParts[0].equals("workflow"))
            wfNameToShow = "wf-"+wfNameParts[1];
        else
            wfNameToShow = iWorkflow.getName();
        iTaskLabel.setHTML(wfNameToShow);

        iStateLabel.setHref("/commander/link/workflowDetails/projects/"+iWorkflow.getProjectName()+"/workflows/"+iWorkflow.getName());

        if(iWorkflow.isCompleted()){
            iTransitionPanel.clear();
            iStateLabel.setHTML("<img src=\"/commander/images/workflow_complete_16px.png\" align=\"absmiddle\"/>&nbsp;"+iWorkflow.getActiveState());
            //addRunAgainWorkflowAnchor();
            //addDeleteWorkflowAnchor();
            return;
        }

        // two pass execution:
        // 1st pass - query inital requests to handle the active state, last state and task, workflow reports, and transitions (just in case they are needed)
        // 2nd pass - based on responses from first pass, query subjob/subworkflow, last action and task info        
        getComponentBase().doRequest(new ChainedCallback() {

            @Override public void onComplete()
            {
                @SuppressWarnings("rawtypes")
                Collection<CommanderRequest> commanderRequests = new ArrayList<CommanderRequest>();
                if(iActiveStateResponse != null){
                    addToListIfNotNull(commanderRequests, queryActiveStateSubActionRequest());
                }
                if(iLastStateResponse != null){
                    addToListIfNotNull(commanderRequests, queryPreviousSubJobRequest());
                }
                if(iTask != null){
                    addToListIfNotNull(commanderRequests, queryTaskInfoRequest());                    
                }
                addToListIfNotNull(commanderRequests, queryWorkflowReportsRequest());
                
                //generateQueryTransitionExecuteAclRequest(commanderRequests);
                
                getComponentBase().doRequest(new ChainedCallback() {

                    @Override public void onComplete()
                    {
                        if(aShowWidgetCallback!=null){
                            aShowWidgetCallback.show();
                        }
                    }
                }, commanderRequests.toArray(new CommanderRequest[0]));
            }
        },     queryActiveStateRequest(), queryPreviousStateRequest(),  
            queryWorkflowTaskRequest(true), queryUserNameRequest(), 
            queryDeployedResourcesRequest(true), queryTransitionsRequest());
    }

    /*
     * Get the active state of the workflow, and once received get the job/workflow-details 
     * associated with that state, if any. If the subjob/workflow is complete or non-existent, 
     * query for the manual transitions out from this state
     */
    private GetStateRequest queryActiveStateRequest(){
        // get the activeState's subjob and check if it is running - if not, enable manual transitions for the user
        GetStateRequest getStateRequest = getComponentBase().getRequestFactory().createGetStateRequest();
        getStateRequest.setProjectName(iWorkflow.getProjectName());
        getStateRequest.setWorkflowName(iWorkflow.getName());
        getStateRequest.setStateName(iWorkflow.getActiveState());
        getStateRequest.setCallback(new DefaultStateCallback(getComponentBase()) {

            @Override public void handleResponse(State response)
            {
                iActiveStateResponse = response;
            }
        });
        return getStateRequest;
    }

    private FindObjectsRequest queryActiveStateSubActionRequest(){

        FindObjectsRequest findObjectsRequest = null;
        // OK, workflow is not complete, but we don't know if it's waiting for subjob, subworkflow or manual action
        String subJob = iActiveStateResponse.getSubjob();
        String subWorkflow = iActiveStateResponse.getWorkflowName();
        if((subJob==null || subJob.equals("")) && (subWorkflow==null || subWorkflow.equals(""))){
            // If the workflow is active/non-complete but there are no subJob or subWorkflow, 
            // there has got to be manual transitions
            // Get them and enable for the user
            iStateLabel.setHTML("<img src=\"/commander/images/workflow_manual_16px.png\" align=\"absmiddle\"/>&nbsp;"+iWorkflow.getActiveState());
            enableTransitions();
        }
        else if(!subJob.equals("")){
            // There is a subjob attached to the state, query that to get the jobDetails
            findObjectsRequest = getComponentBase().getRequestFactory().createFindObjectsRequest(ObjectType.job);
            findObjectsRequest.addFilter(new FindObjectsFilter.EqualsFilter("jobName", subJob));
            findObjectsRequest.setCallback(new FindObjectsResponseCallback() {

                @Override public void handleError(CommanderError error)
                {
                    getComponentBase().handleError(error);
                }

                @Override public void handleResponse(FindObjectsResponse response)
                {
                    iTransitionPanel.clear();
                    // jobs should really only contain either 1 or 0 jobs
                    List<Job> jobs = response.getJobs();
                    boolean jobCompleted = true;
                    for(Job job : jobs){
                        if(!job.getStatus().equals(JobStatus.completed)){
                            jobCompleted = false;
                            iStateLabel.setHTML("<img src=\"/commander/images/workflow_16px.png\" align=\"absmiddle\"/>&nbsp;"+iWorkflow.getActiveState());
                            Anchor subJobAnchor = new Anchor();
                            subJobAnchor.setHTML("<img src=\"/commander/lib/images/icn16px_"+job.getStatus()+((job.getOutcome()!=null)?("_"+job.getOutcome()):(""))+".gif\" align=\"absmiddle\"/>&nbsp;"+job.getName());
                            subJobAnchor.setHref("/commander/link/jobDetails/jobs/"+job.getName());
                            iTransitionPanel.add(subJobAnchor);
                            scheduleRequeryWorkflowData(1000);
                        }
                    }
                    if(jobCompleted){
                        // Looks like the subJob is completed, or has been deleted
                        iStateLabel.setHTML("<img src=\"/commander/images/workflow_manual_16px.png\" align=\"absmiddle\"/>&nbsp;"+iWorkflow.getActiveState());
                        enableTransitions();
                    }

                }
            });
        }
        else if(!subWorkflow.equals("")){
            // There is a subworkflow attached to the state, query that to get the workflowDetails
            findObjectsRequest = getComponentBase().getRequestFactory().createFindObjectsRequest(ObjectType.workflow);
            findObjectsRequest.addFilter(new FindObjectsFilter.EqualsFilter("workflowName", subJob));
            findObjectsRequest.setCallback(new FindObjectsResponseCallback() {

                @Override public void handleError(CommanderError error)
                {
                    getComponentBase().handleError(error);
                }

                @Override public void handleResponse(FindObjectsResponse response)
                {
                    iTransitionPanel.clear();
                    // workflows should really only contain either 1 or 0 workflows
                    List<Workflow> workflows = response.getWorkflows();
                    boolean workflowCompleted = true;
                    for(Workflow workflow : workflows){
                        if(!workflow.isCompleted()){
                            workflowCompleted = false;
                            iStateLabel.setHTML("<img src=\"/commander/images/workflow_16px.png\" align=\"absmiddle\"/>&nbsp;"+iWorkflow.getActiveState());
                            Anchor subWorkflowAnchor = new Anchor();
                            subWorkflowAnchor.setHTML("<img src=\"/commander/images/workflow_16px.png\" align=\"absmiddle\"/>&nbsp;"+workflow.getName());
                            subWorkflowAnchor.setHref("/commander/link/workflowDetails/workflows/"+workflow.getName());
                            iTransitionPanel.add(subWorkflowAnchor);
                            scheduleRequeryWorkflowData(1000);
                        }
                    }
                    if(workflowCompleted){
                        // Looks like the subWorkflow is completed, or has been deleted
                        iStateLabel.setHTML("<img src=\"/commander/images/workflow_manual_16px.png\" align=\"absmiddle\"/>&nbsp;"+iWorkflow.getActiveState());
                        enableTransitions();
                    }

                }
            });
        }
        return findObjectsRequest;
    }
    
    /*
     * Get all the reports currently set on the workflow
     */
    private GetPropertiesRequest queryWorkflowReportsRequest(){
        GetPropertiesRequest getPropertiesRequest = getComponentBase().getRequestFactory().createGetPropertiesRequest();
        getPropertiesRequest.setPath("/projects/"+iWorkflow.getProjectName()+"/workflows/"+iWorkflow.getName()+"/report-urls");
        getPropertiesRequest.setCallback(new PropertySheetCallback() {            
            @Override public void handleResponse(PropertySheet sheet) {
                iReportsPanel.clear();
                
                // add workflow visualization anchor
                Anchor workflowAnchor = new Anchor();
                workflowAnchor.setHTML("<img src=\"/commander/lib/images/icn16px_refreshStop.gif\" align=\"absmiddle\"/>&nbsp;Workflow");
                workflowAnchor.setHref("/commander/link/workflowDetails/projects/"+iWorkflow.getProjectName()+"/workflows/"+iWorkflow.getName()+"#graph");
                iReportsPanel.add(workflowAnchor);
                    
                for (Property property : sheet.getProperties().values()) {
                    // TODO: bad way of doing this, but let it be for now
                    // we are only accepting reports where the name of the link matches the following pattern: "Latest XXX report, [success|warning|error]"
                    //if(property.getName().startsWith(iWorkflow.getObjectId().replaceFirst("workflow-", "")+"_")){
                    if(property.getName().startsWith("Latest ")){
                        Anchor reportAnchor = new Anchor();
                        boolean match = false;
                        for(String suffix : Arrays.asList("success", "warning", "error")){
                            if(property.getName().endsWith(" report, "+suffix)){
                                reportAnchor.setHTML("<img src=\"/commander/lib/images/icn16px_"+suffix+".gif\" align=\"absmiddle\"/>&nbsp;"+property.getName().replaceAll(" report, "+suffix, ""));
                                match = true;
                                break;
                            }
                        }
                        if(!match){
                            reportAnchor.setText(property.getName());                            
                        }
                        reportAnchor.setHref(property.getValue());
                        iReportsPanel.add(reportAnchor);
                    }
                }
            }

            @Override public void handleError(CommanderError error)
            {
                iReportsPanel.clear();
                
                // add workflow visualization anchor
                Anchor workflowAnchor = new Anchor();
                workflowAnchor.setHTML("<img src=\"/commander/lib/images/icn16px_refreshStop.gif\" align=\"absmiddle\"/>&nbsp;Workflow");
                workflowAnchor.setHref("/commander/link/workflowDetails/projects/"+iWorkflow.getProjectName()+"/workflows/"+iWorkflow.getName()+"#graph");
                iReportsPanel.add(workflowAnchor);
            }
        });
        return getPropertiesRequest;
    }

    /*
     * Get the workflow's previous state and action
     */
    private FindObjectsRequest queryPreviousStateRequest(){
        FindObjectsRequest getStatesRequest = getComponentBase().getRequestFactory().createFindObjectsRequest(ObjectType.state);
        getStatesRequest.addFilter(new FindObjectsFilter.EqualsFilter("projectName", iWorkflow.getProjectName()));
        getStatesRequest.addFilter(new FindObjectsFilter.EqualsFilter("workflowName", iWorkflow.getName()));
        //getStatesRequest.addFilter(new FindObjectsFilter.OrFilter(new FindObjectsFilter.IsNotNullFilter("subjob"), new FindObjectsFilter.IsNotNullFilter("subworkflow")));
        getStatesRequest.addFilter(new FindObjectsFilter.IsNotNullFilter("subjob"));
        getStatesRequest.setCallback(new DefaultFindObjectsResponseCallback(getComponentBase()) {

            @Override public void handleResponse(FindObjectsResponse response)
            {
                iLastStateResponse = response;
            }
        });
        return getStatesRequest;
    }

    private FindObjectsRequest queryPreviousSubJobRequest(){
        if(iLastStateResponse==null || iLastStateResponse.getStates().isEmpty()){
            return null;
        }
        // create a findObject-request querying all the subjobs of the workflow
        FindObjectsRequest getSubJobsRequest = getComponentBase().getRequestFactory().createFindObjectsRequest(ObjectType.job);
        getSubJobsRequest.addFilter(new FindObjectsFilter.EqualsFilter("projectName", iWorkflow.getProjectName()));
        getSubJobsRequest.addFilter(new FindObjectsFilter.EqualsFilter("status", "completed"));
        FindObjectsFilter.OrFilter orFilter = new FindObjectsFilter.OrFilter();
        for(State state : iLastStateResponse.getStates()){
            // we do only support subjobs for now
            if(state.getSubjob().isEmpty()){
                continue;
            }
            orFilter.addFilter(new FindObjectsFilter.EqualsFilter("jobName", state.getSubjob()));
        }
        getSubJobsRequest.addFilter(orFilter);
        getSubJobsRequest.addSort("start", Order.descending);
        getSubJobsRequest.setMaxIds(1);
        getSubJobsRequest.setCallback(new DefaultFindObjectsResponseCallback(getComponentBase()) {

            @Override public void handleResponse(FindObjectsResponse response)
            {
                for(Job job : response.getJobs()){
                    String[] jbNameParts = job.getName().split("_",3);
                    String jbNameToShow;
                    if(jbNameParts.length==3 && jbNameParts[0].equals("job"))
                        jbNameToShow = "job-"+jbNameParts[1];
                    else
                        jbNameToShow = job.getName();
                    iLastActionLabel.setHTML("<img src=\"/commander/lib/images/icn16px_"+job.getOutcome()+".gif\" align=\"absmiddle\"/>&nbsp;"+jbNameToShow);
                    iLastActionLabel.setHref("/commander/link/jobDetails/jobs/"+job.getName());
                }
            }
        });
        return getSubJobsRequest;
    }
    
    private GetPropertyRequest queryUserNameRequest(){
        GetPropertyRequest getUserNameRequest = getComponentBase().getRequestFactory().createGetPropertyRequest();
        getUserNameRequest.setPropertyName("/myWorkflow/launchedBy");
        getUserNameRequest.setProjectName(iWorkflow.getProjectName());
        getUserNameRequest.setWorkflowName(iWorkflow.getName());
        getUserNameRequest.setCallback(new PropertyCallback() {
            @Override public void handleResponse(Property response)
            {
                iUserName = response.getValue();
            }

            @Override public void handleError(CommanderError error)
            {
                // the "TASK ID" property on the workflow has not been set yet
                Timer t = new Timer() {
                    @Override public void run() {
                        getComponentBase().doRequest(queryUserNameRequest());
                    }
                };
                // Schedule the timer to run once in 500 ms.
                t.schedule(500);
            }
        });
        return getUserNameRequest;
    }
    
    private GetPropertyRequest queryWorkflowTaskRequest(final Boolean aAsynchronous){
        GetPropertyRequest getTaskIdRequest = getComponentBase().getRequestFactory().createGetPropertyRequest();
        getTaskIdRequest.setPropertyName("/myWorkflow/wfConfiguration");
        getTaskIdRequest.setProjectName(iWorkflow.getProjectName());
        getTaskIdRequest.setWorkflowName(iWorkflow.getName());
        getTaskIdRequest.setCallback(new PropertyCallback() {
            @Override public void handleResponse(Property response)
            {
                iTask = response.getValue();
                if(!aAsynchronous){
                    getComponentBase().doRequest(queryTaskInfoRequest());
                }
            }

            @Override public void handleError(CommanderError error)
            {
                // the "TASK ID" property on the workflow has not been set yet
                Timer t = new Timer() {
                    @Override public void run() {
                        getComponentBase().doRequest(queryWorkflowTaskRequest(false));
                    }
                };
                // Schedule the timer to run once in 500 ms.
                t.schedule(500);
            }
        });
        //Window.alert("mcmahon");
        return getTaskIdRequest;
    }

    private GetPropertiesRequest queryTaskInfoRequest(){
        GetPropertiesRequest getPropertiesRequest = getComponentBase().getRequestFactory().createGetPropertiesRequest();
        getPropertiesRequest.setPath("/projects/"+iWorkflow.getProjectName()+"/wfConfigurations/"+iTask);
        getPropertiesRequest.setRecurse(true);
        getPropertiesRequest.setCallback(new PropertySheetCallback(){

            @Override public void handleError(CommanderError error)
            {
                // it's ok if the property doesn't have a value...
                iTaskLabel.setText(iTask);                        
            }

            @Override public void handleResponse(PropertySheet response)
            {
                Map<String, Property> ps = response.getProperties();
                iTaskLabel.setText(iTask);
                //iTaskLabel.setText(iTask+" - "+ps.get("status").getValue());
                //iTaskLabelPopupText = iTask+" - "+ps.get("status").getValue()+": "+ps.get("title").getValue()+", Reporter: "+ps.get("reporter").getValue();
            }
            
        });
        return getPropertiesRequest;

    }

    public void requeryWorkflowData(){
        GetWorkflowRequest getWorkflowRequest = getComponentBase().getRequestFactory().createGetWorkflowRequest();
        getWorkflowRequest.setProjectName(iWorkflow.getProjectName());
        getWorkflowRequest.setWorkflowName(iWorkflow.getName());
        getWorkflowRequest.setCallback(new WorkflowCallback(){

            @Override public void handleResponse(Workflow response)
            {
                iWorkflow = response;
                updatePanel(null);
            }

            @Override public void handleError(CommanderError error)
            {
                getComponentBase().handleError(error);
            }

        });
        getComponentBase().doRequest(getWorkflowRequest);
    }

    private void scheduleRequeryWorkflowData(int aMilliSecondDelay){
        if(aMilliSecondDelay>0){
            Timer t = new Timer() {
                @Override public void run() {
                    requeryWorkflowData();
                }
            };

            // Schedule the timer to run once in aMilliSecondDelay ms.
            t.schedule(aMilliSecondDelay);
        }
        else{
            requeryWorkflowData();            
        }
    }

    public ComponentBase getComponentBase(){
        return iHomeDashboard;
    }
    public Workflow getWorkflow(){
        return iWorkflow;
    }

    private GetTransitionsRequest queryTransitionsRequest(){
        GetTransitionsRequest getTransitionsRequest = getComponentBase().getRequestFactory().createGetTransitionsRequest();
        getTransitionsRequest.setProjectName(iWorkflow.getProjectName());
        getTransitionsRequest.setWorkflowName(iWorkflow.getName());
        getTransitionsRequest.setStateName(iWorkflow.getActiveState());
        getTransitionsRequest.setCallback(new DefaultTransitionListCallback(getComponentBase()) {

            @Override public void handleResponse(List<Transition> response)
            {
                iTransitionListResponse = response;
                @SuppressWarnings("rawtypes")
                Collection<CommanderRequest> commanderRequests = new ArrayList<CommanderRequest>();
                generateQueryTransitionExecuteAclRequest(commanderRequests);                
                getComponentBase().doRequest(new ChainedCallback() {

                    @Override public void onComplete()
                    {
                        // no-op
                    }
                }, commanderRequests.toArray(new CommanderRequest[0]));
                
            }
        });
        return getTransitionsRequest;
    }

    @SuppressWarnings("rawtypes")
    private void generateQueryTransitionExecuteAclRequest(Collection<CommanderRequest> aCommanderRequests){
        // for each manual transition, check that we have executePrivilege
        for(final Transition transition : iTransitionListResponse){
            if(transition.getTrigger()==null || !transition.getTrigger().equals(Trigger.manual)){
                continue;
            }
            
            CheckAccessRequest checkAccessRequest = getComponentBase().getRequestFactory().createCheckAccessRequest();
            checkAccessRequest.setProjectName(iWorkflow.getProjectName());
            checkAccessRequest.setWorkflowName(iWorkflow.getName());
            checkAccessRequest.setStateName(iWorkflow.getActiveState());
            checkAccessRequest.setTransitionName(transition.getName());
            checkAccessRequest.setCallback(new AccessPrivilegesCallback() {
                
                @Override
                public void handleError(CommanderError error) {
                    getComponentBase().handleError(error);
                }
                
                @Override
                public void handleResponse(AccessPrivileges response) {
                    // remove transition from iTransitionListResponse if it doesn't have executePrivilege
                    if(!response.getExecutePrivilege().equals(Access.allow)){
                        iTransitionListResponse.remove(transition);
                    }
                }
            });
            aCommanderRequests.add(checkAccessRequest);
        }
    }
    
    private void enableTransitions(){
        final WorkflowPanel workflowPanel = this;
        iTransitionPanel.clear();
        if(iTransitionListResponse.isEmpty()){
            // There were likely no available transitions with execute privilege
            return;
        }
        boolean nonManualTrigger = false;
        boolean nonReadableTransitions = false;
        for(final Transition transition : iTransitionListResponse){
            final String transitionName = transition.getName();
            if(transition.getTrigger() == null){
                // TODO: investigate this check - necessary?
                nonReadableTransitions = true;
                continue;
            }
            else if(!transition.getTrigger().equals(Trigger.manual)){
                nonManualTrigger = true;
                continue;
            }
            final Anchor transitionAnchor = new Anchor();
            iTransitionPanel.add(transitionAnchor);
            String myHref = "/commander/link/transitionWorkflow/projects/" + iWorkflow.getProjectName() +
                            "/workflows/" + iWorkflow.getName() + "/states/" + iWorkflow.getActiveState() +
                            "/transitions/" + transitionName;
            //Window.alert("mcmahon: " + myHref);
            transitionAnchor.setHref(myHref);
            transitionAnchor.setHTML(transitionName);
        }
        if(nonManualTrigger){
            scheduleRequeryWorkflowData(500);
        }
        else if(nonReadableTransitions){
            // no-op
        }
        // inject delete and run-again links
        else if(iWorkflow.getActiveState().equals("DONE") ||
                iWorkflow.getActiveState().equals("REJECTED")){
            //addRunAgainWorkflowAnchor();
            //addDeleteWorkflowAnchor();
        }
        // inject links to each deployed resource
        else if(iWorkflow.getActiveState().endsWith("DEPLOYED")){
            addDeploymentAnchors();                        
        }
    }

    private GetPropertiesRequest queryDeployedResourcesRequest(final Boolean aAsynchronous){
        // "commander/link/editResource/resources/$[/myJob/jobId]"."_$platform"
        GetPropertiesRequest getPropertiesRequest = getComponentBase().getRequestFactory().createGetPropertiesRequest();
        getPropertiesRequest.setPath("/projects/"+iWorkflow.getProjectName()+"/workflows/"+iWorkflow.getName()+"/DeployedResources");
        getPropertiesRequest.setRecurse(true);
        getPropertiesRequest.setCallback(new PropertySheetCallback() {
            
            @Override public void handleResponse(PropertySheet sheet) {
                iDeployedResourcesResponse = sheet;
                if(!aAsynchronous){
                    addDeploymentAnchors();
                }
            }

            @Override
            public void handleError(CommanderError error) {
                // no-op - means there are no deployed resources for the moment
            }
        });
        return getPropertiesRequest;
    }
    private void addDeploymentAnchors(){
        if(!iUseRemoteAccess){
            return;
        }
        if(iDeployedResourcesResponse == null){
            queryDeployedResourcesRequest(false);
        }
        else{
            if(useAppServerDeployment()){
                // generate links to all the deployed applications
                // below is a bit of a hack but will do the job
                for(Property p : iDeployedResourcesResponse.getProperties().values()){
                    // each deployment is organized in a propertysheet, recurse into
                    PropertySheet ps = p.getPropertySheet();
                    Property[] properties = ps.getProperties().values().toArray(new Property[0]);
                    if(properties.length != 3){
                        // if there aren't exactly three elements in the array, something went wrong with that deployment
                        continue;
                    }
                    HorizontalPanel hp = new HorizontalPanel();
                    // we don't know which one of elm 1, 2, or 3 that is the app link - try to figure it out dynamically
                    String p1 = "";
                    String p2 = "";
                    String p3 = "";
                    String v1 = "";
                    String v2 = "";
                    String v3 = "";
                    for(int j=0; j<3; j++){
                        String s = properties[j].getName();
                        if(s.endsWith(" - App")){
                            // we've got a match, assign p2 and p3 with the next 2 properties mod 3
                            p1 = s;
                            v1 = properties[j].getValue();
                            p2 = properties[(j+1)%3].getName();
                            v2 = properties[(j+1)%3].getName();
                            p3 = properties[(j+2)%3].getName();
                            v3 = properties[(j+2)%3].getName();
                            break;
                        }
                    }
                    
                    // remove initial "Galaxy " from p1 (deployment name)
                    p1 = p1.replaceFirst("Galaxy ", "");
                    
                    hp.add(new Label(p1.substring(0, p1.lastIndexOf(" - "))+":"));
                    hp.add(new HTML("&nbsp;&nbsp;"));
                    hp.add(new Anchor("App", v1));
                    for(int j=0; j<3; j++){
                        p2 = p2.substring(p2.indexOf("_")+1);
                        p3 = p3.substring(p3.indexOf("_")+1);
                    }
                    hp.add(new HTML("&nbsp;&nbsp;&nbsp;"));
                    hp.add(new Anchor(p2, "/commander/pages/RemoteAccess-1.0.1/index?resource="+v2+"&accessType=ssh"));
                    hp.add(new HTML("&nbsp;&nbsp;&nbsp;"));
                    hp.add(new Anchor(p3, "/commander/pages/RemoteAccess-1.0.1/index?resource="+v3+"&accessType=ssh"));
                    iTransitionPanel.add(hp);
                }
            }
            else{
                // generate links to all the deployed resources
                for (Property property : iDeployedResourcesResponse.getProperties().values()) {
                    if(!property.getName().endsWith(" - App")){
                        iTransitionPanel.add(new Anchor("Access "+property.getName().substring(property.getName().indexOf("_")+1), "/commander/pages/RemoteAccess-1.0.1/index?resource="+property.getName()+"&accessType=ssh"));
                    }                        
                }            
            }            
        }
    }
    private void addRunAgainWorkflowAnchor(){
        // TODO: improve this, as there are four serial requests here
        Anchor runAgainAnchor = new Anchor();
        runAgainAnchor.setHTML("<img src=\"/commander/lib/images/icn16px_run.gif\" align=\"absmiddle\"/>&nbsp;Run again");
        runAgainAnchor.addClickHandler(new ClickHandler() {

            @Override public void onClick(ClickEvent event)
            {
                // Get the formal parameters from the starting state (ENTRY)
                // Get the propertysheetid of the starting state from the workflow - from there, we should be able to backfill the data 
                // into a new wf 
                GetFormalParametersRequest getFormalParametersRequest = getComponentBase().getRequestFactory().createGetFormalParametersRequest();
                getFormalParametersRequest.setProjectName(iWorkflow.getProjectName());
                getFormalParametersRequest.setWorkflowDefinitionName(iWorkflow.getWorkflowDefinitionName());
                getFormalParametersRequest.setStateDefinitionName(iWorkflow.getStartingState());
                getFormalParametersRequest.setCallback(new FormalParameterListCallback() {

                    @Override public void handleError(CommanderError error)
                    {
                        getComponentBase().handleError(error);
                    }

                    @Override public void handleResponse(final List<FormalParameter> aFormalParameterList)
                    {
                        GetStateRequest getStateRequest = getComponentBase().getRequestFactory().createGetStateRequest();
                        getStateRequest.setProjectName(iWorkflow.getProjectName());
                        getStateRequest.setWorkflowName(iWorkflow.getName());
                        getStateRequest.setStateName(iWorkflow.getStartingState());
                        getStateRequest.setCallback(new StateCallback() {

                            @Override public void handleError(CommanderError error)
                            {
                                getComponentBase().handleError(error);                                
                            }

                            @Override public void handleResponse(State response)
                            {
                                GetPropertiesRequest getPropertiesRequest = getComponentBase().getRequestFactory().createGetPropertiesRequest();
                                getPropertiesRequest.setProjectName(response.getProjectName());
                                getPropertiesRequest.setPropertySheetId(response.getPropertySheetId());
                                getPropertiesRequest.setCallback(new PropertySheetCallback() {

                                    @Override public void handleError(CommanderError error)
                                    {
                                        getComponentBase().handleError(error);
                                    }

                                    @Override public void handleResponse(PropertySheet response)
                                    {
                                        RunWorkflowRequest runWorkflowRequest = getComponentBase().getRequestFactory().createRunWorkflowRequest();
                                        runWorkflowRequest.setProjectName(iWorkflow.getProjectName());
                                        runWorkflowRequest.setWorkflowDefinitionName(iWorkflow.getWorkflowDefinitionName());
                                        runWorkflowRequest.setStartingState(iWorkflow.getStartingState());
                                        // OK, so for each of the formal-parameters that we have above, check if the state has such a 
                                        // parameter and pass that along in the runWorkflowRequest
                                        Map<String, Property> properties = response.getProperties();
                                        for(FormalParameter formalParameter : aFormalParameterList){
                                            if(properties.containsKey(formalParameter.getName())){
                                                runWorkflowRequest.addActualParameter(formalParameter.getName(), properties.get(formalParameter.getName()).getValue());
                                            }
                                        }
                                        runWorkflowRequest.setCallback(new WorkflowCallback() {

                                            @Override public void handleError(CommanderError error)
                                            {
                                                getComponentBase().handleError(error);
                                            }

                                            @Override public void handleResponse(Workflow response)
                                            {
                                                Timer t = new Timer() {
                                                    @Override public void run() {
                                                        iHomeDashboard.generateWorkflowListing();
                                                    }
                                                };
                                                // Schedule the timer to update the overall workflow-listing in 1000 ms.
                                                t.schedule(500);
                                            }
                                        });
                                        getComponentBase().doRequest(runWorkflowRequest);
                                    }
                                });
                                getComponentBase().doRequest(getPropertiesRequest);
                            }
                        });
                        getComponentBase().doRequest(getStateRequest);
                    }
                });
                getComponentBase().doRequest(getFormalParametersRequest);
            }
        });
        iTransitionPanel.add(runAgainAnchor);
    }
    private void addDeleteWorkflowAnchor(){

        Anchor deleteAnchor = new Anchor();
        deleteAnchor.setHTML("<img src=\"/commander/lib/images/icn16px_delete.gif\" align=\"absmiddle\"/>&nbsp;Delete");
        deleteAnchor.addClickHandler(new ClickHandler() {

            @Override public void onClick(ClickEvent event)
            {
                if(Window.confirm("You are about to delete workflow "+iWorkflow.getName()+", please confirm.")){
                    DeleteWorkflowRequest deleteWorkflowRequest = getComponentBase().getRequestFactory().createDeleteWorkflowRequest();
                    deleteWorkflowRequest.setProjectName(iWorkflow.getProjectName());
                    deleteWorkflowRequest.setWorkflowName(iWorkflow.getName());
                    deleteWorkflowRequest.setDeleteProcesses(true);
                    deleteWorkflowRequest.setCallback(new CommanderObjectCallback() {

                        @Override public void handleError(CommanderError error)
                        {
                            getComponentBase().handleError(error);
                        }

                        @Override public void handleResponse(CommanderObject response)
                        {
                        }
                    });
                    getComponentBase().doRequest(deleteWorkflowRequest);
                    //taskLabel.setEnabled(false);
                    //stateLabel.setEnabled(false);
                    //workflowLabel.setEnabled(false);
                    //transitionPanel.clear();
                    iHomeDashboard.getFlexTable().getRowFormatter().setVisible(iWorkflowDashboardRow, false);
                    //panel.setVisible(false);                    
                }
            }
        });
        iTransitionPanel.add(deleteAnchor);
    }
}
