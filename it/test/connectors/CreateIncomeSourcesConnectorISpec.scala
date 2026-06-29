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

import config.featureswitch.FeatureSwitch.UseIdempotency
import config.featureswitch.FeatureSwitching
import connectors.httpparser.CreateIncomeSourcesResponseHttpParser
import connectors.stubs.CreateIncomeSourcesAPIStub
import connectors.stubs.CreateIncomeSourcesAPIStub.createIncomeSourcesUri
import helpers.{ComponentSpecBase, WiremockHelper}
import models.DateModel
import models.common.business.*
import models.common.subscription.{CreateIncomeSourcesModel, OverseasProperty, SoleTraderBusinesses, UkProperty}
import play.api.http.Status.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.{AccountingPeriodUtil, UUIDProvider}

class CreateIncomeSourcesConnectorISpec extends ComponentSpecBase with FeatureSwitching {

  private val testIdempotencyKey = "test-uuid"

  private class TestUUIDProvider extends UUIDProvider {
    private var c = 0

    def reset(): Unit = {
      c = 0
    }

    def count(): Int =
      c

    override def getUUID: String = {
      c += 1
      testIdempotencyKey
    }
  }

  private lazy val testUUIDProvider = new TestUUIDProvider

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(configuration)
    .overrides(bind[UUIDProvider].to(testUUIDProvider))
    .build()

  "createIncomeSources" when {
    s"a $NO_CONTENT status response is received" must {
      "return a create income sources success response" in {
        disable(UseIdempotency)
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel())(
          status = NO_CONTENT
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel())

        await(result) mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
      }
    }
    "an unhandled status response is received" must {
      "return an unexpected status failure response" in {
        disable(UseIdempotency)
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel())(
          status = INTERNAL_SERVER_ERROR
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel())

        await(result) mustBe Left(CreateIncomeSourcesResponseHttpParser.UnexpectedStatus(INTERNAL_SERVER_ERROR))
      }
    }
    "retry 3 times when using [idempotencyKey]" must {
      s"status = ($UNPROCESSABLE_ENTITY, $BAD_GATEWAY)" in {
        enable(UseIdempotency)
        testUUIDProvider.reset()
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel(true))(
          statuses = Seq(UNPROCESSABLE_ENTITY, BAD_GATEWAY, NO_CONTENT)
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel(true))

        await(result) mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
        WiremockHelper.verifyPost(
          uri = createIncomeSourcesUri(mtdbsa),
          count = Some(3)
        )
        // An [IdempotencyKey] is generated twice
        // -  Once for the initial post
        // -  A second d time for first retry (UNPROCESSABLE_ENTITY)
        // The same key is used again for BAD_GATEWAY
        testUUIDProvider.count() mustBe 2
      }
      s"status = ($SERVICE_UNAVAILABLE, $GATEWAY_TIMEOUT)" in {
        enable(UseIdempotency)
        testUUIDProvider.reset()
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel(true))(
          statuses = Seq(SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT, NO_CONTENT)
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel(true))

        await(result) mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
        WiremockHelper.verifyPost(
          uri = createIncomeSourcesUri(mtdbsa),
          count = Some(3)
        )
        // An [IdempotencyKey] is only generated for the initial post
        // And the same key is used for both retries
        testUUIDProvider.count() mustBe 1
      }
    }
  }

  lazy val connector: CreateIncomeSourcesConnector = app.injector.instanceOf[CreateIncomeSourcesConnector]
  lazy val mtdbsa: String = "test-mtdbsa"

  def createIncomeSourcesModel(withIdempotency: Boolean = false): CreateIncomeSourcesModel = CreateIncomeSourcesModel(
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
            postcode = Some("test-postcode"),
            country = Some(Country("GB", "United Kingdom"))
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
    )),
    idempotencyKey = if (withIdempotency) Some(testIdempotencyKey) else None
  )

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
}
