/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.agent.tasklist.ukproperty

import common.Constants.ITSASessionKeys
import connectors.stubs.{IncomeTaxSubscriptionConnectorStub, SessionDataConnectorStub}
import forms.agent.UkPropertyStartDateBeforeLimitForm
import helpers.IntegrationTestConstants.{basGatewaySignIn, testUtr}
import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import models.common.PropertyModel
import models.{No, Yes}
import play.api.http.Status._
import play.api.libs.json.{JsString, Json}
import utilities.SubscriptionDataKeys.Property
import utilities.agent.TestConstants.testNino

class PropertyStartDateBeforeLimitControllerISpec extends ComponentSpecBase {

  lazy val controller: PropertyStartDateBeforeLimitController = app.injector.instanceOf[PropertyStartDateBeforeLimitController]

  val serviceNameGovUk = " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK"

  s"GET ${routes.PropertyStartDateBeforeLimitController.show().url}" when {
    "the user is unauthenticated" should {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.ukPropertyStartDateBeforeLimit()

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/business/property-start-date-before-limit"))
        )
      }
    }
    "the user is authenticated" should {
      "display the page" when {
        "the question has been answered No previously" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(PropertyModel(
            startDateBeforeLimit = Some(false)
          )))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          val res = IncomeTaxSubscriptionFrontend.ukPropertyStartDateBeforeLimit()

          res must have(
            httpStatus(OK),
            pageTitle(messages("agent.uk-property.start-date-before-limit.heading") + serviceNameGovUk),
            radioButtonSet(UkPropertyStartDateBeforeLimitForm.startDateBeforeLimit, Some("No")),
          )
        }
        "the question has been answered Yes previously" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, OK, Json.toJson(PropertyModel(
            startDateBeforeLimit = Some(true)
          )))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          val res = IncomeTaxSubscriptionFrontend.ukPropertyStartDateBeforeLimit()

          res must have(
            httpStatus(OK),
            pageTitle(messages("agent.uk-property.start-date-before-limit.heading") + serviceNameGovUk),
            radioButtonSet(UkPropertyStartDateBeforeLimitForm.startDateBeforeLimit, Some("Yes")),
          )
        }
        "the question has not been answered" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

          val res = IncomeTaxSubscriptionFrontend.ukPropertyStartDateBeforeLimit()

          res must have(
            httpStatus(OK),
            pageTitle(messages("agent.uk-property.start-date-before-limit.heading") + serviceNameGovUk),
            radioButtonSet(UkPropertyStartDateBeforeLimitForm.startDateBeforeLimit, None),
          )
        }
      }
    }
  }

  s"POST ${routes.PropertyStartDateBeforeLimitController.submit().url}" when {
    "the user is unauthenticated" should {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDateBeforeLimit()(Some(Yes))

        res must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/client/business/property-start-date-before-limit"))
        )
      }
    }
    "the user is authenticated" should {
      "return BAD_REQUEST when nothing is submitted" in {
        AuthStub.stubAuthSuccess()
        IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
        SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))

        val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDateBeforeLimit()(None)

        res must have(
          httpStatus(BAD_REQUEST),
          pageTitle("Error: " + messages("agent.uk-property.start-date-before-limit.heading") + " - Sign up your clients for Making Tax Digital for Income Tax - GOV.UK")
        )
      }
      "throw an exception" when {
        "failed the save uk property start date before limit" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
          SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(Property)


          val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDateBeforeLimit()(Some(Yes))

          res must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
      "return SEE_OTHER and redirect to check your answers page" when {
        "user selects Yes to start date before limit" when {
          "not in edit mode" in {
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(startDateBeforeLimit = Some(true)))
            IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

            val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDateBeforeLimit()(Some(Yes))

            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.PropertyCheckYourAnswersController.show().url)
            )
          }
          "in edit mode" in {
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(startDateBeforeLimit = Some(true)))
            IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

            val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDateBeforeLimit(isEditMode = true)(Some(Yes))

            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.PropertyCheckYourAnswersController.show(editMode = true).url)
            )
          }
          "in global edit mode" in {
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(startDateBeforeLimit = Some(true)))
            IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

            val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDateBeforeLimit(isEditMode = true, isGlobalEdit = true)(Some(Yes))

            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.PropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url)
            )
          }

        }
      }
      "return SEE_OTHER and redirect to the property start date page" when {
        "user selects No to start date before limit" when {
          "in not edit mode" in {
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(startDateBeforeLimit = Some(false)))
            IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

            val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDateBeforeLimit()(Some(No))


            Then("Should return a SEE_OTHER and redirect to check your answers page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.PropertyStartDateController.show().url)
            )
          }
          "in edit mode" in {
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(startDateBeforeLimit = Some(false)))
            IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

            val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDateBeforeLimit(isEditMode = true)(Some(No))


            Then("Should return a SEE_OTHER and redirect to check your answers page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.PropertyStartDateController.show(editMode = true).url)
            )
          }
          "in global edit mode" in {
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(Property, NO_CONTENT)
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.NINO)(OK, JsString(testNino))
            SessionDataConnectorStub.stubGetSessionData(ITSASessionKeys.UTR)(OK, JsString(testUtr))
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(PropertyModel(startDateBeforeLimit = Some(false)))
            IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

            val res = IncomeTaxSubscriptionFrontend.submitUkPropertyStartDateBeforeLimit(isEditMode = true, isGlobalEdit = true)(Some(No))


            Then("Should return a SEE_OTHER and redirect to check your answers page")
            res must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.PropertyStartDateController.show(editMode = true, isGlobalEdit = true).url)
            )
          }
        }
      }
    }
  }

  "backUrl" must {
    "redirect to Income Sources page when not in edit mode" in {
      controller.backUrl(isEditMode = false, isGlobalEdit = false) mustBe
        controllers.agent.tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    }
    "redirect to UK Property CYA when in edit mode" in {
      controller.backUrl(isEditMode = true, isGlobalEdit = false) mustBe
        routes.PropertyCheckYourAnswersController.show(editMode = true).url
    }
    "redirect to UK Property CYA when in global edit mode" in {
      controller.backUrl(isEditMode = true, isGlobalEdit = true) mustBe
        routes.PropertyCheckYourAnswersController.show(editMode = true, isGlobalEdit = true).url
    }

  }
}

