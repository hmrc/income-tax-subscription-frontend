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

import connectors.IncomeTaxSubscriptionConnector
import models.common.{AccountingMethodModel, AccountingMethodPropertyModel, AccountingYearModel, BusinessNameModel}
import models.individual.business.{AccountingPeriodModel, MatchTaxYearModel, PropertyCommencementDateModel}
import models.individual.incomesource.IncomeSourceModel
import models.individual.subscription.IncomeSourceType
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys._
import utilities.UnitTestTrait

import scala.concurrent.Future

trait MockSubscriptionDetailsService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val returnedCacheMap: CacheMap = CacheMap("", Map())

  object MockSubscriptionDetailsService extends SubscriptionDetailsService(mock[IncomeTaxSubscriptionConnector])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(MockSubscriptionDetailsService.subscriptionDetailsSession)
  }

  private final def mockFetchFromSubscriptionDetails[T](key: String, config: Future[Option[T]]): Unit =
    when(MockSubscriptionDetailsService.fetch[T](ArgumentMatchers.eq(key))(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(config)

  protected final def verifySubscriptionDetailsFetch[T](key: String, someCount: Option[Int]): Unit =
    someCount map (count => verify(MockSubscriptionDetailsService, times(count)).fetch[T](ArgumentMatchers.eq(key))(
      ArgumentMatchers.any(), ArgumentMatchers.any()))

  protected final def verifySubscriptionDetailsSave[T](key: String, someCount: Option[Int]): Unit =
    someCount map (count => verify(MockSubscriptionDetailsService, times(count)).save[T](ArgumentMatchers.eq(key), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any()))


  protected final def setupMockSubscriptionDetailsSaveFunctions(): Unit =
    when(MockSubscriptionDetailsService.save(ArgumentMatchers.any(), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(returnedCacheMap))

  protected final def mockFetchIncomeSourceFromSubscriptionDetails(fetchIncomeSource: Option[IncomeSourceType]): Unit = {
    mockFetchFromSubscriptionDetails[IncomeSourceType](IncomeSource, fetchIncomeSource)
  }

  protected final def mockFetchIndividualIncomeSourceFromSubscriptionDetails(fetchIndividualIncomeSource: Option[IncomeSourceModel]): Unit = {
    mockFetchFromSubscriptionDetails[IncomeSourceModel](IndividualIncomeSource, fetchIndividualIncomeSource)
  }

  protected final def mockFetchBusinessNameFromSubscriptionDetails(fetchBusinessName: Option[BusinessNameModel]): Unit = {
    mockFetchFromSubscriptionDetails[BusinessNameModel](BusinessName, fetchBusinessName)
  }

  protected final def mockFetchAccountingPeriodFromSubscriptionDetails(fetchAccountingPeriodDate: Option[AccountingPeriodModel]): Unit = {
    mockFetchFromSubscriptionDetails[AccountingPeriodModel](AccountingPeriodDate, fetchAccountingPeriodDate)
  }

  protected final def mockFetchAccountingMethodFromSubscriptionDetails(fetchAccountingMethod: Option[AccountingMethodModel]): Unit = {
    mockFetchFromSubscriptionDetails[AccountingMethodModel](AccountingMethod, fetchAccountingMethod)
  }

  protected final def mockFetchPropertyAccountingFromSubscriptionDetails(fetchPropertyAccountingMethod: Option[AccountingMethodPropertyModel]): Unit = {
    mockFetchFromSubscriptionDetails[AccountingMethodPropertyModel](PropertyAccountingMethod, fetchPropertyAccountingMethod)
  }

  protected final def mockFetchPropertyCommencementDateFromSubscriptionDetails(fetchPropertyCommencementDateMethod:
                                                                               Option[PropertyCommencementDateModel]): Unit = {
    mockFetchFromSubscriptionDetails[PropertyCommencementDateModel](PropertyCommencementDate, fetchPropertyCommencementDateMethod)
  }

  protected final def mockFetchSubscriptionIdFromSubscriptionDetails(fetchSubscriptionId: Option[String]): Unit = {
    mockFetchFromSubscriptionDetails[String](MtditId, fetchSubscriptionId)
  }

  protected final def mockFetchMatchTaxYearFromSubscriptionDetails(fetchMatchTaxYear: Option[MatchTaxYearModel]): Unit = {
    mockFetchFromSubscriptionDetails[MatchTaxYearModel](MatchTaxYear, fetchMatchTaxYear)
  }

  protected final def mockFetchSelectedTaxYearFromSubscriptionDetails(fetchSelectedTaxYear: Option[AccountingYearModel]): Unit = {
    mockFetchFromSubscriptionDetails[AccountingYearModel](SelectedTaxYear, fetchSelectedTaxYear)
  }

  protected final def mockFetchPaperlessPreferenceToken(fetchPaperlessPreferenceToken: Option[String]): Unit = {
    mockFetchFromSubscriptionDetails[String](PaperlessPreferenceToken, fetchPaperlessPreferenceToken)
  }

  protected final def mockFetchAllFromSubscriptionDetails(fetchAll: Option[CacheMap]): Unit = {
    when(MockSubscriptionDetailsService.subscriptionDetailsSession.getSubscriptionDetails[CacheMap](ArgumentMatchers.any())
    ).thenReturn(Future.successful(fetchAll))
  }

  protected final def mockDeleteAllFromSubscriptionDetails(deleteAll: HttpResponse): Unit = {
    when(MockSubscriptionDetailsService.deleteAll()(ArgumentMatchers.any())).thenReturn(deleteAll)
  }

  protected final def verifySubscriptionDetailsFetchAll(fetchAll: Option[Int]): Unit = {
    fetchAll map (count => verify(MockSubscriptionDetailsService, times(count)).fetch(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

  protected final def verifySubscriptionDetailsDeleteAll(deleteAll: Option[Int]): Unit = {
    deleteAll map (count => verify(MockSubscriptionDetailsService, times(count)).deleteAll()(ArgumentMatchers.any()))
  }
}
