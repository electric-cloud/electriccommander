
// WorkflowActivityResources.java --
//
// WorkflowActivityResources.java is part of ElectricCommander.
//
// Copyright (c) 2005-2011 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.EC_WorkflowActivity.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;

/**
 * This interface houses a class that extends CssResource.
 *
 * <p>More information here:
 * http://code.google.com/webtoolkit/doc/latest/DevGuideClientBundle.html</p>
 */
public interface WorkflowActivityResources
    extends ClientBundle
{

    //~ Instance fields --------------------------------------------------------

    // The instance of the ClientBundle that must be injected during doInit()
    WorkflowActivityResources RESOURCES = GWT.create(
            WorkflowActivityResources.class);

    //~ Methods ----------------------------------------------------------------

    // Specify explicit stylesheet. Every class in the stylesheet should have a
    // function defined in WorkflowActivityStyles
    @Source("WorkflowActivity.css")
    WorkflowActivityStyles css();
}
