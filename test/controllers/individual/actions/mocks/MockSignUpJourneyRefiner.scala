/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.individual.actions.mocks

import controllers.individual.actions.SignUpJourneyRefiner
import controllers.utils.ReferenceRetrieval
import models.requests.individual.{IdentifierRequest, SignUpRequest}
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MockSignUpJourneyRefiner extends MockitoSugar with MockIdentifierAction {
  suite: Suite =>

  val reference: String = "test-reference"

  val fakeSignUpJourneyRefiner: SignUpJourneyRefiner = new SignUpJourneyRefiner(mock[ReferenceRetrieval]) {
    override def refine[A](request: IdentifierRequest[A]): Future[Either[Result, SignUpRequest[A]]] = {
      Future.successful(Right(SignUpRequest(request, reference, nino)))
    }
  }

}
