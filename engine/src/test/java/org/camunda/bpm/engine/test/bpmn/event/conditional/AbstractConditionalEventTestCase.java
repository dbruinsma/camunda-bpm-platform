/*
 * Copyright 2016 camunda services GmbH.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.test.bpmn.event.conditional;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.impl.EventSubscriptionQueryImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.event.EventType;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
public abstract class AbstractConditionalEventTestCase {

  protected static final String CONDITIONAL_EVENT_PROCESS_KEY = "conditionalEventProcess";
  protected static final String CONDITIONAL_EVENT = "conditionalEvent";
  protected static final String CONDITION_EXPR = "${variable == 1}";
  protected static final String EXPR_SET_VARIABLE = "${execution.setVariable(\"variable\", 1)}";
  protected static final String CONDITIONAL_MODEL = "conditionalModel.bpmn20.xml";
  protected static final String CONDITIONAL_VAR_EVENTS = "create, update";
  protected static final String CONDITIONAL_VAR_EVENT_UPDATE = "update";

  protected static final String TASK_BEFORE_CONDITION = "Before Condition";
  protected static final String TASK_BEFORE_CONDITION_ID = "beforeConditionId";
  protected static final String TASK_AFTER_CONDITION = "After Condition";
  protected static final String TASK_AFTER_SERVICE_TASK = "afterServiceTask";
  protected static final String TASK_IN_SUB_PROCESS_ID = "taskInSubProcess";
  protected static final String TASK_IN_SUB_PROCESS = "Task in Subprocess";
  protected static final String TASK_WITH_CONDITION = "Task with condition";
  protected static final String TASK_WITH_CONDITION_ID = "taskWithCondition";
  protected static final String AFTER_TASK = "After Task";

  protected static final String VARIABLE_NAME = "variable";
  protected static final String TRUE_CONDITION = "${true}";
  protected static final String SUB_PROCESS_ID = "subProcess";
  protected static final String FLOW_ID = "flow";
  protected static final String DELEGATED_PROCESS_KEY = "delegatedProcess";

  protected static final BpmnModelInstance TASK_MODEL = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
          .startEvent()
          .userTask(TASK_BEFORE_CONDITION_ID)
          .name(TASK_BEFORE_CONDITION)
          .endEvent().done();
  protected static final BpmnModelInstance DELEGATED_PROCESS = Bpmn.createExecutableProcess(DELEGATED_PROCESS_KEY)
    .startEvent()
    .serviceTask()
    .camundaExpression(EXPR_SET_VARIABLE)
    .endEvent()
    .done();
  protected static final String TASK_AFTER_OUTPUT_MAPPING = "afterOutputMapping";

  protected List<Task> tasksAfterVariableIsSet;

  @Rule
  public final ProcessEngineRule engine = new ProvidedProcessEngineRule();

  @Rule
  public ExpectedException expectException = ExpectedException.none();

  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected RepositoryService repositoryService;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected EventSubscriptionQueryImpl conditionEventSubscriptionQuery;

  @Before
  public void init() {
    this.runtimeService = engine.getRuntimeService();
    this.taskService = engine.getTaskService();
    this.repositoryService = engine.getRepositoryService();
    this.processEngineConfiguration = engine.getProcessEngineConfiguration();
    this.conditionEventSubscriptionQuery = new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutorTxRequired()).eventType(EventType.CONDITONAL.name());
  }

  @After
  public void checkIfProcessCanBeFinished() {
    //given tasks after variable was set
    assertNotNull(tasksAfterVariableIsSet);

    //when tasks are completed
    for (Task task : tasksAfterVariableIsSet) {
      taskService.complete(task.getId());
    }

    //then
    assertEquals(0, conditionEventSubscriptionQuery.list().size());
    assertNull(taskService.createTaskQuery().singleResult());
    assertNull(runtimeService.createProcessInstanceQuery().singleResult());
    tasksAfterVariableIsSet = null;
  }
}
