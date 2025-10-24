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

package connectors

import connectors.httpparser.CreateIncomeSourcesResponseHttpParser
import connectors.stubs.CreateIncomeSourcesAPIStub
import helpers.ComponentSpecBase
import models.DateModel
import models.common.business._
import models.common.subscription.{CreateIncomeSourcesModel, OverseasProperty, SoleTraderBusinesses, UkProperty}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.AccountingPeriodUtil

class CreateIncomeSourcesConnectorISpec extends ComponentSpecBase {

  "createIncomeSources" when {
    "a NO_CONTENT status response is received" must {
      "return a create income sources success response" in {
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel)(
          status = NO_CONTENT
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel)

        await(result) mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
      }
    }
    "an unhandled status response is received" must {
      "return an unexpected status failure response" in {
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel)(
          status = INTERNAL_SERVER_ERROR
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel)

        await(result) mustBe Left(CreateIncomeSourcesResponseHttpParser.UnexpectedStatus(INTERNAL_SERVER_ERROR))
      }
    }
  }

  lazy val connector: CreateIncomeSourcesConnector = app.injector.instanceOf[CreateIncomeSourcesConnector]
  lazy val mtdbsa: String = "test-mtdbsa"
  lazy val createIncomeSourcesModel: CreateIncomeSourcesModel = CreateIncomeSourcesModel(
    nino = "test-nino",
    soleTraderBusinesses = Some(SoleTraderBusinesses(
      accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
      businesses = Seq(
        SelfEmploymentData(
          id = "test-id",
          startDateBeforeLimit = Some(false),
          businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
          businessName = Some(BusinessNameModel("test-name")),
          businessTradeName = Some(BusinessTradeNameModel("test-trade")),
          businessAddress = Some(BusinessAddressModel(Address(
            lines = Seq("test-line-one", "test-line-two"),
            postcode = Some("test-postcode")
          )))
        )
      )
    )),
    ukProperty = Some(UkProperty(
      startDateBeforeLimit = Some(false),
      accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
      tradingStartDate = DateModel("1", "1", "1980")
    )),
    overseasProperty = Some(OverseasProperty(
      startDateBeforeLimit = Some(false),
      accountingPeriod = AccountingPeriodUtil.getCurrentTaxYear,
      tradingStartDate = DateModel("1", "1", "1980")
    ))
  )

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

}
