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

package controllers.individual.sps

import agent.audit.mocks.MockAuditingService
import config.featureswitch.FeatureSwitch.SPSEnabled
import config.featureswitch.FeatureSwitching
import controllers.ControllerBaseSpec
import play.api.http.Status
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.individual.mocks.MockApplicationCrypto
import uk.gov.hmrc.http.NotFoundException

import scala.concurrent.Future

class SPSHandoffControllerSpec extends ControllerBaseSpec
  with MockAuditingService
  with FeatureSwitching
  with MockApplicationCrypto {

  override def beforeEach(): Unit = {
    disable(SPSEnabled)
    super.beforeEach()
  }

  object TestSPSHandoffController extends SPSHandoffController(
    mockAuditingService,
    mockAuthService,
    mockApplicationCrypto
  )(executionContext, appConfig, mockMessagesControllerComponents)

  override val controllerName: String = "SPSHandoffController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map(
    "spsHandoff" -> TestSPSHandoffController.redirectToSPS
  )

  "Calling the redirectToSPS action of the SPSController with an authorised user" when {
    implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = subscriptionRequest

    def result: Future[Result] = TestSPSHandoffController.redirectToSPS(request)

    "feature switch SPSEnabled set to true" should {

      "Redirect to SPS" in {

        enable(SPSEnabled)
        mockEncrypt()
        status(result) must be(Status.SEE_OTHER)
        redirectLocation(result).get must be(s"http://localhost:9024/paperless/choose/capture?returnUrl=encryptedValue&returnLinkText=encryptedValue&regime=encryptedValue")
      }
    }

    "feature switch SPSEnabled set to false" should {

      "throw a NotFoundException" in {

        mockEncrypt()
        val ex = intercept[NotFoundException](await(result))
        ex.getMessage mustBe ("[SPSHandoffController][redirectToSPS] - SPS FS is not enabled")

      }
    }

  }

  authorisationTests()

}
