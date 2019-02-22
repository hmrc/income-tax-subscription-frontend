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

package core.services.mocks

import core.Constants
import core.services.AuthService
import core.utils.TestConstants
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.auth.core.{AffinityGroup, _}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

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

  def mockNinoAndUtrRetrieval(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(Enrolments(Set(ninoEnrolment, utrEnrolment)), Some(AffinityGroup.Individual)), Some(Admin)),testConfidenceLevel))

  def mockUtrRetrieval(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(Enrolments(Set(utrEnrolment)), Some(AffinityGroup.Individual)), Some(Admin)),testConfidenceLevel))

  def mockNinoRetrieval(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(Enrolments(Set(ninoEnrolment)), Some(AffinityGroup.Individual)), Some(Admin)),testConfidenceLevel))

  def mockNinoRetrievalWithOrg(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(Enrolments(Set(ninoEnrolment)), Some(AffinityGroup.Organisation)), Some(Admin)),testConfidenceLevel))

  def mockNinoRetrievalWithNoAffinity(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(Enrolments(Set(ninoEnrolment)), None), Some(Admin)),testConfidenceLevel))

  def mockIndividualWithNoEnrolments(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(Enrolments(Set.empty), Some(AffinityGroup.Individual)), Some(Admin)),testConfidenceLevel))

  def mockAuthUnauthorised(exception: AuthorisationException = new InvalidBearerToken): Unit =
    when(mockAuthService.authorised())
      .thenReturn(new mockAuthService.AuthorisedFunction(EmptyPredicate) {
        override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier, ec: ExecutionContext) = Future.failed(exception)

        override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
          override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = Future.failed(exception)
        }
      })

  def mockAuthEnrolled(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(Enrolments(Set(ninoEnrolment, utrEnrolment, mtdidEnrolment)), Some(AffinityGroup.Individual)), Some(Admin)), testConfidenceLevel))

  val ninoEnrolment = Enrolment(
    Constants.ninoEnrolmentName,
    Seq(EnrolmentIdentifier(Constants.ninoEnrolmentIdentifierKey, TestConstants.testNino)),
    "Activated"
  )

  val mtdidEnrolment = Enrolment(
    Constants.mtdItsaEnrolmentName,
    Seq(EnrolmentIdentifier(Constants.mtdItsaEnrolmentIdentifierKey, TestConstants.testMTDID)),
    "Activated"
  )

  val utrEnrolment = Enrolment(
    Constants.utrEnrolmentName,
    Seq(EnrolmentIdentifier(Constants.utrEnrolmentIdentifierKey, TestConstants.testUtr)),
    "Activated"
  )

  val testConfidenceLevel = ConfidenceLevel.L200

}
