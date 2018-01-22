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

package incometax.incomesource

import core.config.featureswitch.{FeatureSwitching, TaxYearDeferralFeature}
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.IntegrationTestModels._
import helpers.servicemocks.{AuthStub, KeystoreStub}
import incometax.incomesource.forms.IncomeSourceForm
import incometax.incomesource.models.IncomeSourceModel
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.Messages

class CannotReportYetControllerISpec extends ComponentSpecBase with FeatureSwitching {

  enable(TaxYearDeferralFeature)

  override def afterAll(): Unit = {
    super.afterAll()
    disable(TaxYearDeferralFeature)
  }

  "GET /report-quarterly/income-and-expenses/sign-up/error/cannot-report-yet" when {

    "keystore not applicable" should {
      "show the error other income page" in {

        isEnabled(TaxYearDeferralFeature) shouldBe true

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubFullKeystore()
        When("GET /error/cannot-report-yet is called")
        val res = IncomeTaxSubscriptionFrontend.cannotReportYet()

        Then("Should return a OK with the cannot report yet page")
        res should have(
          httpStatus(OK),
          pageTitle(Messages("cannot-report-yet.title"))
        )
      }
    }
  }


  "POST /report-quarterly/income-and-expenses/sign-up/error/cannot-report-yet" when {

    "not in edit mode" should {

      "select the Continue button on the error other income page whilst on the Business journey" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_business)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource)
          )
        )

        When("POST /error/cannot-report-yet is called")
        val res = IncomeTaxSubscriptionFrontend.submitCannotReportYet(editMode = false)

        Then("Should return a SEE_OTHER with a redirect location of accounting method")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessAccountingMethodURI)
        )
      }

      "select the Continue button on the error other income page whilst on the Both journey" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_both)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource)
          )
        )

        When("POST /error/cannot-report-yet is called")
        val res = IncomeTaxSubscriptionFrontend.submitCannotReportYet(editMode = false)

        Then("Should return a SEE_OTHER with a redirect location of accounting method")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(businessAccountingMethodURI)
        )
      }

      "select the Continue button on the error other income page whilst on the Property journey" in {
        val keystoreIncomeSource = IncomeSourceModel(IncomeSourceForm.option_property)

        Given("I setup the Wiremock stubs")
        AuthStub.stubAuthSuccess()
        KeystoreStub.stubKeystoreData(
          keystoreData(
            incomeSource = Some(keystoreIncomeSource)
          )
        )

        When("POST /error/cannot-report-yet is called")
        val res = IncomeTaxSubscriptionFrontend.submitCannotReportYet(editMode = false)

        Then("Should return a SEE_OTHER with a redirect location of terms page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(otherIncomeURI)
        )
      }
    }
  }

}