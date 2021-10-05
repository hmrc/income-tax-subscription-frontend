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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Configuration, Environment}
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import views.html.individual.incometax.business.ProgressSaved

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProgressSavedController @Inject()(
                                         val progressSavedView: ProgressSaved,
                                         val auditingService: AuditingService,
                                         val authService: AuthService)
                                       (implicit val ec: ExecutionContext,
                                         val appConfig: AppConfig,
                                         val config: Configuration,
                                         val env: Environment,
                                         mcc: MessagesControllerComponents) extends SignUpController with AuthRedirects {

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (isEnabled(SaveAndRetrieve)) {
        Future.successful(Ok(progressSavedView("Monday, 20 October 2021", signInUrl)))
      } else {
        Future.failed(new NotFoundException("[ProgressSavedController][submit] - The save and retrieve feature switch is disabled"))
      }
  }

  val signInUrl: String = ggLoginUrl
}
