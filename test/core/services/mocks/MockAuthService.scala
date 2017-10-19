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

package core.services.mocks

import core.Constants
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import core.services.AuthService
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import core.utils.TestConstants

import scala.concurrent.{ExecutionContext, Future}

trait MockAuthService extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>

  val mockAuthService = mock[AuthService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)

    mockNinoAndUtrRetrieval()
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

  def mockNinoAndUtrRetrieval(): Unit = mockRetrievalSuccess(new ~(Enrolments(Set(ninoEnrolment, utrEnrolment)), Some(AffinityGroup.Individual)))

  def mockUtrRetrieval(): Unit = mockRetrievalSuccess(new ~(Enrolments(Set(utrEnrolment)), Some(AffinityGroup.Individual)))

  def mockNinoRetrieval(): Unit = mockRetrievalSuccess(new ~(Enrolments(Set(ninoEnrolment)), Some(AffinityGroup.Individual)))

  def mockNinoRetrievalWithOrg(): Unit = mockRetrievalSuccess(new ~(Enrolments(Set(ninoEnrolment)), Some(AffinityGroup.Organisation)))

  def mockNinoRetrievalWithNoAffinity(): Unit = mockRetrievalSuccess(new ~(Enrolments(Set(ninoEnrolment)), None))

  def mockIndividualWithNoEnrolments(): Unit = mockRetrievalSuccess(new ~(Enrolments(Set.empty), Some(AffinityGroup.Individual)))

  def mockAuthUnauthorised(exception: AuthorisationException = new InvalidBearerToken): Unit =
    when(mockAuthService.authorised())
      .thenReturn(new mockAuthService.AuthorisedFunction(EmptyPredicate) {
        override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier, ec: ExecutionContext) = Future.failed(exception)

        override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
          override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = Future.failed(exception)
        }
      })

  def mockAuthEnrolled(): Unit = mockRetrievalSuccess(new ~(Enrolments(Set(ninoEnrolment, utrEnrolment, mtdidEnrolment)), Some(AffinityGroup.Individual)))

  val ninoEnrolment = Enrolment(
    Constants.ninoEnrolmentName,
    Seq(EnrolmentIdentifier(Constants.ninoEnrolmentIdentifierKey, TestConstants.testNino)),
    "Activated",
    L200
  )

  val mtdidEnrolment = Enrolment(
    Constants.mtdItsaEnrolmentName,
    Seq(EnrolmentIdentifier(Constants.mtdItsaEnrolmentIdentifierKey, TestConstants.testMTDID)),
    "Activated",
    L200
  )

  val utrEnrolment = Enrolment(
    Constants.utrEnrolmentName,
    Seq(EnrolmentIdentifier(Constants.utrEnrolmentIdentifierKey, TestConstants.testUtr)),
    "Activated",
    L200
  )
}
