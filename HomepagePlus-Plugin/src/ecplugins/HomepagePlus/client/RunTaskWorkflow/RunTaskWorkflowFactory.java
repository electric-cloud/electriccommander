
package ecplugins.HomepagePlus.client.RunTaskWorkflow;

import com.google.gwt.core.client.JavaScriptObject;

import com.electriccloud.commander.gwt.client.Component;
import com.electriccloud.commander.gwt.client.ComponentBaseFactory;
import com.electriccloud.commander.gwt.client.ComponentContext;

/**
 * This factory is responsible for providing instances of the RunTaskWorkflow
 * class.
 */
public class RunTaskWorkflowFactory
    extends ComponentBaseFactory
{
 
    private RunTaskWorkflow iRunTaskWorkflow;
    @Override 
    protected Component createComponent(ComponentContext jso)
    {
        iRunTaskWorkflow = new RunTaskWorkflow();
        return iRunTaskWorkflow;
    }

//    @Override public void onModuleLoad(){
//        super.onModuleLoad();
//        iRunTaskWorkflow.setVisible(true);
//    }
}