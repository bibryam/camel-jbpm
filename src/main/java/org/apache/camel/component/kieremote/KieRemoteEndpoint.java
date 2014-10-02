/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.kieremote;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.hornetq.jms.client.HornetQQueue;
import org.kie.services.client.api.RemoteRestRuntimeEngineFactory;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.api.builder.RemoteJmsRuntimeEngineBuilder;
import org.kie.services.client.api.builder.RemoteRestRuntimeEngineBuilder;
import org.kie.services.client.api.command.RemoteConfiguration;
import org.kie.services.client.api.command.RemoteRuntimeEngine;

public class KieRemoteEndpoint extends DefaultEndpoint {
    private final KieRemoteConfiguration configuration;
    private RemoteRuntimeEngine runtimeEngine;

    public KieRemoteEndpoint(String uri, KieRemoteComponent component, KieRemoteConfiguration configuration) throws URISyntaxException, MalformedURLException {
        super(uri, component);
        this.configuration = configuration;

        if (configuration.getConnectionURL() != null) {
            RemoteRestRuntimeEngineBuilder engineBuilder = RemoteRestRuntimeEngineFactory.newRestBuilder();

            if (configuration.getUserName() != null) {
                engineBuilder.addUserName(configuration.getUserName());
            }
            if (configuration.getPassword() != null) {
                engineBuilder.addPassword(configuration.getPassword());
            }
            if (configuration.getDeploymentId() != null) {
                engineBuilder.addDeploymentId(configuration.getDeploymentId());
            }
            if (configuration.getConnectionURL() != null) {
                engineBuilder.addUrl(configuration.getConnectionURL());
            }
            if (configuration.getProcessInstanceId() != null) {
                engineBuilder.addProcessInstanceId(configuration.getProcessInstanceId());
            }
            if (configuration.getTimeout() != null) {
                engineBuilder.addTimeout(configuration.getTimeout());
            }
            if (configuration.getExtraJaxbClasses() != null) {
                engineBuilder.addExtraJaxbClasses(configuration.getExtraJaxbClasses());
            }

            RemoteRuntimeEngineFactory engineFactory = engineBuilder.buildFactory();
            runtimeEngine = engineFactory.newRuntimeEngine();

        } else {
            RemoteJmsRuntimeEngineBuilder engineBuilder = RemoteRestRuntimeEngineFactory.newJmsBuilder();

            if (configuration.getUserName() != null) {
                engineBuilder.addUserName(configuration.getUserName());
            }
            if (configuration.getPassword() != null) {
                engineBuilder.addPassword(configuration.getPassword());
            }
            if (configuration.getDeploymentId() != null) {
                engineBuilder.addDeploymentId(configuration.getDeploymentId());
            }
            if (configuration.getHost() != null) {
                engineBuilder.addHostName(configuration.getHost());
            }
            if (configuration.getPort() != null) {
                engineBuilder.addJmsConnectorPort(configuration.getPort());
            }

            //options
            if (configuration.getProcessInstanceId() != null) {
                engineBuilder.addProcessInstanceId(configuration.getProcessInstanceId());
            }
            if (configuration.getTimeout() != null) {
                engineBuilder.addTimeout(configuration.getTimeout());
            }
            if (configuration.getExtraJaxbClasses() != null) {
                engineBuilder.addExtraJaxbClasses(configuration.getExtraJaxbClasses());
            }
            if (configuration.getConnectionFactory() != null) {
                engineBuilder.addConnectionFactory(configuration.getConnectionFactory());
            }
            if (configuration.getJbossServerHostName() != null) {
                engineBuilder.addJbossServerHostName(configuration.getJbossServerHostName());
            }

            engineBuilder.addKieSessionQueue(toHornetQQueue(
                    configuration.getSessionQueue() != null ? configuration.getSessionQueue() : RemoteConfiguration.SESSION_QUEUE_NAME));
            engineBuilder.addResponseQueue(toHornetQQueue(
                    configuration.getResponseQueue() != null ? configuration.getResponseQueue() : RemoteConfiguration.RESPONSE_QUEUE_NAME));
            engineBuilder.addTaskServiceQueue(toHornetQQueue(
                    configuration.getTaskServiceQueue() != null ? configuration.getTaskServiceQueue() : RemoteConfiguration.TASK_QUEUE_NAME));

            if (configuration.getRemoteInitialContext() != null) {
                engineBuilder.addRemoteInitialContext(configuration.getRemoteInitialContext());
            }
            if (configuration.getTruststoreLocation() != null) {
                engineBuilder.addTruststoreLocation(configuration.getTruststoreLocation());
            }
            if (configuration.getTruststorePassword() != null) {
                engineBuilder.addTruststorePassword(configuration.getTruststorePassword());
            }
            if (configuration.getKeystoreLocation() != null) {
                engineBuilder.addKeystoreLocation(configuration.getKeystoreLocation());
            }
            if (configuration.getKeystorePassword() != null) {
                engineBuilder.addKeystorePassword(configuration.getKeystorePassword());
            }
            if (configuration.isUseSsl()) {
                engineBuilder.useSsl(configuration.isUseSsl());
            }
            if (configuration.isUseKeystoreAsTruststore()) {
                engineBuilder.useKeystoreAsTruststore();
            }

            RemoteRuntimeEngineFactory engineFactory = engineBuilder.buildFactory();
            runtimeEngine = engineFactory.newRuntimeEngine();
         }
    }

    private HornetQQueue toHornetQQueue(String queueName) {
        return new HornetQQueue(queueName);
    }

    public Producer createProducer() throws Exception {
        return new KieRemoteProducer(this, runtimeEngine);
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("Consumer not supported for " + getClass().getSimpleName() + " endpoint");
    }

    public boolean isSingleton() {
        return true;
    }

    public KieRemoteConfiguration getConfiguration() {
        return configuration;
    }
}
