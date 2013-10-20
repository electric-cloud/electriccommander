
// WorkflowActivityComponent.java --
//
// WorkflowActivityComponent.java is part of ElectricCommander.
//
// Copyright (c) 2005-2013 Electric Cloud, Inc.
// All rights reserved.

package ecplugins.EC_WorkflowActivity.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ca.nanometrics.gflot.client.DataPoint;
import ca.nanometrics.gflot.client.PlotModel;
import ca.nanometrics.gflot.client.SeriesHandler;
import ca.nanometrics.gflot.client.SimplePlot;
import ca.nanometrics.gflot.client.options.PlotOptions;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.client.domain.ErrorCode;
import com.electriccloud.commander.client.domain.Job;
import com.electriccloud.commander.client.domain.JobOutcome;
import com.electriccloud.commander.client.domain.ObjectType;
import com.electriccloud.commander.client.domain.Order;
import com.electriccloud.commander.client.domain.Property;
import com.electriccloud.commander.client.domain.PropertySheet;
import com.electriccloud.commander.client.domain.State;
import com.electriccloud.commander.client.domain.StateDefinition;
import com.electriccloud.commander.client.domain.Transition;
import com.electriccloud.commander.client.domain.Workflow;
import com.electriccloud.commander.client.requests.CommanderRequest;
import com.electriccloud.commander.client.requests.FindObjectsFilter;
import com.electriccloud.commander.client.requests.FindObjectsRequest;
import com.electriccloud.commander.client.requests.GetJobInfoRequest;
import com.electriccloud.commander.client.requests.GetPropertiesRequest;
import com.electriccloud.commander.client.responses.CommanderError;
import com.electriccloud.commander.client.responses.FindObjectsResponse;
import com.electriccloud.commander.client.responses.FindObjectsResponseCallback;
import com.electriccloud.commander.client.responses.JobCallback;
import com.electriccloud.commander.client.responses.PropertySheetCallback;
import com.electriccloud.commander.gwt.client.ComponentBase;
import com.electriccloud.commander.gwt.client.ui.ErrorPanel;
import com.electriccloud.commander.gwt.client.ui.TitledPanel;
import com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder;

import static ecplugins.EC_WorkflowActivity.client.WorkflowActivityResources.RESOURCES;

public class WorkflowActivityComponent extends ComponentBase
{

    // The name of the workflow definition whose corresponding workflows will
    // be displayed.  Expected to exist in GET.
    private String m_workflowDefinitionName
            = Window.Location.getParameter("workflowDefinitionName");

    // The name of the project to which the workflow definition belongs.
    // Expected to exist in GET.
    private String m_projectName
            = Window.Location.getParameter("projectName");

    // The ordered list of states to show in the activity table.  Determined
    // based on which state definitions have the SHOW property set.
    private List<String> m_statesToShow;

    // The overall panel including the header and the table of workflows.
    private TitledPanel m_activityPanel;

    // The table of recent workflows, i.e. instances of the given workflow
    // definition.
    private FlexTable m_activity;
    
    // The CSS formatter for the activity table.
    private FlexCellFormatter m_formatter;

    // A panel at the bottom of the page where error messages can be printed.
    private ErrorPanel m_errorPanel;

    // This map is populated with all workflow filters provided as GET
    // parameters in the URL.
    // - Key: filter property
    // - Value: filter value
    private Map<String, String> m_workflowFilters
            = new HashMap<String, String>();

    // For non-completed workflows, this map is populated with a list of
    // manual transitions for the active state.  These transitions represent
    // actions that the user can currently take on that workflow.
    // - Key: Workflow name
    // - Value: Outbound manual transitions for active state
    private Map<String, List<Transition>> m_manualTransitionMap
            = new HashMap<String, List<Transition>>();

    // This map is populated with all states.
    // - Key: Workflow name + state name
    // - Value: State object
    private Map<String, State> m_stateMap
            = new HashMap<String, State>();

    // This map is populated with subjobs for states that show up in the
    // activity table.
    // - Key: Workflow name + state name
    // - Value: Job object
    private Map<String, Job> m_subJobMap
            = new HashMap<String, Job> ();

