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

package org.apache.camel.component.jbpm;

import java.util.HashMap;
import java.util.Map;

import customer.evaluation.Person;
import customer.evaluation.Request;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("This is an integration test that needs BPMS running on the local machine")
public class JBPMComponentIntegrationTest extends CamelTestSupport {

    @Test
    public void interactsOverRest() throws Exception {
        template.sendBody("direct:rest", null);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {

            @Override
            public void configure() {
                Request request = new Request("1");
                request.setPersonId("Demo");
                request.setAmount(5000);

                Person person = new Person("Demo", "User");
                person.setAge(19);

                Map map = new HashMap();
                map.put("request", request);
                map.put("person", person);

                from("direct:rest")
                        .setHeader(JBPMConstants.PROCESS_ID, constant("customer.evaluation"))
                        .setHeader(JBPMConstants.PARAMETERS, constant(map))
                        .to("jbpm:http://localhost:8080/business-central?userName=bpmsAdmin&password=pa$word1&deploymentId=customer:evaluation:1.0")
                        .to("log:com.ofbizian.jbpm?showAll=true&multiline=true");
            }
        };
    }
}
