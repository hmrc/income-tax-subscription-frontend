/*
 * Copyright 2021 HM Revenue & Customs
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

package testonly.controllers.individual

import java.util.UUID

import auth.individual.StatelessController
import config.AppConfig
import connectors.PaperlessPreferenceTokenConnector
import javax.inject.Inject
import models.PaperlessPreferenceTokenResult.PaperlessPreferenceTokenSuccess
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService}
import testonly.models.preferences.{AddTokenRequest, AddTokenResponse}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}


class PreferenceTokenController @Inject()(val auditingService: AuditingService,
                                          val authService: AuthService,
                                          val appConfig: AppConfig,
                                          paperlessPreferenceTokenConnector: PaperlessPreferenceTokenConnector)
                                         (implicit val ec: ExecutionContext,
                                          mcc: MessagesControllerComponents) extends StatelessController {

  private def storeNino(nino: String)(implicit hc: HeaderCarrier): Future[String] = {
    val token = s"${UUID.randomUUID()}"
    paperlessPreferenceTokenConnector.storeNino(token, nino) flatMap {
      case Right(PaperlessPreferenceTokenSuccess) => Future.successful(token)
      case _ =>
        Future.failed(new InternalServerException("Failed to store paperless preferences token"))
    }
  }

  // n.b. this route must be marked with NOCSRF in the route core.config file
  def addToken(): Action[AnyContent] = Authenticated.asyncUnrestricted { implicit request =>
    implicit user =>
      request.body.asJson flatMap (_.asOpt[AddTokenRequest]) match {
        case Some(AddTokenRequest(nino)) =>
          storeNino(nino).map(token => Created(Json.prettyPrint(Json.toJson(AddTokenResponse(nino, token)))))
        case _ => Future.successful(BadRequest("Invalid json"))
      }
  }

}
