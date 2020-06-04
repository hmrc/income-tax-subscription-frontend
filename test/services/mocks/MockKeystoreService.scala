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
import models.individual.incomesource.{AreYouSelfEmployedModel, IncomeSourceModel, RentUkPropertyModel}
import models.individual.subscription.IncomeSourceType
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.KeystoreService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import utilities.CacheConstants._
import utilities.UnitTestTrait
import utilities.TestModels.emptyCacheMap

import scala.concurrent.Future

trait MockKeystoreService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val returnedCacheMap: CacheMap = CacheMap("", Map())

  object MockKeystoreService extends KeystoreService(mock[SessionCache])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(MockKeystoreService.session)
  }

  private final def mockFetchFromKeyStore[T](key: String, config: Future[Option[T]]): Unit =
    when(MockKeystoreService.session.fetchAndGetEntry[T](ArgumentMatchers.eq(key))(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(config)

  private final def verifyKeystoreFetch[T](key: String, someCount: Option[Int]): Unit =
    someCount map (count => verify(MockKeystoreService.session, times(count)).fetchAndGetEntry[T](ArgumentMatchers.eq(key))(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))

  private final def verifyKeystoreSave[T](key: String, someCount: Option[Int]): Unit =
    someCount map (count => verify(MockKeystoreService.session, times(count)).cache[T](ArgumentMatchers.eq(key), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))


  protected final def setupMockKeystoreSaveFunctions(): Unit =
    when(MockKeystoreService.session.cache(ArgumentMatchers.any(), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnedCacheMap))

  protected final def mockFetchIncomeSourceFromKeyStore(fetchIncomeSource: Option[IncomeSourceType]): Unit = {
    mockFetchFromKeyStore[IncomeSourceType](IncomeSource, fetchIncomeSource)
  }

  protected final def mockFetchIndividualIncomeSourceFromKeyStore(fetchIndividualIncomeSource: Option[IncomeSourceModel]): Unit = {
    mockFetchFromKeyStore[IncomeSourceModel](IndividualIncomeSource, fetchIndividualIncomeSource)
  }

  protected final def mockFetchRentUkPropertyFromKeyStore(fetchRentUkProperty: Option[RentUkPropertyModel]): Unit = {
    mockFetchFromKeyStore[RentUkPropertyModel](RentUkProperty, fetchRentUkProperty)
  }

  protected final def mockFetchAreYouSelfEmployedFromKeyStore(fetchAreYouSelfEmployed: Option[AreYouSelfEmployedModel]): Unit = {
    mockFetchFromKeyStore[AreYouSelfEmployedModel](AreYouSelfEmployed, fetchAreYouSelfEmployed)
  }

  protected final def mockFetchBusinessNameFromKeyStore(fetchBusinessName: Option[BusinessNameModel]): Unit = {
    mockFetchFromKeyStore[BusinessNameModel](BusinessName, fetchBusinessName)
  }

  protected final def mockFetchBusinessPhoneNumberFromKeyStore(fetchBusinessPhoneNumber: Option[BusinessPhoneNumberModel]): Unit = {
    mockFetchFromKeyStore[BusinessPhoneNumberModel](BusinessPhoneNumber, fetchBusinessPhoneNumber)
  }

  protected final def mockFetchBusinessAddressFromKeyStore(fetchBusinessAddress: Option[Address]): Unit = {
    mockFetchFromKeyStore[Address](BusinessAddress, fetchBusinessAddress)
  }

  protected final def mockFetchBusinessStartDateFromKeyStore(fetchBusinessStartDate: Option[BusinessStartDateModel]): Unit = {
    mockFetchFromKeyStore[BusinessStartDateModel](BusinessStartDate, fetchBusinessStartDate)
  }

  protected final def mockFetchAccountingPeriodFromKeyStore(fetchAccountingPeriodDate: Option[AccountingPeriodModel]): Unit = {
    mockFetchFromKeyStore[AccountingPeriodModel](AccountingPeriodDate, fetchAccountingPeriodDate)
  }

  protected final def mockFetchAccountingMethodFromKeyStore(fetchAccountingMethod: Option[AccountingMethodModel]): Unit = {
    mockFetchFromKeyStore[AccountingMethodModel](AccountingMethod, fetchAccountingMethod)
  }

  protected final def mockFetchPropertyAccountingFromKeyStore(fetchPropertyAccountingMethod: Option[AccountingMethodPropertyModel]): Unit = {
    mockFetchFromKeyStore[AccountingMethodPropertyModel](PropertyAccountingMethod, fetchPropertyAccountingMethod)
  }

  protected final def mockFetchSubscriptionIdFromKeyStore(fetchSubscriptionId: Option[String]): Unit = {
    mockFetchFromKeyStore[String](MtditId, fetchSubscriptionId)
  }

  protected final def mockFetchMatchTaxYearFromKeyStore(fetchMatchTaxYear: Option[MatchTaxYearModel]): Unit = {
    mockFetchFromKeyStore[MatchTaxYearModel](MatchTaxYear, fetchMatchTaxYear)
  }

  protected final def mockFetchSelectedTaxYearFromKeyStore(fetchSelectedTaxYear: Option[AccountingYearModel]): Unit = {
    mockFetchFromKeyStore[AccountingYearModel](SelectedTaxYear, fetchSelectedTaxYear)
  }

  protected final def mockFetchPaperlessPreferenceToken(fetchPaperlessPreferenceToken: Option[String]): Unit = {
    mockFetchFromKeyStore[String](PaperlessPreferenceToken, fetchPaperlessPreferenceToken)
  }

  protected final def mockFetchAllFromKeyStore(fetchAll: Option[CacheMap]): Unit = {
    when(MockKeystoreService.session.fetch()(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(fetchAll.getOrElse(emptyCacheMap))
  }

  protected final def mockDeleteAllFromKeyStore(deleteAll: HttpResponse): Unit = {
    when(MockKeystoreService.session.remove()(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(deleteAll)
  }

  protected final def verifyKeystore(
                                      fetchIncomeSource: Option[Int] = None,
                                      saveIncomeSource: Option[Int] = None,
                                      fetchIndividualIncomeSource: Option[Int] = None,
                                      saveIndividualIncomeSource: Option[Int] = None,
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
    verifyKeystoreFetch(IndividualIncomeSource, fetchIndividualIncomeSource)
    verifyKeystoreSave(IndividualIncomeSource, saveIndividualIncomeSource)
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

    fetchAll map (count => verify(MockKeystoreService.session, times(count)).fetch()(ArgumentMatchers.any(), ArgumentMatchers.any()))
    deleteAll map (count => verify(MockKeystoreService.session, times(count)).remove()(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

}
