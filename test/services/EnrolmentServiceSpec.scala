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

package services

import auth.{authenticatedFakeRequest, mockAuthorisedUserIdCL200, mockMtdItSaEnrolled, mockIrSaEnrolled}
import connectors.models.Enrolment.{Enrolled, NotEnrolled}
import org.scalatest.Matchers._
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.mocks.MockEnrolmentService
import uk.gov.hmrc.play.frontend.auth.AuthenticationProviderIds
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.UnitTestTrait

import scala.concurrent.Future

class EnrolmentServiceSpec extends UnitTestTrait
  with MockEnrolmentService {

  val isEnrolled = (e: Enrolled) => e match {
    case x =>
      x shouldBe Enrolled
      Future.successful(Results.Ok)
  }

  val isNotEnrolled = (e: Enrolled) => e match {
    case x =>
      x shouldBe NotEnrolled
      Future.successful(Results.Ok)
  }

  implicit def hcUtil(implicit request: FakeRequest[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

  "EnrolmentService" should {
    "return is enrolled for an mtd-it-sa enrolled user" in {
      implicit val request = authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockMtdItSaEnrolled)
      await(TestEnrolmentService.checkMtdItsaEnrolment(isEnrolled)(hcUtil(request)))
    }

    "return not enrolled for a user without an mtd-it-sa enrolment" in {
      implicit val request = authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockAuthorisedUserIdCL200)
      await(TestEnrolmentService.checkMtdItsaEnrolment(isNotEnrolled)(hcUtil(request)))
    }

    "return is enrolled for an ir-sa enrolled user" in {
      implicit val request = authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockIrSaEnrolled)
      await(TestEnrolmentService.checkIrSaEnrolment(isEnrolled)(hcUtil(request)))
    }

    "return not enrolled for a user without an ir-sa enrolment" in {
      implicit val request = authenticatedFakeRequest(AuthenticationProviderIds.GovernmentGatewayId, mockAuthorisedUserIdCL200)
      await(TestEnrolmentService.checkIrSaEnrolment(isNotEnrolled)(hcUtil(request)))
    }
  }

}
