
// DefaultCompletionCallback.java --
//
// DefaultCompletionCallback.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package com.electriccloud.commander.spring;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import com.electriccloud.commander.transport.CompletionCallback;
import com.electriccloud.log.Log;
import com.electriccloud.log.LogFactory;

public class DefaultCompletionCallback
    implements CompletionCallback
{

    //~ Static fields/initializers ---------------------------------------------

    /** Static log object. */
    private static final Log log = LogFactory.getLog(
            DefaultCompletionCallback.class);

    //~ Instance fields --------------------------------------------------------

    private volatile Throwable m_exception;

    //~ Methods ----------------------------------------------------------------

    @Override public void onError(Throwable e)
    {

        // HTTP request failed
        log.error(e);
        m_exception = e;
    }

    @Override public void onSuccess(@Nullable Collection<String> advisories)
    {
        // Nothing to do
    }

    public void throwOnError()
    {

        if (m_exception != null) {
            throw new CommanderException(m_exception);
        }
    }
}
