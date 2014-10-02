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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ExchangeHelper;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Attachment;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.services.client.api.command.RemoteRuntimeEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieRemoteProducer extends DefaultProducer {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(KieRemoteProducer.class);
    private KieSession kieSession;
    private TaskService taskService;
    private KieRemoteConfiguration configuration;
    private RemoteRuntimeEngine runtimeEngine;

    public KieRemoteProducer(KieRemoteEndpoint endpoint, RemoteRuntimeEngine runtimeEngine) {
        super(endpoint);
        this.configuration = endpoint.getConfiguration();
        this.runtimeEngine = runtimeEngine;
    }

    @Override
    protected void doStart() throws Exception {
        kieSession = runtimeEngine.getKieSession();
        taskService = runtimeEngine.getTaskService();
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (kieSession != null) {
            kieSession = null;
        }

        if (taskService != null) {
            taskService = null;
        }
    }

    public void process(Exchange exchange) throws Exception {
        getOperation(exchange).execute(kieSession, taskService, configuration, exchange);
    }

    Operation getOperation(Exchange exchange) {
        String operation = exchange.getIn().getHeader(KieRemoteConstants.OPERATION, String.class);
        if (operation == null && configuration.getOperation() != null) {
            operation = KieRemoteConstants.OPERATION + configuration.getOperation();
        }
        if (operation == null) {
            operation = KieRemoteConstants.OPERATION + Operation.START_PROCESS;
        }
        LOGGER.trace("Operation: [{}]", operation);
        return Operation.valueOf(operation.substring(KieRemoteConstants.OPERATION.length()).toUpperCase());
    }

    enum Operation {

        //PROCESS OPERATIONS
        START_PROCESS {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                ProcessInstance processInstance = kieSession.startProcess(getProcessId(configuration, exchange), getParameters(configuration, exchange));
                setProcessInstanceResult(exchange, processInstance);
            }
        }, ABORT_PROCESS_INSTANCE {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                kieSession.abortProcessInstance(safe(getProcessInstanceId(configuration, exchange)));
            }
        }, SIGNAL_EVENT {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                Long processInstanceId = getProcessInstanceId(configuration, exchange);
                if (processInstanceId != null) {
                    kieSession.signalEvent(getEventType(configuration, exchange), getEvent(configuration, exchange), processInstanceId);
                } else {
                    kieSession.signalEvent(getEventType(configuration, exchange), getEvent(configuration, exchange));
                }
            }
        }, GET_PROCESS_INSTANCE {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                ProcessInstance processInstance = kieSession.getProcessInstance(safe(getProcessInstanceId(configuration, exchange)));
                setProcessInstanceResult(exchange, processInstance);
            }
        }, GET_PROCESS_INSTANCES {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                Collection<ProcessInstance> processInstances = kieSession.getProcessInstances();
                setResult(exchange, processInstances);
            }
        },

        //RULE OPERATIONS
        FIRE_ALL_RULES {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                Integer max = getMaxNumber(configuration, exchange);
                int rulesFired;
                if (max != null) {
                    rulesFired = kieSession.fireAllRules(max);
                } else {
                    rulesFired = kieSession.fireAllRules();
                }
                setResult(exchange, rulesFired);
            }
        }, GET_FACT_COUNT {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                long factCount = kieSession.getFactCount();
                setResult(exchange, factCount);
            }
        }, GET_GLOBAL {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                Object global = kieSession.getGlobal(getIdentifier(configuration, exchange));
                setResult(exchange, global);
            }
        }, SET_GLOBAL {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                kieSession.setGlobal(getIdentifier(configuration, exchange), getValue(configuration, exchange));
            }
        },

        //WORK ITEM OPERATIONS
        ABORT_WORK_ITEM {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                kieSession.getWorkItemManager().abortWorkItem(safe(getWorkItemId(configuration, exchange)));
            }
        }, COMPLETE_WORK_ITEM {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                kieSession.getWorkItemManager().completeWorkItem(safe(getWorkItemId(configuration, exchange)), getParameters(configuration, exchange));
            }
        },

        //TASK OPERATIONS
        ACTIVATE_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.activate(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange));
            }
        }, ADD_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                long taskId = taskService.addTask(getTask(configuration, exchange), getParameters(configuration, exchange));
                setResult(exchange, taskId);
            }
        }, CLAIM_NEXT_AVAILABLE_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.claimNextAvailable(getUserId(configuration, exchange), getLanguage(configuration, exchange));
            }
        }, CLAIM_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.claim(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange));
            }
        }, COMPLETE_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.complete(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange), getParameters(configuration, exchange));
            }
        }, DELEGATE_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.delegate(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange), getTargetUserId(configuration, exchange));
            }
        }, EXIT_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.exit(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange));
            }
        }, FAIL_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.fail(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange), getParameters(configuration, exchange));
            }
        }, GET_ATTACHMENT {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                Attachment attachment = taskService.getAttachmentById(safe(getAttachmentId(configuration, exchange)));
                setResult(exchange, attachment);
            }
        }, GET_CONTENT {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                Content content = taskService.getContentById(safe(getContentId(configuration, exchange)));
                setResult(exchange, content);
            }
        }, GET_TASK_ASSIGNED_AS_BUSINESS_ADMIN {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                List<TaskSummary> taskSummaries = taskService.getTasksAssignedAsBusinessAdministrator(getUserId(configuration, exchange), getLanguage(configuration, exchange));
                setResult(exchange, taskSummaries);
            }
        }, GET_TASK_ASSIGNED_AS_POTENTIAL_OWNER {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.getTasksAssignedAsPotentialOwnerByStatus(getUserId(configuration, exchange), getStatuses(configuration, exchange), getLanguage(configuration, exchange));
            }
        }, GET_TASK_BY_WORK_ITEM_ID {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                Task task = taskService.getTaskByWorkItemId(safe(getWorkItemId(configuration, exchange)));
                setResult(exchange, task);
            }
        }, GET_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                Task task = taskService.getTaskById(safe(getTaskId(configuration, exchange)));
                setResult(exchange, task);
            }
        }, GET_TASK_CONTENT {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                Map<String, Object> taskContent = taskService.getTaskContent(safe(getTaskId(configuration, exchange)));
                setResult(exchange, taskContent);
            }
        }, GET_TASKS_BY_PROCESS_INSTANCE_ID {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                List<Long> processInstanceIds = taskService.getTasksByProcessInstanceId(safe(getProcessInstanceId(configuration, exchange)));
                setResult(exchange, processInstanceIds);
            }
        }, GET_TASKS_BY_STATUS_BY_PROCESS_INSTANCE_ID {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                List<TaskSummary> taskSummaryList = taskService.getTasksByStatusByProcessInstanceId(safe(getProcessInstanceId(configuration, exchange)), getStatuses(configuration, exchange), getLanguage(configuration, exchange));
                setResult(exchange, taskSummaryList);
            }
        }, GET_TASKS_OWNED {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                List<TaskSummary> summaryList = taskService.getTasksOwned(getUserId(configuration, exchange), getLanguage(configuration, exchange));
                setResult(exchange, summaryList);
            }
        }, NOMINATE_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.nominate(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange), getEntities(configuration, exchange));
            }
        }, RELEASE_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.release(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange));
            }
        }, RESUME_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.resume(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange));
            }
        }, SKIP_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.skip(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange));
            }
        }, START_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.start(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange));
            }
        }, STOP_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.stop(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange));
            }
        }, SUSPEND_TASK {
            @Override
            void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange) {
                taskService.suspend(safe(getTaskId(configuration, exchange)), getUserId(configuration, exchange));
            }
        };

        List<Status> getStatuses(KieRemoteConfiguration configuration, Exchange exchange) {
            List<Status> statusList = exchange.getIn().getHeader(KieRemoteConstants.STATUS_LIST, List.class);
            if (statusList == null) {
                statusList = configuration.getStatuses();
            }
            return statusList;
        }

        List<OrganizationalEntity> getEntities(KieRemoteConfiguration configuration, Exchange exchange) {
            List<OrganizationalEntity> entityList = exchange.getIn().getHeader(KieRemoteConstants.ENTITY_LIST, List.class);
            if (entityList == null) {
                entityList = configuration.getEntities();
            }
            return entityList;
        }

        Long getAttachmentId(KieRemoteConfiguration configuration, Exchange exchange) {
            Long attachmentId = exchange.getIn().getHeader(KieRemoteConstants.ATTACHMENT_ID, Long.class);
            if (attachmentId == null) {
                attachmentId = configuration.getAttachmentId();
            }
            return attachmentId;
        }

        Long getContentId(KieRemoteConfiguration configuration, Exchange exchange) {
            Long contentId = exchange.getIn().getHeader(KieRemoteConstants.CONTENT_ID, Long.class);
            if (contentId == null) {
                contentId = configuration.getContentId();
            }
            return contentId;
        }

        String getTargetUserId(KieRemoteConfiguration configuration, Exchange exchange) {
            String userId = exchange.getIn().getHeader(KieRemoteConstants.TARGET_USER_ID, String.class);
            if (userId == null) {
                userId = configuration.getTargetUserId();
            }
            return userId;
        }

        String getLanguage(KieRemoteConfiguration configuration, Exchange exchange) {
            String language = exchange.getIn().getHeader(KieRemoteConstants.LANGUAGE, String.class);
            if (language == null) {
                language = configuration.getLanguage();
            }
            return language;
        }

        Task getTask(KieRemoteConfiguration configuration, Exchange exchange) {
            Task task = exchange.getIn().getHeader(KieRemoteConstants.TASK, Task.class);
            if (task == null) {
                task = configuration.getTask();
            }
            return task;
        }

        String getUserId(KieRemoteConfiguration configuration, Exchange exchange) {
            String userId = exchange.getIn().getHeader(KieRemoteConstants.USER_ID, String.class);
            if (userId == null) {
                userId = configuration.getUserId();
            }
            return userId;
        }

        long safe(Long aLong) {
            return aLong != null ? aLong : 0;
        }

        Long getTaskId(KieRemoteConfiguration configuration, Exchange exchange) {
            Long taskId = exchange.getIn().getHeader(KieRemoteConstants.TASK_ID, Long.class);
            if (taskId == null) {
                taskId = configuration.getTaskId();
            }
            return taskId;
        }

        Long getWorkItemId(KieRemoteConfiguration configuration, Exchange exchange) {
            Long workItemId = exchange.getIn().getHeader(KieRemoteConstants.WORK_ITEM_ID, Long.class);
            if (workItemId == null) {
                workItemId = configuration.getWorkItemId();
            }
            return workItemId;
        }

        String getIdentifier(KieRemoteConfiguration configuration, Exchange exchange) {
            String identifier = exchange.getIn().getHeader(KieRemoteConstants.IDENTIFIER, String.class);
            if (identifier == null) {
                identifier = configuration.getIdentifier();
            }
            return identifier;
        }

        Integer getMaxNumber(KieRemoteConfiguration configuration, Exchange exchange) {
            Integer max = exchange.getIn().getHeader(KieRemoteConstants.MAX_NUMBER, Integer.class);
            if (max == null) {
                max = configuration.getMaxNumber();
            }
            return max;
        }

        Object getEvent(KieRemoteConfiguration configuration, Exchange exchange) {
            String event = exchange.getIn().getHeader(KieRemoteConstants.EVENT, String.class);
            if (event == null) {
                event = configuration.getEvent();
            }
            return event;
        }

        String getEventType(KieRemoteConfiguration configuration, Exchange exchange) {
            String eventType = exchange.getIn().getHeader(KieRemoteConstants.EVENT_TYPE, String.class);
            if (eventType == null) {
                eventType = configuration.getEventType();
            }
            return eventType;
        }

        String getProcessId(KieRemoteConfiguration configuration, Exchange exchange) {
            String processId = exchange.getIn().getHeader(KieRemoteConstants.PROCESS_ID, String.class);
            if (processId == null) {
                processId = configuration.getProcessId();
            }
            return processId;
        }

        Long getProcessInstanceId(KieRemoteConfiguration configuration, Exchange exchange) {
            Long processInstanceId = exchange.getIn().getHeader(KieRemoteConstants.PROCESS_INSTANCE_ID, Long.class);
            if (processInstanceId == null) {
                processInstanceId = configuration.getProcessInstanceId();
            }
            return processInstanceId;
        }

        Map<String, Object> getParameters(KieRemoteConfiguration configuration, Exchange exchange) {
            Map<String, Object> parameters = exchange.getIn().getHeader(KieRemoteConstants.PARAMETERS, Map.class);
            if (parameters == null) {
                parameters = configuration.getParameters();
            }
            return parameters;
        }

        Object getValue(KieRemoteConfiguration configuration, Exchange exchange) {
            Object value = exchange.getIn().getHeader(KieRemoteConstants.VALUE);
            if (value == null) {
                value = configuration.getValue();
            }
            return value;
        }

        Message getResultMessage(Exchange exchange) {
            return ExchangeHelper.isOutCapable(exchange) ? exchange.getOut() : exchange.getIn();
        }

        void setProcessInstanceResult(Exchange exchange, ProcessInstance processInstance) {
            if (processInstance != null) {
                getResultMessage(exchange).setHeader(KieRemoteConstants.PROCESS_INSTANCE_ID, processInstance.getId());
                getResultMessage(exchange).setHeader(KieRemoteConstants.PROCESS_ID, processInstance.getProcessId());
                getResultMessage(exchange).setHeader(KieRemoteConstants.STATE, processInstance.getState());
            }
        }

        void setResult(Exchange exchange, Object result) {
            getResultMessage(exchange).setBody(result);
        }

        abstract void execute(KieSession kieSession, TaskService taskService, KieRemoteConfiguration configuration, Exchange exchange);
    }
}

