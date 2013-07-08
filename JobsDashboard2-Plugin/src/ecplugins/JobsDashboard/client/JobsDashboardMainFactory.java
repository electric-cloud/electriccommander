
package ecplugins.JobsDashboard.client;

import com.google.gwt.core.client.JavaScriptObject;

import com.electriccloud.commander.gwt.client.Component;
import com.electriccloud.commander.gwt.client.ComponentBaseFactory;
import com.electriccloud.commander.gwt.client.ComponentContext;

/**
 * This factory is responsible for providing instances of the JobsDashboardMain
 * class.
 */
public class JobsDashboardMainFactory
    extends ComponentBaseFactory
{

    @Override protected Component createComponent(ComponentContext jso)
    {
        return new JobsDashboardMain();
    }
}
