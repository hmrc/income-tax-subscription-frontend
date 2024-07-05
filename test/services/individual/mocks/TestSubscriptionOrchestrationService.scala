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

package services.individual.mocks

import models.ConnectorError
import models.common.subscription.{CreateIncomeSourcesModel, SubscriptionSuccess}
import org.mockito.ArgumentMatchers.{any, eq => eql}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.individual.SubscriptionOrchestrationService
import services.mocks.{MockSpsService, MockSubscriptionService}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

import scala.concurrent.Future

trait TestSubscriptionOrchestrationService extends MockSubscriptionService
  with MockKnownFactsService
  with MockEnrolmentService
  with MockSpsService {

  object TestSubscriptionOrchestrationService extends SubscriptionOrchestrationService(
    mockSubscriptionService,
    mockKnownFactsService,
    mockEnrolmentService,
    mockSpsService
  )

}

trait MockSubscriptionOrchestrationService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val mockSubscriptionOrchestrationService: SubscriptionOrchestrationService = mock[SubscriptionOrchestrationService]

  override def beforeEach(): Unit = {
    reset(mockSubscriptionOrchestrationService)
    super.beforeEach()
  }

  private def mockSignUpAndCreateIncomeSourcesFromTaskList(nino: String,
                                                           createIncomeSourcesModel: CreateIncomeSourcesModel
                                                          )(result: Future[Either[ConnectorError, Option[SubscriptionSuccess]]]): Unit = {
    when(mockSubscriptionOrchestrationService.signUpAndCreateIncomeSourcesFromTaskList(
      eql(createIncomeSourcesModel),
      any()
    )(any[HeaderCarrier])).thenReturn(result)
  }

  def mockSignUpAndCreateIncomeSourcesFromTaskListSuccess(nino: String, createIncomeSourcesModel: CreateIncomeSourcesModel): Unit =
    mockSignUpAndCreateIncomeSourcesFromTaskList(nino, createIncomeSourcesModel)(Future.successful(testSubscriptionSuccess))

  def mockSignUpAndCreateIncomeSourcesFromTaskListFailure(nino: String, createIncomeSourcesModel: CreateIncomeSourcesModel): Unit =
    mockSignUpAndCreateIncomeSourcesFromTaskList(nino, createIncomeSourcesModel)(Future.successful(testSubscriptionFailure))


  private def mockEnrolAndRefresh(mtditId: String, nino: String)(result: Future[Either[ConnectorError, String]]): Unit =
    when(
      mockSubscriptionOrchestrationService.enrolAndRefresh
      (eql(mtditId), eql(nino))
      (any[HeaderCarrier])
    )
      .thenReturn(result)

  def mockEnrolAndRefreshSuccess(mtditId: String, nino: String): Unit =
    mockEnrolAndRefresh(mtditId, nino)(Future.successful(Right(mtditId)))

  def mockEnrolFailure(mtditId: String, nino: String): Unit =
    mockEnrolAndRefresh(mtditId, nino)(Future.successful(testEnrolFailure))

  def mockRefreshFailure(mtditId: String, nino: String): Unit =
    mockEnrolAndRefresh(mtditId, nino)(Future.successful(testRefreshProfileFailure))

  def mockEnrolAndRefreshException(mtditId: String, nino: String): Unit =
    mockEnrolAndRefresh(mtditId, nino)(Future.failed(testException))
}
