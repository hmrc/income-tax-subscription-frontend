/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.agent.matching

import config.AppConfig
import connectors.agent.IncomeTaxSessionDataConnector
import controllers.SignUpBaseController
import controllers.agent.actions.IdentifierAction
import play.api.mvc.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClientVAndCHomeController @Inject()(identify: IdentifierAction,
                                          incomeTaxSessionDataConnector: IncomeTaxSessionDataConnector,
                                          appConfig: AppConfig)
                                         (implicit val ec: ExecutionContext, mcc: MessagesControllerComponents) extends SignUpBaseController {

  def handOffVAndC: Action[AnyContent] = identify.async { implicit request =>
    (request.sessionData.fetchMtditid, request.sessionData.fetchNino, request.sessionData.fetchUTR) match {
      case (Some(mtditid), Some(nino), Some(utr)) =>
        incomeTaxSessionDataConnector.setupViewAndChangeSessionData(mtditid, nino, utr).map { _ =>
          Redirect(appConfig.getVAndCUrl)
        }
      case _ => Future.successful(Redirect(appConfig.getVAndCUrl))
    }
  }

}
