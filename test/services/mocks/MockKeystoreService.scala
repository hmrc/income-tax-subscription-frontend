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
import models.individual.incomesource.IncomeSourceModel
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

  protected final def verifyKeystoreFetch[T](key: String, someCount: Option[Int]): Unit =
    someCount map (count => verify(MockKeystoreService.session, times(count)).fetchAndGetEntry[T](ArgumentMatchers.eq(key))(
      ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))

  protected final def verifyKeystoreSave[T](key: String, someCount: Option[Int]): Unit =
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

  protected final def verifyKeyStoreFetchAll(fetchAll: Option[Int]): Unit = {
    fetchAll map (count => verify(MockKeystoreService.session, times(count)).fetch()(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

  protected final def verifyKeyStoreDeleteAll(deleteAll: Option[Int]): Unit = {
    deleteAll map (count => verify(MockKeystoreService.session, times(count)).remove()(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }
}
