
// ShutdownHelper.java --
//
// ShutdownHelper.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package com.electriccloud.commander.spring;

import org.springframework.context.ConfigurableApplicationContext;

import com.electriccloud.log.Log;
import com.electriccloud.log.LogFactory;
import com.electriccloud.log.LogUtil;
import com.electriccloud.service.ServiceManager;

/**
 * Shutdown hook called by the VM when it exits.
 */
public class ShutdownHelper
    implements Runnable
{

    //~ Static fields/initializers ---------------------------------------------

    /** Static log object. */
    private static final Log log = LogFactory.getLog(ShutdownHelper.class);

    //~ Instance fields --------------------------------------------------------

    private final ConfigurableApplicationContext m_context;
    private final ServiceManager                 m_serviceManager;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Shutdown object.
     *
     * @param  context         The spring context. Will be closed by the
     *                         shutdown hook.
     * @param  serviceManager
     */
    ShutdownHelper(
            ConfigurableApplicationContext context,
            ServiceManager                 serviceManager)
    {
        m_context        = context;
        m_serviceManager = serviceManager;
    }

    //~ Methods ----------------------------------------------------------------

    @Override public void run()
    {

        // Stop services
        try {
            m_serviceManager.stop();
        }
        catch (InterruptedException ignored) { // Restore interrupted status
            Thread.currentThread()
                  .interrupt();

            return;
        }

        // Shut down the spring context
        m_context.close();

        // Last log entry
        log.info("SDKExample is unloading");

        // Shutdown logging
        LogUtil.stop();
    }
}
