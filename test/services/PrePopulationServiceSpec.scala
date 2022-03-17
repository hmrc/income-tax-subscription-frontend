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

package services

import models._
import models.common.business.{AccountingMethodModel, BusinessNameModel, BusinessTradeNameModel, SelfEmploymentData}
import models.common.{OverseasPropertyModel, PropertyModel}
import org.mockito.Mockito.reset
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.TestPrePopulationService

class PrePopulationServiceSpec extends TestPrePopulationService {

  val testReference = "testReference"

  private val selfEmploymentsWithAccountingMethod = Some(List(
    PrePopSelfEmployment(
      Some("testBusinessName1"), "testBusinessTradeName1", None, None, None, Some(Accruals)),
    PrePopSelfEmployment(
      Some("testBusinessName2"), "testBusinessTradeName2", None, None, None, Some(Cash))
  ))
  private val testSelfEmployments = List(
    SelfEmploymentData(
      "",
      None,
      Some(BusinessNameModel("testBusinessName1")),
      Some(BusinessTradeNameModel("testBusinessTradeName1"))),
    SelfEmploymentData(
      "",
      None,
      Some(BusinessNameModel("testBusinessName2")),
      Some(BusinessTradeNameModel("testBusinessTradeName2")))
  )

  private val ukProperty = Some(PrePopUkProperty(Some(DateModel("1", "1", "2001")), Some(Cash)))

  private val overseasProperty = Some(PrePopOverseasProperty(Some(DateModel("2", "2", "2002")), Some(Accruals)))

  private val testBusinessAccountingMethod = AccountingMethodModel(Accruals) // first found, see selfEmploymentsWithAccountingMethod
  private val testUkProperty = PropertyModel(Some(Cash), Some(DateModel("1", "1", "2001")), false)
  private val testOverseasProperty = OverseasPropertyModel(Some(Accruals), Some(DateModel("2", "2", "2002")), false)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  "PrePopulationService" when {
    "given a user which has not been seen before" should {
      "save all their information" in {
        val data = PrePopData(selfEmploymentsWithAccountingMethod, ukProperty, overseasProperty)
        mockFetchPrePopFlag(testReference, None)
        mockSaveBusinesses(testReference)
        mockSaveUkProperty(testReference)
        mockSaveOverseasProperty(testReference)
        mockSaveSelfEmploymentsAccountingMethod(testReference)
        mockSavePrePopFlag(testReference)

        await(TestPrePopulationService.prePopulate(testReference, data))

        verifyFetchPrePopFlag(testReference)
        verifySaveBusinesses(1, testReference, testSelfEmployments)
        verifySaveUkProperty(1, testReference, testUkProperty)
        verifyOverseasPropertySave(Some(testOverseasProperty), Some(testReference))
        verifySaveSelfEmploymentsAccountingMethod(1, testReference, testBusinessAccountingMethod)
        verifySavePrePopFlag(1, testReference)
      }
    }
    "given a user which has been seen before" should {
      "not save all their information" in {
        val data = PrePopData(selfEmploymentsWithAccountingMethod, ukProperty, overseasProperty)
        mockFetchPrePopFlag(testReference, Some(true))

        await(TestPrePopulationService.prePopulate(testReference, data))

        verifyFetchPrePopFlag(testReference)
        verifySaveBusinesses(0, testReference, testSelfEmployments)
        verifySaveUkProperty(0, testReference, testUkProperty)
        verifyOverseasPropertySave(None)
        verifySaveSelfEmploymentsAccountingMethod(0, testReference, testBusinessAccountingMethod)
        verifySavePrePopFlag(0, testReference)
      }
    }
  }
}

