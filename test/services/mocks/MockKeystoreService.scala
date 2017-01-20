/*
 * Copyright 2017 HM Revenue & Customs
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

package services.mocks

import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import services.KeystoreService
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.HttpResponse
import utils.MockTrait

import scala.concurrent.Future


trait MockKeystoreService extends MockTrait {

  import services.CacheConstants._

  val returnedCacheMap: CacheMap = CacheMap("", Map())

  object MockKeystoreService extends KeystoreService {
    override val session: SessionCache = mock[SessionCache]
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(MockKeystoreService.session)
  }

  private final def mockFetchFromKeyStore[T](key: String, config: MFO[T]): Unit =
    config ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetchAndGetEntry[T](Matchers.eq(key))(Matchers.any(), Matchers.any())).thenReturn(dataToReturn))

  private final def verifyKeystoreFetch[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetchAndGetEntry[T](Matchers.eq(key))(Matchers.any(), Matchers.any()))

  private final def verifyKeystoreSave[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).cache[T](Matchers.eq(key), Matchers.any())(Matchers.any(), Matchers.any()))


  protected final def setupMockKeystoreSaveFunctions(): Unit =
    when(MockKeystoreService.session.cache(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))

  protected final def setupMockKeystore(
                                         fetchIncomeSource: MFO[IncomeSourceModel] = DoNotConfigure,
                                         fetchBusinessName: MFO[BusinessNameModel] = DoNotConfigure,
                                         fetchAccountingPeriod: MFO[AccountingPeriodModel] = DoNotConfigure,
                                         fetchContactEmail: MFO[EmailModel] = DoNotConfigure,
                                         fetchIncomeType: MFO[IncomeTypeModel] = DoNotConfigure,
                                         fetchTerms: MFO[TermModel] = DoNotConfigure,
                                         fetchAll: MFO[CacheMap] = DoNotConfigure,
                                         deleteAll: MF[HttpResponse] = DoNotConfigure
                                       ): Unit = {
    mockFetchFromKeyStore[IncomeSourceModel](IncomeSource, fetchIncomeSource)
    mockFetchFromKeyStore[BusinessNameModel](BusinessName, fetchBusinessName)
    mockFetchFromKeyStore[AccountingPeriodModel](AccountingPeriod, fetchAccountingPeriod)
    mockFetchFromKeyStore[EmailModel](ContactEmail, fetchContactEmail)
    mockFetchFromKeyStore[IncomeTypeModel](IncomeType, fetchIncomeType)
    mockFetchFromKeyStore[TermModel](Terms, fetchTerms)

    setupMockKeystoreSaveFunctions()

    fetchAll ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetch()(Matchers.any())).thenReturn(dataToReturn))
    deleteAll ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.remove()(Matchers.any())).thenReturn(dataToReturn))
  }

  protected final def verifyKeystore(
                                      fetchIncomeSource: Option[Int] = None,
                                      saveIncomeSource: Option[Int] = None,
                                      fetchBusinessName: Option[Int] = None,
                                      saveBusinessName: Option[Int] = None,
                                      fetchAccountingPeriod: Option[Int] = None,
                                      saveAccountingPeriod: Option[Int] = None,
                                      fetchContactEmail: Option[Int] = None,
                                      saveContactEmail: Option[Int] = None,
                                      fetchIncomeType: Option[Int] = None,
                                      saveIncomeType: Option[Int] = None,
                                      fetchTerms: Option[Int] = None,
                                      saveTerms: Option[Int] = None,
                                      fetchAll: Option[Int] = None,
                                      deleteAll: Option[Int] = None
                                    ): Unit = {
    verifyKeystoreFetch(IncomeSource, fetchIncomeSource)
    verifyKeystoreSave(IncomeSource, saveIncomeSource)
    verifyKeystoreFetch(BusinessName, fetchBusinessName)
    verifyKeystoreSave(BusinessName, saveBusinessName)
    verifyKeystoreFetch(AccountingPeriod, fetchAccountingPeriod)
    verifyKeystoreSave(AccountingPeriod, saveAccountingPeriod)
    verifyKeystoreFetch(ContactEmail, fetchContactEmail)
    verifyKeystoreSave(ContactEmail, saveContactEmail)
    verifyKeystoreFetch(IncomeType, fetchIncomeType)
    verifyKeystoreSave(IncomeType, saveIncomeType)
    verifyKeystoreFetch(Terms, fetchTerms)
    verifyKeystoreSave(Terms, saveTerms)

    fetchAll ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetch()(Matchers.any()))
    deleteAll ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).remove()(Matchers.any()))
  }

}
