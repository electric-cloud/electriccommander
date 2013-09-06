package ecplugins.WorkflowDashboard.client.events;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.SimpleEventBus;

import ecplugins.WorkflowDashboard.client.WorkflowStats;

public class WorkflowProcessedEvent extends GwtEvent<WorkflowProcessedEventHandler> {

    public static final Type<WorkflowProcessedEventHandler> TYPE = new Type<WorkflowProcessedEventHandler>();

    private WorkflowStats wfStats = null;

    public static void register(SimpleEventBus bus, WorkflowProcessedEventHandler handler){
        bus.addHandler(TYPE, handler);
    }

    public WorkflowProcessedEvent(WorkflowStats wfStats) {
        this.wfStats = wfStats;
    }

    @Override
    public Type<WorkflowProcessedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(WorkflowProcessedEventHandler handler) {
        handler.onWorkflowProcessed(this);
    }

    public WorkflowStats getResult() {
        return this.wfStats;
    }
}
