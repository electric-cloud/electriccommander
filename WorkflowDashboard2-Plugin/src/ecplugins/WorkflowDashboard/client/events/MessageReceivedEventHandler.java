package ecplugins.WorkflowDashboard.client.events;

import com.google.gwt.event.shared.EventHandler;

public interface MessageReceivedEventHandler extends EventHandler {
    void onMessageReceived(MessageReceivedEvent event);
}