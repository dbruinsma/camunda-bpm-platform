/*
 * Copyright 2016 camunda services GmbH.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.camunda.bpm.engine.test.bpmn.event.conditional;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.UserTaskBuilder;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.camunda.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;
import static org.junit.Assert.*;

/**
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@RunWith(Parameterized.class)
public class ConditionalEventTriggeredByExecutionListenerTest extends AbstractConditionalEventTestCase {

  protected static final String TASK_AFTER_CONDITIONAL_BOUNDARY_EVENT = "Task after conditional boundary event";
  protected static final String TASK_AFTER_CONDITIONAL_START_EVENT = "Task after conditional start event";
  protected static final String START_EVENT_ID = "startEventId";
  protected static final String END_EVENT_ID = "endEventId";

  private interface ConditionalEventProcessSpecifier {
    BpmnModelInstance specifyConditionalProcess(BpmnModelInstance modelInstance, boolean isInterrupting);

    String expectedActivityName();

    int expectedSubscriptions();
  }

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
      {new ConditionalEventProcessSpecifier() {
        @Override
        public BpmnModelInstance specifyConditionalProcess(BpmnModelInstance modelInstance, boolean isInterrupting) {
          return modify(modelInstance)
            .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent()
            .interrupting(isInterrupting)
            .conditionalEventDefinition()
            .condition(CONDITION_EXPR)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITION)
            .endEvent()
            .done();
        }

        @Override
        public String expectedActivityName() {
          return TASK_AFTER_CONDITION;
        }

        @Override
        public int expectedSubscriptions() {
          return 1;
        }

        @Override
        public String toString() {
          return "ConditionalEventSubProcess";
        }
      }}, {
      new ConditionalEventProcessSpecifier() {
        @Override
        public BpmnModelInstance specifyConditionalProcess(BpmnModelInstance modelInstance, boolean isInterrupting) {
          modelInstance = modify(modelInstance)
            .addSubProcessTo(CONDITIONAL_EVENT_PROCESS_KEY)
            .triggerByEvent()
            .embeddedSubProcess()
            .startEvent()
            .interrupting(isInterrupting)
            .conditionalEventDefinition()
            .condition(CONDITION_EXPR)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITIONAL_START_EVENT)
            .endEvent()
            .done();

          return modify(modelInstance)
            .activityBuilder(TASK_WITH_CONDITION_ID)
            .boundaryEvent()
            .cancelActivity(isInterrupting)
            .conditionalEventDefinition()
            .condition(CONDITION_EXPR)
            .conditionalEventDefinitionDone()
            .userTask()
            .name(TASK_AFTER_CONDITIONAL_BOUNDARY_EVENT)
            .endEvent()
            .done();
        }

        @Override
        public String expectedActivityName() {
          return TASK_AFTER_CONDITIONAL_START_EVENT;
        }

        @Override
        public int expectedSubscriptions() {
          return 2;
        }

        @Override
        public String toString() {
          return "MixedConditionalProcess";
        }
      }}});
  }

  @Parameterized.Parameter
  public ConditionalEventProcessSpecifier specifier;


  @Test
  public void testSetVariableInStartListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .userTask(TASK_WITH_CONDITION_ID)
      .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE)
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(specifier.expectedActivityName(), tasksAfterVariableIsSet.get(0).getName());
  }

  @Test
  public void testNonInterruptingSetVariableInStartListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .userTask(TASK_WITH_CONDITION_ID)
      .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE)
      .name(TASK_WITH_CONDITION)
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //when task is completed
    taskService.complete(taskQuery.singleResult().getId());

    //then start listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(2, tasksAfterVariableIsSet.size());
    assertEquals(specifier.expectedSubscriptions(), conditionEventSubscriptionQuery.list().size());
    for (Task task : tasksAfterVariableIsSet) {
      assertTrue(task.getName().equals(specifier.expectedActivityName()) || task.getName().equals(TASK_WITH_CONDITION));
    }
  }

  @Test
  public void testSetVariableInTakeListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent(END_EVENT_ID)
      .done();
    CamundaExecutionListener listener = modelInstance.newInstance(CamundaExecutionListener.class);
    listener.setCamundaEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setCamundaExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then take listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(specifier.expectedActivityName(), tasksAfterVariableIsSet.get(0).getName());
  }

  @Test
  public void testNonInterruptingSetVariableInTakeListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent(END_EVENT_ID)
      .done();
    CamundaExecutionListener listener = modelInstance.newInstance(CamundaExecutionListener.class);
    listener.setCamundaEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setCamundaExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then take listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(2, tasksAfterVariableIsSet.size());
    assertEquals(specifier.expectedSubscriptions(), conditionEventSubscriptionQuery.list().size());
  }

  @Test
  public void testSetVariableInTakeListenerWithAsyncBefore() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID).camundaAsyncBefore()
      .endEvent(END_EVENT_ID)
      .done();
    CamundaExecutionListener listener = modelInstance.newInstance(CamundaExecutionListener.class);
    listener.setCamundaEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setCamundaExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then take listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(specifier.expectedActivityName(), tasksAfterVariableIsSet.get(0).getName());
  }

  @Test
  public void testNonInterruptingSetVariableInTakeListenerWithAsyncBefore() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID).camundaAsyncBefore()
      .endEvent(END_EVENT_ID)
      .done();
    CamundaExecutionListener listener = modelInstance.newInstance(CamundaExecutionListener.class);
    listener.setCamundaEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setCamundaExpression(EXPR_SET_VARIABLE);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //when task is completed
    taskService.complete(taskQuery.singleResult().getId());

    //then take listener sets variable
    //non interrupting boundary event is triggered
    Task task = taskQuery.singleResult();
    assertEquals(specifier.expectedActivityName(), task.getName());

    //and job was created
    Job job = engine.getManagementService().createJobQuery().singleResult();
    assertNotNull(job);
    assertEquals(1, conditionEventSubscriptionQuery.list().size());

    //when job is executed task is created
    engine.getManagementService().executeJob(job.getId());
    //when both tasks are completed
    taskService.complete(task.getId());
    taskService.complete(taskQuery.singleResult().getId());

    //then no task exist and process instance is ended
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(0, tasksAfterVariableIsSet.size());
    assertNull(runtimeService.createProcessInstanceQuery().singleResult());
  }

  @Test
  public void testSetVariableInEndListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_END, EXPR_SET_VARIABLE)
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();

    //when task is completed
    taskService.complete(task.getId());

    //then end listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(specifier.expectedActivityName(), tasksAfterVariableIsSet.get(0).getName());
  }

  @Test
  public void testNonInterruptingSetVariableInEndListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_END, EXPR_SET_VARIABLE)
      .userTask(TASK_WITH_CONDITION_ID)
      .name(TASK_WITH_CONDITION)
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);
    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());

    //when task is completed
    taskService.complete(taskQuery.singleResult().getId());

    //then end listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(2, tasksAfterVariableIsSet.size());
    for (Task task : tasksAfterVariableIsSet) {
      assertTrue(task.getName().equals(specifier.expectedActivityName()) || task.getName().equals(TASK_WITH_CONDITION));
    }
  }

  @Test
  public void testSetVariableOnParentScopeInTakeListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .subProcess()
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent()
      .subProcessDone()
      .endEvent(END_EVENT_ID)
      .done();
    CamundaExecutionListener listener = modelInstance.newInstance(CamundaExecutionListener.class);
    listener.setCamundaEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setCamundaExpression(EXPR_SET_VARIABLE_ON_PARENT);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(specifier.expectedActivityName(), tasksAfterVariableIsSet.get(0).getName());
  }

  @Test
  public void testNonInterruptingSetVariableOnParentScopeInTakeListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .subProcess()
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .sequenceFlowId(FLOW_ID)
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent()
      .subProcessDone()
      .endEvent(END_EVENT_ID)
      .done();
    CamundaExecutionListener listener = modelInstance.newInstance(CamundaExecutionListener.class);
    listener.setCamundaEvent(ExecutionListener.EVENTNAME_TAKE);
    listener.setCamundaExpression(EXPR_SET_VARIABLE_ON_PARENT);
    modelInstance.<SequenceFlow>getModelElementById(FLOW_ID).builder().addExtensionElement(listener);
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(2, tasksAfterVariableIsSet.size());
  }

  @Test
  public void testSetVariableOnParentScopeInStartListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .subProcess()
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .userTask(TASK_WITH_CONDITION_ID)
      .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE_ON_PARENT)
      .endEvent()
      .subProcessDone()
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(specifier.expectedActivityName(), tasksAfterVariableIsSet.get(0).getName());
  }

  @Test
  public void testNonInterruptingSetVariableOnParentScopeInStartListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .subProcess()
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .userTask(TASK_WITH_CONDITION_ID)
      .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_START, EXPR_SET_VARIABLE_ON_PARENT)
      .endEvent()
      .subProcessDone()
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then start listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(2, tasksAfterVariableIsSet.size());
  }

  @Test
  public void testSetVariableOnParentScopeInEndListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .subProcess()
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_END, EXPR_SET_VARIABLE_ON_PARENT)
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent()
      .subProcessDone()
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, true);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then end listener sets variable
    //conditional event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(specifier.expectedActivityName(), tasksAfterVariableIsSet.get(0).getName());
  }

  @Test
  public void testNonInterruptingSetVariableOnParentScopeInEndListener() {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(CONDITIONAL_EVENT_PROCESS_KEY)
      .startEvent(START_EVENT_ID)
      .subProcess()
      .embeddedSubProcess()
      .startEvent()
      .userTask(TASK_BEFORE_CONDITION_ID)
      .name(TASK_BEFORE_CONDITION)
      .camundaExecutionListenerExpression(ExecutionListener.EVENTNAME_END, EXPR_SET_VARIABLE_ON_PARENT)
      .userTask(TASK_WITH_CONDITION_ID)
      .endEvent()
      .subProcessDone()
      .endEvent(END_EVENT_ID)
      .done();
    modelInstance = specifier.specifyConditionalProcess(modelInstance, false);
    engine.manageDeployment(repositoryService.createDeployment().addModelInstance(CONDITIONAL_MODEL, modelInstance).deploy());

    // given
    ProcessInstance procInst = runtimeService.startProcessInstanceByKey(CONDITIONAL_EVENT_PROCESS_KEY);

    TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(procInst.getId());
    Task task = taskQuery.singleResult();
    assertNotNull(task);
    assertEquals(TASK_BEFORE_CONDITION, task.getName());

    //when task is completed
    taskService.complete(task.getId());

    //then end listener sets variable
    //non interrupting boundary event is triggered
    tasksAfterVariableIsSet = taskQuery.list();
    assertEquals(2, tasksAfterVariableIsSet.size());
  }

}