    // The label of the first column; defaults to Name if not specified in GET.
    private String m_keyLabel;

    // The property from which to retrieve the value of the first column;
    // defaults to workflowName if not 
    private String m_keyProperty;
    
    // The workflows to be displayed in the activity table, as returned by the
    // server.
    private FindObjectsResponse m_workflows;

    // The panel on the right side containing charts (if defined on the
    // workflow definition).
    private VerticalPanel m_chartPanel;

    // The individual charts are stored while they're processed.
    private List<Widget> m_charts = new ArrayList<Widget>();

    // The map of properties that are being charted to the series that chart
    // them.
    private Map<String, SeriesHandler> m_chartProperties =
            new HashMap<String, SeriesHandler> ();

    // The name of the property on a state definition which determines whether
    // the corresponding states show up in the activity table.
    private static final String SHOW = "ec_workflow_activity_show";

    // The name of the property sheet on a workflow definition which defines
    // chart properties and colors.
    private static final String CHARTS = "ec_workflow_activity/charts";

    // The default chart dimensions and colors.
    private static final int CHART_WIDTH  = 325;
    private static final int CHART_HEIGHT = 140; 
    private static final String CHART_DEFAULT_COLOR = "#000000";

    //~ Functions --------------------------------------------------------------

    /**
     * This function is called by SDK infrastructure to initialize the UI parts
     * of this component.
     *
     * @return  A widget that the infrastructure should place in the UI; usually
     *          a panel.
     */
    @Override public Widget doInit()
    {
        WorkflowActivityStyles css = RESOURCES.css();

        // Inject the contents of the CSS file.
        css.ensureInjected();

        // Process the GET parameters in the URL.
        processUrlParameters();

        // Create the panels.
        VerticalPanel vertical = new VerticalPanel();
        HorizontalPanel horizontal = new HorizontalPanel();
        m_activity = new FlexTable();
        m_chartPanel = new VerticalPanel();
        horizontal.add(m_activity);
        horizontal.add(m_chartPanel);
        m_activityPanel.setContentWidget((Widget) horizontal);
        m_errorPanel = getUIFactory().createErrorPanel();
        vertical.add(m_activityPanel);
        vertical.add(m_errorPanel);

        // Set the panel styles.
        m_activity.setStylePrimaryName(css.activityTable());
        m_chartPanel.setStylePrimaryName(css.chartsPanel());
        m_formatter = m_activity.getFlexCellFormatter();

        /**
         * Make all workflow definition related requests, and use the responses
         * to search for workflows.
         */
        getRequestManager().doRequest(new ChainedCallback() {
            @Override
            public void onComplete() {
                getWorkflows();
            }
        }, getStateDefinitionsRequest(), getChartDefinitionsRequest());

        return vertical;
    }

    /**
     * Look for "key_" and "filter_" GET parameters and store them for the
     * workflows query to add accordingly.
     * 
     * Create the activity panel after adding the filters to the title.
     */
    private void processUrlParameters() {
        Map<String, List<String>> getParameters = Window.Location
                .getParameterMap();
        String title = "Activity - " + m_projectName + " "
                + m_workflowDefinitionName;
        for (Entry<String, List<String>> stringListEntry
                : getParameters.entrySet()) {
            String name = stringListEntry.getKey();
            List<String> values = stringListEntry.getValue();
            String value  = values.isEmpty() ? "" : values.get(0);
            if (name.startsWith("key_")) {
                // We only support one "key_" parameter, so the last one wins.
                m_keyLabel = name.substring(4);
                m_keyProperty = value;
            }
            if (name.startsWith("filter_")) {
                String filter = name.substring(7);
                m_workflowFilters.put(filter, value);
                title += " (" + filter + "=" + value + ")";
            }
        }
        m_activityPanel = getUIFactory().createTitledPanel(title);
    }

