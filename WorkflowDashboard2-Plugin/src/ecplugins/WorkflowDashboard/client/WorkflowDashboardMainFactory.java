
package ecplugins.WorkflowDashboard.client;

import com.electriccloud.commander.gwt.client.Component;
import com.electriccloud.commander.gwt.client.ComponentBaseFactory;
import com.google.gwt.core.client.JavaScriptObject;
import com.electriccloud.commander.gwt.client.ComponentContext;

/**
 * This factory is responsible for providing instances of the WorkflowDashboardMain
 * class.
 */
public class WorkflowDashboardMainFactory
    extends ComponentBaseFactory
{

    @Override protected Component createComponent(ComponentContext jso)
    {
        return new WorkflowDashboardMain();
    }


}
