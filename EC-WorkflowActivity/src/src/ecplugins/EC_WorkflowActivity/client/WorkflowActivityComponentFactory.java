
package ecplugins.EC_WorkflowActivity.client;

import com.electriccloud.commander.gwt.client.ComponentContext;

import com.electriccloud.commander.gwt.client.Component;
import com.electriccloud.commander.gwt.client.ComponentBaseFactory;

/**
 * This factory is responsible for providing instances of the WorkflowActivityComponent
 * class.
 */
public class WorkflowActivityComponentFactory
    extends ComponentBaseFactory
{

    @Override
    protected Component createComponent(ComponentContext jso) {
        return new WorkflowActivityComponent();
    }
}