    /**
     * This function gets the state definitions for the GET-specified workflow
     * definition.  If one or more state definition sets the SHOW property,
     * only their corresponding states will be displayed in the activity table.
     * If no state definitions set the SHOW property, all states will be
     * displayed.
     */
    private FindObjectsRequest getStateDefinitionsRequest()
    {
        FindObjectsRequest stateDefinitionsRequest = getRequestManager()
                .getRequestFactory().createFindObjectsRequest(
                ObjectType.stateDefinition);
        stateDefinitionsRequest.addSort("index", Order.ascending);
        stateDefinitionsRequest.addFilter(new FindObjectsFilter.EqualsFilter(
                "projectName", m_projectName));
        stateDefinitionsRequest.addFilter(new FindObjectsFilter.EqualsFilter(
                "workflowDefinitionName", m_workflowDefinitionName));
        stateDefinitionsRequest.addSelect(SHOW, false);
        stateDefinitionsRequest.setCallback(new FindObjectsResponseCallback() {
            @Override
            public void handleError(@NotNull CommanderError error) {
                   m_errorPanel.addErrorMessage(error.getMessage());
            }

            /**
             * Store all the state definitions in one list, and just the ones
             * that have the SHOW property set in another.  If one or more of
             * the definitions have the SHOW property set, we will only show
             * those states.  Otherwise, we will show them all.
             */
            @Override
            public void handleResponse(@Nullable FindObjectsResponse response) {
                List<String> allStates = new ArrayList<String>();
                List<String> selectedStates = new ArrayList<String>();
                ListIterator<StateDefinition> iterator =
                        response.getStateDefinitions().listIterator();
                while (iterator.hasNext()) {
                    StateDefinition stateDefinition = iterator.next();
                    allStates.add(stateDefinition.getName());
                    String selected = getSelectedProperty(
                            response.getSelects(stateDefinition.getObjectId()),
                            SHOW);
                    if (selected != null
                            && !selected.isEmpty()
                            && !selected.equals("0")) {
                        selectedStates.add(stateDefinition.getName());
                    }
                }
                m_statesToShow = selectedStates.size() > 0
                        ? selectedStates
                        : allStates;

                // Create the header row for the activity table now that we
                // know which states are going to be included.
                WorkflowActivityStyles css = RESOURCES.css();

                m_activity.setWidget(0, 0, new Label(
                        m_keyLabel == null ? "Name" : m_keyLabel));
                m_formatter.setStylePrimaryName(0, 0,
                        css.activityTableHeader());

                // Create a table for the state names.
                FlexTable pipeline = new FlexTable();
                FlexCellFormatter formatter = pipeline.getFlexCellFormatter();
                int col = 0;
                for (String stateToShow : m_statesToShow)
                {
                    pipeline.setWidget(0, col, new Label(stateToShow));
                    formatter.setStylePrimaryName(0, col, css.pipelineHeader());
                    col++;

                    // Show an arrow unless it's the final state.
                    if (m_statesToShow.indexOf(stateToShow) <
                            m_statesToShow.size() - 1) {
                        pipeline.setWidget(0, col, null);
                        formatter.setStylePrimaryName(0, col,
                                css.pipelineArrow());
                        col++;
                    }
                }
                m_activity.setWidget(0, 1, pipeline);
                m_formatter.setStylePrimaryName(0, 1,
                        css.activityTableHeader());

                m_activity.setWidget(0, 2, new Label("Actions"));
                m_formatter.setStylePrimaryName(0, 2,
                        css.activityTableHeaderLast());
            }
        });

        return stateDefinitionsRequest;
    }

