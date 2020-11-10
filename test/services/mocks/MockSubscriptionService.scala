/*
 * Copyright 2020 HM Revenue & Customs
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
import models.individual.subscription.{SubscriptionFailureResponse, SubscriptionSuccess}
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

  private def mockCreateIncomeSources(mtdbsa: String, summaryModel: SummaryModel, isPropertyNextTaxYearEnabled: Boolean = false)
                                     (result: Future[PostCreateIncomeSourceResponse]): Unit = {
    if(summaryModel.isInstanceOf[IndividualSummary]) {
      when(mockSubscriptionService.createIncomeSources(
        ArgumentMatchers.eq(mtdbsa),
        ArgumentMatchers.eq(summaryModel.asInstanceOf[IndividualSummary]),
        ArgumentMatchers.eq(isPropertyNextTaxYearEnabled)
      )(ArgumentMatchers.any[HeaderCarrier]))
        .thenReturn(result)
    } else {
      when(mockSubscriptionService.createIncomeSources(
        ArgumentMatchers.eq(mtdbsa),
        ArgumentMatchers.eq(summaryModel.asInstanceOf[AgentSummary]),
        ArgumentMatchers.eq(isPropertyNextTaxYearEnabled)
      )(ArgumentMatchers.any[HeaderCarrier]))
        .thenReturn(result)
    }
  }

  def mockSignUpIncomeSourcesSuccess(nino: String): Unit =
    mockSignUpIncomeSources(nino)(Future.successful(testSignUpIncomeSourcesSuccess))

  def mockSignUpIncomeSourcesFailure(nino: String): Unit =
    mockSignUpIncomeSources(nino)(Future.successful(testSignUpIncomeSourcesFailure))

  def mockSignUpIncomeSourcesException(nino: String): Unit =
    mockSignUpIncomeSources(nino)(Future.failed(testException))

  def mockCreateIncomeSourcesSuccess(mtdbsa: String, summaryModel: SummaryModel, isPropertyNextTaxYearEnabled: Boolean): Unit =
    mockCreateIncomeSources(mtdbsa, summaryModel, isPropertyNextTaxYearEnabled)(Future.successful(testCreateIncomeSourcesSuccess))

  def mockCreateIncomeSourcesFailure(mtdbsa: String, summaryModel: SummaryModel, isPropertyNextTaxYearEnabled: Boolean): Unit =
    mockCreateIncomeSources(mtdbsa, summaryModel, isPropertyNextTaxYearEnabled)(Future.successful(testCreateIncomeSourcesFailure))

  def mockCreateIncomeSourcesException(mtdbsa: String, individualSummary: IndividualSummary, isPropertyNextTaxYearEnabled: Boolean): Unit =
    mockCreateIncomeSources(mtdbsa, individualSummary, isPropertyNextTaxYearEnabled)(Future.failed(testException))

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
