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
import connectors.stubs.CreateIncomeSourcesAPIStub.{StubResponse, createIncomeSourcesUri}
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

import scala.collection.mutable

class CreateIncomeSourcesConnectorISpec extends ComponentSpecBase {

  private val testIdempotencyKey = "test-uuid"

  private class TestUUIDProvider extends UUIDProvider {
    private val keys: mutable.Queue[String] = mutable.Queue.empty

    def setKeys(values: Seq[String]): Unit = {
      keys.clear()
      keys.addAll(values)
    }

    override def getUUID: String = if (keys.nonEmpty) keys.dequeue() else testIdempotencyKey
  }

  private lazy val testUUIDProvider = new TestUUIDProvider

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(configuration)
    .overrides(bind[UUIDProvider].to(testUUIDProvider))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    testUUIDProvider.setKeys(Seq(testIdempotencyKey))
  }

  "createIncomeSources" when {
    s"a $NO_CONTENT status response is received" must {
      "return a success response with an idempotency key" in {
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel())(status = NO_CONTENT)

        await(connector.createIncomeSources(mtdbsa, createIncomeSourcesModel())) mustBe
          Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
      }
    }
    "an unhandled status response is received" must {
      "return an unexpected status failure response" in {
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel())(
          status = INTERNAL_SERVER_ERROR
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel())

        await(result) mustBe Left(CreateIncomeSourcesResponseHttpParser.UnexpectedStatus(INTERNAL_SERVER_ERROR))
      }
    }
    "retry 3 times when using [idempotencyKey]" must {
      s"status = ($UNPROCESSABLE_ENTITY, $BAD_GATEWAY)" in {
        testUUIDProvider.setKeys(Seq(testIdempotencyKey, "replacement-key"))
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel())(
          responses = Seq(
            StubResponse(UNPROCESSABLE_ENTITY, Some("003")),
            StubResponse(BAD_GATEWAY),
            StubResponse(NO_CONTENT)
          )
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel())

        await(result) mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
        WiremockHelper.verifyPost(
          uri = createIncomeSourcesUri(mtdbsa),
          count = Some(3)
        )
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 1, idempotencyKey = testIdempotencyKey)
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 2, idempotencyKey = "replacement-key")
      }
      s"status = ($SERVICE_UNAVAILABLE, $GATEWAY_TIMEOUT)" in {
        testUUIDProvider.setKeys(Seq(testIdempotencyKey))
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel())(
          responses = Seq(
            StubResponse(SERVICE_UNAVAILABLE),
            StubResponse(GATEWAY_TIMEOUT),
            StubResponse(NO_CONTENT)
          )
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel())

        await(result) mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
        WiremockHelper.verifyPost(
          uri = createIncomeSourcesUri(mtdbsa),
          count = Some(3)
        )
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 3, idempotencyKey = testIdempotencyKey)
      }
      s"status = $UNPROCESSABLE_ENTITY and code = 830" in {
        testUUIDProvider.setKeys(Seq(testIdempotencyKey, "replacement-key"))
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel())(
          responses = Seq(
            StubResponse(UNPROCESSABLE_ENTITY, Some("830")),
            StubResponse(NO_CONTENT)
          )
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel())

        await(result) mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
        WiremockHelper.verifyPost(
          uri = createIncomeSourcesUri(mtdbsa),
          count = Some(2)
        )
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 2, idempotencyKey = testIdempotencyKey)
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 0, idempotencyKey = "replacement-key")
      }
      s"status = ($UNPROCESSABLE_ENTITY with code 830, $BAD_GATEWAY)" in {
        testUUIDProvider.setKeys(Seq(testIdempotencyKey, "replacement-key"))
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel())(
          responses = Seq(
            StubResponse(UNPROCESSABLE_ENTITY, Some("830")),
            StubResponse(BAD_GATEWAY),
            StubResponse(NO_CONTENT)
          )
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel())

        await(result) mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
        WiremockHelper.verifyPost(
          uri = createIncomeSourcesUri(mtdbsa),
          count = Some(3)
        )
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 3, idempotencyKey = testIdempotencyKey)
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 0, idempotencyKey = "replacement-key")
      }
      s"status = ($UNPROCESSABLE_ENTITY with code 003, $UNPROCESSABLE_ENTITY with code 830)" in {
        testUUIDProvider.setKeys(Seq(testIdempotencyKey, "replacement-key", "unused-key"))
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel())(
          responses = Seq(
            StubResponse(UNPROCESSABLE_ENTITY, Some("003")),
            StubResponse(UNPROCESSABLE_ENTITY, Some("830")),
            StubResponse(NO_CONTENT)
          )
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel())

        await(result) mustBe Right(CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesSuccess)
        WiremockHelper.verifyPost(
          uri = createIncomeSourcesUri(mtdbsa),
          count = Some(3)
        )
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 1, idempotencyKey = testIdempotencyKey)
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 2, idempotencyKey = "replacement-key")
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 0, idempotencyKey = "unused-key")
      }
    }
    "return an error when using [idempotencyKey]" must {
      "run-out of retries" in {
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel())(
          responses = Seq(
            StubResponse(BAD_GATEWAY),
            StubResponse(BAD_GATEWAY),
            StubResponse(BAD_GATEWAY),
            StubResponse(BAD_GATEWAY)
          )
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel())

        await(result) mustBe Left(CreateIncomeSourcesResponseHttpParser.UnexpectedStatus(BAD_GATEWAY))
        WiremockHelper.verifyPost(
          uri = createIncomeSourcesUri(mtdbsa),
          count = Some(4)
        )
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 4, idempotencyKey = testIdempotencyKey)
      }
      s"run-out of retries when status = $UNPROCESSABLE_ENTITY and code = 830" in {
        testUUIDProvider.setKeys(Seq(testIdempotencyKey, "replacement-key"))
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel())(
          responses = Seq.fill(4)(StubResponse(UNPROCESSABLE_ENTITY, Some("830")))
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel())

        await(result) mustBe Left(CreateIncomeSourcesResponseHttpParser.UnexpectedStatus(UNPROCESSABLE_ENTITY, Some("830")))
        WiremockHelper.verifyPost(
          uri = createIncomeSourcesUri(mtdbsa),
          count = Some(4)
        )
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 4, idempotencyKey = testIdempotencyKey)
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 0, idempotencyKey = "replacement-key")
      }
      s"when return status = $UNPROCESSABLE_ENTITY and code is not 003" in {
        val code = "999"
        CreateIncomeSourcesAPIStub.stubCreateIncomeSources(mtdbsa, createIncomeSourcesModel())(
          responses = Seq(
            StubResponse(UNPROCESSABLE_ENTITY, Some(code))
          )
        )

        val result = connector.createIncomeSources(mtdbsa, createIncomeSourcesModel())

        await(result) mustBe Left(CreateIncomeSourcesResponseHttpParser.UnexpectedStatus(UNPROCESSABLE_ENTITY, Some(code)))
        WiremockHelper.verifyPost(
          uri = createIncomeSourcesUri(mtdbsa),
          count = Some(1)
        )
        CreateIncomeSourcesAPIStub.verifyIdempotencyKeyRequestCount(mtdbsa, expectedCount = 1, idempotencyKey = testIdempotencyKey)
      }
    }
  }

  lazy val connector: CreateIncomeSourcesConnector = app.injector.instanceOf[CreateIncomeSourcesConnector]
  lazy val mtdbsa: String = "test-mtdbsa"

  def createIncomeSourcesModel(): CreateIncomeSourcesModel = CreateIncomeSourcesModel(
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
    idempotencyKey = Some(testIdempotencyKey)
  )

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
}
