/**
 * Copyright 2016, deepsense.io
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

package io.deepsense.commons.rest.client

import java.io.FileNotFoundException
import java.net.URL

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import spray.client.pipelining._
import spray.http.{HttpResponse, StatusCodes}
import spray.httpx.SprayJsonSupport

import io.deepsense.commons.models.Id
import io.deepsense.commons.rest.client.req.NotebookClientRequest
import io.deepsense.commons.utils.Logging
import io.deepsense.commons.json.NotebookRestClientProtocol._

/**
  * Exception that will translate to an http error response with a specific
  * status code.
  */
case class NotebookHttpException(
                                statusCode: Int,
                                msg: String,
                                cause: Throwable = null)
  extends Exception(msg, cause)


class NotebookRestClient(
    notebooksServerAddress: URL,
    workflowId: Id,
    nodeId: Id,
    pollInterval: FiniteDuration,
    retryCountLimit: Int
)(implicit override val as: ActorSystem)
  extends Logging with RestClient with SprayJsonSupport {

  def apiUrl: java.net.URL = notebooksServerAddress
  def credentials: Option[spray.http.HttpCredentials] = None
  def userId: Option[java.util.UUID] = None
  def userName: Option[String] = None

  implicit val timeout: Timeout = 10.minutes

  private val postPath = endpointPath("jupyter/HeadlessNotebook")
  private val getPath = endpointPath(f"jupyter/HeadlessNotebook/$workflowId%s_$nodeId%s.pdf")

  private val poller: ActorRef = as.actorOf(Props(new GetPdfActor(this, pollInterval, retryCountLimit)))

  def fetchPdf(): Future[Array[Byte]] = fetchHttpResponse(Get(getPath)).flatMap { resp =>
    resp.status match {
      case StatusCodes.NotFound =>
        Future.failed(new FileNotFoundException(s"pdf for workflow s$workflowId and node s$nodeId not found"))
      case StatusCodes.OK =>
        Future.successful(resp.entity.data.toByteArray)
      case statusCode =>
        Future.failed(NotebookHttpException(statusCode.intValue, s"Notebook server responded with $statusCode " +
          s"when asked for pdf for workflow $workflowId and node $nodeId"))
    }
  }

  def pollPdf(): Future[Array[Byte]] = {
    (poller ? GetPdfActor.GetPdf(workflowId, nodeId)).mapTo[Array[Byte]]
  }

  def generatePdf(language: String): Future[HttpResponse] = {
    val req = NotebookClientRequest(workflowId, nodeId, language)
    fetchHttpResponse(Post(postPath, req))
  }

  def generateAndFetchPdf(language: String): Future[Array[Byte]] = {
    generatePdf(language).flatMap(_ => pollPdf())
  }

  def toFactory: NotebooksClientFactory =
    new NotebooksClientFactory(notebooksServerAddress, pollInterval, retryCountLimit)

}

class NotebooksClientFactory(notebooksServerAddress: URL, pollInterval: FiniteDuration, retryCountLimit: Int)
  (implicit system: ActorSystem) {
  def createNotebookForNode(workflow: Id, node: Id): NotebookRestClient = {
    new NotebookRestClient(notebooksServerAddress, workflow, node, pollInterval, retryCountLimit)
  }
}