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

package services

import common.Constants.ITSASessionKeys

import connectors.httpparser.SaveSessionDataHttpParser
import connectors.httpparser.SaveSessionDataHttpParser.UnexpectedStatusFailure
import models.SessionData
import org.scalatestplus.play.PlaySpec
import org.scalatest.matchers.must.Matchers
import org.mockito.ArgumentMatchers
import controllers.individual.actions.mocks.MockConfirmationJourneyRefiner
import org.scalatest.concurrent.ScalaFutures
import org.mockito.Mockito.when
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SignedUpDateServiceSpec extends PlaySpec with Matchers with MockConfirmationJourneyRefiner with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockSessionDataService: SessionDataService = mock[SessionDataService]

  val service = new SignedUpDateService(mockSessionDataService)

  "getSignedUpDate" when {
    "date already exists in session" should {
      "return the existing date" in {
        val existingDate = LocalDate.now
        val sessionData = SessionData(Map(
          ITSASessionKeys.SIGNED_UP_DATE -> Json.toJson(existingDate)
        ))

        val result = service.getSignedUpDate(sessionData)

        result.futureValue mustBe existingDate
      }
    }

    "date does not exist in session" should {
      "save current date and return it" in {
        val sessionData = SessionData(Map.empty)
        when(mockSessionDataService.saveSignedUpDate(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Right(())))

        val result = service.getSignedUpDate(sessionData)

        result.futureValue mustBe LocalDate.now()
      }

      "throw exception when save fails" in {
        val sessionData = SessionData(Map.empty)
        val error = UnexpectedStatusFailure(500)
        when(mockSessionDataService.saveSignedUpDate(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Left(error)))

        val result = service.getSignedUpDate(sessionData)

        val exception = result.failed.futureValue

        exception mustBe a[InternalServerException]
        exception.getMessage must include("Failure when saving signed up date to session")
      }
    }
  }
}
