
// ErrorHandler.java --
//
// ErrorHandler.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package com.electriccloud.commander.spring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.electriccloud.commander.client.responses.CommanderError;
import com.electriccloud.commander.client.responses.CommanderErrorHandler;

public class ErrorHandler
    implements CommanderErrorHandler
{

    //~ Instance fields --------------------------------------------------------

    private final List<CommanderError> m_errors = Collections.synchronizedList(
            new ArrayList<CommanderError>());

    //~ Methods ----------------------------------------------------------------

    @Override public void handleError(@NotNull CommanderError error)
    {
        m_errors.add(error);
    }

    public void throwOnError()
    {

        if (m_errors.isEmpty()) {
            return;
        }

        // Find an error code that isn't BatchFailed
        for (CommanderError error : m_errors) {

            if (!"BatchFailed".equals(error.getCode())) {
                throw new CommanderException(error);
            }
        }

        throw new CommanderException(m_errors.get(0));
    }

    public Iterable<CommanderError> getErrors()
    {
        return m_errors;
    }
}
