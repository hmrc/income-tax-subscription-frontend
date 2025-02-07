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

package controllers.individual.tasklist.overseasproperty

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import forms.individual.business.ForeignPropertyStartDateBeforeLimitForm
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.AuthStub
import models.common.OverseasPropertyModel
import models.{No, Yes}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.{AccountingPeriodUtil, SubscriptionDataKeys}

import java.time.LocalDate

class ForeignPropertyStartDateBeforeLimitControllerISpec extends ComponentSpecBase {

  s"GET ${routes.ForeignPropertyStartDateBeforeLimitController.show()}" when {
    "the user is unauthenticated" should {
      "redirect the user to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.foreignPropertyStartDateBeforeLimit()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/business/foreign-property-start-date-before-limit"))
        )
      }
    }
    "the user is authenticated" should {
      "display the page" when {
        "the question has not previously been answered" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            id = SubscriptionDataKeys.OverseasProperty,
            responseStatus = NO_CONTENT
          )

          val result = IncomeTaxSubscriptionFrontend.foreignPropertyStartDateBeforeLimit()

          result must have(
            httpStatus(OK),
            pageTitle(messages("individual.foreign-property.start-date-before-limit.heading", startDateLimit.getYear.toString) + serviceNameGovUk),
            radioButtonSet(ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimit, None)
          )
        }
        "the question has been answered 'Yes' previously" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            id = SubscriptionDataKeys.OverseasProperty,
            responseStatus = OK,
            responseBody = Json.toJson(OverseasPropertyModel(startDateBeforeLimit = Some(true)))
          )

          val result = IncomeTaxSubscriptionFrontend.foreignPropertyStartDateBeforeLimit(isEditMode = true)

          result must have(
            httpStatus(OK),
            pageTitle(messages("individual.foreign-property.start-date-before-limit.heading", startDateLimit.getYear.toString) + serviceNameGovUk),
            radioButtonSet(ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimit, Some("Yes"))
          )
        }
        "the question has been answered 'No' previously" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            id = SubscriptionDataKeys.OverseasProperty,
            responseStatus = OK,
            responseBody = Json.toJson(OverseasPropertyModel(startDateBeforeLimit = Some(false)))
          )

          val result = IncomeTaxSubscriptionFrontend.foreignPropertyStartDateBeforeLimit(isGlobalEdit = true)

          result must have(
            httpStatus(OK),
            pageTitle(messages("individual.foreign-property.start-date-before-limit.heading", startDateLimit.getYear.toString) + serviceNameGovUk),
            radioButtonSet(ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimit, Some("No"))
          )
        }
      }
    }
  }

  s"POST ${routes.ForeignPropertyStartDateBeforeLimitController.submit().url}" when {
    "the user is unauthenticated" should {
      "redirect the user to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.submitForeignPropertyStartDateBeforeLimit()(request = None)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/business/foreign-property-start-date-before-limit"))
        )
      }
    }
    "the user is authenticated" should {
      "display a bad request error" when {
        "the user does not select an answer" in {
          AuthStub.stubAuthSuccess()

          val result = IncomeTaxSubscriptionFrontend.submitForeignPropertyStartDateBeforeLimit()(request = None)

          result must have(
            httpStatus(BAD_REQUEST),
            pageTitle(s"Error: ${messages("individual.foreign-property.start-date-before-limit.heading", startDateLimit.getYear.toString) + serviceNameGovUk}"),
            radioButtonSet(ForeignPropertyStartDateBeforeLimitForm.startDateBeforeLimit, None)
          )
        }
      }
      "redirect to the property start date page" when {
        "the user answers 'No'" when {
          "not in edit mode" in {
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              id = SubscriptionDataKeys.OverseasProperty,
              responseStatus = NO_CONTENT
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(
              property = OverseasPropertyModel(startDateBeforeLimit = Some(false))
            )
            IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(
              id = SubscriptionDataKeys.IncomeSourceConfirmation
            )

            val result = IncomeTaxSubscriptionFrontend.submitForeignPropertyStartDateBeforeLimit()(request = Some(No))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.ForeignPropertyStartDateController.show().url)
            )
          }
          "in edit mode" in {
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              id = SubscriptionDataKeys.OverseasProperty,
              responseStatus = OK,
              responseBody = Json.toJson(OverseasPropertyModel(startDateBeforeLimit = Some(true)))
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(
              property = OverseasPropertyModel(startDateBeforeLimit = Some(false))
            )
            IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(
              id = SubscriptionDataKeys.IncomeSourceConfirmation
            )

            val result = IncomeTaxSubscriptionFrontend.submitForeignPropertyStartDateBeforeLimit(isEditMode = true)(request = Some(No))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.ForeignPropertyStartDateController.show(editMode = true).url)
            )
          }
          "in global edit mode" in {
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              id = SubscriptionDataKeys.OverseasProperty,
              responseStatus = OK,
              responseBody = Json.toJson(OverseasPropertyModel(startDateBeforeLimit = Some(true)))
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(
              property = OverseasPropertyModel(startDateBeforeLimit = Some(false))
            )
            IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(
              id = SubscriptionDataKeys.IncomeSourceConfirmation
            )

            val result = IncomeTaxSubscriptionFrontend.submitForeignPropertyStartDateBeforeLimit(isGlobalEdit = true)(request = Some(No))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.ForeignPropertyStartDateController.show(isGlobalEdit = true).url)
            )
          }
        }
      }
      "redirect to the property accounting method page" when {
        "the user answers 'Yes'" when {
          "not in edit mode" in {
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              id = SubscriptionDataKeys.OverseasProperty,
              responseStatus = NO_CONTENT
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(
              property = OverseasPropertyModel(startDateBeforeLimit = Some(true))
            )
            IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(
              id = SubscriptionDataKeys.IncomeSourceConfirmation
            )

            val result = IncomeTaxSubscriptionFrontend.submitForeignPropertyStartDateBeforeLimit()(request = Some(Yes))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.OverseasPropertyAccountingMethodController.show().url)
            )
          }
        }
      }
      "redirect to the foreign property check your answers page" when {
        "the user answers 'Yes'" when {
          "in edit mode" in {
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              id = SubscriptionDataKeys.OverseasProperty,
              responseStatus = OK,
              responseBody = Json.toJson(OverseasPropertyModel(startDateBeforeLimit = Some(false)))
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(
              property = OverseasPropertyModel(startDateBeforeLimit = Some(true))
            )
            IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(
              id = SubscriptionDataKeys.IncomeSourceConfirmation
            )

            val result = IncomeTaxSubscriptionFrontend.submitForeignPropertyStartDateBeforeLimit(isEditMode = true)(request = Some(Yes))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.OverseasPropertyCheckYourAnswersController.show(editMode = true).url)
            )
          }
          "in global edit mode" in {
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              id = SubscriptionDataKeys.OverseasProperty,
              responseStatus = OK,
              responseBody = Json.toJson(OverseasPropertyModel(startDateBeforeLimit = Some(false)))
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(
              property = OverseasPropertyModel(startDateBeforeLimit = Some(true))
            )
            IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(
              id = SubscriptionDataKeys.IncomeSourceConfirmation
            )

            val result = IncomeTaxSubscriptionFrontend.submitForeignPropertyStartDateBeforeLimit(isGlobalEdit = true)(request = Some(Yes))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.OverseasPropertyCheckYourAnswersController.show(isGlobalEdit = true).url)
            )
          }
        }
      }
      "return an internal server error" when {
        "there was a problem fetching the property income source during saving" in {
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            id = SubscriptionDataKeys.OverseasProperty,
            responseStatus = INTERNAL_SERVER_ERROR
          )

          val result = IncomeTaxSubscriptionFrontend.submitForeignPropertyStartDateBeforeLimit()(request = Some(Yes))

          result must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "there was a problem saving the property income source" in {
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            id = SubscriptionDataKeys.OverseasProperty,
            responseStatus = NO_CONTENT
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(
            id = SubscriptionDataKeys.OverseasProperty
          )

          val result = IncomeTaxSubscriptionFrontend.submitForeignPropertyStartDateBeforeLimit()(request = Some(Yes))

          result must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "there was a problem when removing the income sources confirmation" in {
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            id = SubscriptionDataKeys.OverseasProperty,
            responseStatus = NO_CONTENT
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveOverseasProperty(
            property = OverseasPropertyModel(startDateBeforeLimit = Some(true))
          )
          IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetailsFailure(
            id = SubscriptionDataKeys.IncomeSourceConfirmation
          )

          val result = IncomeTaxSubscriptionFrontend.submitForeignPropertyStartDateBeforeLimit()(request = Some(Yes))

          result must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }

  lazy val serviceNameGovUk = " - Use software to send Income Tax updates - GOV.UK"
  lazy val startDateLimit: LocalDate = AccountingPeriodUtil.getStartDateLimit

}
