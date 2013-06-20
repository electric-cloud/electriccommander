
package ecplugins.HomepagePlus.client.Dashboard;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;

import com.electriccloud.commander.gwt.client.Component;
import com.electriccloud.commander.gwt.client.ComponentBaseFactory;
import com.electriccloud.commander.gwt.client.ComponentContext;

/**
 * This factory is responsible for providing instances of the HomeDashboard
 * class.
 */
public class HomeDashboardFactory
    extends ComponentBaseFactory
    implements EntryPoint
{
 
    @Override
    protected Component createComponent(ComponentContext jso)
    {
        return new HomeDashboard();
    }
}
