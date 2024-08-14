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

package services

import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import models._
import models.common.business._
import models.common.{OverseasPropertyModel, PropertyModel}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.MockSubscriptionDetailsService
import utilities.MockUUIDProvider

class PrePopulationServiceSpec extends PlaySpec with Matchers with MockSubscriptionDetailsService with MockUUIDProvider {

  val testReference = "testReference"
  val prePopulationService: PrePopulationService = new PrePopulationService(mockSubscriptionDetailsService, mockUUIDProvider)

  private val selfEmploymentsWithAccountingMethod = Some(List(
    PrePopSelfEmployment(
      Some("testBusines$Name1"), "testBusinessTradeName1" * 35, None, None, None, Some(Accruals)),
    PrePopSelfEmployment(
      Some("testBusinessName2"), "testBusinessTradeName2", Some("testAddressLine1"), Some("ZZ1 1ZZ"), Some(DateModel("1", "1", "1989")), Some(Cash))
  ))
  private val ukProperty = Some(PrePopUkProperty(Some(DateModel("1", "1", "2001")), Some(Cash)))
  private val overseasProperty = Some(PrePopOverseasProperty(Some(DateModel("2", "2", "2002")), Some(Accruals)))

  private val testSelfEmployments: Seq[SelfEmploymentData] = Seq(
    SelfEmploymentData("testUUID", None, Some(BusinessNameModel("testBusines Name1")), None, None),
    SelfEmploymentData(
      "testUUID",
      Some(BusinessStartDate(DateModel("1", "1", "1989"))),
      Some(BusinessNameModel("testBusinessName2")),
      Some(BusinessTradeNameModel("testBusinessTradeName2")),
      Some(BusinessAddressModel(Address(lines = Seq("testAddressLine1"), postcode = Some("ZZ1 1ZZ"))))
    )
  )
  private val testUkProperty = PropertyModel(Some(Cash), Some(DateModel("1", "1", "2001")))
  private val testOverseasProperty = OverseasPropertyModel(Some(Accruals), Some(DateModel("2", "2", "2002")))

  "PrePopulationService" when {
    "given a user which has not been seen before" should {
      "clean and save all their information" in {
        mockFetchPrePopFlag(None)
        mockUUID("testUUID")
        mockSaveBusinesses(testSelfEmployments, Some(Accruals))(Right(PostSubscriptionDetailsSuccessResponse))
        mockSaveProperty(testUkProperty)(Right(PostSubscriptionDetailsSuccessResponse))
        mockSaveOverseasProperty(testOverseasProperty)(Right(PostSubscriptionDetailsSuccessResponse))
        mockSaveIncomeSourceConfirmation(Right(PostSubscriptionDetailsSuccessResponse))
        mockSavePrePopFlag(flag = true)(Right(PostSubscriptionDetailsSuccessResponse))

        val data = PrePopData(selfEmploymentsWithAccountingMethod, ukProperty, overseasProperty)

        await(prePopulationService.prePopulate(testReference, data))
      }
    }
    "given a user which has been seen before" should {
      "not save all their information" in {
        mockFetchPrePopFlag(Some(true))

        val data = PrePopData(selfEmploymentsWithAccountingMethod, ukProperty, overseasProperty)

        await(prePopulationService.prePopulate(testReference, data))
      }
    }
  }
}

