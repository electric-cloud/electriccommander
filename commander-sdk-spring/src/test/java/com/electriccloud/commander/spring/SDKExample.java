
// SDKExample.java --
//
// SDKExample.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package com.electriccloud.commander.spring;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import com.electriccloud.log.Log;
import com.electriccloud.log.LogFactory;
import com.electriccloud.nio.SocketManager;
import com.electriccloud.service.ServiceManager;

public class SDKExample
{

    //~ Static fields/initializers ---------------------------------------------

    /** Static log object. */
    private static final Log log = LogFactory.getLog(SDKExample.class);

    //~ Instance fields --------------------------------------------------------

    @NonNls @NotNull private final AnnotationConfigApplicationContext m_context;

    //~ Constructors -----------------------------------------------------------

    /**
     * Instantiate the Commander SDK using Spring.
     *
     * @param   url  The commander server url.
     *
     * @throws  InterruptedException
     */
    SDKExample(@Nullable URL url)
        throws InterruptedException
    {

        // Create the Spring context object from the Spring
        // @Configuration-annotated classes.
        // noinspection resource
        m_context = new AnnotationConfigApplicationContext();
        m_context.setId("SDKExample");
        m_context.setDisplayName("SDKExample");
        m_context.register(SDKConfiguration.class, ExampleConfiguration.class);

        // Collect properties into a map to be passed into the Spring
        // environment.
        @NonNls Builder<String, Object> builder = ImmutableMap.builder();

        // If an url was provided, use that. Otherwise, it will default to
        // https://localhost:8443
        if (url != null) {
            builder.put("COMMANDER_HOST", url.getHost())
                   .put("COMMANDER_PORT", Integer.toString(url.getPort()))
                   .put("COMMANDER_SCHEME", url.getProtocol());
        }

        Map<String, Object> properties = builder.build();

        m_context.getEnvironment()
                 .getPropertySources()
                 .addFirst(new MapPropertySource("arguments", properties));

        // This call does the actual Spring initialization
        m_context.refresh();

        // Get the service manager bean and start the SocketManager, which
        // handles the HTTP connections to the Commander server.
        ServiceManager serviceManager = m_context.getBean(ServiceManager.class);

        serviceManager.start(SocketManager.class, null);

        // Register a shutdown hook to attempt some orderly shutdown in the
        // event that someone kills the VM
        Runtime.getRuntime()
               .addShutdownHook(new Thread(
                       new ShutdownHelper(m_context, serviceManager),
                       "SDKExample-Shutdown"));

        //
        log.info("SDKExample has been loaded");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Shut down the context and clean up.
     */
    public void close()
    {
        m_context.close();
    }

    public <T> T getBean(@NotNull Class<T> cls)
    {
        return m_context.getBean(cls);
    }

    //~ Methods ----------------------------------------------------------------

    public static void main(String[] args)
        throws MalformedURLException, InterruptedException
    {
        SDKExample sdkExample = new SDKExample(new URL(
                    "https://localhost:8443"));

        try {
            ExampleBean bean = sdkExample.getBean(ExampleBean.class);

            bean.login("admin", "changeme");
            bean.createProject("Default");
            bean.logout();
        }
        finally {
            sdkExample.close();
        }
    }
}
