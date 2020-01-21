/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.agent

import _root_.agent.helpers.IntegrationTestConstants._
import _root_.agent.helpers.servicemocks._
import _root_.agent.helpers.{ComponentSpecBase, SessionCookieCrumbler}
import core.config.featureswitch.FeatureSwitching
import helpers.IntegrationTestModels.testEnrolmentKey
import helpers.servicemocks.{SubscriptionStub, TaxEnrolmentsStub}
import play.api.http.Status._
import play.api.i18n.Messages

class CheckYourAnswersControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "GET /check-your-answers" when {
    "keystore returns all data" should {
      "show the check your answers page" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()

        When("GET /check-your-answers is called")
        val res = IncomeTaxSubscriptionFrontend.checkYourAnswers()

        Then("Should return a OK with the check your answers page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("agent.summary.title"))
        )
      }
    }
  }


  "POST /check-your-answers" when {
    "The whole subscription process was successful" when {
      "agent is authorised" should {
        "call subscription on the back end service and redirect to confirmation page" in {
          Given("I setup the wiremock stubs")
          AuthStub.stubAuthSuccess()
          KeystoreStub.stubFullKeystore()
          SubscriptionStub.stubSuccessfulSubscription(checkYourAnswersURI)
          TaxEnrolmentsStub.stubUpsertEnrolmentResult(testEnrolmentKey.asString, NO_CONTENT)
          KeystoreStub.stubPutMtditId()

          When("I call POST /check-your-answers")
          val res = IncomeTaxSubscriptionFrontend.submitCheckYourAnswers()

          Then("The result should have a status of SEE_OTHER and redirect to the confirmation page")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(confirmationURI)
          )

          val cookieMap = SessionCookieCrumbler.getSessionMap(res)
          cookieMap(ITSASessionKeys.MTDITID) shouldBe testMTDID

        }
      }
    }
  }

}
