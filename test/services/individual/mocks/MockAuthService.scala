/*
 * Copyright 2022 HM Revenue & Customs
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

package services.individual.mocks

import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.individual.TestConstants.testCredId
import utilities.individual.{Constants, TestConstants}

import scala.concurrent.{ExecutionContext, Future}

trait MockAuthService extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>

  val mockAuthService: AuthService = mock[AuthService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthService)

    mockNinoAndUtrRetrieval()
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
            new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] =
              body.apply(retrievalValue.asInstanceOf[A])
          }
        })
  }

  def mockNinoAndUtrRetrieval(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(new ~(Enrolments(Set(ninoEnrolment, utrEnrolment)),
    Some(AffinityGroup.Individual)), Some(User)),testConfidenceLevel), Some(Credentials(testCredId,""))))

  def mockUtrRetrieval(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(new ~(Enrolments(Set(utrEnrolment)),
    Some(AffinityGroup.Individual)), Some(User)),testConfidenceLevel), Some(Credentials(testCredId,""))))

  def mockNinoRetrieval(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(new ~(Enrolments(Set(ninoEnrolment)),
    Some(AffinityGroup.Individual)), Some(User)),testConfidenceLevel), Some(Credentials(testCredId,""))))

  def mockNinoRetrievalWithOrg(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(new ~(Enrolments(Set(ninoEnrolment)),
    Some(AffinityGroup.Organisation)), Some(User)),testConfidenceLevel), Some(Credentials(testCredId,""))))

  def mockNinoRetrievalWithNoAffinity(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(new ~(Enrolments(Set(ninoEnrolment)), None), Some(User))
    ,testConfidenceLevel), Some(Credentials(testCredId,""))))

  def mockIndividualWithNoEnrolments(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(new ~(Enrolments(Set.empty),
    Some(AffinityGroup.Individual)), Some(User)),testConfidenceLevel), Some(Credentials(testCredId,""))))

  def mockAuthUnauthorised(exception: AuthorisationException = new InvalidBearerToken): Unit =
    when(mockAuthService.authorised())
      .thenReturn(new mockAuthService.AuthorisedFunction(EmptyPredicate) {
        override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Nothing] = Future.failed(exception)

        override def retrieve[A](retrieval: Retrieval[A]): mockAuthService.AuthorisedFunctionWithResult[A] =
          new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
          override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = Future.failed(exception)
        }
      })

  def mockAuthEnrolled(): Unit = mockRetrievalSuccess(new ~(new ~(new ~(new ~(Enrolments(Set(ninoEnrolment, utrEnrolment, mtdidEnrolment)),
    Some(AffinityGroup.Individual)), Some(User)), testConfidenceLevel), Some(Credentials(testCredId,""))))

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
