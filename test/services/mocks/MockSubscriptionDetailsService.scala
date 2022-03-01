/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.httpparser.RetrieveReferenceHttpParser.RetrieveReferenceResponse
import models.common._
import models.common.business.{AccountingMethodModel, BusinessNameModel, SelfEmploymentData}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.{ArgumentMatcher, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{Json, Writes}
import services.SubscriptionDetailsService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.cache.client.CacheMap
import utilities.SubscriptionDataKeys.subscriptionId
import utilities.{SubscriptionDataKeys, UnitTestTrait}

import scala.concurrent.Future

trait MockSubscriptionDetailsService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach {

  val mockSubscriptionDetailsService: SubscriptionDetailsService = mock[SubscriptionDetailsService]

  val returnedCacheMap: CacheMap = CacheMap("", Map())
  val mockConnector: IncomeTaxSubscriptionConnector = mock[IncomeTaxSubscriptionConnector]
  var testData: CacheMap = CacheMap("", Map.empty)

  object MockSubscriptionDetailsService extends SubscriptionDetailsService(mockConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
    testData = CacheMap("", Map.empty)
  }

  def mockRetrieveReferenceSuccess(utr: String)(reference: String): Unit = {
    when(mockConnector.retrieveReference(
      ArgumentMatchers.eq(utr)
    )(ArgumentMatchers.any())) thenReturn Future.successful(Right(reference))
  }

  def mockRetrievePrePopFlag(result: Option[Boolean]): Unit =
    mockFetchFromSubscriptionDetails[Boolean](SubscriptionDataKeys.PrePopFlag, result)

  def mockRetrieveReference(utr: String)(response: RetrieveReferenceResponse): Unit = {
    when(mockConnector.retrieveReference(ArgumentMatchers.eq(utr))(ArgumentMatchers.any())) thenReturn Future.successful(response)
  }

  private final def mockFetchFromSubscriptionDetails[T](key: String, config: Option[T])(implicit writes: Writes[T]): Unit = {
    testData = config match {
      case Some(data) => CacheMap("", testData.data.updated(key, Json.toJson(data)))
      case _ => testData
    }
    when(mockConnector.getSubscriptionDetails[CacheMap](ArgumentMatchers.any(), ArgumentMatchers.eq(SubscriptionDataKeys.subscriptionId))(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testData)))
  }

  protected final def verifySubscriptionDetailsFetch[T](key: String, someCount: Option[Int]): Unit =
    someCount map (count => verify(mockConnector, times(count))
      .getSubscriptionDetails[T](ArgumentMatchers.any(), ArgumentMatchers.eq(SubscriptionDataKeys.subscriptionId))(
        ArgumentMatchers.any(), ArgumentMatchers.any())
      )

  case class CacheMapKeyMatcher(key: String) extends ArgumentMatcher[CacheMap] {
    def matches(cacheMap: CacheMap): Boolean = cacheMap.data.keys.toSet.contains(key)
  }

  case class SelfEmploymentListMatcher(wanted: List[SelfEmploymentData]) extends ArgumentMatcher[List[SelfEmploymentData]] {
    def matches(offered: List[SelfEmploymentData]): Boolean = {
      val zipped = offered.zip(wanted)
      offered.length == wanted.length &&
        zipped.map { case (o, w) =>
          o.businessStartDate == w.businessStartDate && // everything equal apart from id
            o.businessName == w.businessName &&
            o.businessTradeName == w.businessTradeName &&
            o.businessAddress == w.businessAddress &&
            o.confirmed == w.confirmed
        }.forall(b => b) // check for any not equal
    }
  }

  protected final def verifySubscriptionDetailsSave[T](key: String, count: Int): Unit =
    verify(mockConnector, times(count)).saveSubscriptionDetails[CacheMap](
      ArgumentMatchers.any(),
      ArgumentMatchers.eq(subscriptionId),
      argThat(CacheMapKeyMatcher(key)),
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  protected final def verifySubscriptionDetailsSaveWithField[T](count: Int, field: String, wanted: T): Unit =
    verify(mockConnector, times(count)).saveSubscriptionDetails[T](
      ArgumentMatchers.any(),
      ArgumentMatchers.eq(field),
      ArgumentMatchers.eq(wanted),
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  protected final def verifySubscriptionDetailsSaveWithField[T](count: Int, field: String, matcher: ArgumentMatcher[T]): Unit =
    verify(mockConnector, times(count)).saveSubscriptionDetails[T](
      ArgumentMatchers.any(),
      ArgumentMatchers.eq(field),
      argThat(matcher),
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  protected final def verifySubscriptionDetailsSaveWithField[T](count: Int, field: String): Unit =
    verify(mockConnector, times(count)).saveSubscriptionDetails[T](
      ArgumentMatchers.any(),
      ArgumentMatchers.eq(field),
      ArgumentMatchers.any()
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  protected final def setupMockSubscriptionDetailsSaveFunctions(): Unit = {
    mockFetchFromSubscriptionDetails[String]("fakeKey", None)

    when(mockConnector.saveSubscriptionDetails(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Right(PostSubscriptionDetailsSuccessResponse)))
  }

  protected final def mockFetchIncomeSourceFromSubscriptionDetails(fetchIncomeSource: Option[IncomeSourceModel]): Unit = {
    mockFetchFromSubscriptionDetails[IncomeSourceModel](SubscriptionDataKeys.IncomeSource, fetchIncomeSource)
  }

  protected final def mockFetchIndividualIncomeSourceFromSubscriptionDetails(fetchIndividualIncomeSource: Option[IncomeSourceModel]): Unit = {
    mockFetchFromSubscriptionDetails[IncomeSourceModel](SubscriptionDataKeys.IncomeSource, fetchIndividualIncomeSource)
  }

  protected final def mockFetchBusinessNameFromSubscriptionDetails(fetchBusinessName: Option[BusinessNameModel]): Unit = {
    mockFetchFromSubscriptionDetails[BusinessNameModel](SubscriptionDataKeys.BusinessName, fetchBusinessName)
  }

  protected final def mockFetchAccountingMethodFromSubscriptionDetails(fetchAccountingMethod: Option[AccountingMethodModel]): Unit = {
    mockFetchFromSubscriptionDetails[AccountingMethodModel](SubscriptionDataKeys.AccountingMethod, fetchAccountingMethod)
  }

  protected final def mockFetchSubscriptionIdFromSubscriptionDetails(fetchSubscriptionId: Option[String]): Unit = {
    mockFetchFromSubscriptionDetails[String](SubscriptionDataKeys.MtditId, fetchSubscriptionId)
  }

  protected final def mockFetchSelectedTaxYearFromSubscriptionDetails(fetchSelectedTaxYear: Option[AccountingYearModel]): Unit = {
    mockFetchFromSubscriptionDetails[AccountingYearModel](SubscriptionDataKeys.SelectedTaxYear, fetchSelectedTaxYear)
  }

  protected final def mockFetchPaperlessPreferenceToken(fetchPaperlessPreferenceToken: Option[String]): Unit = {
    mockFetchFromSubscriptionDetails[String](SubscriptionDataKeys.PaperlessPreferenceToken, fetchPaperlessPreferenceToken)
  }

  protected final def mockFetchLastUpdatedTimestamp(fetchLastUpdatedTimestamp: Option[TimestampModel]): Unit = {
    when(mockConnector.getSubscriptionDetails[TimestampModel](ArgumentMatchers.any(), ArgumentMatchers.eq(SubscriptionDataKeys.lastUpdatedTimestamp))(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(fetchLastUpdatedTimestamp))
  }

  protected final def mockFetchAllFromSubscriptionDetails(fetchAll: Option[CacheMap]): Unit = {
    when(mockConnector.getSubscriptionDetails[CacheMap](ArgumentMatchers.any(), ArgumentMatchers.eq(SubscriptionDataKeys.subscriptionId))(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(fetchAll))
  }

  protected final def mockFetchProperty(property: Option[PropertyModel]): Unit = {
    when(mockConnector.getSubscriptionDetails[PropertyModel](ArgumentMatchers.any(), ArgumentMatchers.eq(SubscriptionDataKeys.Property))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(property))
  }

  protected final def verifyPropertySave(property: Option[PropertyModel]): Unit = {
    property match {
      case Some(value) => verify(mockConnector, times(1))
        .saveSubscriptionDetails[PropertyModel](ArgumentMatchers.any(), ArgumentMatchers.eq(SubscriptionDataKeys.Property), ArgumentMatchers.eq(value))(any(), any())
      case None => verify(mockConnector, times(0))
        .saveSubscriptionDetails[PropertyModel](ArgumentMatchers.any(), ArgumentMatchers.eq(SubscriptionDataKeys.Property), any())(any(), any())
    }
  }

  protected final def mockDeleteAllFromSubscriptionDetails(deleteAll: HttpResponse): Unit = {
    when(mockConnector.deleteAll(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(deleteAll)
  }

  protected final def verifySubscriptionDetailsFetchAll(fetchAll: Option[Int]): Unit = {
    fetchAll map (count => verify(mockConnector, times(count)).getSubscriptionDetails(ArgumentMatchers.any(), ArgumentMatchers.any())
    (ArgumentMatchers.any(), ArgumentMatchers.any()))
  }

  protected final def mockFetchOverseasProperty(overseasProperty: Option[OverseasPropertyModel]): Unit = {
    when(mockConnector.getSubscriptionDetails[OverseasPropertyModel](ArgumentMatchers.any(), ArgumentMatchers.eq(SubscriptionDataKeys.OverseasProperty))
      (ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(overseasProperty))
  }

  protected final def verifyOverseasPropertySave(property: Option[OverseasPropertyModel]): Unit = {
    property match {
      case Some(value) => verify(
        mockConnector,
        times(1)
      ).saveSubscriptionDetails[OverseasPropertyModel](
        ArgumentMatchers.any(),
        ArgumentMatchers.eq(SubscriptionDataKeys.OverseasProperty),
        ArgumentMatchers.eq(value)
      )(any(), any())
      case None => verify(
        mockConnector,
        times(0)
      ).saveSubscriptionDetails[OverseasPropertyModel](any(), ArgumentMatchers.eq(SubscriptionDataKeys.Property), any())(any(), any())
    }
  }

  protected final def mockFetchAllSelfEmployments(selfEmployments: Option[Seq[SelfEmploymentData]] = None): Unit = {
    when(mockConnector.getSubscriptionDetails[Seq[SelfEmploymentData]](any(), ArgumentMatchers.eq(SubscriptionDataKeys.BusinessesKey))(any(), any()))
      .thenReturn(Future.successful(selfEmployments))
  }

  protected final def verifySubscriptionDetailsDeleteAll(deleteAll: Option[Int]): Unit = {
    deleteAll map (count => verify(mockConnector, times(count)).deleteAll(ArgumentMatchers.any())(ArgumentMatchers.any()))
  }

}
