/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.business

import javax.inject.{Inject, Singleton}

import auth.RegistrationController
import config.BaseControllerConfig
import connectors.RawResponseReads
import connectors.models.address.AddressLookupRequest
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import services.{AddressLookupService, AuthService}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class BusinessAddressController @Inject()(val baseConfig: BaseControllerConfig,
                                          val messagesApi: MessagesApi,
                                          val authService: AuthService,
                                          addressLookupService: AddressLookupService
                                         ) extends RegistrationController with RawResponseReads {

  def continueUrl(implicit request: Request[AnyContent]) =
    controllers.business.routes.BusinessAddressController.callBack("").absoluteURL().replace("""\?*.+$""", "")

  def init(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      addressLookupService.init(AddressLookupRequest(continueUrl)).map {
        case Right(url) => Redirect(url)
        case Left(err) => throw new InternalServerException("BusinessAddressController.init failed unexpectedly, status=" + err.status)
      }
  }

  def callBack(id: String): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Ok(id))
  }

}
