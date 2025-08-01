/*
 * Copyright 2020 HM Revenue & Customs
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

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants.testNino
import helpers.servicemocks.PrePopStub
import models.common.business.Address
import models.prepop.{PrePopData, PrePopSelfEmployment}
import models.{Accruals, Cash, DateModel, ErrorModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.http.HeaderCarrier

class PrePopConnectorISpec extends ComponentSpecBase {

  private lazy val connector: PrePopConnector = app.injector.instanceOf[PrePopConnector]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "PrePopConnector.getPrePopData" must {
    "return prepop data" when {
      "a successful response with full data is received" in {
        PrePopStub.stubGetPrePop(testNino)(
          status = OK,
          body = Json.obj(
            "selfEmployment" -> Json.arr(
              Json.obj(
                "name" -> "ABC",
                "trade" -> "Plumbing",
                "address" -> Json.obj(
                  "lines" -> Json.arr(
                    "1 long road"
                  ),
                  "postcode" -> "ZZ1 1ZZ"
                ),
                "startDate" -> Json.obj(
                  "day" -> "01",
                  "month" -> "02",
                  "year" -> "2000"
                ),
                "accountingMethod" -> "cash"
              )
            ),
            "ukPropertyAccountingMethod" -> "accruals",
            "foreignPropertyAccountingMethod" -> "cash"
          )
        )

        connector.getPrePopData(testNino).futureValue mustBe Right(PrePopData(
          selfEmployment = Some(Seq(PrePopSelfEmployment(
            name = Some("ABC"),
            trade = Some("Plumbing"),
            address = Some(Address(
              lines = Seq(
                "1 long road"
              ),
              postcode = Some("ZZ1 1ZZ")
            )),
            startDate = Some(DateModel(
              day = "01",
              month = "02",
              year = "2000"
            )),
            accountingMethod = Some(Cash)
          ))),
          ukPropertyAccountingMethod = Some(Accruals),
          foreignPropertyAccountingMethod = Some(Cash)
        ))

      }
      "a successful response with minimal data is recieved" in {
        PrePopStub.stubGetPrePop(testNino)(
          status = OK,
          body = Json.obj()
        )

        connector.getPrePopData(testNino).futureValue mustBe Right(PrePopData(None, None, None))
      }
    }
    "return a failure" when {
      "the json received could not be parsed" in {
        PrePopStub.stubGetPrePop(testNino)(
          status = OK,
          body = JsString("Invalid Json")
        )

        connector.getPrePopData(testNino).futureValue mustBe Left(ErrorModel(OK, "Unable to parse json received into a PrePopData model"))
      }
      "an unexpected status is returned" in {
        PrePopStub.stubGetPrePop(testNino)(
          status = INTERNAL_SERVER_ERROR,
          body = Json.obj()
        )

        connector.getPrePopData(testNino).futureValue mustBe Left(ErrorModel(INTERNAL_SERVER_ERROR, "{}"))
      }
    }
  }

}
