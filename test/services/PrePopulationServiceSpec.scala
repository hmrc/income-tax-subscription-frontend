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

import models._
import models.common.{OverseasPropertyModel, PropertyModel}
import org.mockito.Mockito.reset
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.mocks.TestPrePopulationService

class PrePopulationServiceSpec extends TestPrePopulationService {

  val testReference = "testReference"

  private val selfEmploymentsWithAccountingMethod = Some(List(
    PrePopSelfEmployment(
      Some("testBusines$Name1"), "testBusinessTradeName1" * 35, None, None, None, Some(Accruals)),
    PrePopSelfEmployment(
      Some("testBusinessName2"), "testBusinessTradeName2", None, None, None, Some(Cash))
  ))

  private val ukProperty = Some(PrePopUkProperty(Some(DateModel("1", "1", "2001")), Some(Cash)))

  private val overseasProperty = Some(PrePopOverseasProperty(Some(DateModel("2", "2", "2002")), Some(Accruals)))

  private val testUkProperty = PropertyModel(Some(Cash), Some(DateModel("1", "1", "2001")))
  private val testOverseasProperty = OverseasPropertyModel(Some(Accruals), Some(DateModel("2", "2", "2002")))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  "PrePopulationService" when {
    "given a user which has not been seen before" should {
      "clean and save all their information" in {
        val data = PrePopData(selfEmploymentsWithAccountingMethod, ukProperty, overseasProperty)
        mockFetchPrePopFlag(testReference, None)
        mockSaveBusinesses(testReference)
        mockSaveUkProperty(testReference)
        mockSaveOverseasProperty(testReference)
        mockSavePrePopFlag(testReference)
        mockDeleteIncomeSourceConfirmationSuccess()

        await(TestPrePopulationService.prePopulate(testReference, data))

        verifyFetchPrePopFlag(testReference)
        verifySaveBusinesses(1, testReference)
        verifySaveUkProperty(1, testReference, testUkProperty)
        verifyOverseasPropertySave(Some(testOverseasProperty), Some(testReference))
        verifySavePrePopFlag(1, testReference, value = true)
      }
    }
    "given a user which has been seen before" should {
      "not save all their information" in {
        val data = PrePopData(selfEmploymentsWithAccountingMethod, ukProperty, overseasProperty)
        mockFetchPrePopFlag(testReference, Some(true))

        await(TestPrePopulationService.prePopulate(testReference, data))

        verifyFetchPrePopFlag(testReference)
        verifySaveBusinesses(0, testReference)
        verifySaveUkProperty(0, testReference, testUkProperty)
        verifyOverseasPropertySave(None)
        verifySavePrePopFlag(0, testReference, value = true)
      }
    }
  }
}

