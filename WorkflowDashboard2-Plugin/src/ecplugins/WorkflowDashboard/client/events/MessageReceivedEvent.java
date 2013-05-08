package ecplugins.WorkflowDashboard.client.events;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.SimpleEventBus;

public class MessageReceivedEvent extends GwtEvent<MessageReceivedEventHandler> {

    public static final Type<MessageReceivedEventHandler> TYPE = new Type<MessageReceivedEventHandler>();

    private final String message;
    
    public static void register(SimpleEventBus bus, MessageReceivedEventHandler handler){
        bus.addHandler(TYPE, handler);
    }

    public MessageReceivedEvent(String message) {
        this.message = message;
    }

    @Override
    public Type<MessageReceivedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(MessageReceivedEventHandler handler) {
        handler.onMessageReceived(this);
    }

    public String getMessage() {
        return message;
    }
}