    /**
     * This function looks for chart definitions on the GET-specified workflow
     * definition.  If they exist, we record the properties being charted to
     * include when querying for workflows.  We also create and store the charts
     * so the values can be easily added when processing workflows.
     */
    private GetPropertiesRequest getChartDefinitionsRequest() {
        GetPropertiesRequest getPropertiesRequest = getRequestManager()
                .getRequestFactory().createGetPropertiesRequest();
        getPropertiesRequest.setProjectName(m_projectName);
        getPropertiesRequest.setWorkflowDefinitionName(
                m_workflowDefinitionName);
        getPropertiesRequest.setPath(CHARTS);
        getPropertiesRequest.setRecurse(true);
        getPropertiesRequest.setCallback(new PropertySheetCallback() {
            
            @Override
            public void handleError(@NotNull CommanderError error) {
                // Ignore non-existent sheets.
                if (!error.getCode().equals(
                        ErrorCode.NoSuchProperty.toString())) {
                    m_errorPanel.addErrorMessage(error.getMessage());
                }
            }

            /**
             * Iterate through all the nested sheets representing individual
             * charts.  Store the properties that need to be added to the
             * query for workflows.  Create the charts and set their title,
             * color, series labels, etc.
             */
            @Override
            public void handleResponse(@Nullable PropertySheet response) {
                WorkflowActivityStyles css = RESOURCES.css();
                Iterator<Entry<String, Property>> sheets =
                        response.getProperties().entrySet().iterator();
                while (sheets.hasNext()) {
                    Entry<String, Property> sheet = sheets.next();
                    Map<String, Property> properties = sheet.getValue()
                            .getPropertySheet().getProperties();
                    Iterator<Entry<String, Property>> propertyIterator = 
                            properties.entrySet().iterator();

                    PlotModel  model = new PlotModel();
                    SimplePlot chart = new SimplePlot(model, new PlotOptions());
                    chart.setHeight(CHART_HEIGHT);
                    chart.setWidth(CHART_WIDTH);

                    // The name of the nested sheet is the chart's title.
                    Label title = new Label(sheet.getKey());
                    title.setStylePrimaryName(css.chartsTitle());
                    m_charts.add(title);

                    // Go through each property.  When we get to the property
                    // name, create a new series.  If the label wasn't set, we
                    // skip the series.  If the color wasn't set, we default it.
                    String label = "";
                    String color = CHART_DEFAULT_COLOR;
                    while (propertyIterator.hasNext()) {
                        Entry<String, Property> property =
                                propertyIterator.next();
                        String name = property.getKey();
                        String value = property.getValue().getValue();
                        if (name.contains("-color")) {
                            color = value;
                        }
                        if (name.contains("-label")) {
                            label = value;
                        }
                        if (name.contains("-propertyName")
                                && !label.isEmpty()) {
                            SeriesHandler series = model.addSeries(label,
                                    color);
                            m_chartProperties.put(value, series);
                            label = "";
                            color = CHART_DEFAULT_COLOR;
                        }
                    }
                    // Store the chart for now.  We can only add it to the panel
                    // after all the values from the workflows have been added,
                    // otherwise the values won't show up and the charts will be
                    // blank.
                    m_charts.add(chart);
                }
            }
        });

        return getPropertiesRequest;
    }
    
    /**
     * This function gets the workflows to be displayed in the activity table.
     * 
     * For each workflow, it passes off control to the getStates function to
     * make subsequent requests before populating the activity table with the
     * row corresponding to that workflow.
     */
    private void getWorkflows()
    {
        FindObjectsRequest workflowsRequest = getRequestManager()
                .getRequestFactory()
                .createFindObjectsRequest(ObjectType.workflow);
        workflowsRequest.addSort("workflowId", Order.descending);
        workflowsRequest.addFilter(new FindObjectsFilter.EqualsFilter(
                "projectName", m_projectName));
        workflowsRequest.addFilter(new FindObjectsFilter.EqualsFilter(
                "workflowDefinitionName", m_workflowDefinitionName));

        // If a key was specified in the URL, add the corresponding property
        // as a select to the findObjects query.
        if (m_keyProperty != null) {
            workflowsRequest.addSelect(m_keyProperty, false);
        }

        // If any charts were specified, add the properties they're looking for
        // as selects to the findObjects query.
        if (m_chartProperties.size() > 0) {
            Iterator<String> chartProperties = m_chartProperties.keySet()
                    .iterator();
            while (chartProperties.hasNext()) {
                String chartProperty = chartProperties.next();
                workflowsRequest.addSelect(chartProperty, false);
            }
        }

        // Add all filters specified in the URL to the request.
        for (String filter : m_workflowFilters.keySet()) {
            workflowsRequest.addFilter(new FindObjectsFilter.EqualsFilter(
                    filter, m_workflowFilters.get(filter)));
        }
        workflowsRequest.setMaxIds(20);

        workflowsRequest.setCallback(new FindObjectsResponseCallback() {
            @Override
            public void handleError(@NotNull CommanderError error) {
                   m_errorPanel.addErrorMessage(error.getMessage());
            }

            /**
             * Save the response.  For each workflow, pass the control to
             * getStates to make and process subsequent requests.  Also, extract
             * any specified values needed to build charts.
             */
            @Override
            public void handleResponse(@Nullable FindObjectsResponse response)
            {
                m_workflows = response;
                ListIterator<Workflow> iterator = response.getWorkflows()
                        .listIterator();
                while (iterator.hasNext()) {
                    Workflow workflow = iterator.next();
                    getStates(workflow);
                    if (m_chartProperties.size() > 0) {
                        getChartValues(workflow);
                    }
                }
                // Now that we've worked through all the workflows, add the
                // charts to the panel.
                Iterator<Widget> charts = m_charts.iterator();
                while (charts.hasNext()) {
                    m_chartPanel.add(charts.next());
                }
            }
        });

        getRequestManager().doRequest(workflowsRequest);
    }

