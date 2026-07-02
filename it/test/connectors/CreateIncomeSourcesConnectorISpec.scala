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
import connectors.stubs.CreateIncomeSourcesAPIStub.{StubResponse, createIncomeSourcesUri}
import helpers.{ComponentSpecBase, WiremockHelper}
import models.{DateModel, ErrorModel}
import models.common.business.*
import models.common.subscription.{CreateIncomeSourcesModel, OverseasProperty, SoleTraderBusinesses, UkProperty}
import play.api.http.Status.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.http.HeaderCarrier
import utilities.{AccountingPeriodUtil, UUIDProvider}

import scala.collection.mutable

class CreateIncomeSourcesConnectorISpec extends ComponentSpecBase with FeatureSwitching {

  private val testIdempotencyKey = "test-uuid"

  private case class Item(
    status: Option[Int] = None,
    code: Option[String] = None,
    isNewKey: Boolean
  )

  private class TestUUIDProvider extends UUIDProvider {
    var data: mutable.Seq[Item] = mutable.Seq()

    override def getAndNoteNewKeyForStatusAndCode(status: Option[Int], code: Option[String]): String = {
      data = data ++ mutable.Seq(Item(status, code, isNewKey = true))
      testIdempotencyKey
    }

    override def noteSameKeyForStatusAndCode(status: Option[Int], code: Option[String]): Unit = {
      data = data ++ mutable.Seq(Item(status, code, isNewKey = false))
    }

    def reset(): Unit =
      data = mutable.Seq()
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
        Seq(false, true).foreach { withIdempotency =>
          if (withIdempotency) {
            enable(UseIdempotency)
          } else {
            disable(UseIdempotency)
          }
          CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel(withIdempotency))(
            status = NO_CONTENT
          )

          val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel(withIdempotency))

          await(result) mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
        }
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
          responses = Seq(
            StubResponse(UNPROCESSABLE_ENTITY, Some("003")),
            StubResponse(BAD_GATEWAY),
            StubResponse(NO_CONTENT)
          )
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel(true))

        await(result) mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
        WiremockHelper.verifyPost(
          uri = createIncomeSourcesUri(mtdbsa),
          count = Some(3)
        )
        // An [IdempotencyKey] is generated twice
        // -  Once for the initial post
        // -  A second d time for first retry (UNPROCESSABLE_ENTITY, "003")
        // The same key is used again for BAD_GATEWAY
        testUUIDProvider.data mustBe mutable.Seq(
          Item(isNewKey = true),
          Item(Some(UNPROCESSABLE_ENTITY), Some("003"), isNewKey = true),
          Item(Some(BAD_GATEWAY), isNewKey = false)
        )
      }
      s"status = ($SERVICE_UNAVAILABLE, $GATEWAY_TIMEOUT)" in {
        enable(UseIdempotency)
        testUUIDProvider.reset()
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel(true))(
          responses = Seq(
            StubResponse(SERVICE_UNAVAILABLE),
            StubResponse(GATEWAY_TIMEOUT),
            StubResponse(NO_CONTENT)
          )
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel(true))

        await(result) mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
        WiremockHelper.verifyPost(
          uri = createIncomeSourcesUri(mtdbsa),
          count = Some(3)
        )
        // An [IdempotencyKey] is only generated for the initial post
        // And the same key is used for both retries
        testUUIDProvider.data mustBe mutable.Seq(
          Item(isNewKey =  true),
          Item(Some(SERVICE_UNAVAILABLE), isNewKey = false),
          Item(Some(GATEWAY_TIMEOUT), isNewKey = false)
        )
      }
    }
    "return an error when using [idempotencyKey]" must {
      "run-out of retries" in {
        enable(UseIdempotency)
        testUUIDProvider.reset()
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel(true))(
          responses = Seq(
            StubResponse(BAD_GATEWAY),
            StubResponse(BAD_GATEWAY),
            StubResponse(BAD_GATEWAY),
            StubResponse(BAD_GATEWAY)
          )
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel(true))

        await(result) mustBe Left(CreateIncomeSourcesResponseHttpParser.UnexpectedStatus(BAD_GATEWAY))
      }
      s"when return status = $UNPROCESSABLE_ENTITY and code is not 003" in {
        val code = "999"
        enable(UseIdempotency)
        testUUIDProvider.reset()
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel(true))(
          responses = Seq(
            StubResponse(UNPROCESSABLE_ENTITY, Some(code))
          )
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel(true))

        await(result) mustBe Left(CreateIncomeSourcesResponseHttpParser.UnexpectedStatus(UNPROCESSABLE_ENTITY, Some(code)))
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
