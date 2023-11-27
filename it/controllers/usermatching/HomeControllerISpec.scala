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

package controllers.usermatching

import common.Constants.ITSASessionKeys
import helpers.IntegrationTestConstants._
import helpers.servicemocks._
import helpers.{ComponentSpecBase, SessionCookieCrumbler}
import models.status.MandationStatus.Voluntary
import play.api.http.Status._

class HomeControllerISpec extends ComponentSpecBase with SessionCookieCrumbler {

  "GET /report-quarterly/income-and-expenses/sign-up" should {
    "return the guidance page" in {
      When("We hit to the guidance page route")
      val res = IncomeTaxSubscriptionFrontend.startPage()

      Then("Return the guidance page")
      res must have(
        httpStatus(SEE_OTHER),
        redirectURI(IndividualURI.indexURI)
      )
    }
  }

  "GET /report-quarterly/income-and-expenses/sign-up/index" when {
    "the user both nino and utr enrolments" when {
      "the user has a subscription" should {
        "redirect to the claim enrolment journey page" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          CitizenDetailsStub.stubCIDUserWithNinoAndUtrAndName(testNino, testUtr, testFirstName, testLastName)
          SubscriptionStub.stubGetSubscriptionFound()

          When("GET /index is called")
          val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("Should return a SEE OTHER with the claim enrolment journey page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.addMTDITOverviewURI)
          )
        }
      }

      "the user does not have a subscription" when {
        "the user is eligible" when {

          "redirect to the SPSHandoff controller" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            CitizenDetailsStub.stubCIDUserWithNinoAndUtrAndName(testNino, testUtr, testFirstName, testLastName)
            SubscriptionStub.stubGetNoSubscription()
            EligibilityStub.stubEligibilityResponse(testUtr)(response = true)
            MandationStatusStub.stubGetMandationStatus(testNino, testUtr)(Voluntary, Voluntary)
            When("GET /index is called")
            val res = IncomeTaxSubscriptionFrontend.indexPage()

            Then("Should return a SEE OTHER and re-direct to the SPSHandoff controller")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.spsHandoffRouteURI)
            )
          }
        }

        "the user is ineligible" should {
          "redirect to the Not eligible page" in {
            Given("I setup the Wiremock stubs")
            AuthStub.stubAuthSuccess()
            CitizenDetailsStub.stubCIDUserWithNinoAndUtrAndName(testNino, testUtr, testFirstName, testLastName)
            SubscriptionStub.stubGetNoSubscription()
            EligibilityStub.stubEligibilityResponse(testUtr)(response = false)

            When("GET /index is called")
            val res = IncomeTaxSubscriptionFrontend.indexPage()

            Then("Should return a SEE OTHER and re-direct to the not eligible page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(IndividualURI.notEligibleURI)
            )
          }
        }
      }

      "the subscription call fails" should {
        "return an internal server error" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthSuccess()
          CitizenDetailsStub.stubCIDUserWithNinoAndUtrAndName(testNino, testUtr, testFirstName, testLastName)
          SubscriptionStub.stubGetSubscriptionFail()

          When("GET /index is called")
          val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }

    "the user only has a nino in enrolment" when {
      "CID returned a record with UTR" when {
        "the user is eligible" when {
          "the user has a name" should {
            "continue normally" in {
              Given("I setup the Wiremock stubs")
              AuthStub.stubAuthNoUtr()
              SubscriptionStub.stubGetNoSubscription()
              CitizenDetailsStub.stubCIDUserWithNinoAndUtrAndName(testNino, testUtr, testFirstName, testLastName)
              EligibilityStub.stubEligibilityResponse(testUtr)(response = true)
              MandationStatusStub.stubGetMandationStatus(testNino, testUtr)(Voluntary, Voluntary)
              When("GET /index is called")
              val res = IncomeTaxSubscriptionFrontend.indexPage()

              Then("Should return a SEE OTHER and re-direct to the sps page")
              res must have(
                httpStatus(SEE_OTHER),
                redirectURI(IndividualURI.spsHandoffRouteURI)
              )

              val cookie = getSessionMap(res)
              cookie.keys must contain(ITSASessionKeys.UTR)
              cookie(ITSASessionKeys.UTR) mustBe testUtr

              cookie.keys must contain(ITSASessionKeys.FULLNAME)
              cookie(ITSASessionKeys.FULLNAME) mustBe testFullName
            }
            "the user has no name" should {
              "continue normally" in {
                Given("I setup the Wiremock stubs")
                AuthStub.stubAuthNoUtr()
                SubscriptionStub.stubGetNoSubscription()
                CitizenDetailsStub.stubCIDUserWithNinoAndUtrAndNoName(testNino, testUtr)
                EligibilityStub.stubEligibilityResponse(testUtr)(response = true)
                MandationStatusStub.stubGetMandationStatus(testNino, testUtr)(Voluntary, Voluntary)
                When("GET /index is called")
                val res = IncomeTaxSubscriptionFrontend.indexPage()

                Then("Should return a SEE OTHER and re-direct to the sps page")
                res must have(
                  httpStatus(SEE_OTHER),
                  redirectURI(IndividualURI.spsHandoffRouteURI)
                )

                val cookie = getSessionMap(res)
                cookie.keys must contain(ITSASessionKeys.UTR)
                cookie(ITSASessionKeys.UTR) mustBe testUtr

                cookie.keys must not contain ITSASessionKeys.FULLNAME
              }
            }
          }
        }
      }

      "CID returned a record with out a UTR" should {
        "continue normally" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthNoUtr()
          CitizenDetailsStub.stubCIDUserWithNinoAndUtrAndName(testNino, testUtr, testFirstName, testLastName)
          SubscriptionStub.stubGetNoSubscription()
          CitizenDetailsStub.stubCIDUserWithNoUtr(testNino)

          When("GET /index is called")
          val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("Should return a SEE OTHER and re-direct to the no nino page")
          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(IndividualURI.noSaURI)
          )

          val cookie = getSessionMap(res)
          cookie.keys must not contain ITSASessionKeys.UTR
          cookie.keys must not contain ITSASessionKeys.FULLNAME
        }
      }

      "CID could not find the user" should {
        "display error page" in {
          Given("I setup the Wiremock stubs")
          AuthStub.stubAuthNoUtr()
          SubscriptionStub.stubGetNoSubscription()
          CitizenDetailsStub.stubCIDNotFound(testNino)

          When("GET /index is called")
          val res = IncomeTaxSubscriptionFrontend.indexPage()

          Then("Should return an INTERNAL_SERVER_ERROR")
          res must have(
            httpStatus(INTERNAL_SERVER_ERROR),
            pageTitle("Sorry, we are experiencing technical difficulties - 500")
          )

          val cookie = getSessionMap(res)
          cookie.keys must not contain ITSASessionKeys.UTR
          cookie.keys must not contain ITSASessionKeys.FULLNAME
        }
      }
    }
  }
}



