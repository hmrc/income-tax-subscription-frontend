/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.individual.accountingperiod

import config.featureswitch.FeatureSwitch.EmailCaptureConsent
import config.featureswitch.FeatureSwitching
import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import forms.individual.accountingperiod.AccountingPeriodForm
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.basGatewaySignIn
import helpers.servicemocks.AuthStub
import models.common.BusinessAccountingPeriod
import models.common.BusinessAccountingPeriod._
import play.api.http.Status._
import play.api.libs.json.Json
import utilities.SubscriptionDataKeys.AccountingPeriod

class AccountingPeriodControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val serviceNameGovUk = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"

  s"GET ${controllers.individual.accountingperiod.routes.AccountingPeriodController.show.url}" when {
    "user is not authorised" must {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.accountingPeriod

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn())
        )
      }
    }
    "user is authorised" must {
      "return OK" when {
        "an accounting period is returned" which {
          "is the sixth the fifth" in {
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              AccountingPeriod, OK, Json.toJson(BusinessAccountingPeriod.SixthAprilToFifthApril.key))

            val result = IncomeTaxSubscriptionFrontend.accountingPeriod

            result must have(
              httpStatus(OK),
              pageTitle(messages("accounting-period.heading") + serviceNameGovUk),
              radioButtonSet(AccountingPeriodForm.fieldName, Some("6 April to 5 April"))
            )
          }
          "is the first to thirty first" in {
            AuthStub.stubAuthSuccess()
            IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(
              AccountingPeriod, OK, Json.toJson(BusinessAccountingPeriod.FirstAprilToThirtyFirstMarch.key))

            val result = IncomeTaxSubscriptionFrontend.accountingPeriod

            result must have(
              httpStatus(OK),
              pageTitle(messages("accounting-period.heading") + serviceNameGovUk),
              radioButtonSet(AccountingPeriodForm.fieldName, Some("1 April to 31 March"))
            )
          }
        }
        "no accounting period is returned" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubGetSubscriptionDetails(AccountingPeriod, NO_CONTENT)

          val result = IncomeTaxSubscriptionFrontend.accountingPeriod

          result must have(
            httpStatus(OK),
            pageTitle(messages("accounting-period.heading") + serviceNameGovUk),
            radioButtonSet(AccountingPeriodForm.fieldName, None)
          )
        }
      }
    }
  }

  s"POST ${controllers.individual.accountingperiod.routes.AccountingPeriodController.submit.url}" when {
    "user is not authorised" must {
      "redirect to the login page" in {
        AuthStub.stubUnauthorised()

        val result = IncomeTaxSubscriptionFrontend.submitAccountingPeriod(Some(SixthAprilToFifthApril))

        result must have(
          httpStatus(SEE_OTHER),
          redirectURI(basGatewaySignIn())
        )
      }
    }
    "user is authorised" must {
      "return SEE_OTHER and save the accounting period" when {
        "email capture consent feature switch enabled" in {
          enable(EmailCaptureConsent)
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[BusinessAccountingPeriod](AccountingPeriod, SixthAprilToFifthApril)

          val result = IncomeTaxSubscriptionFrontend.submitAccountingPeriod(Some(SixthAprilToFifthApril))

          result must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.individual.email.routes.CaptureConsentController.show().url)
          )
        }
        "email capture consent feature switch disabled" in {
          disable(EmailCaptureConsent)
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[BusinessAccountingPeriod](AccountingPeriod, FirstAprilToThirtyFirstMarch)

          val result = IncomeTaxSubscriptionFrontend.submitAccountingPeriod(Some(FirstAprilToThirtyFirstMarch))

          result must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.individual.routes.WhatYouNeedToDoController.show.url)
          )
        }
        "user selects 'neither of these'" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails[BusinessAccountingPeriod](AccountingPeriod, OtherAccountingPeriod)

          val result = IncomeTaxSubscriptionFrontend.submitAccountingPeriod(Some(OtherAccountingPeriod))

          result must have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.individual.accountingperiod.routes.AccountingPeriodNonStandardController.show.url)
          )
        }
      }
      "return BAD_REQUEST" when {
        "no accounting period is selected" in {
          AuthStub.stubAuthSuccess()

          val result = IncomeTaxSubscriptionFrontend.submitAccountingPeriod(None)

          result must have(
            httpStatus(BAD_REQUEST),
            errorDisplayed()
          )
        }
      }
      "return an exception" when {
        "failed to save accounting period" in {
          AuthStub.stubAuthSuccess()
          IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetailsFailure(AccountingPeriod)

          val result = IncomeTaxSubscriptionFrontend.submitAccountingPeriod(Some(FirstAprilToThirtyFirstMarch))

          result must have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }


}
