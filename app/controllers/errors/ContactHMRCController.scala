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
import play.api.mvc.*
import services.AuthService
import uk.gov.hmrc.hmrcfrontend.config.ContactFrontendConfig
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
    authenticate(request) { _ =>
      redirectToContactFrontend
    }
  }

  private def redirectToContactFrontend(implicit request: Request[_]): Result = {
    val baseUrl: String = cfc.baseUrl.getOrElse("")
    Redirect(
      url = s"$baseUrl/contact/report-technical-problem",
      queryStringParams = Map.empty[String, Seq[String]] ++
        cfc.serviceId.map("service" -> Seq.apply(_)) ++
        cfc.referrerUrl.map("referrerUrl" -> Seq.apply(_))
    )
  }
}
