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

package auth

import services.mocks.MockEnrolmentService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.UnitTestTrait

import scala.concurrent.Future

trait MockAuthTestController extends UnitTestTrait
  with MockAuthConnector
  with MockEnrolmentService {

  object AuthTestController extends FrontendController with AuthorisedForIncomeTaxSA {
    override lazy val applicationConfig = mockConfig
    override lazy val authConnector = TestAuthConnector
    override lazy val enrolmentService = TestEnrolmentService
    override lazy val postSignInRedirectUrl = controllers.routes.EligibleController.showEligible().url

    val authorisedAsyncAction = Authorised.async {
      implicit user => implicit request => Future.successful(Ok)
    }

  }

}
