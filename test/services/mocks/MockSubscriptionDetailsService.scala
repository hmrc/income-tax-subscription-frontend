/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.httpparser.DeleteSubscriptionDetailsHttpParser.DeleteSubscriptionDetailsSuccessResponse
import connectors.httpparser.PostSubscriptionDetailsHttpParser.{PostSubscriptionDetailsResponse, PostSubscriptionDetailsSuccessResponse, UnexpectedStatusFailure}
import connectors.httpparser.RetrieveReferenceHttpParser
import connectors.httpparser.RetrieveReferenceHttpParser.{Created, Existence, RetrieveReferenceResponse}
import models.AccountingMethod
import models.common._
import models.common.business.{BusinessNameModel, SelfEmploymentData}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito._
import org.mockito.{ArgumentMatcher, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.SubscriptionDetailsService
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.http.HttpResponse
import utilities.SubscriptionDataKeys._
import utilities.{SubscriptionDataKeys, UnitTestTrait}

import scala.concurrent.Future

//scalastyle:off

trait MockSubscriptionDetailsService extends UnitTestTrait with MockitoSugar with BeforeAndAfterEach with MockMandationStatusService with MockGetEligibilityStatusService {

  val mockConnector: IncomeTaxSubscriptionConnector = mock[IncomeTaxSubscriptionConnector]
  val crypto: ApplicationCrypto = app.injector.instanceOf[ApplicationCrypto]

  object MockSubscriptionDetailsService extends SubscriptionDetailsService(mockConnector, mockMandationStatusService, mockGetEligibilityStatusService, crypto)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  protected final def mockFetchPrePopFlag(reference: String, flag: Option[Boolean]): Unit = {
    when(mockConnector.getSubscriptionDetails[Boolean](ArgumentMatchers.eq(reference), ArgumentMatchers.eq(SubscriptionDataKeys.PrePopFlag))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(flag))
  }

  def mockSaveOverseasProperty(reference: String): Unit =
    setupMockSubscriptionDetailsSaveFunctions(reference, SubscriptionDataKeys.OverseasProperty)

  def mockSaveUkProperty(reference: String): Unit =
    setupMockSubscriptionDetailsSaveFunctions(reference, SubscriptionDataKeys.Property)

  def mockSaveBusinesses(reference: String): Unit =
    setupMockSubscriptionDetailsSaveFunctions(reference, SubscriptionDataKeys.SoleTraderBusinessesKey)

  def mockSaveIncomeSourceConfirmation(reference: String): Unit =
    setupMockSubscriptionDetailsSaveFunctions(reference, SubscriptionDataKeys.IncomeSourceConfirmation)

  def verifySaveIncomeSourceConfirmation(reference: String, count: Int): Unit =
    verifySubscriptionDetailsSaveWithField(count = count, field = SubscriptionDataKeys.IncomeSourceConfirmation, reference = reference, wanted = true)

  def mockSavePrePopFlag(reference: String): Unit =
    setupMockSubscriptionDetailsSaveFunctions(reference, SubscriptionDataKeys.PrePopFlag)

  def verifySaveOverseasProperty(count: Int, reference: String, overseasPropertyModel: OverseasPropertyModel): Unit =
    verifySubscriptionDetailsSaveWithField[OverseasPropertyModel](reference, count, SubscriptionDataKeys.OverseasProperty, overseasPropertyModel)

  def verifySaveUkProperty(count: Int, reference: String, propertyModel: PropertyModel): Unit =
    verifySubscriptionDetailsSaveWithField[PropertyModel](reference, count, Property, propertyModel)

  // Businesses get a uuid added - need to create a bespoke matcher which ignores id.
  // Businesses passed in need an empty string for id.
  class BusinessSequenceMatcher(val expectedSeq: Seq[SoleTraderBusiness]) extends ArgumentMatcher[Seq[SoleTraderBusiness]] {
    override def matches(providedSeq: Seq[SoleTraderBusiness]): Boolean = {
      providedSeq.forall(provided => expectedSeq.contains(provided.copy(id = "")))
    }
  }

  def verifySaveBusinesses(count: Int, reference: String): Future[PostSubscriptionDetailsResponse] =
    verify(mockConnector, times(count)).saveSubscriptionDetails[SoleTraderBusinesses](
      ArgumentMatchers.eq(reference),
      ArgumentMatchers.eq(SoleTraderBusinessesKey),
      ArgumentMatchers.any()
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  def verifyFetchBusinessName(count: Int, reference: String): Future[Option[BusinessNameModel]] =
    verify(mockConnector, times(count)).getSubscriptionDetails[BusinessNameModel](
      ArgumentMatchers.eq(reference),
      ArgumentMatchers.eq(BusinessName)
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  def verifyFetchSelectedTaxYear(count: Int, reference: String): Future[Option[AccountingYearModel]] =
    verify(mockConnector, times(count)).getSubscriptionDetails[AccountingYearModel](
      ArgumentMatchers.eq(reference),
      ArgumentMatchers.eq(SelectedTaxYear)
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  def verifySaveSelectedTaxYear(count: Int, reference: String): Future[PostSubscriptionDetailsResponse] =
    verify(mockConnector, times(count)).saveSubscriptionDetails[AccountingYearModel](
      ArgumentMatchers.eq(reference),
      ArgumentMatchers.eq(SelectedTaxYear),
      ArgumentMatchers.any()
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  def verifySavePrePopFlag(count: Int, reference: String, value: Boolean): Future[PostSubscriptionDetailsResponse] =
    verify(mockConnector, times(count)).saveSubscriptionDetails[Boolean](
      ArgumentMatchers.eq(reference),
      ArgumentMatchers.eq(PrePopFlag),
      ArgumentMatchers.eq(value)
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  def verifyFetchPrePopFlag(reference: String): Future[Option[Nothing]] =
    verify(mockConnector, atLeastOnce())
      .getSubscriptionDetails(ArgumentMatchers.eq(reference), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

  def mockRetrieveReferenceSuccess(utr: String, existence: String => Existence = RetrieveReferenceHttpParser.Created)(reference: String): Unit = {
    when(mockConnector.retrieveReference(
      ArgumentMatchers.eq(utr)
    )(ArgumentMatchers.any())) thenReturn Future.successful(Right(existence(reference)))
  }

  def mockRetrieveReferenceSuccessFromSubscriptionDetails(utr: String)(reference: String): Unit = {
    when(mockConnector.retrieveReference(
      ArgumentMatchers.eq(utr)
    )(ArgumentMatchers.any())) thenReturn Future.successful(Right(Created(reference)))
  }

  def mockRetrieveReference(utr: String)(response: RetrieveReferenceResponse): Unit = {
    when(mockConnector.retrieveReference(ArgumentMatchers.eq(utr))(ArgumentMatchers.any())) thenReturn Future.successful(response)
  }

  protected final def verifySubscriptionDetailsFetchWithField[T](reference: String, count: Int, field: String): Unit =
    verify(mockConnector, times(count))
      .getSubscriptionDetails[T](ArgumentMatchers.eq(reference), ArgumentMatchers.eq(field))(
        ArgumentMatchers.any(), ArgumentMatchers.any())

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
    verify(mockConnector, times(count)).saveSubscriptionDetails[T](
      ArgumentMatchers.any(),
      ArgumentMatchers.eq(key),
      ArgumentMatchers.any(),
    )(ArgumentMatchers.any(), ArgumentMatchers.any())


  protected final def verifySubscriptionDetailsSaveWithField[T](count: Int, field: String, wanted: T): Unit =
    verify(mockConnector, times(count)).saveSubscriptionDetails[T](
      ArgumentMatchers.any(),
      ArgumentMatchers.eq(field),
      ArgumentMatchers.eq(wanted),
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  protected final def verifySubscriptionDetailsSaveWithField[T](reference: String, count: Int, field: String, wanted: T): Unit =
    verify(mockConnector, times(count)).saveSubscriptionDetails[T](
      ArgumentMatchers.eq(reference),
      ArgumentMatchers.eq(field),
      ArgumentMatchers.eq(wanted),
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  protected final def verifySubscriptionDetailsSaveWithField[T](count: Int, field: String, matcher: ArgumentMatcher[T]): Unit =
    verify(mockConnector, times(count)).saveSubscriptionDetails[T](
      ArgumentMatchers.any(),
      ArgumentMatchers.eq(field),
      argThat(matcher),
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  protected final def verifySubscriptionDetailsSaveWithField[T](reference: String, count: Int, field: String): Unit =
    verify(mockConnector, times(count)).saveSubscriptionDetails[T](
      ArgumentMatchers.eq(reference),
      ArgumentMatchers.eq(field),
      ArgumentMatchers.any()
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  protected final def verifySubscriptionDetailsSaveWithField[T](count: Int, field: String): Unit =
    verify(mockConnector, times(count)).saveSubscriptionDetails[T](
      ArgumentMatchers.any(),
      ArgumentMatchers.eq(field),
      ArgumentMatchers.any()
    )(ArgumentMatchers.any(), ArgumentMatchers.any())

  protected final def setupMockSubscriptionDetailsSaveFunctions(): Unit = {

    when(mockConnector.saveSubscriptionDetails(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Right(PostSubscriptionDetailsSuccessResponse)))
  }

  protected final def setupMockSubscriptionDetailsSaveFunctionsFailure(): Unit = {
    when(mockConnector.saveSubscriptionDetails(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Left(UnexpectedStatusFailure(500))))
  }

  protected final def setupMockSubscriptionDetailsSaveFunctions(reference: String, id: String): Unit =
    when(mockConnector.saveSubscriptionDetails(ArgumentMatchers.eq(reference), ArgumentMatchers.eq(id), ArgumentMatchers.any())(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Right(PostSubscriptionDetailsSuccessResponse)))

  protected final def mockFetchBusinessName(fetchBusinessName: Option[BusinessNameModel]): Unit = {
    when(mockConnector.getSubscriptionDetails[BusinessNameModel](ArgumentMatchers.any(), ArgumentMatchers.eq(SubscriptionDataKeys.BusinessName))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(fetchBusinessName))
  }

  protected final def mockFetchSelectedTaxYear(fetchSelectedTaxYear: Option[AccountingYearModel]): Unit = {
    when(mockConnector.getSubscriptionDetails[AccountingYearModel](ArgumentMatchers.any(), ArgumentMatchers.eq(SubscriptionDataKeys.SelectedTaxYear))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(fetchSelectedTaxYear))
  }

  protected final def mockFetchLastUpdatedTimestamp(fetchLastUpdatedTimestamp: Option[TimestampModel]): Unit = {
    when(mockConnector.getSubscriptionDetails[TimestampModel](ArgumentMatchers.any(), ArgumentMatchers.eq(SubscriptionDataKeys.lastUpdatedTimestamp))(
      ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(fetchLastUpdatedTimestamp))
  }

  protected final def mockFetchProperty(property: Option[PropertyModel]): Unit = {
    when(mockConnector.getSubscriptionDetails[PropertyModel](ArgumentMatchers.any(), ArgumentMatchers.eq(SubscriptionDataKeys.Property))
      (ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(property))
  }

  protected final def verifyPropertySave(maybePropertyModel: Option[PropertyModel], maybeReference: Option[String] = None): Unit =
    verify(mockConnector, times(maybePropertyModel.size))
      .saveSubscriptionDetails[PropertyModel](
        maybeReference match { case None => ArgumentMatchers.any(); case Some(reference) => ArgumentMatchers.eq(reference) },
        ArgumentMatchers.eq(SubscriptionDataKeys.Property),
        maybePropertyModel match { case None => ArgumentMatchers.any(); case Some(propertyModel) => ArgumentMatchers.eq(propertyModel) }
      )(any(), any())

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

  protected final def verifyOverseasPropertySave(maybeOverseasPropertyModel: Option[OverseasPropertyModel], maybeReference: Option[String] = None): Unit =
    verify(
      mockConnector,
      times(maybeOverseasPropertyModel.size)
    ).saveSubscriptionDetails[OverseasPropertyModel](
      maybeReference match { case None => ArgumentMatchers.any(); case Some(reference) => ArgumentMatchers.eq(reference) },
      ArgumentMatchers.eq(SubscriptionDataKeys.OverseasProperty),
      maybeOverseasPropertyModel match { case None => ArgumentMatchers.any(); case Some(overseasPropertyModel) => ArgumentMatchers.eq(overseasPropertyModel) }
    )(any(), any())

  protected final def mockFetchAllSelfEmployments(selfEmployments: Seq[SelfEmploymentData] = Seq.empty, accountingMethod: Option[AccountingMethod] = None): Unit = {
    when(mockConnector.getSubscriptionDetails[SoleTraderBusinesses](any(), ArgumentMatchers.eq(SubscriptionDataKeys.SoleTraderBusinessesKey))(any(), any()))
      .thenReturn(Future.successful(Some(SoleTraderBusinesses(selfEmployments.map(_.toSoleTraderBusiness), accountingMethod = accountingMethod))))
  }

  protected final def verifySubscriptionDetailsDeleteAll(deleteAll: Option[Int]): Unit = {
    deleteAll map (count => verify(mockConnector, times(count)).deleteAll(ArgumentMatchers.any())(ArgumentMatchers.any()))
  }

  protected final def mockFetchIncomeSourceConfirmation(flag: Option[Boolean]): Unit = {
    when(mockConnector.getSubscriptionDetails[Boolean](any(), ArgumentMatchers.eq(SubscriptionDataKeys.IncomeSourceConfirmation))(any(), any()))
      .thenReturn(Future.successful(flag))
  }

  protected final def mockSaveIncomeSourceConfirmation(flag: Option[Boolean]): Unit = {
    when(mockConnector.getSubscriptionDetails[Boolean](any(), ArgumentMatchers.eq(SubscriptionDataKeys.IncomeSourceConfirmation))(any(), any()))
      .thenReturn(Future.successful(flag))
  }

  protected final def mockDeleteIncomeSourceConfirmationSuccess(): Unit = {
    when(mockConnector.deleteSubscriptionDetails(any(), ArgumentMatchers.eq(SubscriptionDataKeys.IncomeSourceConfirmation))(any()))
      .thenReturn(Future.successful(Right(DeleteSubscriptionDetailsSuccessResponse)))
  }

  protected final def mockFetchEligibilityInterruptPassed(flag: Option[Boolean]): Unit = {
    when(mockConnector.getSubscriptionDetails[Boolean](any(), ArgumentMatchers.eq(SubscriptionDataKeys.EligibilityInterruptPassed))(any(), any()))
      .thenReturn(Future.successful(flag))
  }

}
