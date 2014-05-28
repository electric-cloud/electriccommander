
// SDKConfiguration.java --
//
// SDKConfiguration.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package com.electriccloud.commander.spring;

import org.apache.http.HttpHost;

import org.dom4j.io.SAXReader;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import com.electriccloud.commander.client.CommanderRequestFactory;
import com.electriccloud.commander.client.requests.CommanderRequest;
import com.electriccloud.commander.client.requests.impl.CommanderRequestFactoryImpl;
import com.electriccloud.commander.transport.CommanderSession;
import com.electriccloud.commander.transport.CommanderSessionFactory;
import com.electriccloud.commander.transport.CommanderSessionFactoryImpl;
import com.electriccloud.commander.transport.CommanderSessionImpl;
import com.electriccloud.commander.transport.RequestEnvelope;
import com.electriccloud.commander.transport.RequestEnvelopeImpl;
import com.electriccloud.commander.transport.RequestFormat;
import com.electriccloud.commander.transport.XmlSerializer;
import com.electriccloud.nio.NIORequestFactory;
import com.electriccloud.nio.NIORequestFactoryImpl;
import com.electriccloud.nio.SocketManager;
import com.electriccloud.nio.SocketManagerImpl;
import com.electriccloud.queue.ExecuteThreadFactory;
import com.electriccloud.security.KeystoreManager;
import com.electriccloud.security.KeystoreManagerImpl;
import com.electriccloud.service.ServiceManager;
import com.electriccloud.service.ServiceManagerImpl;
import com.electriccloud.util.EnableThreadScope;
import com.electriccloud.xml.CachingEntityResolver;
import com.electriccloud.xml.NonValidatingSAXReader;

import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;

import static com.electriccloud.commander.transport.RequestFormat.xml;

/**
 * Spring configuration for Commander SDK.
 */
@Configuration @EnableAspectJAutoProxy @EnableThreadScope
@PropertySource(
    "${CDDL2PROCEDURE_PROPERTIES:classpath:com/electriccloud/huawei/cddlconverter/cddlconverter.properties}"
)
public class SDKConfiguration
{

    //~ Instance fields --------------------------------------------------------

    @Autowired private Environment m_environment;

    //~ Methods ----------------------------------------------------------------

    @Bean public CommanderRequestFactory commanderRequestFactory()
    {
        return new CommanderRequestFactoryImpl();
    }

    @Bean public CommanderSession commanderSession()
    {
        CommanderSession commander = new CommanderSessionImpl() {
            @NotNull @Override public RequestEnvelope createRequestEnvelope(
                    CommanderRequest<?>... requests)
            {
                RequestEnvelope envelope = requestEnvelope();

                envelope.setSocketManager(socketManager());
                envelope.setHttpHost(m_host);
                envelope.setRequestFormat(xml);
                envelope.setSessionId(m_sessionId);
                envelope.addRequests(requests);
                envelope.setTargetHost(m_targetHost);

                return envelope;
            }
        };

        String   sessionId = m_environment.getProperty("COMMANDER_SESSIONID");
        String   hostname  = m_environment.getProperty("COMMANDER_HOST",
                "localhost");
        Integer  port      = m_environment.getProperty("COMMANDER_PORT",
                Integer.class, 8443);
        String   scheme    = m_environment.getProperty("COMMANDER_SCHEME",
                "https");
        HttpHost host      = new HttpHost(hostname, port, scheme);

        commander.setSessionId(sessionId);
        commander.setHttpHost(host);
        commander.setTargetHost(host);

        return commander;
    }

    @Bean public CommanderSessionFactory commanderSessionFactory()
    {
        return new CommanderSessionFactoryImpl() {
            @Override protected CommanderSession createSession(
                    @NotNull HttpHost       host,
                    @Nullable RequestFormat requestFormat,
                    HttpHost                targetHost)
            {
                CommanderSession session = commanderSession();

                session.setHttpHost(host);
                session.setRequestFormat(requestFormat);
                session.setTargetHost(targetHost);

                return session;
            }
        };
    }

    @Bean public NIORequestFactory nioRequestFactory()
    {
        return new NIORequestFactoryImpl();
    }

    @Bean public ServiceManager serviceManager()
    {
        return new ServiceManagerImpl();
    }

    @Bean public SocketManager socketManager()
    {
        SocketManager manager = new SocketManagerImpl();

        manager.setConnectTimeout(m_environment.getProperty(
                "OUTBOUND_CONNECT_TIMEOUT", Integer.class, 30000));
        manager.setIdleTimeout(m_environment.getProperty(
                "IDLE_CONNECTION_TIMEOUT", Integer.class, 300000));
        manager.setThreadFactory(new ExecuteThreadFactory("Commander"));
        manager.setMaxConnectionsPerRoute(m_environment.getProperty(
                "MAX_CONNECTIONS_PER_ROUTE", Integer.class, 20));
        manager.setMaxConnectionsTotal(m_environment.getProperty(
                "MAX_CONNECTIONS", Integer.class, 2000));

        return manager;
    }

    @Bean
    @Scope(
        value     = "thread",
        proxyMode = TARGET_CLASS
    )
    public SAXReader xmlParser()
    {
        CachingEntityResolver entityResolver = new CachingEntityResolver();

        entityResolver.setEntityLocation("conf");

        @NonNls SAXReader saxReader = new NonValidatingSAXReader();

        saxReader.setEntityResolver(entityResolver);
        saxReader.setStripWhitespaceText(true);
        saxReader.setMergeAdjacentText(true);
        saxReader.setEncoding("UTF-8");

        return saxReader;
    }

    @Bean
    @Scope("prototype")
    public XmlSerializer xmlSerializer()
    {
        return new XmlSerializer();
    }

    @Bean KeystoreManager keystoreManager()
    {
        KeystoreManager keystoreManager = new KeystoreManagerImpl();

        keystoreManager.setKeystore(m_environment.getProperty(
                "COMMANDER_KEYSTORE", "keystore"));
        keystoreManager.setKeystorePassword(m_environment.getProperty(
                "COMMANDER_KEYSTORE_PASSWORD", "abcdef"));

        return keystoreManager;
    }

    @Bean
    @Scope("prototype")
    RequestEnvelope requestEnvelope()
    {
        return new RequestEnvelopeImpl();
    }
}
