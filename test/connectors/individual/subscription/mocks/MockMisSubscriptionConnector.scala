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

import connectors.individual.subscription.MultipleIncomeSourcesSubscriptionConnector
import connectors.individual.subscription.httpparsers.CreateIncomeSourcesResponseHttpParser.PostCreateIncomeSourceResponse
import connectors.individual.subscription.httpparsers.SignUpIncomeSourcesResponseHttpParser.PostSignUpIncomeSourcesResponse
import models.common.subscription.SignUpSourcesFailure.SignUpIncomeSourcesFailureResponse
import models.common.subscription.SignUpSuccessResponse.SignUpSuccessful
import models.common.subscription._
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

  private def setupMockMisSignUp(nino: String, utr: String, taxYear: String)(result: Future[PostSignUpIncomeSourcesResponse]): Unit =
    when(mockMisSubscriptionConnector.signUp(
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(utr),
      ArgumentMatchers.eq(taxYear)
    )(ArgumentMatchers.any[HeaderCarrier])).thenReturn(result)

  def setupMockSignUpIncomeSourcesSuccess(nino: String, utr: String, taxYear: String): Unit =
    setupMockMisSignUp(nino, utr, taxYear)(Future.successful(Right(SignUpSuccessful(testMTDID))))

  def setupMockSignUpIncomeSourcesFailure(nino: String, utr: String, taxYear: String): Unit =
    setupMockMisSignUp(nino, utr, taxYear)(Future.successful(Left(SignUpIncomeSourcesFailureResponse(BAD_REQUEST))))

  def setupMockSignUpIncomeSourcesException(nino: String, utr: String, taxYear: String): Unit =
    setupMockMisSignUp(nino, utr, taxYear)(Future.failed(testException))

  private def setupMockCreateIncomeSourcesFromTaskList(mtdbsa: String, createIncomeSourcesModel: CreateIncomeSourcesModel)
                                                      (result: Future[PostCreateIncomeSourceResponse]): Unit =
    when(mockMisSubscriptionConnector.createIncomeSourcesFromTaskList(ArgumentMatchers.eq(mtdbsa),
      ArgumentMatchers.eq(createIncomeSourcesModel))(ArgumentMatchers.any[HeaderCarrier]))
      .thenReturn(result)

  def setupMockCreateIncomeSourcesFromTaskListSuccess(mtdbsa: String, model: CreateIncomeSourcesModel): Unit =
    setupMockCreateIncomeSourcesFromTaskList(mtdbsa, model)(Future.successful(testCreateIncomeSourcesFromTaskListSuccess))

  def setupMockCreateIncomeSourcesFromTaskListFailure(mtdbsa: String, model: CreateIncomeSourcesModel): Unit =
    setupMockCreateIncomeSourcesFromTaskList(mtdbsa, model)(Future.successful(Left(CreateIncomeSourcesFailure(BAD_REQUEST))))

  def setupMockCreateIncomeSourcesFromTaskListException(mtdbsa: String, model: CreateIncomeSourcesModel): Unit =
    setupMockCreateIncomeSourcesFromTaskList(mtdbsa, model)(Future.failed(testException))
}
