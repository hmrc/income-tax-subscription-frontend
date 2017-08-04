/*
 * Copyright 2017 HM Revenue & Customs
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

package services.mocks

import common.Constants
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import services.AuthService
import uk.gov.hmrc.auth.core.ConfidenceLevel.L50
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.~

import uk.gov.hmrc.play.http.HeaderCarrier
import utils.TestConstants

import scala.collection.JavaConverters._
import scala.concurrent.Future

trait MockAuthService extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>


  val mockAuthService = mock[AuthService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)

    mockNinoRetrieval()
  }

  def mockAuthSuccess(): Unit = {
    when(mockAuthService.authorised())
      .thenReturn(new mockAuthService.AuthorisedFunction(EmptyPredicate) {
        override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier) = body
      })
  }

  def mockRetrievalSuccess[T](retrievalValue: T): Unit = {
    when(mockAuthService.authorised())
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier): Future[B] = body.apply(retrievalValue.asInstanceOf[A])
          }
        })
  }

  def mockNinoRetrieval(): Unit = mockRetrievalSuccess(new ~(Enrolments(Set(ninoEnrolment)), Some(AffinityGroup.Individual)))

  def mockAuthUnauthorised(exception: AuthorisationException = new InvalidBearerToken): Unit =
    when(mockAuthService.authorised())
      .thenReturn(new mockAuthService.AuthorisedFunction(EmptyPredicate) {
        override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier) = Future.failed(exception)

        override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
          override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier): Future[B] = Future.failed(exception)
        }
      })

  def mockAuthEnrolled(): Unit = mockRetrievalSuccess(new ~(Enrolments(Set(ninoEnrolment, mtdidEnrolment)), Some(AffinityGroup.Individual)))

  val ninoEnrolment = Enrolment(
    Constants.ninoEnrolmentName,
    Seq(EnrolmentIdentifier(Constants.ninoEnrolmentIdentifierKey, TestConstants.testNino)),
    "Activated",
    L50
  )

  val mtdidEnrolment = Enrolment(
    Constants.mtdItsaEnrolmentName,
    Seq(EnrolmentIdentifier(Constants.mtdItsaEnrolmentIdentifierKey, TestConstants.testMTDID)),
    "Activated",
    L50
  )
}
