
// DefaultCallback.java --
//
// DefaultCallback.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package com.electriccloud.commander.spring;

import org.jetbrains.annotations.Nullable;

import com.electriccloud.commander.client.responses.CommanderErrorHandler;
import com.electriccloud.commander.client.responses.CommanderObject;
import com.electriccloud.commander.client.responses.DefaultCommanderObjectCallback;

public class DefaultCallback
    extends DefaultCommanderObjectCallback
{

    //~ Constructors -----------------------------------------------------------

    public DefaultCallback(CommanderErrorHandler errorHandler)
    {
        super(errorHandler);
    }

    //~ Methods ----------------------------------------------------------------

    @Override public void handleResponse(@Nullable CommanderObject response)
    {
        // Ignore response
    }
}
