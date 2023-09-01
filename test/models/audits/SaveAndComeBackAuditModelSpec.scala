/*
 * Copyright 2023 HM Revenue & Customs
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

package models.audits

import models.audits.SaveAndComebackAuditing.SaveAndComeBackAuditModel
import models.common.business._
import models.common.{AccountingYearModel, OverseasPropertyModel, PropertyModel}
import models.{Cash, Current, DateModel}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json

class SaveAndComeBackAuditModelSpec extends PlaySpec with GuiceOneServerPerSuite {
  private val currentYear = 2023
  private val selectedTaxYear = Some(AccountingYearModel(Current))
  private val selfEmployments = Seq(
    SelfEmploymentData(
      id = "id",
      businessStartDate = Some(BusinessStartDate(DateModel("6", "5", "2020"))),
      businessName = Some(BusinessNameModel("Money Business")),
      businessTradeName = Some(BusinessTradeNameModel("Consulting")),
      businessAddress = Some(BusinessAddressModel(Address(Seq("line 1"), Some("ZZ1 1ZZ"))))
    )
  )
  private val selfEmploymentAccountingMethod = Some(AccountingMethodModel(Cash))
  private val property = Some(PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("6", "5", "2020")),
    confirmed = true
  ))
  private val overseasProperty = Some(OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("6", "5", "2020")),
    confirmed = true
  ))

  "SaveAndComeBackAudit Model" should {
    "convert all data to the correct format" in {
      val expectedDetail =
        """
          |{
          |"userType": "individual",
          |"saUtr": "testUtr",
          |"nino": "testNino",
          |"saveAndRetrieveLocation": "testLocation",
          |"income": [
          |  {
          |    "incomeSource": "ukProperty",
          |    "commencementDate": "2020-05-06",
          |    "accountingType": "Cash"
          |  },
          |  {
          |    "incomeSource": "foreignProperty",
          |    "commencementDate": "2020-05-06",
          |    "accountingType": "Cash"
          |  },
          |  {
          |    "incomeSource": "selfEmployment",
          |    "numberOfBusinesses": "1",
          |    "accountingType": "Cash",
          |    "businesses": [
          |      {
          |        "businessName": "Money Business",
          |        "businessCommencementDate": "2020-05-06",
          |        "businessTrade": "Consulting",
          |        "businessAddress": {
          |          "lines": [
          |            "line 1"
          |          ],
          |          "postcode": "ZZ1 1ZZ"
          |        }
          |      }
          |    ]
          |  }
          |],
          |"taxYear": "2022-2023"
          |}
          |""".stripMargin

      val auditData = SaveAndComeBackAuditModel(
        userType = SaveAndComebackAuditing.individualUserType,
        utr = "testUtr",
        nino = "testNino",
        saveAndRetrieveLocation = "testLocation",
        currentTaxYear = currentYear,
        selectedTaxYear = selectedTaxYear,
        selfEmployments = selfEmployments,
        maybeSelfEmploymentAccountingMethod = selfEmploymentAccountingMethod,
        maybePropertyModel = property,
        maybeOverseasPropertyModel = overseasProperty
      )

      auditData.detail mustBe Json.parse(expectedDetail)
      auditData.auditType mustBe "SignUpSaveAndComeBack"
    }

    "convert all data with multiple self employments to the correct format" in {
      val expectedDetail =
        """
          |{
          |"userType": "individual",
          |"saUtr": "testUtr",
          |"nino": "testNino",
          |"saveAndRetrieveLocation": "testLocation",
          |"income": [
          |  {
          |    "incomeSource": "ukProperty",
          |    "commencementDate": "2020-05-06",
          |    "accountingType": "Cash"
          |  },
          |  {
          |    "incomeSource": "foreignProperty",
          |    "commencementDate": "2020-05-06",
          |    "accountingType": "Cash"
          |  },
          |  {
          |    "incomeSource": "selfEmployment",
          |    "numberOfBusinesses": "2",
          |    "accountingType": "Cash",
          |    "businesses": [
          |      {
          |        "businessName": "Money Business",
          |        "businessCommencementDate": "2020-05-06",
          |        "businessTrade": "Consulting",
          |        "businessAddress": {
          |          "lines": [
          |            "line 1"
          |          ],
          |          "postcode": "ZZ1 1ZZ"
          |        }
          |      },
          |      {
          |        "businessName": "Money Business",
          |        "businessCommencementDate": "2020-05-06",
          |        "businessTrade": "Consulting",
          |        "businessAddress": {
          |          "lines": [
          |            "line 1"
          |          ],
          |          "postcode": "ZZ1 1ZZ"
          |        }
          |      }
          |    ]
          |  }
          |],
          |"taxYear": "2022-2023"
          |}
          |""".stripMargin

      val auditData = SaveAndComeBackAuditModel(
        userType = SaveAndComebackAuditing.individualUserType,
        utr = "testUtr",
        nino = "testNino",
        saveAndRetrieveLocation = "testLocation",
        currentTaxYear = currentYear,
        selectedTaxYear = selectedTaxYear,
        selfEmployments = selfEmployments :+
          SelfEmploymentData(
            id = "id",
            businessStartDate = Some(BusinessStartDate(DateModel("6", "5", "2020"))),
            businessName = Some(BusinessNameModel("Money Business")),
            businessTradeName = Some(BusinessTradeNameModel("Consulting")),
            businessAddress = Some(BusinessAddressModel(Address(Seq("line 1"), Some("ZZ1 1ZZ"))))
          ),
        maybeSelfEmploymentAccountingMethod = selfEmploymentAccountingMethod,
        maybePropertyModel = property,
        maybeOverseasPropertyModel = overseasProperty
      )

      auditData.detail mustBe Json.parse(expectedDetail)
      auditData.auditType mustBe "SignUpSaveAndComeBack"
    }

    "convert data without self employment data to the correct format" in {
      val expectedDetail =
        """
          |{
          |"userType": "individual",
          |"saUtr": "testUtr",
          |"nino": "testNino",
          |"saveAndRetrieveLocation": "testLocation",
          |"income": [
          |  {
          |    "incomeSource": "ukProperty",
          |    "commencementDate": "2020-05-06",
          |    "accountingType": "Cash"
          |  },
          |  {
          |    "incomeSource": "foreignProperty",
          |    "commencementDate": "2020-05-06",
          |    "accountingType": "Cash"
          |  }
          |],
          |"taxYear": "2022-2023"
          |}
          |""".stripMargin

      val auditData = SaveAndComeBackAuditModel(
        userType = SaveAndComebackAuditing.individualUserType,
        utr = "testUtr",
        nino = "testNino",
        saveAndRetrieveLocation = "testLocation",
        currentTaxYear = currentYear,
        selectedTaxYear = selectedTaxYear,
        selfEmployments = Seq.empty,
        maybeSelfEmploymentAccountingMethod = selfEmploymentAccountingMethod,
        maybePropertyModel = property,
        maybeOverseasPropertyModel = overseasProperty
      )

      auditData.detail mustBe Json.parse(expectedDetail)
      auditData.auditType mustBe "SignUpSaveAndComeBack"
    }

    "convert data without self employment and overseas property data to the correct format" in {
      val expectedDetail =
        """
          |{
          |"userType": "individual",
          |"saUtr": "testUtr",
          |"nino": "testNino",
          |"saveAndRetrieveLocation": "testLocation",
          |"income": [
          |  {
          |    "incomeSource": "ukProperty",
          |    "commencementDate": "2020-05-06",
          |    "accountingType": "Cash"
          |  }
          |],
          |"taxYear": "2022-2023"
          |}
          |""".stripMargin

      val auditData = SaveAndComeBackAuditModel(
        userType = SaveAndComebackAuditing.individualUserType,
        utr = "testUtr",
        nino = "testNino",
        saveAndRetrieveLocation = "testLocation",
        currentTaxYear = currentYear,
        selectedTaxYear = selectedTaxYear,
        selfEmployments = Seq.empty,
        maybeSelfEmploymentAccountingMethod = selfEmploymentAccountingMethod,
        maybePropertyModel = property,
        maybeOverseasPropertyModel = None
      )

      auditData.detail mustBe Json.parse(expectedDetail)
      auditData.auditType mustBe "SignUpSaveAndComeBack"
    }

    "convert data without any income source to the correct format" in {
      val expectedDetail =
        """
          |{
          |"userType": "individual",
          |"saUtr": "testUtr",
          |"nino": "testNino",
          |"saveAndRetrieveLocation": "testLocation",
          |"income": [],
          |"taxYear": "2022-2023"
          |}
          |""".stripMargin

      val auditData = SaveAndComeBackAuditModel(
        userType = SaveAndComebackAuditing.individualUserType,
        utr = "testUtr",
        nino = "testNino",
        saveAndRetrieveLocation = "testLocation",
        currentTaxYear = currentYear,
        selectedTaxYear = selectedTaxYear,
        selfEmployments = Seq.empty,
        maybeSelfEmploymentAccountingMethod = None,
        maybePropertyModel = None,
        maybeOverseasPropertyModel = None
      )

      auditData.detail mustBe Json.parse(expectedDetail)
      auditData.auditType mustBe "SignUpSaveAndComeBack"
    }

    "convert agent data without any income source to the correct format" in {
      val expectedDetail =
        """
          |{
          |"userType": "individual",
          |"saUtr": "testUtr",
          |"nino": "testNino",
          |"saveAndRetrieveLocation": "testLocation",
          |"income": [],
          |"taxYear": "2022-2023",
          |"agentReferenceNumber": "testAgentReferenceNumber"
          |}
          |""".stripMargin

      val auditData = SaveAndComeBackAuditModel(
        userType = SaveAndComebackAuditing.individualUserType,
        utr = "testUtr",
        nino = "testNino",
        saveAndRetrieveLocation = "testLocation",
        maybeAgentReferenceNumber = Some("testAgentReferenceNumber"),
        currentTaxYear = currentYear,
        selectedTaxYear = selectedTaxYear,
        selfEmployments = Seq.empty,
        maybeSelfEmploymentAccountingMethod = None,
        maybePropertyModel = None,
        maybeOverseasPropertyModel = None
      )

      auditData.detail mustBe Json.parse(expectedDetail)
      auditData.auditType mustBe "SignUpSaveAndComeBack"
    }

    "convert empty data to the correct format" in {
      val auditData = SaveAndComeBackAuditModel(
        userType = SaveAndComebackAuditing.individualUserType,
        utr = "testUtr",
        nino = "testNino",
        saveAndRetrieveLocation = "testLocation",
        currentTaxYear = currentYear,
        selectedTaxYear = None,
        selfEmployments = Seq.empty,
        maybeSelfEmploymentAccountingMethod = None,
        maybePropertyModel = None,
        maybeOverseasPropertyModel = None
      )

      auditData.detail mustBe Json.obj(
        "userType" -> "individual",
        "saUtr" -> "testUtr",
        "nino" -> "testNino",
        "saveAndRetrieveLocation" -> "testLocation",
        "income" -> Json.arr()
      )
      auditData.auditType mustBe "SignUpSaveAndComeBack"
    }
  }
}
