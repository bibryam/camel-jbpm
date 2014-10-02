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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;


//@Ignore("This is an integration test that needs BPMS running on the local machine")
 public class KieRemoteComponentTest extends CamelTestSupport {

    @Test
    public void interactsOverRest() throws Exception {
        template.sendBody("direct:rest", null);
    }

    @Test
    public void interactsOverJMS() throws Exception {
        template.sendBody("direct:jms", null);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() {

                from("direct:rest")
                        .setHeader(KieRemoteConstants.PROCESS_ID, constant("customer.evaluation"))
                        .to("kie-remote:http://127.0.0.1:8080/business-central?userName=erics&password=bpmsuite&deploymentId=customer:evaluation:1.0")
                        .to("log:com.ofbizian.jbpm?showAll=true&multiline=true");


                from("direct:jms")
                        .setHeader(KieRemoteConstants.PROCESS_ID, constant("customer.evaluation"))
                        .to("kie-remote:127.0.0.1:5445?userName=erics&password=bpmsuite&deploymentId=customer:evaluation:1.0&timeout=5")
                        .to("log:com.ofbizian.jbpm?showAll=true&multiline=true");
            }
        };
    }
}
