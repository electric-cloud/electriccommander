package ecplugins.WorkflowDashboard.client;

import com.electriccloud.commander.client.domain.Job;
import com.electriccloud.commander.client.domain.Workflow;

/**
 * Created by IntelliJ IDEA.
 * User: jthorpe
 * Date: 3/14/12
 * Time: 3:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class WorkflowData{
    
    private boolean iCompletedWithSuccess;
    private boolean iManualTransition;
    private Job iAssociatedJob;
    private boolean iHasActiveJob;
    private boolean iHasActiveWorkflow;
    private String  iDescription;

    WorkflowData(){
        this.iCompletedWithSuccess=false;
        this.iAssociatedJob=null;
        this.iManualTransition=false;
        this.iHasActiveWorkflow=false;
        this.iHasActiveJob=false;
        this.iDescription="";
    }

    public boolean getCompletedWithSuccess(){
        return this.iCompletedWithSuccess;
    }

    public void setCompletedWithSuccess(boolean value){
        this.iCompletedWithSuccess=value;
    }

    public boolean getIsManualTransition(){
        return this.iManualTransition;
    }
    
    public void setIsManualTransition(boolean value){
        iManualTransition=value;    
    } 
    
    public Job getAssociatedJob(){
        //not implemented
        return null;
    }

    public boolean getHasActiveWorkflow(){
        return this.iHasActiveWorkflow;
    }

    public void setHasActiveWorkflow(boolean value){
        this.iHasActiveWorkflow=value;
    }

    public boolean getHasActiveJob(){
        return this.iHasActiveJob;
    }

    public void setHasActiveJob(boolean value){
        iHasActiveJob=value;
    }

    public String getDescription(){
        return this.iDescription;
    }

    public void setDescription(String value){
        iDescription=value;
    }


}
