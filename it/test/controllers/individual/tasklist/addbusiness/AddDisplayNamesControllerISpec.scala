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

  s"GET ${routes.AddDisplayNamesController.show().url}" should {
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

  s"POST ${routes.AddDisplayNamesController.submit().url}" should {
    "save distinct display names and redirect to the SPS handoff" in {
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, businesses)
      val expectedBusinesses = businessesWithDisplayNames(Seq("First business", "Second business"))
      IncomeTaxSubscriptionConnectorStub.stubSaveSoleTraderBusinesses()
      IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

      val result = IncomeTaxSubscriptionFrontend.post(path)(
        Map(
          "business-display-name[0]" -> Seq("First business"),
          "business-display-name[1]" -> Seq("Second business")
        )
      )

      result must have(
        httpStatus(SEE_OTHER),
        redirectURI(IndividualURI.spsHandoffRouteURI)
      )
      IncomeTaxSubscriptionConnectorStub.verifySaveSoleTraderBusinesses(expectedBusinesses)
    }

    "return to check your answers when submitted in edit mode" in {
      AuthStub.stubAuthSuccess()
      IncomeTaxSubscriptionConnectorStub.stubSoleTraderBusinessesDetails(OK, businesses)
      val expectedBusinesses = businessesWithDisplayNames(Seq("First business", "Second business"))
      IncomeTaxSubscriptionConnectorStub.stubSaveSoleTraderBusinesses()
      IncomeTaxSubscriptionConnectorStub.stubDeleteIncomeSourceConfirmation(OK)

      val result = IncomeTaxSubscriptionFrontend.post(s"$path?editMode=true")(
        Map(
          "business-display-name[0]" -> Seq("First business"),
          "business-display-name[1]" -> Seq("Second business")
        )
      )

      result must have(
        httpStatus(SEE_OTHER),
        redirectURI(IndividualURI.globalCheckYourAnswersURI)
      )
      IncomeTaxSubscriptionConnectorStub.verifySaveSoleTraderBusinesses(expectedBusinesses)
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
      IncomeTaxSubscriptionConnectorStub.verifySaveSoleTraderBusinesses(
        businessesWithDisplayNames(Seq("Same business", "Same business")),
        count = 0
      )
    }
  }
}