    /**
     * For the given workflow, this function gets the relevant states as
     * determined earlier by the getStateDefinitions function.
     * 
     * It stores the states so they can be processed later, and it calls the
     * getTransitionsAndJobs function for each state to get and store more
     * information needed to populate the activity table.
     * 
     * Once all these requests have been made and processed, it calls the
     * processWorkflow function to populate the activity table with the row
     * corresponding to the given workflow.
     * 
     * @param workflow -- The workflow whose states are being retrieved.
     */
    private void getStates(final Workflow workflow)
    {
        final String workflowName = workflow.getName();

        FindObjectsRequest statesRequest = getRequestManager()
                .getRequestFactory()
                .createFindObjectsRequest(ObjectType.state);
        statesRequest.addFilter(new FindObjectsFilter.EqualsFilter(
                "projectName", m_projectName));
        statesRequest.addFilter(new FindObjectsFilter.EqualsFilter(
                "workflowName", workflowName));

        statesRequest.setCallback(new FindObjectsResponseCallback() {
            @Override
            public void handleError(@NotNull CommanderError error) {
                   m_errorPanel.addErrorMessage(error.getMessage());
            }

            /**
             * Iterate through the response and for each state, save the state
             * and collect subsequent requests for manual transitions and
             * subjobs.  Then, execute these collected requests with a final
             * callback to create a row in the activity table for the given
             * workflow.
             */
            @Override
            public void handleResponse(@Nullable FindObjectsResponse response) {
                ListIterator<State> iterator = response.getStates()
                        .listIterator();
                List<CommanderRequest<?>> requests =
                        new ArrayList<CommanderRequest<?>> ();
                while (iterator.hasNext()) {
                    State state = iterator.next();
                    String stateName = state.getName();
                    m_stateMap.put(workflowName + "/" + stateName, state);
                    // We only need to get manual transition and/or subjob info
                    // for states which are shown in the table and the current
                    // active state.
                    if (m_statesToShow.contains(stateName)
                            || state.isActive()) {
                        requests.addAll(getTransitionsAndJobs(workflow, state));
                    }
                }
                getRequestManager().doRequest(new ChainedCallback() {
                    /**
                     * Once all of the manual transition and subjob requests
                     * are complete and the results have been stored, we have
                     * all the info we need to add the given workflow to the
                     * activity table, which is what the processWorkflow method
                     * will do.
                     */
                    @Override
                    public void onComplete() {
                        processWorkflow(workflow);
                    }
                }, requests);
            }

        });

        getRequestManager().doRequest(statesRequest);
    }

