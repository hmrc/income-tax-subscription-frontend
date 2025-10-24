/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors.individual.subscription.mocks

import connectors.individual.TaxEnrolmentsConnector
import connectors.individual.httpparsers.AllocateEnrolmentResponseHttpParser.{AllocateEnrolmentResponse, EnrolFailure, EnrolSuccess}
import connectors.individual.httpparsers.UpsertEnrolmentResponseHttpParser.{KnownFactsFailure, KnownFactsSuccess, UpsertEnrolmentResponse}
import models.common.subscription._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import utilities.individual.TestConstants.{testErrorMessage, testException}

import scala.concurrent.Future

trait MockTaxEnrolmentsConnector extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockTaxEnrolmentsConnector)
  }

  private def mockUpsertEnrolment(enrolmentKey: EnrolmentKey,
                                  enrolmentVerifiers: EnrolmentVerifiers
                                 )(response: Future[UpsertEnrolmentResponse]): Unit =
    when(mockTaxEnrolmentsConnector.upsertEnrolment(
      ArgumentMatchers.eq(enrolmentKey),
      ArgumentMatchers.eq(enrolmentVerifiers)
    )(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

  def mockUpsertEnrolmentSuccess(enrolmentKey: EnrolmentKey,
                                 enrolmentVerifiers: EnrolmentVerifiers): Unit =
    mockUpsertEnrolment(enrolmentKey, enrolmentVerifiers)(Future.successful(Right(KnownFactsSuccess)))

  def mockUpsertEnrolmentFailure(enrolmentKey: EnrolmentKey,
                                 enrolmentVerifiers: EnrolmentVerifiers): Unit =
    mockUpsertEnrolment(enrolmentKey, enrolmentVerifiers)(Future.successful(Left(KnownFactsFailure(testErrorMessage))))

  def mockUpsertEnrolmentException(enrolmentKey: EnrolmentKey,
                                   enrolmentVerifiers: EnrolmentVerifiers): Unit =
    mockUpsertEnrolment(enrolmentKey, enrolmentVerifiers)(Future.failed(testException))

  def verifyUpsertEnrolment(enrolmentKey: EnrolmentKey,
                            enrolmentVerifiers: EnrolmentVerifiers,
                            count: Int = 1): Unit = {
    verify(mockTaxEnrolmentsConnector, times(count))
      .upsertEnrolment(
        ArgumentMatchers.eq(enrolmentKey),
        ArgumentMatchers.eq(enrolmentVerifiers)
      )(ArgumentMatchers.any())
  }


  private def mockAllocateEnrolment(groupId: String,
                                    enrolmentKey: EnrolmentKey,
                                    enrolmentRequest: EmacEnrolmentRequest
                                   )(response: Future[AllocateEnrolmentResponse]): Unit =
    when(mockTaxEnrolmentsConnector.allocateEnrolment(
      ArgumentMatchers.eq(groupId),
      ArgumentMatchers.eq(enrolmentKey),
      ArgumentMatchers.eq(enrolmentRequest)
    )(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(response)

  def mockAllocateEnrolmentSuccess(groupId: String,
                                   enrolmentKey: EnrolmentKey,
                                   enrolmentRequest: EmacEnrolmentRequest): Unit =
    mockAllocateEnrolment(groupId, enrolmentKey, enrolmentRequest)(Future.successful(Right(EnrolSuccess)))

  def mockAllocateEnrolmentFailure(groupId: String,
                                   enrolmentKey: EnrolmentKey,
                                   enrolmentRequest: EmacEnrolmentRequest): Unit =
    mockAllocateEnrolment(groupId, enrolmentKey, enrolmentRequest)(Future.successful(Left(EnrolFailure(testErrorMessage))))

  def mockAllocateEnrolmentException(groupId: String,
                                     enrolmentKey: EnrolmentKey,
                                     enrolmentRequest: EmacEnrolmentRequest): Unit =
    mockAllocateEnrolment(groupId, enrolmentKey, enrolmentRequest)(Future.failed(testException))

  def verifyAllocateEnrolment(groupId: String,
                              enrolmentKey: EnrolmentKey,
                              enrolmentRequest: EmacEnrolmentRequest,
                              count: Int = 1): Unit = {
    verify(mockTaxEnrolmentsConnector, times(count))
      .allocateEnrolment(
        ArgumentMatchers.eq(groupId),
        ArgumentMatchers.eq(enrolmentKey),
        ArgumentMatchers.eq(enrolmentRequest)
      )(ArgumentMatchers.any())
  }

}
