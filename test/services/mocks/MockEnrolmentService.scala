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

import audit.Logging
import auth.MockAuthConnector
import connectors.mocks.MockEnrolmentConnector
import connectors.models.Enrolment.Enrolled
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import play.api.mvc.Result
import services.EnrolmentService
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.UnitTestTrait

import scala.concurrent.Future

trait MockEnrolmentService extends UnitTestTrait
  with MockAuthConnector
  with MockEnrolmentConnector {

  val mockEnrolmentService = mock[EnrolmentService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEnrolmentService)
  }

  def setupMockEnrolmentGetNino(arn: String): Unit =
    when(mockEnrolmentService.getNino(ArgumentMatchers.any[HeaderCarrier]())).thenReturn(Future.successful(Some(arn)))

  def setupMockEnrolmentGetARNFailure(exception: Throwable): Unit =
    when(mockEnrolmentService.getNino(ArgumentMatchers.any[HeaderCarrier]())).thenReturn(Future.failed(exception))

  def setupMockEnrolmentCheckItsaEnrolment(): Unit =
    when(mockEnrolmentService.checkItsaEnrolment(ArgumentMatchers.any[Enrolled => Future[Result]]())(ArgumentMatchers.any[HeaderCarrier]()))
      .thenAnswer(new Answer[Future[Result]] {
        override def answer(invocation: InvocationOnMock): Future[Result] = {
          invocation.getArgument[Enrolled => Future[Result]](0).apply(Enrolled)
        }
      })

  object TestEnrolmentService extends EnrolmentService(TestAuthConnector, TestEnrolmentConnector, app.injector.instanceOf[Logging])

}
