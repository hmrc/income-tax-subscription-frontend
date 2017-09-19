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
import connectors.models.address._
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request}
import services.{AddressLookupService, AuthService, KeystoreService}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class BusinessAddressController @Inject()(val baseConfig: BaseControllerConfig,
                                          val messagesApi: MessagesApi,
                                          val authService: AuthService,
                                          addressLookupService: AddressLookupService,
                                          keystoreService: KeystoreService
                                         ) extends RegistrationController with RawResponseReads {


  private[controllers] def continueUrl(implicit request: Request[AnyContent]): String =
    controllers.business.routes.BusinessAddressController.callBack("").absoluteURL().replace("""\?*.+$""", "")

  private[controllers] def initConfig(implicit request: Request[AnyContent]): AddressLookupInitRequest =
    AddressLookupInitRequest(continueUrl,
      Some(LookupPage(
        heading = Some(Messages("business.address.lookup.heading")),
        filterLabel = Some(Messages("business.address.lookup.name_or_number")),
        submitLabel = Some(Messages("business.address.lookup.submit")),
        manualAddressLinkText = Some(Messages("business.address.lookup.enter_manually"))
      )),
      Some(
        SelectPage(
          title = Some(Messages("business.address.select.title")),
          heading = Some(Messages("business.address.select.heading")),
          editAddressLinkText = Some(Messages("business.address.select.edit"))
        )
      ),
      Some(
        ConfirmPage(
          heading = Some(Messages("business.address.confirm.heading")),
          searchAgainLinkText = Some(Messages("business.address.confirm.change"))
        )
      ),
      Some(
        EditPage(
          heading = Some(Messages("business.address.edit.heading")),
          line1Label = Some(Messages("business.address.edit.add_line_1")),
          line2Label = Some(Messages("business.address.edit.add_line_2")),
          line3Label = Some(Messages("business.address.edit.add_line_3"))
        )
      )
    )


  def init(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      addressLookupService.init(initConfig).flatMap {
        case Right(url) => Future.successful(Redirect(url))
        case Left(err) => Future.failed(new InternalServerException("BusinessAddressController.init failed unexpectedly, status=" + err.status))
      }
  }

  def callBack(id: String): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      addressLookupService.retrieveAddress(id).flatMap {
        // TODO goto business start page when it's available
        case Right(address) => keystoreService.saveBusinessAddress(address.address).map {
          _ => NotImplemented
        }
        case Left(UnexpectedStatusReturned(status)) =>
          Future.failed(new InternalServerException("BusinessAddressController.callBack failed unexpectedly, status=" + status))
        case Left(MalformatAddressReturned) =>
          Future.failed(new InternalServerException("BusinessAddressController.callBack failed unexpectedly, malformed address retrieved"))
      }
  }

}
