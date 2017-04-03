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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import services.KeystoreService
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.HttpResponse
import utils.MockTrait

import scala.concurrent.Future


trait MockKeystoreService extends MockTrait {

  import services.CacheConstants._

  val returnedCacheMap: CacheMap = CacheMap("", Map())

  object MockKeystoreService extends KeystoreService(mock[SessionCache])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(MockKeystoreService.session)
  }

  private final def mockFetchFromKeyStore[T](key: String, config: MFO[T]): Unit =
    config ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetchAndGetEntry[T](ArgumentMatchers.eq(key))(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(dataToReturn))

  private final def verifyKeystoreFetch[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetchAndGetEntry[T](ArgumentMatchers.eq(key))(ArgumentMatchers.any(), ArgumentMatchers.any()))

  private final def verifyKeystoreSave[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).cache[T](ArgumentMatchers.eq(key), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))


  protected final def setupMockKeystoreSaveFunctions(): Unit =
    when(MockKeystoreService.session.cache(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnedCacheMap))

  protected final def setupMockKeystore(
                                         fetchIncomeSource: MFO[IncomeSourceModel] = DoNotConfigure,
                                         fetchBusinessName: MFO[BusinessNameModel] = DoNotConfigure,
                                         fetchAccountingPeriodDate: MFO[AccountingPeriodModel] = DoNotConfigure,
                                         fetchAccountingMethod: MFO[AccountingMethodModel] = DoNotConfigure,
                                         fetchTerms: MFO[TermModel] = DoNotConfigure,
                                         fetchNotEligible: MFO[NotEligibleModel] = DoNotConfigure,
                                         fetchOtherIncome: MFO[OtherIncomeModel] = DoNotConfigure,
                                         fetchSubscriptionId: MFO[String] = DoNotConfigure,
                                         fetchAccountingPeriodPrior: MFO[AccountingPeriodPriorModel] = DoNotConfigure,
                                         fetchRegisterNextAccountingPeriod: MFO[RegisterNextAccountingPeriodModel] = DoNotConfigure,
                                         fetchClientDetails: MFO[ClientDetailsModel] = DoNotConfigure,
                                         fetchAll: MFO[CacheMap] = DoNotConfigure,
                                         deleteAll: MF[HttpResponse] = DoNotConfigure
                                       ): Unit = {
    mockFetchFromKeyStore[IncomeSourceModel](IncomeSource, fetchIncomeSource)
    mockFetchFromKeyStore[BusinessNameModel](BusinessName, fetchBusinessName)
    mockFetchFromKeyStore[AccountingPeriodModel](AccountingPeriodDate, fetchAccountingPeriodDate)
    mockFetchFromKeyStore[AccountingMethodModel](AccountingMethod, fetchAccountingMethod)
    mockFetchFromKeyStore[TermModel](Terms, fetchTerms)
    mockFetchFromKeyStore[NotEligibleModel](NotEligible, fetchNotEligible)
    mockFetchFromKeyStore[OtherIncomeModel](OtherIncome, fetchOtherIncome)
    mockFetchFromKeyStore[String](MtditId, fetchSubscriptionId)
    mockFetchFromKeyStore[AccountingPeriodPriorModel](AccountingPeriodPrior, fetchAccountingPeriodPrior)
    mockFetchFromKeyStore[RegisterNextAccountingPeriodModel](RegisterNextAccountingPeriod, fetchRegisterNextAccountingPeriod)
    mockFetchFromKeyStore[ClientDetailsModel](ClientDetails, fetchClientDetails)

    setupMockKeystoreSaveFunctions()

    fetchAll ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetch()(ArgumentMatchers.any())).thenReturn(dataToReturn))
    deleteAll ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.remove()(ArgumentMatchers.any())).thenReturn(dataToReturn))
  }

  protected final def verifyKeystore(
                                      fetchIncomeSource: Option[Int] = None,
                                      saveIncomeSource: Option[Int] = None,
                                      fetchBusinessName: Option[Int] = None,
                                      saveBusinessName: Option[Int] = None,
                                      fetchAccountingPeriodDate: Option[Int] = None,
                                      saveAccountingPeriodDate: Option[Int] = None,
                                      fetchAccountingMethod: Option[Int] = None,
                                      saveAccountingMethod: Option[Int] = None,
                                      fetchTerms: Option[Int] = None,
                                      saveTerms: Option[Int] = None,
                                      fetchNotEligible: Option[Int] = None,
                                      saveNotEligible: Option[Int] = None,
                                      fetchOtherIncome: Option[Int] = None,
                                      saveOtherIncome: Option[Int] = None,
                                      fetchSubscriptionId: Option[Int] = None,
                                      saveSubscriptionId: Option[Int] = None,
                                      fetchAccountingPeriodPrior: Option[Int] = None,
                                      saveAccountingPeriodPrior: Option[Int] = None,
                                      fetchRegisterNextAccountingPeriod: Option[Int] = None,
                                      saveRegisterNextAccountingPeriod: Option[Int] = None,
                                      fetchClientDetails: Option[Int] = None,
                                      saveClientDetails: Option[Int] = None,
                                      fetchAll: Option[Int] = None,
                                      deleteAll: Option[Int] = None
                                    ): Unit = {
    verifyKeystoreFetch(IncomeSource, fetchIncomeSource)
    verifyKeystoreSave(IncomeSource, saveIncomeSource)
    verifyKeystoreFetch(BusinessName, fetchBusinessName)
    verifyKeystoreSave(BusinessName, saveBusinessName)
    verifyKeystoreFetch(AccountingPeriodDate, fetchAccountingPeriodDate)
    verifyKeystoreSave(AccountingPeriodDate, saveAccountingPeriodDate)
    verifyKeystoreFetch(AccountingMethod, fetchAccountingMethod)
    verifyKeystoreSave(AccountingMethod, saveAccountingMethod)
    verifyKeystoreFetch(Terms, fetchTerms)
    verifyKeystoreSave(Terms, saveTerms)
    verifyKeystoreFetch(NotEligible, fetchNotEligible)
    verifyKeystoreSave(NotEligible, saveNotEligible)
    verifyKeystoreFetch(OtherIncome, fetchOtherIncome)
    verifyKeystoreSave(OtherIncome, saveOtherIncome)
    verifyKeystoreFetch(MtditId, fetchSubscriptionId)
    verifyKeystoreSave(MtditId, saveSubscriptionId)
    verifyKeystoreFetch(AccountingPeriodPrior, fetchAccountingPeriodPrior)
    verifyKeystoreSave(AccountingPeriodPrior, saveAccountingPeriodPrior)
    verifyKeystoreFetch(RegisterNextAccountingPeriod, fetchRegisterNextAccountingPeriod)
    verifyKeystoreSave(RegisterNextAccountingPeriod, saveRegisterNextAccountingPeriod)
    verifyKeystoreFetch(ClientDetails, fetchClientDetails)
    verifyKeystoreSave(ClientDetails, saveClientDetails)

    fetchAll ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetch()(ArgumentMatchers.any()))
    deleteAll ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).remove()(ArgumentMatchers.any()))
  }

}
