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
import java.net.URL;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.kie.services.client.api.RemoteRestRuntimeEngineFactory;
import org.kie.services.client.api.RemoteRuntimeEngineFactory;
import org.kie.services.client.api.builder.RemoteJmsRuntimeEngineBuilder;
import org.kie.services.client.api.builder.RemoteRestRuntimeEngineBuilder;
import org.kie.services.client.api.command.RemoteRuntimeEngine;

public class KieRemoteEndpoint extends DefaultEndpoint {
    private final KieRemoteConfiguration configuration;
    private RemoteRuntimeEngine runtimeEngine;

    public KieRemoteEndpoint(String uri, KieRemoteComponent component, KieRemoteConfiguration configuration) throws URISyntaxException, MalformedURLException {
        super(uri, component);
        this.configuration = configuration;

        if ("http".equalsIgnoreCase(configuration.getConnectionURL().getProtocol())) {
            RemoteRestRuntimeEngineBuilder engineBuilder = RemoteRestRuntimeEngineFactory.newRestBuilder();
            engineBuilder
                    .addUserName(configuration.getUserName())
                    .addPassword(configuration.getPassword())
                    .addUrl(configuration.getConnectionURL())
                    .addDeploymentId(configuration.getDeploymentId());

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

        } else if ("jms".equalsIgnoreCase(configuration.getConnectionURL().getProtocol())) {
            RemoteJmsRuntimeEngineBuilder engineBuilder = RemoteRestRuntimeEngineFactory.newJmsBuilder();
            engineBuilder
                    .addUserName(configuration.getUserName())
                    .addPassword(configuration.getPassword())
                    .addHostName(configuration.getConnectionURL().getHost())
                    .addJmsConnectorPort(configuration.getConnectionURL().getPort())

                    .addDeploymentId(configuration.getDeploymentId())
                    .addProcessInstanceId(configuration.getProcessInstanceId())
                    .addTimeout(configuration.getTimeout())
                    .addExtraJaxbClasses(configuration.getExtraJaxbClasses())

                    .addConnectionFactory(configuration.getConnectionFactory())
                    .addJbossServerHostName(configuration.getJbossServerHostName())
                    .addKieSessionQueue(configuration.getKieSessionQueue())
                    .addResponseQueue(configuration.getResponseQueue())
                    .addTaskServiceQueue(configuration.getTaskServiceQueue())
                    .addRemoteInitialContext(configuration.getRemoteInitialContext())

                    .addTruststoreLocation(configuration.getTruststoreLocation())
                    .addTruststorePassword(configuration.getTruststorePassword())
                    .addKeystoreLocation(configuration.getKeystoreLocation())
                    .addKeystorePassword(configuration.getKeystorePassword())

                    //  .doNotUseSsl()
                    .useSsl(configuration.isUseSsl());
                    if (configuration.isUseKeystoreAsTruststore()) {
                        engineBuilder.useKeystoreAsTruststore();
                    }

            RemoteRuntimeEngineFactory engineFactory = engineBuilder.buildFactory();
            runtimeEngine = engineFactory.newRuntimeEngine();
         } else {
            throw new UnsupportedOperationException("protocol");
        }
    }

    public Producer createProducer() throws Exception {
        return new KieRemoteProducer(this, runtimeEngine);
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("Consumer not supported for KieRemote endpoint");
    }

    public boolean isSingleton() {
        return true;
    }

    public KieRemoteConfiguration getConfiguration() {
        return configuration;
    }
}
