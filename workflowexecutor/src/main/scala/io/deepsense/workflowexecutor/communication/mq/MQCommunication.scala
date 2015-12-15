/**
 * Copyright 2015, deepsense.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.deepsense.workflowexecutor.communication.mq

import io.deepsense.models.workflows.Workflow

object MQCommunication {
  val mqActorSystemName = "rabbitmq"

  def subscriberName(topic: String): String = s"${topic}_subscriber"
  def publisherName(topic: String): String = s"${topic}_publisher"
  def queueName(topic: String): String = s"${topic}_to_executor"

  object Actor {

    object Publisher {
      val seahorse = prefixedName("seahorse")
      val notebook = prefixedName("notebook")
      def workflow(id: Workflow.Id): String = prefixedName(id.toString)
      private[this] def prefixedName = name("publisher") _
    }

    object Subscriber {
      val seahorse = prefixedName("seahorse")
      val notebook = prefixedName("notebook")
      val workflows: String = prefixedName("workflows")
      private[this] def prefixedName = name("subscriber") _
    }
    private[this] def name(prefix: String)(suffix: String): String = s"${prefix}_$suffix"
  }

  object Exchange {
    val seahorse = "seahorse"
  }

  object Topic {
    private[this] val workflowPrefix = "workflow"
    val notebook = "notebook"
    val seahorse = "seahorse"
    val workflows = s"${workflowPrefix}.*"
    def workflow(workflowId: Workflow.Id): String = s"${workflowPrefix}.${workflowId.toString}"
  }
}
