/*
 * Copyright 2022 HM Revenue & Customs
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

package models

import org.scalatest.Matchers._
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsSuccess, Json}

class EligibilitySpec extends PlaySpec with GuiceOneServerPerSuite {

  "the Eligibility model" should {

    "handle missing prepop" in {
      val valueWithoutPrepop =
        """{
          |  "eligibleCurrentYear": true,
          |  "eligibleNextYear": true
          |}""".stripMargin
      Json.parse(valueWithoutPrepop).validate[EligibilityStatus] should be (JsSuccess(EligibilityStatus(true, true, None)))
    }

    "handle old value of 'eligible'" in {
      val valueWithoutPrepop =
        """{
          |  "eligible": true,
          |  "eligibleCurrentYear": true,
          |  "eligibleNextYear": true
          |}""".stripMargin
      Json.parse(valueWithoutPrepop).validate[EligibilityStatus] should be (JsSuccess(EligibilityStatus(true, true, None)))
    }

    "handle empty prepop" in {
      val valueWithEmptyPrepop =
        """{
          |  "eligibleCurrentYear": true,
          |  "eligibleNextYear": true,
          |  "prepop": {}
          |}""".stripMargin
      Json.parse(valueWithEmptyPrepop).validate[EligibilityStatus] should be (JsSuccess(EligibilityStatus(true, true, Some(PrePopData(None, None, None)))))
    }

    "handle trivial prepop" in {
      val valueWithTrivialPrepop =
        """{
          |  "eligibleCurrentYear": true,
          |  "eligibleNextYear": true,
          |  "prepop": {
          |    "selfEmployments": []
          |  }
          |}""".stripMargin
      Json.parse(valueWithTrivialPrepop).validate[EligibilityStatus] should be (JsSuccess(EligibilityStatus(true, true, Some(PrePopData(Some(List.empty), None, None)))))
    }

    "handle minimal prepop" in {
      val minimalPrepop =
        """{
          |  "selfEmployments": [
          |    {
          |      "businessTradeName": "B"
          |    }
          |  ]
          |}""".stripMargin
      Json.parse(minimalPrepop).validate[PrePopData] should be (
        JsSuccess(
          PrePopData(
            Some(List(
              PrePopSelfEmployment(
                None,
                "B",
                None,
                None,
                None,
                None,
                None)
            )),
            None,
            None
          )
        )
      )
    }

    "handle full prepop" in {
      val fullPrepop =
        """{
          |  "selfEmployments": [
          |    {
          |      "businessName": "A",
          |      "businessTradeName": "B",
          |      "businessAddressFirstLine": "C",
          |      "businessAddressPostCode": "D",
          |      "businessStartDate": {
          |        "day": "E1",
          |        "month": "E2",
          |        "year": "E3"
          |      },
          |      "businessCeasedDate": {
          |        "day": "F1",
          |        "month": "F2",
          |        "year": "F3"
          |      },
          |      "businessAccountingPeriodStart": {
          |        "day": "G1",
          |        "month": "G2",
          |        "year": "G3"
          |      },
          |      "businessAccountingPeriodEnd": {
          |        "day": "H1",
          |        "month": "H2",
          |        "year": "H3"
          |      },
          |      "businessAccountingMethod": "Cash"
          |    }
          |  ],
          |  "ukProperty": {
          |    "ukPropertyCommencementDate": {
          |        "day": "J1",
          |        "month": "J2",
          |        "year": "J3"
          |      },
          |    "ukPropertyAccountingMethod": "Cash"
          |  },
          |  "overseasProperty": {
          |    "overseasPropertyCommencementDate": {
          |        "day": "L1",
          |        "month": "L2",
          |        "year": "L3"
          |      },
          |    "overseasPropertyAccountingMethod": "Accruals"
          |  }
          |}""".stripMargin
      val parseResult = Json.parse(fullPrepop).validate[PrePopData]
      parseResult.isSuccess mustBe true
      parseResult.get should be (
        PrePopData(
          Some(List(
            PrePopSelfEmployment(
              Some("A"),
              "B",
              Some("C"),
              Some("D"),
              Some(DateModel("E1", "E2", "E3")),
              Some(DateModel("F1", "F2", "F3")),
              Some(Cash))
          )),
          Some(PrePopUkProperty(
            Some(DateModel("J1", "J2", "J3")),
            Some(Cash))),
          Some(PrePopOverseasProperty(
            Some(DateModel("L1", "L2", "L3")),
            Some(Accruals)))
        )
      )
    }

  }
}
