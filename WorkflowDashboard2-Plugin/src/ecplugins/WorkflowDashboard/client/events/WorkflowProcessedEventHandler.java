package ecplugins.WorkflowDashboard.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface WorkflowProcessedEventHandler extends EventHandler {
    void onWorkflowProcessed(WorkflowProcessedEvent event);
}