
// SDKSupport.java --
//
// SDKSupport.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package com.electriccloud.commander.spring;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.Nullable;

import org.springframework.beans.factory.annotation.Autowired;

import com.electriccloud.commander.client.CommanderRequestFactory;
import com.electriccloud.commander.client.requests.CommanderRequest;
import com.electriccloud.commander.client.responses.DefaultLoginResponseCallback;
import com.electriccloud.commander.client.responses.LoginResponse;
import com.electriccloud.commander.transport.CommanderSession;

import static java.util.concurrent.TimeUnit.MINUTES;

import static com.electriccloud.commander.client.domain.BatchMode.single;

public class SDKSupport
{

    //~ Instance fields --------------------------------------------------------

    @Autowired protected CommanderRequestFactory m_factory;
    @Autowired protected CommanderSession        m_commander;

    /** Used to turn errors into exceptions. */
    protected final ErrorHandler m_errorHandler = new ErrorHandler();

    /** Used to handle communication errors. */
    protected final DefaultCompletionCallback m_completionCallback =
        new DefaultCompletionCallback();

    //~ Methods ----------------------------------------------------------------

    public String login(
            String username,
            String password)
        throws InterruptedException
    {
        final AtomicReference<String> sessionIdRef =
            new AtomicReference<String>("");
        CommanderRequest<?>           login        = //
            m_factory.createLoginRequest()
                     .setUserName(username)
                     .setPassword(password)
                     .setCallback(new DefaultLoginResponseCallback(
                             m_errorHandler) {
                             @Override public void handleResponse(
                                     @Nullable LoginResponse response)
                             {

                                 if (response != null) {
                                     sessionIdRef.set(response.getSessionId());
                                 }
                             }
                         });

        sendRequests(login);
        m_commander.setSessionId(sessionIdRef.get());

        return sessionIdRef.get();
    }

    public void logout()
        throws InterruptedException
    {
        CommanderRequest<?> logout = //
            m_factory.createLogoutRequest()
                     .setCallback(new DefaultCallback(m_errorHandler));

        sendRequests(logout);
    }

    /**
     * Send request to the commander server. Throws CommanderException if there
     * are any problems.
     *
     * @param   requests
     *
     * @throws  InterruptedException
     */
    void sendRequests(Collection<CommanderRequest<?>> requests)
        throws InterruptedException
    {
        m_commander.createRequestEnvelope()
                   .addRequests(requests)
                   .setBatchMode(single)
                   .setCompletionCallback(m_completionCallback)
                   .setTimeout(Long.valueOf(MINUTES.toSeconds(10))
                                   .intValue())
                   .sendAndWait(10, MINUTES);
        m_completionCallback.throwOnError();
        m_errorHandler.throwOnError();
    }

    /**
     * Send request to the commander server. Throws CommanderException if there
     * are any problems.
     *
     * @param   requests
     *
     * @throws  InterruptedException
     */
    void sendRequests(CommanderRequest<?>... requests)
        throws InterruptedException
    {
        m_commander.createRequestEnvelope()
                   .addRequests(requests)
                   .setBatchMode(single)
                   .setCompletionCallback(m_completionCallback)
                   .setTimeout(Long.valueOf(MINUTES.toSeconds(10))
                                   .intValue())
                   .sendAndWait(10, MINUTES);
        m_completionCallback.throwOnError();
        m_errorHandler.throwOnError();
    }
}
