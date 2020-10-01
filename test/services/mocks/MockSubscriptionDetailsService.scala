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
import connectors.httpparser.PostSubscriptionDetailsHttpParser.PostSubscriptionDetailsSuccessResponse
import models.common._
import models.individual.business.PropertyCommencementDateModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{Json, Writes}
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys._
import utilities.UnitTestTrait

import scala.concurrent.Future

trait MockSubscriptionDetailsService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {


  val mockSubscriptionDetailsService: SubscriptionDetailsService = mock[SubscriptionDetailsService]

  val returnedCacheMap: CacheMap = CacheMap("", Map())
  val mockConnector = mock[IncomeTaxSubscriptionConnector]
  var testData: CacheMap = CacheMap("", Map.empty)

  object MockSubscriptionDetailsService extends SubscriptionDetailsService(mockConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(MockSubscriptionDetailsService.subscriptionDetailsSession)
    testData = CacheMap("", Map.empty)
  }

  private final def mockFetchFromSubscriptionDetails[T](key: String, config: Option[T])(implicit writes: Writes[T]): Unit = {
    testData = config match {
      case Some(data) => CacheMap("", testData.data.updated(key, Json.toJson(data)))
      case _ => testData
    }
    when(mockConnector.getSubscriptionDetails[CacheMap](ArgumentMatchers.eq(subscriptionId))(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testData)))
  }

  protected final def verifySubscriptionDetailsFetch[T](key: String, someCount: Option[Int]): Unit =
    someCount map (count => verify(mockConnector, times(count)).getSubscriptionDetails[T](ArgumentMatchers.eq(subscriptionId))(
      ArgumentMatchers.any(), ArgumentMatchers.any()))

  protected final def verifySubscriptionDetailsSave[T](key: String, someCount: Option[Int]): Unit =
    someCount map (count => verify(mockConnector, times(count)).saveSubscriptionDetails[T](ArgumentMatchers.eq(subscriptionId), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any()))


  protected final def setupMockSubscriptionDetailsSaveFunctions(): Unit =
    mockFetchFromSubscriptionDetails[String]("fakeKey", None)

  when(mockConnector.saveSubscriptionDetails(ArgumentMatchers.any(), ArgumentMatchers.any())(
    ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Right(PostSubscriptionDetailsSuccessResponse)))

  protected final def mockFetchIncomeSourceFromSubscriptionDetails(fetchIncomeSource: Option[IncomeSourceModel]): Unit = {
    mockFetchFromSubscriptionDetails[IncomeSourceModel](IncomeSource, fetchIncomeSource)
  }

  protected final def mockFetchIndividualIncomeSourceFromSubscriptionDetails(fetchIndividualIncomeSource: Option[IncomeSourceModel]): Unit = {
    mockFetchFromSubscriptionDetails[IncomeSourceModel](IncomeSource, fetchIndividualIncomeSource)
  }

  protected final def mockFetchBusinessNameFromSubscriptionDetails(fetchBusinessName: Option[BusinessNameModel]): Unit = {
    mockFetchFromSubscriptionDetails[BusinessNameModel](BusinessName, fetchBusinessName)
  }

  protected final def mockFetchAccountingMethodFromSubscriptionDetails(fetchAccountingMethod: Option[AccountingMethodModel]): Unit = {
    mockFetchFromSubscriptionDetails[AccountingMethodModel](AccountingMethod, fetchAccountingMethod)
  }

  protected final def mockFetchPropertyAccountingFromSubscriptionDetails(fetchPropertyAccountingMethod: Option[AccountingMethodPropertyModel]): Unit = {
    mockFetchFromSubscriptionDetails[AccountingMethodPropertyModel](PropertyAccountingMethod, fetchPropertyAccountingMethod)
  }

  protected final def mockFetchForeignPropertyAccountingFromSubscriptionDetails(fetchForeignPropertyAccountingMethod: Option[AccountingMethodPropertyModel]): Unit = {
    mockFetchFromSubscriptionDetails[AccountingMethodPropertyModel](OverseasPropertyAccountingMethod, fetchForeignPropertyAccountingMethod)
  }

  protected final def mockFetchPropertyCommencementDateFromSubscriptionDetails(fetchPropertyCommencementDateMethod:
                                                                               Option[PropertyCommencementDateModel]): Unit = {
    mockFetchFromSubscriptionDetails[PropertyCommencementDateModel](PropertyCommencementDate, fetchPropertyCommencementDateMethod)
  }

  protected final def mockFetchSubscriptionIdFromSubscriptionDetails(fetchSubscriptionId: Option[String]): Unit = {
    mockFetchFromSubscriptionDetails[String](MtditId, fetchSubscriptionId)
  }

  protected final def mockFetchSelectedTaxYearFromSubscriptionDetails(fetchSelectedTaxYear: Option[AccountingYearModel]): Unit = {
    mockFetchFromSubscriptionDetails[AccountingYearModel](SelectedTaxYear, fetchSelectedTaxYear)
  }

  protected final def mockFetchPaperlessPreferenceToken(fetchPaperlessPreferenceToken: Option[String]): Unit = {
    mockFetchFromSubscriptionDetails[String](PaperlessPreferenceToken, fetchPaperlessPreferenceToken)
  }

  protected final def mockFetchAllFromSubscriptionDetails(fetchAll: Option[CacheMap]): Unit = {
    when(mockConnector.getSubscriptionDetails[CacheMap](ArgumentMatchers.eq(subscriptionId))(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(fetchAll))
  }

  protected final def mockDeleteAllFromSubscriptionDetails(deleteAll: HttpResponse): Unit = {
    when(mockConnector.deleteAll()(ArgumentMatchers.any())).thenReturn(deleteAll)
  }

  protected final def verifySubscriptionDetailsFetchAll(fetchAll: Option[Int]): Unit = {
    fetchAll map (count => verify(mockConnector, times(count)).getSubscriptionDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

  protected final def verifySubscriptionDetailsDeleteAll(deleteAll: Option[Int]): Unit = {
    deleteAll map (count => verify(mockConnector, times(count)).deleteAll()(ArgumentMatchers.any()))
  }
}
