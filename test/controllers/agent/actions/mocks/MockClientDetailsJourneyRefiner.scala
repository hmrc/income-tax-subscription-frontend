/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.agent.actions.mocks

import controllers.agent.actions.ClientDetailsJourneyRefiner
import models.requests.agent.IdentifierRequest
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MockClientDetailsJourneyRefiner extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val fakeClientDetailsJourneyRefiner: ClientDetailsJourneyRefiner = new ClientDetailsJourneyRefiner() {
    override def refine[A](request: IdentifierRequest[A]): Future[Either[Result, IdentifierRequest[A]]] = {
      Future.successful(Right(request))
    }
  }

}
