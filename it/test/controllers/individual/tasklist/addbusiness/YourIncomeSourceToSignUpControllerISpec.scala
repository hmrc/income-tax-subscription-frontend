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

package controllers.individual.tasklist.addbusiness

import common.Constants.ITSASessionKeys
import config.featureswitch.FeatureSwitch.DisplayName
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.IndividualURI.apaHandOffControllerUrI
import helpers.IntegrationTestConstants.{basGatewaySignIn, testNino, testUtr}
import helpers.IntegrationTestModels.*
import helpers.servicemocks.AuthStub
import models.EligibilityStatus
import models.common.business.BusinessNameModel
import models.status.MandationStatus.Voluntary
import models.status.MandationStatusModel
import play.api.http.Status.*
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys

class YourIncomeSourceToSignUpControllerISpec extends ComponentSpecBase {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(DisplayName)
  }

  val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"

  private def stubCompleteIncomeSources(selfEmployments: Seq[models.common.business.SelfEmploymentData]): Unit = {
    AuthStub.stubAuthSuccess()
    IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, selfEmployments)
    SessionDataConnectorStub.stubGetAllSessionData(Map(
      ITSASessionKeys.JourneyStateKey -> JsString(models.individual.JourneyStep.SignUp.key)
    ))
    IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
    IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
    IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true)
  }

  s"GET ${routes.YourIncomeSourceToSignUpController.show.url}" should {
    "return SEE_OTHER to login page" when {
      "user is unauthorised" in {
        AuthStub.stubUnauthorised()
        val res = IncomeTaxSubscriptionFrontend.yourIncomeSources()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn())
        )
      }
    }
    "return OK" when {
      "there are no income sources added" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, NO_CONTENT)
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.PrePopFlag, OK, Json.toJson(Some(true)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear, NO_CONTENT)
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.JourneyStateKey -> JsString(models.individual.JourneyStep.SignUp.key),
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
        ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
        ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(
          eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None
          ))
        ))

        When(s"GET ${routes.YourIncomeSourceToSignUpController.show.url} is called")
        val res = IncomeTaxSubscriptionFrontend.yourIncomeSources()

        Then("Should return a OK with the income source page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("your-income-source.heading") + serviceNameGovUk)
        )
      }
      "there are multiple income sources added" in {
        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(NO_CONTENT, testBusinesses.getOrElse(Seq.empty))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.PrePopFlag, OK, Json.toJson(Some(true)))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.SelectedTaxYear, NO_CONTENT)
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.JourneyStateKey -> JsString(models.individual.JourneyStep.SignUp.key),
          ITSASessionKeys.NINO -> JsString(testNino),
          ITSASessionKeys.UTR -> JsString(testUtr),
          ITSASessionKeys.MANDATION_STATUS -> Json.toJson(MandationStatusModel(Voluntary, Voluntary)),
          ITSASessionKeys.ELIGIBILITY_STATUS -> Json.toJson(EligibilityStatus(
            eligibleCurrentYear = true, eligibleNextYear = true, exemptionReason = None
          ))
        ))

        When(s"GET ${routes.YourIncomeSourceToSignUpController.show.url} is called")
        val res = IncomeTaxSubscriptionFrontend.yourIncomeSources()

        Then("Should return a OK with the income source page")
        res must have(
          httpStatus(OK),
          pageTitle(messages("your-income-source.heading") + serviceNameGovUk)
        )
      }
    }
  }

  s"POST ${routes.YourIncomeSourceToSignUpController.submit.url}" when {
    "the user has complete businesses" should {
      "redirect to the declaration page" in {
        Given("I setup the wiremock stubs")
        AuthStub.stubAuthSuccess()

        IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(
          OK,
          Seq(testBusiness("12345", confirmed = true))
        )
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))
        SessionDataConnectorStub.stubGetAllSessionData(Map(
          ITSASessionKeys.JourneyStateKey -> JsString(models.individual.JourneyStep.SignUp.key)
        ))

        IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true)

        When(s"POST ${routes.YourIncomeSourceToSignUpController.submit.url} is called")
        val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSources()

        Then("Should return a SEE_OTHER to the sps handoff page")
        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(apaHandOffControllerUrI)
        )

        IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true, Some(1))
      }

      "redirect to add display names when duplicate names exist and the feature is enabled" in {
        enable(DisplayName)
        stubCompleteIncomeSources(Seq(
          testBusiness("business-1", confirmed = true),
          testBusiness("business-2", confirmed = true)
        ))

        val result = IncomeTaxSubscriptionFrontend.submitYourIncomeSources()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.individual.tasklist.addbusiness.routes.AddDisplayNamesController.show().url)
        )
        IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails[Boolean](
          SubscriptionDataKeys.IncomeSourceConfirmation,
          true,
          Some(1)
        )
      }

      "redirect to SPS when names are unique and the feature is enabled" in {
        enable(DisplayName)
        stubCompleteIncomeSources(Seq(
          testBusiness("business-1", confirmed = true),
          testBusiness("business-2", confirmed = true).copy(
            businessName = Some(BusinessNameModel("Another business"))
          )
        ))

        val result = IncomeTaxSubscriptionFrontend.submitYourIncomeSources()

        result must have(httpStatus(SEE_OTHER), redirectURI(apaHandOffControllerUrI))
      }

      "redirect to SPS when duplicate names exist but the feature is not enabled" in {
        stubCompleteIncomeSources(Seq(
          testBusiness("business-1", confirmed = true),
          testBusiness("business-2", confirmed = true)
        ))

        val result = IncomeTaxSubscriptionFrontend.submitYourIncomeSources()

        result must have(httpStatus(SEE_OTHER), redirectURI(apaHandOffControllerUrI))
      }
    }
    "the user has incomplete businesses" should {
      "redirect to the incomplete income sources page" when {
        "a sole trader business is incomplete" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.JourneyStateKey -> JsString(models.individual.JourneyStep.SignUp.key)
          ))
          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(
            OK, Seq(testBusiness("12345"))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

          val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSources()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.individual.tasklist.routes.IncomeSourcesIncompleteController.show.url)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true, Some(0))
        }
        "a uk property business is incomplete" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.JourneyStateKey -> JsString(models.individual.JourneyStep.SignUp.key)
          ))
          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(
            OK, Seq(testBusiness("12345", confirmed = true))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel.copy(confirmed = false)))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel))

          val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSources()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.individual.tasklist.routes.IncomeSourcesIncompleteController.show.url)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true, Some(0))
        }
        "a foreign property business is incomplete" in {
          AuthStub.stubAuthSuccess()
          SessionDataConnectorStub.stubGetAllSessionData(Map(
            ITSASessionKeys.JourneyStateKey -> JsString(models.individual.JourneyStep.SignUp.key)
          ))
          IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(
            OK, Seq(testBusiness("12345", confirmed = true))
          )
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.Property, OK, Json.toJson(testFullPropertyModel))
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(SubscriptionDataKeys.OverseasProperty, OK, Json.toJson(testFullOverseasPropertyModel.copy(confirmed = false)))

          val res = IncomeTaxSubscriptionFrontend.submitYourIncomeSources()

          res must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.individual.tasklist.routes.IncomeSourcesIncompleteController.show.url)
          )

          IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails[Boolean](SubscriptionDataKeys.IncomeSourceConfirmation, true, Some(0))
        }
      }
    }
  }
}
