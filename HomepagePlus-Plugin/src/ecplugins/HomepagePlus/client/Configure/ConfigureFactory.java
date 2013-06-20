
package ecplugins.HomepagePlus.client.Configure;

import com.google.gwt.core.client.JavaScriptObject;

import com.electriccloud.commander.gwt.client.Component;
import com.electriccloud.commander.gwt.client.ComponentBaseFactory;
import com.electriccloud.commander.gwt.client.ComponentContext;

/**
 * This factory is responsible for providing instances of the Configure
 * class.
 */
public class ConfigureFactory
    extends ComponentBaseFactory
{
 
    @Override
    protected Component createComponent(ComponentContext jso)
    {
        return new Configure();
    }
}