    /**
     * For the given state, this function gets and stores outbound manual
     * transition and subjob information to be processed later by the
     * processWorkflow function.
     * 
     * @param workflow -- The workflow to which the state belongs.
     * @param state    -- The state whose transition and job info is retreived.
     */
    private List<CommanderRequest<?>> getTransitionsAndJobs(
            Workflow workflow,
            State state)
    {
        String stateName = state.getName();
        final String workflowName = state.getWorkflowName();
        final String mapIndex = workflowName + "/" + stateName;

        List<CommanderRequest<?>> requests =
                new ArrayList<CommanderRequest<?>> ();

        // Only get manual transitions for the active state of a non-completed
        // workflow.
        if (!workflow.isCompleted() && state.isActive()) {
            FindObjectsRequest transitionsRequest = getRequestManager()
                    .getRequestFactory()
                    .createFindObjectsRequest(ObjectType.transition);
            transitionsRequest.addFilter(new FindObjectsFilter.EqualsFilter(
                    "projectName", m_projectName));
            transitionsRequest.addFilter(new FindObjectsFilter.EqualsFilter(
                    "workflowName", workflowName));
            transitionsRequest.addFilter(new FindObjectsFilter.EqualsFilter(
                    "stateName", stateName));
            transitionsRequest.addFilter(new FindObjectsFilter.EqualsFilter(
                    "trigger", "manual"));
    
            transitionsRequest.setCallback(new FindObjectsResponseCallback() {
                @Override
                public void handleError(@NotNull CommanderError error) {
                       m_errorPanel.addErrorMessage(error.getMessage());
                }

                /**
                 * Just store the result for now; processWorkflow will use it
                 * later on.
                 */
                @Override
                public void handleResponse(
                        @Nullable FindObjectsResponse response) {
                    m_manualTransitionMap.put(workflowName,
                            response.getTransitions());
                }
    
            });
            requests.add(transitionsRequest);
        }
        

        // Only get subjob information for states which we're displaying that
        // actually have a subjob.
        if (m_statesToShow.contains(stateName)
                && !state.getSubjob().isEmpty()) {
            GetJobInfoRequest jobRequest = getRequestManager()
                    .getRequestFactory()
                    .createGetJobInfoRequest();
            jobRequest.setJobId(state.getSubjob());
            
            jobRequest.setCallback(new JobCallback() {
                @Override
                public void handleError(@NotNull CommanderError error) {
                       m_errorPanel.addErrorMessage(error.getMessage());
                }
                
                /**
                 * Just store the result for now; processWorkflow will use it
                 * later on.
                 */
                @Override
                public void handleResponse(@Nullable Job response) {
                    m_subJobMap.put(mapIndex, response);
                }
            });
            requests.add(jobRequest);
        }

        return requests;
    }

    /**
     * This function takes all the information collected by the "get" functions
     * and populates a row in the activity table for the given workflow.
     * 
     * @param workflow -- The workflow to process and add a row to the activity
     *                       table.
     */
    private void processWorkflow(Workflow workflow)
    {
        String workflowName = workflow.getName();
        Integer row = m_activity.getRowCount();
        WorkflowActivityStyles css = RESOURCES.css();
        Boolean even = row % 2 == 0;
        String style = even ? css.activityTableEven() : css.activityTableOdd();

        // First, create a string for the first column.  If a key was specified
        // as a GET parameter in the URL, get the selected property value for
        // that key and display it.  Otherwise, default to the workflow name.
        String key = null;
        if (m_keyLabel != null) {
            Map<String, Property> selects =
                    m_workflows.getSelects(workflow.getObjectId());
            String selected = getSelectedProperty(selects, m_keyProperty);
            if (selected != null && !selected.isEmpty()) {
                key = selected;
            }
        }
        if (key == null) {
            key = workflowName;
        }
        Anchor nameLink = new Anchor();
        nameLink.setHref(createWorkflowLink(workflow));
        nameLink.setText(key);
        m_activity.setWidget(row, 0, nameLink);
        m_formatter.setStylePrimaryName(row, 0, style);

        // Go through each state and graphically display the current subjob
        // status.

        FlexTable pipeline = new FlexTable();
        FlexCellFormatter formatter = pipeline.getFlexCellFormatter();
        int col = 0;
        for (String stateName : m_statesToShow)
        {
            pipeline.setWidget(0, col, null);
            String mapIndex = workflowName + "/" + stateName;
            String backgroundStyle;
            if (m_subJobMap.containsKey(mapIndex)) {
                Job job = m_subJobMap.get(mapIndex);
                formatter.getElement(0, col).setAttribute("onclick",
                        "window.location='" + createJobLink(job) + "'");

                JobOutcome outcome = job.getOutcome();
                if (outcome.equals(JobOutcome.success)) {
                    backgroundStyle = css.pipelineSuccess();
                } else if (outcome.equals(JobOutcome.warning)) {
                    backgroundStyle = css.pipelineWarning();
                } else {
                    backgroundStyle = css.pipelineError();
                }
            } else {
                backgroundStyle = css.pipelineNoJob();
            }
            formatter.setStylePrimaryName(0, col, backgroundStyle);
            col++;
            
            // Show an arrow unless it's the final state.
            if (m_statesToShow.indexOf(stateName) < m_statesToShow.size() - 1) {
                pipeline.setWidget(0, col, null);
                formatter.setStylePrimaryName(0, col, css.pipelineArrow());
                col++;
            }
        }
        m_activity.setWidget(row, 1, pipeline);
        m_formatter.setStylePrimaryName(row, 1, style);

        // Finally, create the actions for this workflow.  This is basically the
        // standard actions along with any available manual transitions.
        FlexTable actions = new FlexTable();
        formatter = actions.getFlexCellFormatter();
        col = 0;
        Anchor viewLink = new Anchor();
        viewLink.setHref(createWorkflowLink(workflow));
        viewLink.setText("View");
        actions.setWidget(0, col, viewLink);
        col++;
        if (m_manualTransitionMap.containsKey(workflowName)) {
            ListIterator<Transition> iterator = m_manualTransitionMap
                    .get(workflowName)
                    .listIterator();
            while (iterator.hasNext()) {
                actions.setWidget(0, col, new Label("|"));
                formatter.setStylePrimaryName(0, col,
                        css.actionsSeparator());
                col++;

                Transition transition = iterator.next();
                Anchor transitionLink = new Anchor();
                transitionLink.setHref(createTransitionLink(transition));
                transitionLink.setText(transition.getName());
                actions.setWidget(0, col, transitionLink);
                col++;
            }
        }
        m_activity.setWidget(row, 2, actions);
        m_formatter.setStylePrimaryName(row, 2, even ?
                css.activityTableEvenLast() : css.activityTableOddLast());
    }

