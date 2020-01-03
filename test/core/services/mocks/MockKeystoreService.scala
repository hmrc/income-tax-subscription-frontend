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

package core.services.mocks

import core.models.YesNo
import core.services.KeystoreService
import core.utils.MockTrait
import incometax.business.models._
import incometax.business.models.address.Address
import incometax.incomesource.models.{RentUkPropertyModel, AreYouSelfEmployedModel}
import incometax.subscription.models.IncomeSourceType
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}

import scala.concurrent.Future


trait MockKeystoreService extends MockTrait {

  import core.services.CacheConstants._

  val returnedCacheMap: CacheMap = CacheMap("", Map())

  object MockKeystoreService extends KeystoreService(mock[SessionCache])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(MockKeystoreService.session)
  }

  private final def mockFetchFromKeyStore[T](key: String, config: MFO[T]): Unit =
    config ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetchAndGetEntry[T](ArgumentMatchers.eq(key))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(dataToReturn))

  private final def verifyKeystoreFetch[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetchAndGetEntry[T](ArgumentMatchers.eq(key))(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))

  private final def verifyKeystoreSave[T](key: String, someCount: Option[Int]): Unit =
    someCount ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).cache[T](ArgumentMatchers.eq(key), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))


  protected final def setupMockKeystoreSaveFunctions(): Unit =
    when(MockKeystoreService.session.cache(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnedCacheMap))

  protected final def setupMockKeystore(
                                         fetchIncomeSource: MFO[IncomeSourceType] = DoNotConfigure,
                                         fetchRentUkProperty: MFO[RentUkPropertyModel] = DoNotConfigure,
                                         fetchAreYouSelfEmployed: MFO[AreYouSelfEmployedModel] = DoNotConfigure,
                                         fetchBusinessName: MFO[BusinessNameModel] = DoNotConfigure,
                                         fetchBusinessPhoneNumber: MFO[BusinessPhoneNumberModel] = DoNotConfigure,
                                         fetchBusinessAddress: MFO[Address] = DoNotConfigure,
                                         fetchMatchTaxYear: MFO[MatchTaxYearModel] = DoNotConfigure,
                                         fetchBusinessStartDate: MFO[BusinessStartDateModel] = DoNotConfigure,
                                         fetchAccountingPeriodDate: MFO[AccountingPeriodModel] = DoNotConfigure,
                                         fetchPropertyAccountingMethod: MFO[AccountingMethodPropertyModel] = DoNotConfigure,
                                         fetchAccountingMethod: MFO[AccountingMethodModel] = DoNotConfigure,
                                         fetchSelectedTaxYear: MFO[AccountingYearModel] = DoNotConfigure,
                                         fetchTerms: MFO[Boolean] = DoNotConfigure,
                                         fetchOtherIncome: MFO[YesNo] = DoNotConfigure,
                                         fetchSubscriptionId: MFO[String] = DoNotConfigure,
                                         fetchPaperlessPreferenceToken: MFO[String] = DoNotConfigure,
                                         fetchAll: MFO[CacheMap] = DoNotConfigure,
                                         deleteAll: MF[HttpResponse] = DoNotConfigure
                                       ): Unit = {
    mockFetchFromKeyStore[IncomeSourceType](IncomeSource, fetchIncomeSource)
    mockFetchFromKeyStore[RentUkPropertyModel](RentUkProperty, fetchRentUkProperty)
    mockFetchFromKeyStore[AreYouSelfEmployedModel](AreYouSelfEmployed, fetchAreYouSelfEmployed)
    mockFetchFromKeyStore[BusinessNameModel](BusinessName, fetchBusinessName)
    mockFetchFromKeyStore[BusinessPhoneNumberModel](BusinessPhoneNumber, fetchBusinessPhoneNumber)
    mockFetchFromKeyStore[Address](BusinessAddress, fetchBusinessAddress)
    mockFetchFromKeyStore[MatchTaxYearModel](MatchTaxYear, fetchMatchTaxYear)
    mockFetchFromKeyStore[AccountingYearModel](SelectedTaxYear, fetchSelectedTaxYear)
    mockFetchFromKeyStore[BusinessStartDateModel](BusinessStartDate, fetchBusinessStartDate)
    mockFetchFromKeyStore[AccountingPeriodModel](AccountingPeriodDate, fetchAccountingPeriodDate)
    mockFetchFromKeyStore[AccountingMethodModel](AccountingMethod, fetchAccountingMethod)
    mockFetchFromKeyStore[AccountingMethodPropertyModel](PropertyAccountingMethod, fetchPropertyAccountingMethod)
    mockFetchFromKeyStore[Boolean](Terms, fetchTerms)
    mockFetchFromKeyStore[YesNo](OtherIncome, fetchOtherIncome)
    mockFetchFromKeyStore[String](MtditId, fetchSubscriptionId)
    mockFetchFromKeyStore[String](PaperlessPreferenceToken, fetchPaperlessPreferenceToken)

    setupMockKeystoreSaveFunctions()

    fetchAll ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.fetch()(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(dataToReturn))
    deleteAll ifConfiguredThen (dataToReturn => when(MockKeystoreService.session.remove()(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(dataToReturn))
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
                                      fetchTerms: Option[Int] = None,
                                      saveTerms: Option[Int] = None,
                                      fetchOtherIncome: Option[Int] = None,
                                      saveOtherIncome: Option[Int] = None,
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
    verifyKeystoreFetch(Terms, fetchTerms)
    verifyKeystoreSave(Terms, saveTerms)
    verifyKeystoreFetch(OtherIncome, fetchOtherIncome)
    verifyKeystoreSave(OtherIncome, saveOtherIncome)
    verifyKeystoreFetch(MtditId, fetchSubscriptionId)
    verifyKeystoreSave(MtditId, saveSubscriptionId)
    verifyKeystoreFetch(PaperlessPreferenceToken, fetchPaperlessPreferenceToken)
    verifyKeystoreSave(PaperlessPreferenceToken, savePaperlessPreferenceToken)

    fetchAll ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).fetch()(ArgumentMatchers.any(), ArgumentMatchers.any()))
    deleteAll ifDefinedThen (count => verify(MockKeystoreService.session, times(count)).remove()(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

}
