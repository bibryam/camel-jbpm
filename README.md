Camel jBPM Component
====================
A Camel component that uses kie-remote-client API for interacting with jBPM over rest or jms.


Supports the following operations:
============================================

Process operations: *START_PROCESS, ABORT_PROCESS_INSTANCE, SIGNAL_EVENT, GET_PROCESS_INSTANCE, GET_PROCESS_INSTANCES*

Rule operations: *FIRE_ALL_RULES, GET_FACT_COUNT, GET_GLOBAL, SET_GLOBAL*

Work Item operations: *ABORT_WORK_ITEM, COMPLETE_WORK_ITEM*

Task Operations: *ACTIVATE_TASK, ADD_TASK, CLAIM_NEXT_AVAILABLE_TASK, CLAIM_TASK, COMPLETE_TASK, DELEGATE_TASK, EXIT_TASK,
FAIL_TASK, GET_ATTACHMENT, GET_CONTENT, GET_TASK_ASSIGNED_AS_BUSINESS_ADMIN, GET_TASK_ASSIGNED_AS_POTENTIAL_OWNER,
GET_TASK_BY_WORK_ITEM_ID, GET_TASK, GET_TASK_CONTENT, GET_TASKS_BY_PROCESS_INSTANCE_ID, GET_TASKS_BY_STATUS_BY_PROCESS_INSTANCE_ID,
GET_TASKS_OWNED, NOMINATE_TASK, RELEASE_TASK, RESUME_TASK, SKIP_TASK, RESUME_TASK, START_TASK, STOP_TASK, SUSPEND_TASK*


Author
======
Bilgin Ibryam  twitter: @bibryam


License
=======
ASLv2


Example Usage
=============


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



TODO
====
Write unit tests

