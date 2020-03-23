/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers

import java.net.URLEncoder

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, Call, Request}
import services.AuthService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignOutController @Inject()(appConfig: AppConfig,
                                  authService: AuthService)
                                 (implicit ec: ExecutionContext) extends FrontendController {

  def signOut(origin: String): Action[AnyContent] = Action.async { implicit request =>
    authService.authorised().retrieve(affinityGroup) {
      case Some(Agent) =>
        Future.successful(Redirect(appConfig.ggSignOutUrl(
          appConfig.baseUrl + controllers.agent.routes.ExitSurveyController.show(origin = origin).url
        )))
      case Some(_) =>
        Future.successful(Redirect(appConfig.ggSignOutUrl(
          appConfig.baseUrl + controllers.individual.subscription.routes.ExitSurveyController.show(origin = origin).url
        )))
      case None =>
        Future.failed(new InternalServerException("unexpected state"))
    }
  }

}


object SignOutController {

  def signOut(origin: Call)(implicit request: Request[AnyContent]): Call = signOut(origin = origin.url)

  def signOut(origin: String): Call =
    routes.SignOutController.signOut(origin = URLEncoder.encode(origin, "UTF-8"))
}