    /**
     * For a single workflow, get all the property values that need to be
     * charted and add them to the appropriate series.
     */
    private void getChartValues(final Workflow workflow)
    {
        Iterator<String> iterator = m_chartProperties.keySet().iterator();
        Map<String, Property> selects =
                m_workflows.getSelects(workflow.getObjectId());

        // If a key was specified in the URL, check to see if the values are
        // numeric.  If they are, use those as the x-axis.  Otherwise, use the
        // workflow ID.
        Long xAxis = workflow.getId();
        if (m_keyLabel != null) {
            try {
                String selected = getSelectedProperty(selects, m_keyProperty);
                if (selected != null && !selected.isEmpty()) {
                    xAxis = Long.parseLong(selected);
                }
            } catch (NumberFormatException e) {
                xAxis = workflow.getId();
            }
        }

        while (iterator.hasNext()) {
            String property = iterator.next();
            String value = getSelectedProperty(selects, property);
            if (value != null) {
                m_chartProperties.get(property).add(new DataPoint(xAxis,
                        Double.parseDouble(value)));
            }
        }
    }

    /**
     * Helper functions.
     */

    private static String getSelectedProperty(
            Map<String, Property> selects,
            String propertyName)
    {
        Property property;
        if (selects == null) {
            property = null;
        } else {
            property = selects.get(propertyName);
        }
        if (property == null) {
            return null;
        }
        return property.getValue();
    }

    private static String createWorkflowLink(Workflow workflow)
    {
        return CommanderUrlBuilder.createLinkUrl("workflowDetails",
                "projects",     workflow.getProjectName(),
                "workflows",    workflow.getName())
                .buildString();
    }

    private static String createJobLink(Job job)
    {
        return CommanderUrlBuilder.createLinkUrl("jobDetails",
                "jobs",         job.getName())
                .buildString();
    }

    private static String createTransitionLink(Transition transition)
    {
        return CommanderUrlBuilder.createLinkUrl("transitionWorkflow",
                "projects",     transition.getProjectName(),
                "workflows",    transition.getWorkflowName(),
                "states",       transition.getStateName(),
                "transitions",  transition.getName())
                .buildString();
    }
}
