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

package controllers.individual.tasklist.ukproperty

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import forms.individual.business.PropertyStartDateBeforeLimitForm
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.AuthStub
import models.common.PropertyModel
import models.{No, Yes}
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.{AccountingPeriodUtil, SubscriptionDataKeys}

import java.time.LocalDate

class PropertyStartDateBeforeLimitControllerISpec extends ComponentSpecBase {

  s"GET ${routes.PropertyStartDateBeforeLimitController.show()}" when {
    "the user is unauthenticated" should {
      "redirect the user to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.ukPropertyStartDateBeforeLimit()

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/business/property-start-date-before-limit"))
        )
      }
    }
    "the user is authenticated" should {
      "display the page" when {
        "the question has not previously been answered" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            id = SubscriptionDataKeys.Property,
            responseStatus = NO_CONTENT
          )

          val result = IncomeTaxSubscriptionFrontend.ukPropertyStartDateBeforeLimit()

          result must have(
            httpStatus(OK),
            pageTitle(messages("property.start-date-before-limit.heading", startDateLimit.getYear.toString) + serviceNameGovUk),
            radioButtonSet(PropertyStartDateBeforeLimitForm.startDateBeforeLimit, None)
          )
        }
        "the question has been answered 'Yes' previously" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            id = SubscriptionDataKeys.Property,
            responseStatus = OK,
            responseBody = Json.toJson(PropertyModel(startDateBeforeLimit = Some(true)))
          )

          val result = IncomeTaxSubscriptionFrontend.ukPropertyStartDateBeforeLimit(isEditMode = true)

          result must have(
            httpStatus(OK),
            pageTitle(messages("property.start-date-before-limit.heading", startDateLimit.getYear.toString) + serviceNameGovUk),
            radioButtonSet(PropertyStartDateBeforeLimitForm.startDateBeforeLimit, Some("Yes"))
          )
        }
        "the question has been answered 'No' previously" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            id = SubscriptionDataKeys.Property,
            responseStatus = OK,
            responseBody = Json.toJson(PropertyModel(startDateBeforeLimit = Some(false)))
          )

          val result = IncomeTaxSubscriptionFrontend.ukPropertyStartDateBeforeLimit(isGlobalEdit = true)

          result must have(
            httpStatus(OK),
            pageTitle(messages("property.start-date-before-limit.heading", startDateLimit.getYear.toString) + serviceNameGovUk),
            radioButtonSet(PropertyStartDateBeforeLimitForm.startDateBeforeLimit, Some("No"))
          )
        }
      }
    }
  }

  s"POST ${routes.PropertyStartDateBeforeLimitController.submit().url}" when {
    "the user is unauthenticated" should {
      "redirect the user to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.submitUKPropertyStartDateBeforeLimit()(request = None)

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn("/business/property-start-date-before-limit"))
        )
      }
    }
    "the user is authenticated" should {
      "display a bad request error" when {
        "the user does not select an answer" in {
          AuthStub.stubAuthSuccess()

          val result = IncomeTaxSubscriptionFrontend.submitUKPropertyStartDateBeforeLimit()(request = None)

          result must have(
            httpStatus(BAD_REQUEST),
            pageTitle(s"Error: ${messages("property.start-date-before-limit.heading", startDateLimit.getYear.toString) + serviceNameGovUk}"),
            radioButtonSet(PropertyStartDateBeforeLimitForm.startDateBeforeLimit, None)
          )
        }
      }
      "redirect to the property start date page" when {
        "the user answers 'No'" when {
          "not in edit mode" in {
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              id = SubscriptionDataKeys.Property,
              responseStatus = NO_CONTENT
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(
              property = PropertyModel(startDateBeforeLimit = Some(false))
            )
            IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(
              id = SubscriptionDataKeys.IncomeSourceConfirmation
            )

            val result = IncomeTaxSubscriptionFrontend.submitUKPropertyStartDateBeforeLimit()(request = Some(No))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.PropertyStartDateController.show().url)
            )
          }
          "in edit mode" in {
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              id = SubscriptionDataKeys.Property,
              responseStatus = OK,
              responseBody = Json.toJson(PropertyModel(startDateBeforeLimit = Some(true)))
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(
              property = PropertyModel(startDateBeforeLimit = Some(false))
            )
            IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(
              id = SubscriptionDataKeys.IncomeSourceConfirmation
            )

            val result = IncomeTaxSubscriptionFrontend.submitUKPropertyStartDateBeforeLimit(isEditMode = true)(request = Some(No))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.PropertyStartDateController.show(editMode = true).url)
            )
          }
          "in global edit mode" in {
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              id = SubscriptionDataKeys.Property,
              responseStatus = OK,
              responseBody = Json.toJson(PropertyModel(startDateBeforeLimit = Some(true)))
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(
              property = PropertyModel(startDateBeforeLimit = Some(false))
            )
            IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(
              id = SubscriptionDataKeys.IncomeSourceConfirmation
            )

            val result = IncomeTaxSubscriptionFrontend.submitUKPropertyStartDateBeforeLimit(isGlobalEdit = true)(request = Some(No))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.PropertyStartDateController.show(isGlobalEdit = true).url)
            )
          }
        }
      }
      "redirect to the property accounting method page" when {
        "the user answers 'Yes'" when {
          "not in edit mode" in {
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              id = SubscriptionDataKeys.Property,
              responseStatus = NO_CONTENT
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(
              property = PropertyModel(startDateBeforeLimit = Some(true))
            )
            IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(
              id = SubscriptionDataKeys.IncomeSourceConfirmation
            )

            val result = IncomeTaxSubscriptionFrontend.submitUKPropertyStartDateBeforeLimit()(request = Some(Yes))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.PropertyAccountingMethodController.show().url)
            )
          }
        }
      }
      "redirect to the property check your answers page" when {
        "the user answers 'Yes'" when {
          "in edit mode" in {
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              id = SubscriptionDataKeys.Property,
              responseStatus = OK,
              responseBody = Json.toJson(PropertyModel(startDateBeforeLimit = Some(false)))
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(
              property = PropertyModel(startDateBeforeLimit = Some(true))
            )
            IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(
              id = SubscriptionDataKeys.IncomeSourceConfirmation
            )

            val result = IncomeTaxSubscriptionFrontend.submitUKPropertyStartDateBeforeLimit(isEditMode = true)(request = Some(Yes))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.PropertyCheckYourAnswersController.show(editMode = true).url)
            )
          }
          "in global edit mode" in {
            AuthStub.stubAuthSuccess()

            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              id = SubscriptionDataKeys.Property,
              responseStatus = OK,
              responseBody = Json.toJson(PropertyModel(startDateBeforeLimit = Some(false)))
            )
            IncomeTaxSubscriptionConnectorStub.stubSaveProperty(
              property = PropertyModel(startDateBeforeLimit = Some(true))
            )
            IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetails(
              id = SubscriptionDataKeys.IncomeSourceConfirmation
            )

            val result = IncomeTaxSubscriptionFrontend.submitUKPropertyStartDateBeforeLimit(isGlobalEdit = true)(request = Some(Yes))

            result must have(
              httpStatus(SEE_OTHER),
              redirectURI(routes.PropertyCheckYourAnswersController.show(isGlobalEdit = true).url)
            )
          }
        }
      }
      "return an internal server error" when {
        "there was a problem fetching the property income source during saving" in {
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            id = SubscriptionDataKeys.Property,
            responseStatus = INTERNAL_SERVER_ERROR
          )

          val result = IncomeTaxSubscriptionFrontend.submitUKPropertyStartDateBeforeLimit()(request = Some(Yes))

          result must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "there was a problem saving the property income source" in {
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            id = SubscriptionDataKeys.Property,
            responseStatus = NO_CONTENT
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(
            id = SubscriptionDataKeys.Property
          )

          val result = IncomeTaxSubscriptionFrontend.submitUKPropertyStartDateBeforeLimit()(request = Some(Yes))

          result must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
        "there was a problem when removing the income sources confirmation" in {
          AuthStub.stubAuthSuccess()

          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
            id = SubscriptionDataKeys.Property,
            responseStatus = NO_CONTENT
          )
          IncomeTaxSubscriptionConnectorStub.stubSaveProperty(
            property = PropertyModel(startDateBeforeLimit = Some(true))
          )
          IncomeTaxSubscriptionConnectorStub.stubDeleteSubscriptionDetailsFailure(
            id = SubscriptionDataKeys.IncomeSourceConfirmation
          )

          val result = IncomeTaxSubscriptionFrontend.submitUKPropertyStartDateBeforeLimit()(request = Some(Yes))

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
