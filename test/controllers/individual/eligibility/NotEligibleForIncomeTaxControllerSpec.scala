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

package controllers.individual.eligibility

import agent.audit.mocks.MockAuditingService
import controllers.ControllerBaseSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.{Action, AnyContent, Codec, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import views.html.individual.incometax.eligibility.NotEligibleForIncomeTax

import scala.concurrent.Future

class NotEligibleForIncomeTaxControllerSpec extends ControllerBaseSpec with MockAuditingService {

  override val controllerName: String = "NotEligibleForIncomeTaxController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  val mockNotEligibleForIncomeTax: NotEligibleForIncomeTax = mock[NotEligibleForIncomeTax]
  when(mockNotEligibleForIncomeTax()(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
    .thenReturn(HtmlFormat.empty)

  object TestCannotUseServiceController extends NotEligibleForIncomeTaxController(
    mockNotEligibleForIncomeTax,
    mockAuditingService,
    mockAuthService
  )

  "Calling the show action of the Not Eligible For Income Tax Controller" when {

    def call: Future[Result] = TestCannotUseServiceController.show(subscriptionRequest)

    "return ok (200)" in {
      val result = call
      status(result) mustBe OK
      contentType(result) mustBe Some(HTML)
      charset(result) mustBe Some(Codec.utf_8.charset)
    }
  }
}


