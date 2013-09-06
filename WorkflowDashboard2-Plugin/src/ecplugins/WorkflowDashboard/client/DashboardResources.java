package ecplugins.WorkflowDashboard.client;

/**
 * Created by IntelliJ IDEA.
 * User: jthorpe
 * Date: 2/28/12
 * Time: 2:44 PM
 * To change this template use File | Settings | File Templates.
 */

// CIManagerResources.java --
//
// CIManagerResources.java is part of ElectricCommander.
//
// Copyright (c) 2005-2012 Electric Cloud, Inc.
// All rights reserved.
//

import com.electriccloud.commander.gwt.client.ui.BundledResources;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;


@SuppressWarnings({"InterfaceNeverImplemented"})
public interface DashboardResources
        extends BundledResources
{

    //~ Instance fields --------------------------------------------------------

    // The instance of the ClientBundle that must be injected during doInit()
    DashboardResources RESOURCES = GWT.create(DashboardResources.class);

    //~ Methods ----------------------------------------------------------------

    @Source("images/icon_ci_box_red.png")
    ImageResource buildFailedIcon();

    @Source("images/icon_ci_box_grey.png")
    ImageResource buildNoDataIcon();

    @Source("images/icon_ci_box_green.png")
    ImageResource buildSuccessIcon();

    @Source("images/workflow_complete_20px.png")
    ImageResource workflowCompletedIcon();

    @Source("images/workflow_manual_20px.png")
    ImageResource workflowManualTransitionIcon();
    
    @Source("images/workflow_20px.png")
    ImageResource runningWorkflowIcon();

    @Source("images/icon_ci_box_yellow.png")
    ImageResource buildWarningIcon();

    @Source("styles/custom.css")
    DashboardStyles DashboardStyles();


    @Source("images/icon_ci_box_running.png")
    ImageResource runningIcon();

    @Source("images/icn20px_running_success.png")
    ImageResource runningSuccessIcon();

    @Source("images/ICON_PIE_000.png")
    ImageResource successRatio000Icon();

    @Source("images/ICON_PIE_010.png")
    ImageResource successRatio010Icon();

    @Source("images/ICON_PIE_020.png")
    ImageResource successRatio020Icon();

    @Source("images/ICON_PIE_025.png")
    ImageResource successRatio025Icon();

    @Source("images/ICON_PIE_030.png")
    ImageResource successRatio030Icon();

    @Source("images/ICON_PIE_040.png")
    ImageResource successRatio040Icon();

    @Source("images/ICON_PIE_050.png")
    ImageResource successRatio050Icon();

    @Source("images/ICON_PIE_060.png")
    ImageResource successRatio060Icon();

    @Source("images/ICON_PIE_070.png")
    ImageResource successRatio070Icon();

    @Source("images/ICON_PIE_075.png")
    ImageResource successRatio075Icon();

    @Source("images/ICON_PIE_080.png")
    ImageResource successRatio080Icon();

    @Source("images/ICON_PIE_090.png")
    ImageResource successRatio090Icon();

    @Source("images/ICON_PIE_100.png")
    ImageResource successRatio100Icon();

    @Source("images/btn_treeBlank.gif")
    ImageResource treeBlankButton();

    @Source("images/icn16px_unknown.gif")
    ImageResource unknownIcon();

    @Source("images/icn16px_warning.gif")
    ImageResource warningIcon();

    @Source("images/bullet_blue_collapse.png")
    ImageResource collapseTreeIcon();

    @Source("images/bullet_blue_expand.png")
    ImageResource expandTreeIcon();

    @Source("images/RedLight.png")
    ImageResource RedLightIcon();

    @Source("images/YellowLight.png")
    ImageResource YellowLightIcon();

    @Source("images/GreenLight.png")
    ImageResource GreenLightIcon();
}
