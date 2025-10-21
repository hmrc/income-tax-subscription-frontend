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

import connectors.httpparser.DeleteSessionDataHttpParser.DeleteSessionDataResponse
import connectors.httpparser.GetSessionDataHttpParser.GetSessionDataResponse
import connectors.httpparser.SaveSessionDataHttpParser
import connectors.httpparser.SaveSessionDataHttpParser.SaveSessionDataResponse
import models.SessionData.Data
import models.status.MandationStatusModel
import models.{EligibilityStatus, YesNo}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import services.{SessionDataService, Throttle}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

trait MockSessionDataService extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  implicit class Response[T](response: GetSessionDataResponse[T]) {
    def toOption(): Option[T] = {
      response match {
        case Right(value) => value
        case Left(_) => None
      }
    }
  }

  val mockSessionDataService: SessionDataService = mock[SessionDataService]

  override def beforeEach(): Unit = {
    reset(mockSessionDataService)
    mockSessionData()
    super.beforeEach()
  }

  mockSessionData()

  def mockFetchReference(reference: Option[String]): Unit = {
    when(mockSessionDataService.fetchReference(ArgumentMatchers.any()))
      .thenReturn(reference)
  }

  def mockSaveReferenceSuccess(reference: String): Unit = {
    when(mockSessionDataService.saveReference(ArgumentMatchers.eq(reference))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(SaveSessionDataHttpParser.SaveSessionDataSuccessResponse)))
  }

  def mockSaveReferenceStatusFailure(reference: String)(status: Int): Unit = {
    when(mockSessionDataService.saveReference(ArgumentMatchers.eq(reference))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Left(SaveSessionDataHttpParser.UnexpectedStatusFailure(status))))
  }

  def mockFetchThrottlePassed(throttle: Throttle)(result: GetSessionDataResponse[Boolean]): Unit = {
    when(mockSessionDataService.fetchThrottlePassed(ArgumentMatchers.any(), ArgumentMatchers.eq(throttle)))
      .thenReturn(result.toOption())
  }

  def mockSaveThrottlePassed(throttle: Throttle)(result: SaveSessionDataResponse): Unit = {
    when(mockSessionDataService.saveThrottlePassed(ArgumentMatchers.eq(throttle))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchMandationStatus(result: GetSessionDataResponse[MandationStatusModel]): Unit = {
    when(mockSessionDataService.fetchMandationStatus(ArgumentMatchers.any()))
      .thenReturn(result.toOption())
  }

  def mockSaveMandationStatus(mandationStatus: MandationStatusModel)(result: SaveSessionDataResponse): Unit = {
    when(mockSessionDataService.saveMandationStatus(ArgumentMatchers.eq(mandationStatus))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockDeleteSessionAll(result: DeleteSessionDataResponse): Unit = {
    when(mockSessionDataService.deleteSessionAll(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchEligibilityStatus(result: GetSessionDataResponse[EligibilityStatus]): Unit = {
    when(mockSessionDataService.fetchEligibilityStatus(ArgumentMatchers.any()))
      .thenReturn(result.toOption())
  }

  def mockSaveEligibilityStatus(eligibilityStatus: EligibilityStatus)(result: SaveSessionDataResponse): Unit = {
    when(mockSessionDataService.saveEligibilityStatus(ArgumentMatchers.eq(eligibilityStatus))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchNino(result: GetSessionDataResponse[String]): Unit = {
    when(mockSessionDataService.fetchNino(ArgumentMatchers.any()))
      .thenReturn(result.toOption())
  }

  def mockSaveNino(nino: String)(result: SaveSessionDataResponse): Unit = {
    when(mockSessionDataService.saveNino(ArgumentMatchers.eq(nino))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchUTR(result: GetSessionDataResponse[String]): Unit = {
    when(mockSessionDataService.fetchUTR(ArgumentMatchers.any()))
      .thenReturn(result.toOption())
  }

  def mockSaveUTR(utr: String)(result: SaveSessionDataResponse): Unit = {
    when(mockSessionDataService.saveUTR(ArgumentMatchers.eq(utr))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchSoftwareStatus(result: GetSessionDataResponse[YesNo]): Unit = {
    when(mockSessionDataService.fetchSoftwareStatus(ArgumentMatchers.any()))
      .thenReturn(result.toOption())
  }

  def mockSaveSoftwareStatus(softwareStatus: YesNo)(result: SaveSessionDataResponse): Unit = {
    when(mockSessionDataService.saveSoftwareStatus(ArgumentMatchers.eq(softwareStatus))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchConsentStatus(result: GetSessionDataResponse[YesNo]): Unit = {
    when(mockSessionDataService.fetchConsentStatus(ArgumentMatchers.any()))
      .thenReturn(result.toOption())
  }

  def mockSaveConsentStatus(consentStatus: YesNo)(result: SaveSessionDataResponse): Unit = {
    when(mockSessionDataService.saveConsentStatus(ArgumentMatchers.eq(consentStatus))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockFetchEmailPassed(result: GetSessionDataResponse[Boolean]): Unit = {
    when(mockSessionDataService.fetchEmailPassed(ArgumentMatchers.any()))
      .thenReturn(result.toOption())
  }

  def mockSaveEmailPassed(emailPassed: Boolean)(result: SaveSessionDataResponse): Unit = {
    when(mockSessionDataService.saveEmailPassed(ArgumentMatchers.eq(emailPassed))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def mockSessionData(sessionData: Data = Map()): Unit = {
    when(mockSessionDataService.getAllSessionData()(any(), any())).thenReturn(
      Future.successful(sessionData)
    )
  }
}
