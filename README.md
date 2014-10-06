Camel jBPM Component
====================
A Camel component that uses kie-remote-client API for interacting with jBPM over rest or jms.

There is also OSGI features allowing to deploy the component on OSGI container such as Fuse 6.1
(In OSGI environment the jms connectivity currently doesn't work, it only supports RESTS API).


The component supports the following operations:
================================================

Process operations: *START_PROCESS, ABORT_PROCESS_INSTANCE, SIGNAL_EVENT, GET_PROCESS_INSTANCE, GET_PROCESS_INSTANCES*

Rule operations: *FIRE_ALL_RULES, GET_FACT_COUNT, GET_GLOBAL, SET_GLOBAL*

Work Item operations: *ABORT_WORK_ITEM, COMPLETE_WORK_ITEM*

Task Operations: *ACTIVATE_TASK, ADD_TASK, CLAIM_NEXT_AVAILABLE_TASK, CLAIM_TASK, COMPLETE_TASK, DELEGATE_TASK, EXIT_TASK,
FAIL_TASK, GET_ATTACHMENT, GET_CONTENT, GET_TASK_ASSIGNED_AS_BUSINESS_ADMIN, GET_TASK_ASSIGNED_AS_POTENTIAL_OWNER,
GET_TASK_BY_WORK_ITEM_ID, GET_TASK, GET_TASK_CONTENT, GET_TASKS_BY_PROCESS_INSTANCE_ID, GET_TASKS_BY_STATUS_BY_PROCESS_INSTANCE_ID,
GET_TASKS_OWNED, NOMINATE_TASK, RELEASE_TASK, RESUME_TASK, SKIP_TASK, RESUME_TASK, START_TASK, STOP_TASK, SUSPEND_TASK*


Installing the demo module on Fuse 6.1
======================================
    fabric:create --wait-for-provisioning

    fabric:profile-edit --repositories mvn:com.ofbizian/camel-jbpm-features/1.1.0/xml/features default
    fabric:profile-create --parents feature-camel camel-jbpm-demo-profile
    fabric:profile-edit --features camel-jbpm-demo/1.1.0 camel-jbpm-demo-profile
    container-create-child --profile camel-jbpm-demo-profile root camel-jbpm-demo-container


Example route
=============


    public class JBPMComponentTest extends CamelTestSupport {

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
                            .setHeader(JBPMConstants.PROCESS_ID, constant("customer.evaluation"))
                            .to("jbpm:http://127.0.0.1:8080/business-central?userName=erics&password=bpmsuite&deploymentId=customer:evaluation:1.0")
                            .to("log:com.ofbizian.jbpm?showAll=true&multiline=true");


                    from("direct:jms")
                            .setHeader(JBPMConstants.PROCESS_ID, constant("customer.evaluation"))
                            .to("jbpm:127.0.0.1:5445?userName=erics&password=bpmsuite&deploymentId=customer:evaluation:1.0&timeout=5")
                            .to("log:com.ofbizian.jbpm?showAll=true&multiline=true");
                }
            };
        }
    }


License
=======
ASLv2


TODO
====
Write unit tests
