/*
 * Copyright 2019 HM Revenue & Customs
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

package agent.services.mocks

import agent.common.Constants
import agent.utils.TestConstants
import core.services.AuthService
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAgentAuthService extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>

  val mockAuthService = mock[AuthService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)

    mockAgent()
  }

  def mockAuthSuccess(): Unit = {
    when(mockAuthService.authorised())
      .thenReturn(new mockAuthService.AuthorisedFunction(EmptyPredicate) {
        override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier, ec: ExecutionContext) = body
      })
  }

  def mockRetrievalSuccess[T](retrievalValue: T): Unit = {
    when(mockAuthService.authorised())
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = body.apply(retrievalValue.asInstanceOf[A])
          }
        })
  }

  def mockAgent(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(Enrolments(Set(arnEnrolment)), Some(AffinityGroup.Agent)), Some(User)), testConfidenceLevel))

  def mockNotAgent(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(Enrolments(Set()), Some(AffinityGroup.Agent)), Some(User)), testConfidenceLevel))

  def mockAuthUnauthorised(exception: AuthorisationException = new InvalidBearerToken): Unit =
    when(mockAuthService.authorised())
      .thenReturn(new mockAuthService.AuthorisedFunction(EmptyPredicate) {
        override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier, ec: ExecutionContext) = Future.failed(exception)

        override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
          override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = Future.failed(exception)
        }
      })

  val arnEnrolment = Enrolment(
    Constants.agentServiceEnrolmentName,
    Seq(EnrolmentIdentifier(Constants.agentServiceIdentifierKey, TestConstants.testARN)),
    "Activated"
  )

  val testConfidenceLevel = ConfidenceLevel.L200

}
