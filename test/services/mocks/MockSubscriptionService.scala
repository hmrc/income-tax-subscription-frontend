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

package services.mocks

import connectors.individual.subscription.httpparsers.CreateIncomeSourcesResponseHttpParser.PostCreateIncomeSourceResponse
import connectors.individual.subscription.httpparsers.GetSubscriptionResponseHttpParser.GetSubscriptionResponse
import connectors.individual.subscription.httpparsers.SignUpIncomeSourcesResponseHttpParser.PostSignUpIncomeSourcesResponse
import connectors.individual.subscription.httpparsers.SubscriptionResponseHttpParser.SubscriptionResponse
import connectors.individual.subscription.mocks.{MockMisSubscriptionConnector, MockSubscriptionConnector}
import models.common.subscription.{CreateIncomeSourcesModel, SubscriptionFailureResponse, SubscriptionSuccess}
import models.{AgentSummary, IndividualSummary, SummaryModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import services.SubscriptionService
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

import scala.concurrent.Future

trait MockSubscriptionService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val mockSubscriptionService: SubscriptionService = mock[SubscriptionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSubscriptionService)
  }

  private def mockCreateSubscription(nino: String, summaryModel: SummaryModel, arn: Option[String])(result: Future[SubscriptionResponse]): Unit =
    when(mockSubscriptionService.submitSubscription(
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(summaryModel),
      ArgumentMatchers.eq(arn)
    )(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  private def mockSignUpIncomeSources(nino: String)(result: Future[PostSignUpIncomeSourcesResponse]): Unit =
    when(mockSubscriptionService.signUpIncomeSources(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  private def mockCreateIncomeSources(nino: String, mtdbsa: String, summaryModel: SummaryModel)
                                     (result: Future[PostCreateIncomeSourceResponse]): Unit = {
    summaryModel match {
      case summary: IndividualSummary =>
        when(mockSubscriptionService.createIncomeSources(
          ArgumentMatchers.eq(nino),
          ArgumentMatchers.eq(mtdbsa),
          ArgumentMatchers.eq(summary)
        )(ArgumentMatchers.any[HeaderCarrier]))
          .thenReturn(result)
      case summary: AgentSummary =>
        when(mockSubscriptionService.createIncomeSources(
          ArgumentMatchers.eq(nino),
          ArgumentMatchers.eq(mtdbsa),
          ArgumentMatchers.eq(summary)
        )(ArgumentMatchers.any[HeaderCarrier]))
          .thenReturn(result)
    }
  }

  private def mockCreateIncomeSourcesFromTaskList(mtdbsa: String, createIncomeSourcesModel: CreateIncomeSourcesModel)
                                                 (result: Future[PostCreateIncomeSourceResponse]): Unit = {

    when(mockSubscriptionService.createIncomeSourcesFromTaskList(
      ArgumentMatchers.eq(mtdbsa),
      ArgumentMatchers.eq(createIncomeSourcesModel)
    )(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)
  }

  def mockSignUpIncomeSourcesSuccess(nino: String): Unit =
    mockSignUpIncomeSources(nino)(Future.successful(testSignUpIncomeSourcesSuccess))

  def mockSignUpIncomeSourcesFailure(nino: String): Unit =
    mockSignUpIncomeSources(nino)(Future.successful(testSignUpIncomeSourcesFailure))

  def mockSignUpIncomeSourcesException(nino: String): Unit =
    mockSignUpIncomeSources(nino)(Future.failed(testException))

  def mockCreateIncomeSourcesSuccess(nino: String, mtdbsa: String, summaryModel: SummaryModel): Unit =
    mockCreateIncomeSources(nino, mtdbsa, summaryModel)(Future.successful(testCreateIncomeSourcesSuccess))

  def mockCreateIncomeSourcesFailure(nino: String, mtdbsa: String, summaryModel: SummaryModel): Unit =
    mockCreateIncomeSources(nino, mtdbsa, summaryModel)(Future.successful(testCreateIncomeSourcesFailure))

  def mockCreateIncomeSourcesException(nino: String, mtdbsa: String, individualSummary: IndividualSummary): Unit =
    mockCreateIncomeSources(nino, mtdbsa, individualSummary)(Future.failed(testException))

  def mockCreateIncomeSourcesFromTaskListSuccess(mtdbsa: String, createIncomeSourcesModel: CreateIncomeSourcesModel): Unit =
    mockCreateIncomeSourcesFromTaskList(mtdbsa, createIncomeSourcesModel)(Future.successful(testCreateIncomeSourcesFromTaskListSuccess))

  def mockCreateIncomeSourcesFromTaskListFailure(mtdbsa: String, createIncomeSourcesModel: CreateIncomeSourcesModel): Unit =
    mockCreateIncomeSourcesFromTaskList(mtdbsa, createIncomeSourcesModel)(Future.successful(testCreateIncomeSourcesFromTaskListFailure))

  def mockCreateIncomeSourcesFromTaskListException(mtdbsa: String, createIncomeSourcesModel: CreateIncomeSourcesModel): Unit =
    mockCreateIncomeSourcesFromTaskList(mtdbsa, createIncomeSourcesModel)(Future.failed(testException))

  def mockCreateSubscriptionSuccess(nino: String, summaryModel: SummaryModel, arn: Option[String]): Unit =
    mockCreateSubscription(nino, summaryModel, arn)(Future.successful(testSubscriptionSuccess))

  def mockCreateSubscriptionFailure(nino: String, summaryModel: SummaryModel, arn: Option[String]): Unit =
    mockCreateSubscription(nino, summaryModel, arn)(Future.successful(testSubscriptionFailure))

  def mockCreateSubscriptionException(nino: String, summaryModel: SummaryModel, arn: Option[String]): Unit =
    mockCreateSubscription(nino, summaryModel, arn)(Future.failed(testException))

  private def mockGetSubscription(nino: String)(result: Future[GetSubscriptionResponse]): Unit =
    when(mockSubscriptionService.getSubscription(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockGetSubscriptionFound(nino: String): Unit =
    mockGetSubscription(nino)(Future.successful(Right(Some(SubscriptionSuccess(testMTDID)))))

  def setupMockGetSubscriptionNotFound(nino: String): Unit =
    mockGetSubscription(nino)(Future.successful(Right(None)))

  def setupMockGetSubscriptionFailure(nino: String): Unit =
    mockGetSubscription(nino)(Future.successful(Left(SubscriptionFailureResponse(BAD_REQUEST))))

  def setupMockGetSubscriptionException(nino: String): Unit =
    mockGetSubscription(nino)(Future.failed(testException))
}

trait TestSubscriptionService extends MockSubscriptionConnector with MockMisSubscriptionConnector {

  object TestSubscriptionService extends SubscriptionService(
    subscriptionConnector = mockSubscriptionConnector,
    multipleIncomeSourcesSubscriptionConnector = mockMisSubscriptionConnector
  )

}
