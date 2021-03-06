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
package org.flowable.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.TaskCompletionBuilder;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Falko Menge
 */
public class DelegateTaskTest extends PluggableFlowableTestCase {

    /**
     * @see <a href="https://activiti.atlassian.net/browse/ACT-380">https://activiti.atlassian.net/browse/ACT-380</a>
     */
    @Test
    @Deployment
    public void testGetCandidates() {
        runtimeService.startProcessInstanceByKey("DelegateTaskTest.testGetCandidates");

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        @SuppressWarnings("unchecked")
        Set<String> candidateUsers = (Set<String>) taskService.getVariable(task.getId(), DelegateTaskTestTaskListener.VARNAME_CANDIDATE_USERS);
        assertThat(candidateUsers)
                .containsOnly("kermit", "gonzo");

        @SuppressWarnings("unchecked")
        Set<String> candidateGroups = (Set<String>) taskService.getVariable(task.getId(), DelegateTaskTestTaskListener.VARNAME_CANDIDATE_GROUPS);
        assertThat(candidateGroups)
                .containsOnly("management", "accountancy");
    }

    @Test
    @Deployment
    public void testChangeCategoryInDelegateTask() {

        // Start process instance
        Map<String, Object> variables = new HashMap<>();
        variables.put("approvers", Collections.singletonList("kermit")); // , "gonzo", "mispiggy"));
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("delegateTaskTest", variables);

        // Assert there are three tasks with the default category
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        for (org.flowable.task.api.Task task : tasks) {
            assertThat(task.getCategory()).isEqualTo("approval");
            Map<String, Object> taskVariables = new HashMap<>();
            taskVariables.put("outcome", "approve");

            TaskCompletionBuilder taskCompletionBuilder = taskService.createTaskCompletionBuilder();
            taskCompletionBuilder
                    .taskId(task.getId())
                    .variablesLocal(taskVariables)
                    .complete();
        }

        // After completion, the task category should be changed in the script listener working on the delegate task
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();
        for (HistoricTaskInstance historicTaskInstance : historyService.createHistoricTaskInstanceQuery().list()) {
            assertThat(historicTaskInstance.getCategory()).isEqualTo("approved");
        }
    }

}
