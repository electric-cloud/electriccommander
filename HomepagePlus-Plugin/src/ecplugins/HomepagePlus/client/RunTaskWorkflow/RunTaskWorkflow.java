//
// Copyright (c) 2005-2012 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.HomepagePlus.client.RunTaskWorkflow;

import java.util.List;
import java.util.Map;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.electriccloud.commander.client.ChainedCallback;
import com.electriccloud.commander.gwt.client.ComponentBase;
import com.electriccloud.commander.client.domain.Property;
import com.electriccloud.commander.client.domain.PropertySheet;
import com.electriccloud.commander.client.domain.Workflow;
import com.electriccloud.commander.client.requests.GetPropertiesRequest;
import com.electriccloud.commander.client.requests.GetPropertyRequest;
import com.electriccloud.commander.client.requests.RunWorkflowRequest;
import com.electriccloud.commander.client.requests.SetPropertyRequest;
import com.electriccloud.commander.client.responses.DefaultPropertyCallback;
import com.electriccloud.commander.client.responses.DefaultPropertySheetCallback;
import com.electriccloud.commander.client.responses.DefaultWorkflowCallback;
import com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder;
import static com.electriccloud.commander.gwt.client.util.CommanderUrlBuilder.createLinkUrl;

public class RunTaskWorkflow extends ComponentBase {
    private static RunTaskWorkflowUiBinder uiBinder =
        GWT.create(RunTaskWorkflowUiBinder.class);

    interface RunTaskWorkflowUiBinder extends UiBinder<Widget, RunTaskWorkflow> {}

    @UiField SimplePanel loading;
    @UiField SimplePanel runPanel;
    @UiField VerticalPanel panel;
    @UiField ListBox taskListBox;
    @UiField CheckBox showWorkflowDetails;
    @UiField Anchor run;
    @UiField Style style;

    private int iClickX;
    private int iClickY;
    private String iUserName;

    interface Style extends CssResource {
    }

    private String iProjectName = "jpmdis";

    @Override public Widget doInit()
    {
        Widget w = uiBinder.createAndBindUi(this);
        doRequest(new ChainedCallback() {

            @SuppressWarnings("deprecation")
            @Override public void onComplete() {
                DeferredCommand.addCommand(new Command() {

                    @Override public void execute() {
                        loading.setVisible(false);
                        panel.setVisible(true);
                        runPanel.setVisible(true);
                    }
                });
            }
        }, queryUserName(), populateListBox());
        run.setHTML("<img src=\"/commander/lib/images/icn16px_run.gif\" align=\"absmiddle\"/>&nbsp;Run");
        return w;
    }

    private GetPropertiesRequest populateListBox(){
        GetPropertiesRequest gpr = getRequestFactory().createGetPropertiesRequest();
        gpr.setPath("/myUser/userSettings/workflowConfigurations");
        gpr.setRecurse(true);
        gpr.setCallback(new DefaultPropertySheetCallback(this) {

            @Override public void handleResponse(PropertySheet sheet) {

                taskListBox.clear();
                //taskListBox.addItem("");

                // Loop on the properties to construct all the listbox task options
                for (Property property : sheet.getProperties().values()) {
                    String project = "";
                    String start = "";
                    String wf = "";
                    String key = "";
                    PropertySheet ps = property.getPropertySheet();
                    String taskLabel = property.getName();
                    for (Map.Entry<String, Property> entry : ps.getProperties().entrySet()) {
                        key = entry.getKey();
                        if (key.equals("project"))
                            project = entry.getValue().getValue();
                        if (key.equals("wf"))
                            wf = entry.getValue().getValue();
                        if (key.equals("start"))
                            start = entry.getValue().getValue();
                        //Window.alert(entry.getKey());
                    }
                    taskListBox.addItem(taskLabel, project + "/workflowDefinitions/" + wf + "?startingState=" + start);
                }
                //taskListBox.setVisibleItemCount(Math.min(i, 5));
                taskListBox.setVisibleItemCount(1);
                taskListBox.setWidth("250px");
            }
        });
        return gpr;
    }

    private GetPropertyRequest queryUserName(){
        // Get the username of the user that triggered the workflow
        GetPropertyRequest getUserNameRequest = getRequestFactory().createGetPropertyRequest();
        getUserNameRequest.setPropertyName("/myUser/userName");
        getUserNameRequest.setCallback(new DefaultPropertyCallback(this) {

            @Override public void handleResponse(Property response) {
                iUserName = response.getValue();
            }
        });
        return getUserNameRequest;
    }

    @UiHandler("run")
    void handleRunClick(ClickEvent e) {
        int idx = taskListBox.getSelectedIndex();
        String txt = taskListBox.getItemText(idx);
        String val = taskListBox.getValue(idx);
        if(txt.equals("")) {
            Window.alert("You need to select a task in the list");
            return;
        }
        String taskId = taskListBox.getValue(taskListBox.getSelectedIndex());
        String myHref = "/commander/link/runWorkflow/projects/" + val;
        //Window.alert("mcmahon: " + myHref);
        Window.Location.assign(myHref);
    }
}
