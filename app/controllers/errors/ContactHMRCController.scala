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

package controllers.errors

import config.AppConfig
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuthService
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig
import uk.gov.hmrc.http.InternalServerException
import views.html.errors.ContactHMRC

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ContactHMRCController @Inject()(view: ContactHMRC,
                                      authService: AuthService,
                                      appConfig: AppConfig,
                                      cfc: ContactFrontendConfig)
                                     (implicit mcc: MessagesControllerComponents, ec: ExecutionContext)
  extends ErrorBaseController(authService, appConfig) {

  def show: Action[AnyContent] = Action.async { implicit request =>
    authenticate(request) { isAgent =>
      Ok(view(
        postAction = controllers.errors.routes.ContactHMRCController.submit,
        isAgent = isAgent
      ))
    }
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    authenticate(request) { isAgent =>
      (cfc.baseUrl, cfc.referrerUrl, cfc.serviceId) match {
        case (Some(baseUrl), Some(referrerUrl), Some(serviceId)) =>
          Redirect(
            url = baseUrl,
            queryStringParams = Map(
              "service" -> Seq(serviceId),
              "referrerUrl" -> Seq(referrerUrl)
            )
          )
        case _ =>
          throw new InternalServerException("Contact frontend npt defined")
      }
    }
  }
}
