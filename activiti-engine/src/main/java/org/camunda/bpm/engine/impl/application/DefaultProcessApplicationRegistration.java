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
package org.camunda.bpm.engine.impl.application;

import org.camunda.bpm.application.ProcessApplicationReference;
import org.camunda.bpm.application.ProcessApplicationRegistration;

/**
 * @author Daniel Meyer
 *
 */
public class DefaultProcessApplicationRegistration implements ProcessApplicationRegistration {

  protected ProcessApplicationManager processApplicationManager;
  protected String deploymentId;
  protected String processEngineName;
  protected ProcessApplicationReference reference;

  /**
   * @param processApplicationManager
   * @param reference 
   */
  public DefaultProcessApplicationRegistration(ProcessApplicationManager processApplicationManager, ProcessApplicationReference reference, String deploymentId, String processEnginenName) {
    this.processApplicationManager = processApplicationManager;
    this.reference = reference;
    this.deploymentId = deploymentId;
    this.processEngineName = processEnginenName;
  }

  // called by the pa
  public void unregister() {
    processApplicationManager.removeProcessApplication(deploymentId);
  }
    
  public String getDeploymentId() {
    return deploymentId;
  }
  
  public String getProcessEngineName() {
    return processEngineName;
  }
  
  public ProcessApplicationReference getReference() {
    return reference;
  }

}
