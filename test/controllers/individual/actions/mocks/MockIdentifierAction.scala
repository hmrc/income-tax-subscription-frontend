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

package controllers.individual.actions.mocks

import config.MockConfig
import controllers.individual.actions.IdentifierAction
import models.requests.individual.IdentifierRequest
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{BodyParsers, Request, Result}
import play.api.{Configuration, Environment}
import services.SessionDataService
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MockIdentifierAction extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mtditid: String = "XAIT0000000001"
  val nino: String = "AA000000A"
  val utr: String = "1234567890"

  val fakeIdentifierAction: IdentifierAction = new IdentifierAction(
    mock[AuthConnector], mock[BodyParsers.Default], mock[Configuration], mock[Environment]
  )(MockConfig, mock[SessionDataService]) {
    override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
      block(IdentifierRequest(
        request = request,
        mtditid = Some(mtditid),
        nino = nino,
        utr = Some(utr)
      ))
    }
  }

}
