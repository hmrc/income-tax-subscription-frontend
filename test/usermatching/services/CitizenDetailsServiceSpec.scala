/*
 * Copyright 2017 HM Revenue & Customs
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

package usermatching.services

import incometax.subscription.models.{CitizenDetailsFailureResponse, CitizenDetailsSuccess}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.BAD_REQUEST
import usermatching.httpparsers.CitizenDetailsResponseHttpParser.GetCitizenDetailsResponse
import usermatching.services.mocks.TestCitizenDetailsService
import core.utils.TestConstants._
import core.utils.UnitTestTrait

import scala.concurrent.Future

class CitizenDetailsServiceSpec extends UnitTestTrait with TestCitizenDetailsService with ScalaFutures {

  "lookupUtr" should {
    def call: Future[GetCitizenDetailsResponse] = TestCitizenDetailsService.lookupUtr(testNino)

    "return a success from the CitizenDetailsConnector" in {
      mockLookupUserWithUtr(testNino)(testUtr)

      val result = call

      whenReady(result)(_ mustBe Right(Some(CitizenDetailsSuccess(Some(testUtr)))))
    }

    "return a failure from the CitizenDetailsConnector" in {
      mockLookupFailure(testNino)

      val result = call

      whenReady(result)(_ mustBe Left(CitizenDetailsFailureResponse(BAD_REQUEST)))
    }
  }

}
