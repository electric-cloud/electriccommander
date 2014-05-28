
// ExampleBean.java --
//
// ExampleBean.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package com.electriccloud.commander.spring;

import org.jetbrains.annotations.Nullable;

import org.springframework.stereotype.Component;

import com.electriccloud.commander.client.domain.Project;
import com.electriccloud.commander.client.requests.CreateProjectRequest;
import com.electriccloud.commander.client.responses.DefaultProjectCallback;

@Component public class ExampleBean
    extends SDKSupport
{

    //~ Methods ----------------------------------------------------------------

    public void createProject(String projectName)
        throws InterruptedException
    {

        // Create a project, e.g.
        CreateProjectRequest createProject =
            m_factory.createCreateProjectRequest()
                     .setProjectName(projectName)
                     .setCallback(new DefaultProjectCallback(m_errorHandler) {
                             @Override public void handleResponse(
                                     @Nullable Project response)
                             {
                                 // Do something with the project, if you want.
                             }
                         });

        // Send the request -- will block until complete, invoking the
        // callback/error handler as appropriate.
        sendRequests(createProject);
    }
}
