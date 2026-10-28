/*
 * Copyright 2026 HM Revenue & Customs
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

import connectors.stubs.IncomeTaxSubscriptionConnectorStub
import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.IndividualURI
import helpers.IntegrationTestModels.testBusiness
import helpers.servicemocks.AuthStub
import models.common.SoleTraderBusinesses
import models.common.business.BusinessNameModel
import play.api.http.Status.*
import utilities.SubscriptionDataKeys.SoleTraderBusinessesKey

class AddDisplayNamesControllerISpec extends ComponentSpecBase {

  private val businesses = Seq(
    testBusiness("business-1"),
    testBusiness("business-unique").copy(businessName = Some(BusinessNameModel("Unique business"))),
    testBusiness("business-2")
  )
  private val pageTitleSuffix = " - Sign up for Making Tax Digital for Income Tax - GOV.UK"
  private val path = "/business/sole-trader-display-names"

  private def businessesWithDisplayNames(displayNames: Seq[String]): SoleTraderBusinesses =
    SoleTraderBusinesses(businesses.map(_.toSoleTraderBusiness)).addDisplayNames(displayNames)

  s"GET ${routes.AddDisplayNamesController.show.url}" should {
    "show the display names page for businesses with duplicate names" in {
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, businesses)

      val result = IncomeTaxSubscriptionFrontend.get(path)

      result must have(
        httpStatus(OK),
        pageTitle(messages("individual.business-display-name.title") + pageTitleSuffix)
      )
    }
  }

  s"POST ${routes.AddDisplayNamesController.submit.url}" should {
    "save distinct display names and redirect to check your answers" in {
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, businesses)
      val expectedBusinesses = businessesWithDisplayNames(Seq("First business", "Second business"))
      IncomeTaxSubscriptionConnectorStub.stubSaveSubscriptionDetails(SoleTraderBusinessesKey, expectedBusinesses)(
        SoleTraderBusinesses.encryptedFormat(sensitiveFormat)
      )
      IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

      val result = IncomeTaxSubscriptionFrontend.post(path)(
        Map(
          "business-display-name[0]" -> Seq("First business"),
          "business-display-name[1]" -> Seq("Second business")
        )
      )

      result must have(
        httpStatus(SEE_OTHER),
        redirectURI(IndividualURI.globalCheckYourAnswersURI)
      )
      IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails(SoleTraderBusinessesKey, expectedBusinesses, Some(1))(
        SoleTraderBusinesses.encryptedFormat(sensitiveFormat)
      )
    }

    "reject duplicate display names" in {
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, businesses)

      val result = IncomeTaxSubscriptionFrontend.post(path)(
        Map(
          "business-display-name[0]" -> Seq("Same business"),
          "business-display-name[1]" -> Seq("Same business")
        )
      )

      result must have(httpStatus(BAD_REQUEST))
      IncomeTaxSubscriptionConnectorStub.verifySaveSubscriptionDetails(
        SoleTraderBusinessesKey,
        businessesWithDisplayNames(Seq("Same business", "Same business")),
        Some(0)
      )(SoleTraderBusinesses.encryptedFormat(sensitiveFormat))
    }
  }
}
