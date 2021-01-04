/*
 * Copyright 2021 HM Revenue & Customs
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

package services.agent.mocks

import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.agent.TestConstants.testCredId
import utilities.agent.{Constants, TestConstants}

import scala.concurrent.{ExecutionContext, Future}

trait MockAgentAuthService extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>

  val mockAuthService: AuthService = mock[AuthService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)

    mockAgent()
  }

  def mockAuthSuccess(): Unit = {
    when(mockAuthService.authorised())
      .thenReturn(new mockAuthService.AuthorisedFunction(EmptyPredicate) {
        override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = body
      })
  }

  def mockRetrievalSuccess[T](retrievalValue: T): Unit = {
    when(mockAuthService.authorised())
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]): mockAuthService.AuthorisedFunctionWithResult[A] =
            new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {override def apply[B](body: A =>
              Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = body.apply(retrievalValue.asInstanceOf[A])
          }
        })
  }

  def mockAgent(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(new ~(Enrolments(Set(arnEnrolment)), Some(AffinityGroup.Agent)), Some(User)), testConfidenceLevel), Some(Credentials(testCredId,""))))

  def mockNotAgent(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(new ~(Enrolments(Set()), Some(AffinityGroup.Agent)), Some(User)), testConfidenceLevel), Some(Credentials(testCredId,""))))

  def mockAuthUnauthorised(exception: AuthorisationException = new InvalidBearerToken): Unit =
    when(mockAuthService.authorised())
      .thenReturn(new mockAuthService.AuthorisedFunction(EmptyPredicate) {
        override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Nothing] = Future.failed(exception)

        override def retrieve[A](retrieval: Retrieval[A]): mockAuthService.AuthorisedFunctionWithResult[A] =
          new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {override def apply[B](body: A =>
            Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = Future.failed(exception)
        }
      })

  val arnEnrolment = Enrolment(
    Constants.agentServiceEnrolmentName,
    Seq(EnrolmentIdentifier(Constants.agentServiceIdentifierKey, TestConstants.testARN)),
    "Activated"
  )

  val testConfidenceLevel: ConfidenceLevel.L200.type = ConfidenceLevel.L200

}
