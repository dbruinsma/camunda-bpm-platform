/* Licensed under the Apache License, Version 2.0 (the "License");
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
package com.camunda.fox.platform.subsystem.impl.deployment.marker;

import java.util.List;

import org.camunda.bpm.application.AbstractProcessApplication;
import org.camunda.bpm.application.impl.metadata.spi.ProcessesXml;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentUnit;

import com.camunda.fox.platform.subsystem.impl.util.ProcessesXmlWrapper;

/**
 * 
 * @author Daniel Meyer
 *
 */
public class ProcessApplicationAttachments {

  private static final AttachmentKey<Boolean> MARKER = AttachmentKey.create(Boolean.class);
  private static final AttachmentKey<Boolean> PART_OF_MARKER = AttachmentKey.create(Boolean.class);
  private static final AttachmentKey<AttachmentList<ProcessesXmlWrapper>> PROCESSES_XML_LIST = AttachmentKey.createList(ProcessesXmlWrapper.class);
  private static final AttachmentKey<ComponentDescription> PA_COMPONENT = AttachmentKey.create(ComponentDescription.class);

  /**
   * Attach the parsed ProcessesXml file to a deployment unit.
   *  
   */
  public static void addProcessesXml(DeploymentUnit unit, ProcessesXmlWrapper processesXmlWrapper) {
    unit.addToAttachmentList(PROCESSES_XML_LIST, processesXmlWrapper);
  }

  /**
   * Returns the attached {@link ProcessesXml} marker or null;
   *  
   */
  public static List<ProcessesXmlWrapper> getProcessesXmls(DeploymentUnit deploymentUnit) {
    return deploymentUnit.getAttachmentList(PROCESSES_XML_LIST);
  }

  /** 
   * marks a a {@link DeploymentUnit} as a process application 
   */
  public static void mark(DeploymentUnit unit) {
    unit.putAttachment(MARKER, Boolean.TRUE);    
  }
  
  /** 
   * marks a a {@link DeploymentUnit} as part of a process application 
   */
  public static void markPartOfProcessApplication(DeploymentUnit unit) {
    if(unit.getParent() != null && unit.getParent() != unit) {
      unit.getParent().putAttachment(PART_OF_MARKER, Boolean.TRUE);
    }      
  }
  
  /**
   * return true if the deployment unit is either itself a process 
   * application or part of a process application.
   */
  public static boolean isPartOfProcessApplication(DeploymentUnit unit) {
    if(isProcessApplication(unit)) {
      return true;
    }
    if(unit.getParent() != null && unit.getParent() != unit) {
      return unit.getParent().hasAttachment(PART_OF_MARKER);
    }
    return false;
  }       
  
  /**
   * Returns true if the {@link DeploymentUnit} itself is a process application (carries a processes.xml)
   * 
   */
  public static boolean isProcessApplication(DeploymentUnit deploymentUnit) {
    return deploymentUnit.hasAttachment(MARKER);
  }
  
  /**
   * Returns the {@link ComponentDescription} for the {@link AbstractProcessApplication} component
   */
  public static ComponentDescription getProcessApplicationComponent(DeploymentUnit deploymentUnit) {
    return deploymentUnit.getAttachment(PA_COMPONENT);
  }
  
  /**
   * Attach the {@link ComponentDescription} for the {@link AbstractProcessApplication} component   
   */
  public static void attachProcessApplicationComponent(DeploymentUnit deploymentUnit, ComponentDescription componentDescription){
    deploymentUnit.putAttachment(PA_COMPONENT, componentDescription);
  }
  
  private ProcessApplicationAttachments() {

  }
}
