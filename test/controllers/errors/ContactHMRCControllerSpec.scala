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

package controllers.errors

import config.{AppConfig, MockConfig}
import controllers.individual.ControllerBaseSpec
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import services.individual.mocks.MockAuthService
import services.mocks.MockAuditingService
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthorisedFunctions}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig
import views.html.errors.ContactHMRC
import views.html.individual.tasklist.taxyear.NonEligibleVoluntary

import scala.concurrent.Future

class ContactHMRCControllerSpec extends ControllerBaseSpec {

  override val appConfig: AppConfig = MockConfig

  private val baseUrl = "/hello-world"
  private val serviceId = "testId"
  private val referrerUrl = "testUrl"

  override val controllerName: String = "ContactHMRCController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map.empty

  trait Setup {
    val contactHMRC: ContactHMRC = mock[ContactHMRC]
    val cfc: ContactFrontendConfig = mock[ContactFrontendConfig]
    val controller: ContactHMRCController = new ContactHMRCController(
      contactHMRC,
      mockAuthService,
      appConfig,
      cfc
    )
  }

  "show" must {
    "return OK with the page content" should {
      "for Individual" in new Setup {
        mockRetrievalSuccess(Some(AffinityGroup.Individual))
        when(contactHMRC(
          ArgumentMatchers.eq(routes.ContactHMRCController.submit),
          ArgumentMatchers.eq(false)
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          subscriptionRequest
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }

      "for Agent" in new Setup {
        mockRetrievalSuccess(Some(AffinityGroup.Agent))
        when(contactHMRC(
          ArgumentMatchers.eq(routes.ContactHMRCController.submit),
          ArgumentMatchers.eq(true)
        )(any(), any())).thenReturn(HtmlFormat.empty)

        val result: Future[Result] = controller.show(
          subscriptionRequest
        )

        status(result) mustBe OK
        contentType(result) mustBe Some(HTML)
      }
    }
  }

  "submit" must {
    "return SEE_OTHER to the Your Income Sources page" in new Setup {
      mockRetrievalSuccess(None)
      when(cfc.baseUrl).thenReturn(Some(baseUrl))
      when(cfc.serviceId).thenReturn(Some(serviceId))
      when(cfc.referrerUrl(any())).thenReturn(Some(referrerUrl))
      val url = s"$baseUrl?service=$serviceId&referrerUrl=$referrerUrl"

      val result: Future[Result] = controller.submit(
        subscriptionRequest
      )

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(url)
    }
  }

  authorisationTests()
}
