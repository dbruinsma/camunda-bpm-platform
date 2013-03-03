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
package org.activiti.engine.impl.cmd;

import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.application.ProcessApplicationReference;

/**
 * @author Daniel Meyer
 *
 */
public class GetProcessApplicationForDeployment implements Command<String> {

  protected String deploymentId;

  public GetProcessApplicationForDeployment(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  public String execute(CommandContext commandContext) {
    
    ProcessApplicationReference reference = Context.getProcessEngineConfiguration()
      .getProcessApplicationManager()
      .getProcessApplicationForDeployment(deploymentId);
    
    if(reference != null) {
      return reference.getName();
    } else {
      return null;
    }
  }

}
