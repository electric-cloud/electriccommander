package ecplugins.WorkflowDashboard.client;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;


import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: jthorpe
 * Date: 4/17/12
 * Time: 2:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class WorkflowStats {
    private int totalWorkflows;
    private int numberOfWorkflowsCompletedWithSuccess;
    private int numberOfWorkflowsCompletedWithError;
    private int numberOfWorkflowsWaitingForManualIntervention;
    private int numberOfActiveWorkflows;
    private String projectName;
    private String workflowName;
    private HTML htmlStatusContainer;
    private Label lblAvgCycleTime;
    private String workflowStartTime;
    private String workflowLastModifiedTime;
    private long totalCycleTimeMs;


    
    
    public WorkflowStats(){

        this.totalWorkflows=0;
        this.numberOfWorkflowsCompletedWithSuccess=0;
        this.numberOfWorkflowsCompletedWithError=0;
        this.numberOfWorkflowsWaitingForManualIntervention=0;
        this.projectName="";
        this.workflowName="";
        this.htmlStatusContainer=null;
        this.lblAvgCycleTime=null;
        this.numberOfActiveWorkflows=0;
    }

    public void setTotalWorkflows(int value){
        this.totalWorkflows=value;
    }

    public int getTotalWorkflows(){
        return this.totalWorkflows;
    }


    public int getTotalWorkflowsProcessed(){
        return this.numberOfWorkflowsCompletedWithError+this.numberOfWorkflowsCompletedWithSuccess+this.getNumberOfWorkflowsWaitingForManualIntervention()+this.numberOfActiveWorkflows;
    }

    public int getNumberOfActiveWorkflows(){
        return this.numberOfActiveWorkflows;
    }

    public void setNumberOfActiveWorkflows(int value){
        this.numberOfActiveWorkflows=value;
    }

    public int getNumberOfWorkflowsCompleted(){
        return this.numberOfWorkflowsCompletedWithSuccess+this.numberOfWorkflowsCompletedWithError;
    }

    public void setNumberOfWorkflowsCompletedWithSuccess(int value){
        this.numberOfWorkflowsCompletedWithSuccess=value;
    }

    public int getNumberOfWorkflowsCompletedWithSuccess(){
        return this.numberOfWorkflowsCompletedWithSuccess;
    }

    public void setNumberOfWorkflowsCompletedWithError(int value){
        this.numberOfWorkflowsCompletedWithError=value;
    }

    public int getNumberOfWorkflowsCompletedWithError(){
        return this.numberOfWorkflowsCompletedWithError;
    }

    public void setNumberOfWorkflowsWaitingForManualIntervention(int value){
        this.numberOfWorkflowsWaitingForManualIntervention=value;
    }

    public int getNumberOfWorkflowsWaitingForManualIntervention(){
        return this.numberOfWorkflowsWaitingForManualIntervention;
    }

    public int getSuccessRate(){
        if(this.getTotalWorkflowsProcessed()==0){
            return 0;
        }
        else{
            return (int)(((double)this.numberOfWorkflowsCompletedWithSuccess/(double)this.getTotalWorkflowsProcessed())*100);
        }

    }

    public void setProjectName(String value){
        this.projectName=value;
    }

    public String getProjectName(){
        return this.projectName;
    }

    public void setWorkflowName(String value){
        this.workflowName=value;
    }

    public String getWorkflowName(){
        return this.workflowName;
    }

    public String getWfKey(){
        return this.projectName+"-"+this.workflowName;
    }

    public void setHtmlStatusContainer(HTML value){
        this.htmlStatusContainer=value;
    }

    public HTML getHtmlStatusContainer(){
        return this.htmlStatusContainer;
    }

    public void setAvgCycleTimeContainer(Label value){
        this.lblAvgCycleTime=value;
    }

    public Label getAvgCycleTimeContainer(){
        return this.lblAvgCycleTime;
    }


    public void setWorkflowStartTime(String value){
        this.workflowStartTime=value;
    }
    
    public String getWorkflowStartTime(){
        return this.workflowStartTime;
    }
    
    public void  setWorkflowLastModifiedTime(String value){
        this.workflowLastModifiedTime=value;
    }
    
    public String getWorkflowLastModifiedTime(){
        return this.workflowLastModifiedTime;
    }

    public String getAverageCycleTimeAsString(){

        //DateTimeFormat dtf = new DateTimeFormat();

        if(this.getTotalWorkflowsProcessed()==0){
            return this.convertMsToHumanReadable(this.totalCycleTimeMs);
        }
        else{
            long avg = this.totalCycleTimeMs/this.getTotalWorkflowsProcessed();
            return this.convertMsToHumanReadable(avg);
        }

    }

    public void setTotalCycleTimeMs(long value){
        this.totalCycleTimeMs=value;
    }

    public long getTotalCycleTimeMs(){
        return this.totalCycleTimeMs;
    }

    public String convertMsToHumanReadable(long ms){

        if(ms==0){
            return "None";
        }

        String result;

        result="";

        int x = (int)(ms / 1000);
        int seconds = x % 60;
        x /= 60;
        int minutes = x % 60;
        x /= 60;
        int hours = x % 24;
        x /= 24;
        int days = x;

        if(days>0){
            result=days + " day ";
        }
        if(hours>0){
            result+=(hours+" hr ");
        }

        if(minutes>0){
            result+=(minutes + " min ");
        }

        if(seconds>0){
            result+=(seconds+" sec ");
        }

        return result;
    }


    public String getDump(){
        String msg="Project Name: "+this.getProjectName()+"\n";
        msg+="Workflow Name: "+this.getWorkflowName()+"\n";
        msg+="Workflow Completed With Success: "+this.getNumberOfWorkflowsCompletedWithSuccess()+"\n";
        msg+="Workflow Completed With Error: "+this.getNumberOfWorkflowsCompletedWithError()+"\n";
        msg+="Workflow Awaiting Manual Intervention: "+this.getNumberOfWorkflowsWaitingForManualIntervention()+"\n";
        msg+="Workflows Processed: "+this.getTotalWorkflowsProcessed()+"\n";
        msg+="Success Rate: "+this.getSuccessRate()+"\n";

        return msg;
    }

    public long calculateCycleTime(String d1, String d2){

        long cycleTime=0;

        //GWT has problems with Z format dates, workaround is to replace with +000
        if(d1.endsWith("Z")){
            d1=d1.replace("Z","+000");
        }

        if(d2.endsWith("Z")){
            d2=d2.replace("Z","+000");
        }


        DateTimeFormat dtf = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.ISO_8601);
        Date date1 = dtf.parse(d1);

        Date date2=dtf.parse(d2);

        int i = date1.compareTo(date2);

        if(i==0){
            cycleTime=0;
        }
        if(i<0){
            cycleTime=date2.getTime()-date1.getTime();
        }

        if(i>0){
            cycleTime=date1.getTime()-date2.getTime();
        }




        return cycleTime;

    }
}
