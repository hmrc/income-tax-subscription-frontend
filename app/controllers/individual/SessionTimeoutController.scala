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

package controllers.individual

import play.api.mvc._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.individual.Timeout

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SessionTimeoutController @Inject()(val timeoutView: Timeout, mcc: MessagesControllerComponents, val config: Configuration, val env: Environment)
  extends FrontendController(mcc) with AuthRedirects {

  val show: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(timeoutView()))
  }

  val keepAlive: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok.withSession(request.session))
  }

  val timeout: Action[AnyContent] = Action.async {
    Future.successful(toGGLogin(controllers.individual.matching.routes.HomeController.index.url).withNewSession)
  }

}
