Camel jBPM Component
====================
A Camel component that uses kie-remote-client API for interacting with jBPM.

There is also OSGI features allowing to deploy the component on OSGI container such as Fuse 6.2

The component supports the following operations
===============================================

Process operations: *START_PROCESS, ABORT_PROCESS_INSTANCE, SIGNAL_EVENT, GET_PROCESS_INSTANCE, GET_PROCESS_INSTANCES*

Rule operations: *FIRE_ALL_RULES, GET_FACT_COUNT, GET_GLOBAL, SET_GLOBAL*

Work Item operations: *ABORT_WORK_ITEM, COMPLETE_WORK_ITEM*

Task Operations: *ACTIVATE_TASK, ADD_TASK, CLAIM_NEXT_AVAILABLE_TASK, CLAIM_TASK, COMPLETE_TASK, DELEGATE_TASK, EXIT_TASK,
FAIL_TASK, GET_ATTACHMENT, GET_CONTENT, GET_TASK_ASSIGNED_AS_BUSINESS_ADMIN, GET_TASK_ASSIGNED_AS_POTENTIAL_OWNER,
GET_TASK_BY_WORK_ITEM_ID, GET_TASK, GET_TASK_CONTENT, GET_TASKS_BY_PROCESS_INSTANCE_ID, GET_TASKS_BY_STATUS_BY_PROCESS_INSTANCE_ID,
GET_TASKS_OWNED, NOMINATE_TASK, RELEASE_TASK, RESUME_TASK, SKIP_TASK, RESUME_TASK, START_TASK, STOP_TASK, SUSPEND_TASK*



Running the demo on Fuse 6.2 (or 6.1)
=====================================
  
Start BPMS using [Docker image](https://github.com/bibryam/dockerfiles/tree/master/eap-bpms) and [clone](https://github.com/bibryam/bpmsuite-customer-evaluation-repo.git) a jbpm process, build and deploy it. Then deploy the demo feature to Fuse and it will start creating process instances.

    JBossFuse:admin@root> features:addurl mvn:com.ofbizian/camel-jbpm-features/1.0.0/xml/features 
    JBossFuse:admin@root> features:install camel-jbpm-demo/1.0.0
    JBossFuse:admin@root> log:tail

 
Example route
=============

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
                    from("direct:rest")
                            .setHeader(JBPMConstants.PROCESS_ID, constant("customer.evaluation"))
                            .to("jbpm:http://127.0.0.1:8080/business-central?userName=erics&password=bpmsuite&deploymentId=customer:evaluation:1.0")
                            .to("log:com.ofbizian.jbpm?showAll=true&multiline=true");
                }
            };
        }
    }

License
=======
ASLv2
