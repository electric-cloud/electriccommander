
// CommanderException.java --
//
// CommanderException.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package com.electriccloud.commander.spring;

import com.electriccloud.commander.client.responses.CommanderError;

@SuppressWarnings("HardCodedStringLiteral")
public class CommanderException
    extends RuntimeException
{

    //~ Instance fields --------------------------------------------------------

    private final CommanderError m_error;

    //~ Constructors -----------------------------------------------------------

    public CommanderException(CommanderError error)
    {
        super(String.format("Request failed with code: [%s] message: '%s'",
                error.getCode(), error.getMessage()));
        m_error = error;
    }

    public CommanderException(Throwable cause)
    {
        super(cause);
        m_error = null;
    }

    //~ Methods ----------------------------------------------------------------

    public CommanderError getError()
    {
        return m_error;
    }
}
