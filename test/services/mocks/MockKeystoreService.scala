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

package services.mocks

import models.common.{AccountingMethodModel, AccountingMethodPropertyModel, AccountingYearModel, BusinessNameModel}
import models.individual.business.address.Address
import models.individual.business.{AccountingPeriodModel, BusinessPhoneNumberModel, BusinessStartDateModel, MatchTaxYearModel}
import models.individual.incomesource.{AreYouSelfEmployedModel, RentUkPropertyModel}
import models.individual.subscription.IncomeSourceType
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, times, verify, when}
import services.KeystoreService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import utilities.MockTrait
import utilities.CacheConstants._

import scala.concurrent.Future

trait MockKeystoreService extends MockTrait {

  val returnedCacheMap: CacheMap = CacheMap("", Map())

  object MockKeystoreService extends KeystoreService(mock[SessionCache])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(MockKeystoreService.session)
  }

  private final def mockFetchFromKeyStore[T](key: String, config: MFO[T]): Unit =
    config ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetchAndGetEntry[T](ArgumentMatchers.eq(key))(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(dataToReturn))

  private final def verifyKeystoreFetch[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetchAndGetEntry[T](ArgumentMatchers.eq(key))(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))

  private final def verifyKeystoreSave[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).cache[T](ArgumentMatchers.eq(key), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))


  protected final def setupMockKeystoreSaveFunctions(): Unit =
    when(MockKeystoreService.session.cache(ArgumentMatchers.any(), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnedCacheMap))

  protected final def mockFetchIncomeSourceFromKeyStore(fetchIncomeSource: MFO[IncomeSourceType]): Unit = {
    mockFetchFromKeyStore[IncomeSourceType](IncomeSource, fetchIncomeSource)
  }

  protected final def mockFetchRentUkPropertyFromKeyStore(fetchRentUkProperty: MFO[RentUkPropertyModel]): Unit = {
    mockFetchFromKeyStore[RentUkPropertyModel](RentUkProperty, fetchRentUkProperty)
  }

  protected final def mockFetchAreYouSelfEmployedFromKeyStore(fetchAreYouSelfEmployed: MFO[AreYouSelfEmployedModel]): Unit = {
    mockFetchFromKeyStore[AreYouSelfEmployedModel](AreYouSelfEmployed, fetchAreYouSelfEmployed)
  }

  protected final def mockFetchBusinessNameFromKeyStore(fetchBusinessName: MFO[BusinessNameModel]): Unit = {
    mockFetchFromKeyStore[BusinessNameModel](BusinessName, fetchBusinessName)
  }

  protected final def mockFetchBusinessPhoneNumberFromKeyStore(fetchBusinessPhoneNumber: MFO[BusinessPhoneNumberModel]): Unit = {
    mockFetchFromKeyStore[BusinessPhoneNumberModel](BusinessPhoneNumber, fetchBusinessPhoneNumber)
  }

  protected final def mockFetchBusinessAddressFromKeyStore(fetchBusinessAddress: MFO[Address]): Unit = {
    mockFetchFromKeyStore[Address](BusinessAddress, fetchBusinessAddress)
  }

  protected final def mockFetchBusinessStartDateFromKeyStore(fetchBusinessStartDate: MFO[BusinessStartDateModel]): Unit = {
    mockFetchFromKeyStore[BusinessStartDateModel](BusinessStartDate, fetchBusinessStartDate)
  }

  protected final def mockFetchAccountingPeriodFromKeyStore(fetchAccountingPeriodDate: MFO[AccountingPeriodModel]): Unit = {
    mockFetchFromKeyStore[AccountingPeriodModel](AccountingPeriodDate, fetchAccountingPeriodDate)
  }

  protected final def mockFetchAccountingMethodFromKeyStore(fetchAccountingMethod: MFO[AccountingMethodModel]): Unit = {
    mockFetchFromKeyStore[AccountingMethodModel](AccountingMethod, fetchAccountingMethod)
  }

  protected final def mockFetchPropertyAccountingFromKeyStore(fetchPropertyAccountingMethod: MFO[AccountingMethodPropertyModel]): Unit = {
    mockFetchFromKeyStore[AccountingMethodPropertyModel](PropertyAccountingMethod, fetchPropertyAccountingMethod)
  }

  protected final def mockFetchSubscriptionIdFromKeyStore(fetchSubscriptionId: MFO[String]): Unit = {
    mockFetchFromKeyStore[String](MtditId, fetchSubscriptionId)
  }

  protected final def mockFetchMatchTaxYearFromKeyStore(fetchMatchTaxYear: MFO[MatchTaxYearModel]): Unit = {
    mockFetchFromKeyStore[MatchTaxYearModel](MatchTaxYear, fetchMatchTaxYear)
  }

  protected final def mockFetchSelectedTaxYearFromKeyStore(fetchSelectedTaxYear: MFO[AccountingYearModel]): Unit = {
    mockFetchFromKeyStore[AccountingYearModel](SelectedTaxYear, fetchSelectedTaxYear)
  }

  protected final def mockFetchPaperlessPreferenceToken(fetchPaperlessPreferenceToken: MFO[String]): Unit = {
    mockFetchFromKeyStore[String](PaperlessPreferenceToken, fetchPaperlessPreferenceToken)
  }

  protected final def mockFetchAllFromKeyStore(fetchAll: MFO[CacheMap]): Unit = {
    fetchAll ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetch()(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(dataToReturn))
  }

  protected final def mockDeleteAllFromKeyStore(deleteAll: MF[HttpResponse]): Unit = {
    deleteAll ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.remove()(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(dataToReturn))
  }

  protected final def verifyKeystore(
                                      fetchIncomeSource: Option[Int] = None,
                                      saveIncomeSource: Option[Int] = None,
                                      fetchRentUkProperty: Option[Int] = None,
                                      saveRentUkProperty: Option[Int] = None,
                                      fetchAreYouSelfEmployed: Option[Int] = None,
                                      saveAreYouSelfEmployed: Option[Int] = None,
                                      fetchBusinessName: Option[Int] = None,
                                      saveBusinessName: Option[Int] = None,
                                      fetchBusinessPhoneNumber: Option[Int] = None,
                                      saveBusinessPhoneNumber: Option[Int] = None,
                                      fetchBusinessAddress: Option[Int] = None,
                                      saveBusinessAddress: Option[Int] = None,
                                      fetchMatchTaxYear: Option[Int] = None,
                                      saveMatchTaxYear: Option[Int] = None,
                                      fetchSelectedTaxYear: Option[Int] = None,
                                      saveSelectedTaxYear: Option[Int] = None,
                                      fetchBusinessStartDate: Option[Int] = None,
                                      saveBusinessStartDate: Option[Int] = None,
                                      fetchAccountingPeriodDate: Option[Int] = None,
                                      saveAccountingPeriodDate: Option[Int] = None,
                                      fetchAccountingMethod: Option[Int] = None,
                                      saveAccountingMethod: Option[Int] = None,
                                      fetchPropertyAccountingMethod: Option[Int] = None,
                                      savePropertyAccountingMethod: Option[Int] = None,
                                      fetchSubscriptionId: Option[Int] = None,
                                      saveSubscriptionId: Option[Int] = None,
                                      fetchPaperlessPreferenceToken: Option[Int] = None,
                                      savePaperlessPreferenceToken: Option[Int] = None,
                                      fetchAll: Option[Int] = None,
                                      deleteAll: Option[Int] = None
                                    ): Unit = {
    verifyKeystoreFetch(IncomeSource, fetchIncomeSource)
    verifyKeystoreSave(IncomeSource, saveIncomeSource)
    verifyKeystoreFetch(RentUkProperty, fetchRentUkProperty)
    verifyKeystoreSave(RentUkProperty, saveRentUkProperty)
    verifyKeystoreFetch(AreYouSelfEmployed, fetchAreYouSelfEmployed)
    verifyKeystoreSave(AreYouSelfEmployed, saveAreYouSelfEmployed)
    verifyKeystoreFetch(BusinessName, fetchBusinessName)
    verifyKeystoreSave(BusinessName, saveBusinessName)
    verifyKeystoreFetch(BusinessPhoneNumber, fetchBusinessPhoneNumber)
    verifyKeystoreSave(BusinessPhoneNumber, saveBusinessPhoneNumber)
    verifyKeystoreFetch(BusinessAddress, fetchBusinessAddress)
    verifyKeystoreSave(BusinessAddress, saveBusinessAddress)
    verifyKeystoreFetch(BusinessStartDate, fetchBusinessStartDate)
    verifyKeystoreSave(BusinessStartDate, saveBusinessStartDate)
    verifyKeystoreFetch(MatchTaxYear, fetchMatchTaxYear)
    verifyKeystoreSave(MatchTaxYear, saveMatchTaxYear)
    verifyKeystoreFetch(SelectedTaxYear, fetchSelectedTaxYear)
    verifyKeystoreSave(SelectedTaxYear, saveSelectedTaxYear)
    verifyKeystoreFetch(AccountingPeriodDate, fetchAccountingPeriodDate)
    verifyKeystoreSave(AccountingPeriodDate, saveAccountingPeriodDate)
    verifyKeystoreFetch(AccountingMethod, fetchAccountingMethod)
    verifyKeystoreSave(AccountingMethod, saveAccountingMethod)
    verifyKeystoreFetch(PropertyAccountingMethod, fetchPropertyAccountingMethod)
    verifyKeystoreSave(PropertyAccountingMethod, savePropertyAccountingMethod)
    verifyKeystoreFetch(MtditId, fetchSubscriptionId)
    verifyKeystoreSave(MtditId, saveSubscriptionId)
    verifyKeystoreFetch(PaperlessPreferenceToken, fetchPaperlessPreferenceToken)
    verifyKeystoreSave(PaperlessPreferenceToken, savePaperlessPreferenceToken)

    fetchAll ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetch()(ArgumentMatchers.any(), ArgumentMatchers.any()))
    deleteAll ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).remove()(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

}
