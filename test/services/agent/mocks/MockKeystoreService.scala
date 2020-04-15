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

package services.agent.mocks

import utilities.agent.CacheConstants._
import models.agent.{AccountingMethodModel, AccountingMethodPropertyModel, AccountingYearModel, BusinessNameModel}
import models.individual.business.{AccountingPeriodModel, MatchTaxYearModel}
import models.individual.subscription.IncomeSourceType
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import services.agent.KeystoreService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import utilities.MockTrait

import scala.concurrent.{ExecutionContext, Future}


trait MockKeystoreService extends MockTrait {



  val returnedCacheMap: CacheMap = CacheMap("", Map())

  object MockKeystoreService extends KeystoreService(mock[SessionCache])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(MockKeystoreService.session)
  }

  private final def mockFetchFromKeyStore[T](key: String, config: MFO[T]): Unit =
    config ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetchAndGetEntry[T](ArgumentMatchers.eq(key))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])).thenReturn(dataToReturn))

  private final def verifyKeystoreFetch[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetchAndGetEntry[T]
      (ArgumentMatchers.eq(key))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext]))

  private final def verifyKeystoreSave[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).cache[T]
      (ArgumentMatchers.eq(key), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext]))


  protected final def setupMockKeystoreSaveFunctions(): Unit =
    when(MockKeystoreService.session.cache(ArgumentMatchers.any(), ArgumentMatchers.any())
    (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])).thenReturn(Future.successful(returnedCacheMap))

  protected final def mockFetchIncomeSourceFromKeyStore(fetchIncomeSource: MFO[IncomeSourceType]): Unit = {
    mockFetchFromKeyStore[IncomeSourceType](IncomeSource, fetchIncomeSource)
  }

  protected final def mockFetchBusinessNameFromKeyStore(fetchBusinessName: MFO[BusinessNameModel]): Unit = {
    mockFetchFromKeyStore[BusinessNameModel](BusinessName, fetchBusinessName)
  }

  protected final def mockFetchAccountingPeriodFromKeyStore(fetchAccountingPeriodDate: MFO[AccountingPeriodModel]): Unit = {
    mockFetchFromKeyStore[AccountingPeriodModel](AccountingPeriodDate, fetchAccountingPeriodDate)
  }

  protected final def mockFetchAccountingMethodFromKeyStore(fetchAccountingMethod: MFO[AccountingMethodModel]): Unit = {
    mockFetchFromKeyStore[AccountingMethodModel](AccountingMethod, fetchAccountingMethod)
  }

  protected final def mockFetchPropertyAccountingFromKeyStore(fetchPropertyAccountingMethod: MFO[AccountingMethodPropertyModel]): Unit = {
    mockFetchFromKeyStore[AccountingMethodPropertyModel](AccountingMethodProperty, fetchPropertyAccountingMethod)
  }

  protected final def mockFetchSubscriptionIdFromKeyStore(fetchSubscriptionId: MFO[String]): Unit = {
    mockFetchFromKeyStore[String](MtditId, fetchSubscriptionId)
  }

  protected final def mockFetchMatchTaxYearFromKeyStore(fetchMatchTaxYear: MFO[MatchTaxYearModel]): Unit = {
    mockFetchFromKeyStore[MatchTaxYearModel](MatchTaxYear, fetchMatchTaxYear)
  }

  protected final def mockFetchWhatYearToSignUpFromKeyStore(fetchWhatYearToSignUp: MFO[AccountingYearModel]): Unit = {
    mockFetchFromKeyStore[AccountingYearModel](WhatYearToSignUp, fetchWhatYearToSignUp)
  }

  protected final def mockFetchAllFromKeyStore(fetchAll: MFO[CacheMap]): Unit = {
    fetchAll ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetch()(ArgumentMatchers.any(),
      ArgumentMatchers.any[ExecutionContext])).thenReturn(dataToReturn))
  }

  protected final def mockDeleteAllFromKeyStore(deleteAll: MF[HttpResponse]): Unit = {
    deleteAll ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.remove()(ArgumentMatchers.any(),
      ArgumentMatchers.any[ExecutionContext])).thenReturn(dataToReturn))
  }

  protected final def verifyKeystore(
                                      fetchIncomeSource: Option[Int] = None,
                                      saveIncomeSource: Option[Int] = None,
                                      fetchBusinessName: Option[Int] = None,
                                      saveBusinessName: Option[Int] = None,
                                      fetchMatchTaxYear: Option[Int] = None,
                                      saveMatchTaxYear: Option[Int] = None,
                                      fetchWhatYearToSignUp: Option[Int] = None,
                                      saveWhatYearToSignUp: Option[Int] = None,
                                      fetchAccountingPeriodDate: Option[Int] = None,
                                      saveAccountingPeriodDate: Option[Int] = None,
                                      fetchAccountingMethod: Option[Int] = None,
                                      saveAccountingMethod: Option[Int] = None,
                                      fetchPropertyAccountingMethod: Option[Int] = None,
                                      savePropertyAccountingMethod: Option[Int] = None,
                                      fetchSubscriptionId: Option[Int] = None,
                                      saveSubscriptionId: Option[Int] = None,
                                      fetchAll: Option[Int] = None,
                                      deleteAll: Option[Int] = None
                                    ): Unit = {
    verifyKeystoreFetch(IncomeSource, fetchIncomeSource)
    verifyKeystoreSave(IncomeSource, saveIncomeSource)
    verifyKeystoreFetch(BusinessName, fetchBusinessName)
    verifyKeystoreSave(BusinessName, saveBusinessName)
    verifyKeystoreFetch(MatchTaxYear, fetchMatchTaxYear)
    verifyKeystoreSave(MatchTaxYear, saveMatchTaxYear)
    verifyKeystoreFetch(WhatYearToSignUp, fetchWhatYearToSignUp)
    verifyKeystoreSave(WhatYearToSignUp, saveWhatYearToSignUp)
    verifyKeystoreFetch(AccountingPeriodDate, fetchAccountingPeriodDate)
    verifyKeystoreSave(AccountingPeriodDate, saveAccountingPeriodDate)
    verifyKeystoreFetch(AccountingMethod, fetchAccountingMethod)
    verifyKeystoreSave(AccountingMethod, saveAccountingMethod)
    verifyKeystoreFetch(AccountingMethodProperty, fetchPropertyAccountingMethod)
    verifyKeystoreSave(AccountingMethodProperty, savePropertyAccountingMethod)
    verifyKeystoreFetch(MtditId, fetchSubscriptionId)
    verifyKeystoreSave(MtditId, saveSubscriptionId)

    fetchAll ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetch()(ArgumentMatchers.any(),
      ArgumentMatchers.any[ExecutionContext]))
    deleteAll ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).remove()(ArgumentMatchers.any(),
      ArgumentMatchers.any[ExecutionContext]))
  }

}
