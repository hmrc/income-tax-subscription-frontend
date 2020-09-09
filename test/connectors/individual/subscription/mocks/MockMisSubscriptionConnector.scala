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

package connectors.individual.subscription.mocks

import connectors.individual.subscription.MultipleIncomeSourcesSubscriptionConnector
import connectors.individual.subscription.httpparsers.CreateIncomeSourcesResponseHttpParser.PostCreateIncomeSourceResponse
import connectors.individual.subscription.httpparsers.SignUpIncomeSourcesResponseHttpParser.PostSignUpIncomeSourcesResponse
import models.individual.business.BusinessSubscriptionDetailsModel
import models.individual.subscription._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.http.HeaderCarrier
import utilities.UnitTestTrait
import utilities.individual.TestConstants._

import scala.concurrent.Future

trait MockMisSubscriptionConnector extends UnitTestTrait with MockitoSugar {
  val mockMisSubscriptionConnector: MultipleIncomeSourcesSubscriptionConnector = mock[MultipleIncomeSourcesSubscriptionConnector]

  private def setupMockMisSignUp(nino: String)(result: Future[PostSignUpIncomeSourcesResponse]): Unit =
    when(mockMisSubscriptionConnector.signUp(ArgumentMatchers.eq(nino))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockSignUpIncomeSourcesSuccess(nino: String): Unit =
    setupMockMisSignUp(nino)(Future.successful(Right(SignUpIncomeSourcesSuccess(testMTDID))))

  def setupMockSignUpIncomeSourcesFailure(nino: String): Unit =
    setupMockMisSignUp(nino)(Future.successful(Left(SignUpIncomeSourcesFailureResponse(BAD_REQUEST))))

  def setupMockSignUpIncomeSourcesBadFormatting(nino: String): Unit =
    setupMockMisSignUp(nino)(Future.successful(Left(BadlyFormattedSignUpIncomeSourcesResponse)))

  def setupMockSignUpIncomeSourcesException(nino: String): Unit =
    setupMockMisSignUp(nino)(Future.failed(testException))


  private def setupMockCreateIncomeSources(mtdbsa: String, businessSubscriptionDetailsModel: BusinessSubscriptionDetailsModel)
                                          (result: Future[PostCreateIncomeSourceResponse]): Unit =
    when(mockMisSubscriptionConnector.createIncomeSources(ArgumentMatchers.eq(mtdbsa),
      ArgumentMatchers.eq(businessSubscriptionDetailsModel))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockCreateIncomeSourcesSuccess(mtdbsa: String, model: BusinessSubscriptionDetailsModel): Unit =
    setupMockCreateIncomeSources(mtdbsa, model)(Future.successful(testCreateIncomeSourcesSuccess))

  def setupMockCreateIncomeSourcesFailure(mtdbsa: String, model: BusinessSubscriptionDetailsModel): Unit =
    setupMockCreateIncomeSources(mtdbsa, model)(Future.successful(Left(CreateIncomeSourcesFailureResponse(BAD_REQUEST))))

  def setupMockCreateIncomeSourcesBadFormatting(mtdbsa: String, model: BusinessSubscriptionDetailsModel): Unit =
    setupMockCreateIncomeSources(mtdbsa, model)(Future.successful(Left(BadlyFormattedCreateIncomeSourcesResponse)))

  def setupMockCreateIncomeSourcesException(mtdbsa: String, model: BusinessSubscriptionDetailsModel): Unit =
    setupMockCreateIncomeSources(mtdbsa, model)(Future.failed(testException))
}
