
// ExampleConfiguration.java --
//
// ExampleConfiguration.java is part of ElectricCommander.
//
// Copyright (c) 2005-2014 Electric Cloud, Inc.
// All rights reserved.
//

package com.electriccloud.commander.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Example configuration used with {@link SDKConfiguration}.
 *
 * <p>Will load @Component beans in the com.electriccloud.commander.spring
 * package.</p>
 */
@ComponentScan("com.electriccloud.commander.spring")
@Configuration @EnableAspectJAutoProxy public class ExampleConfiguration { }
