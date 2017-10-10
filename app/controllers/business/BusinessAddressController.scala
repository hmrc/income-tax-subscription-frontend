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
import connectors.models.address._
import models.address.Address
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.{AddressLookupService, AuthService, KeystoreService}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

@Singleton
class BusinessAddressController @Inject()(val baseConfig: BaseControllerConfig,
                                          val messagesApi: MessagesApi,
                                          val authService: AuthService,
                                          addressLookupService: AddressLookupService,
                                          keystoreService: KeystoreService
                                         ) extends RegistrationController {


  private[controllers] def callbackUrl(editMode: Boolean)(implicit request: Request[AnyContent]): String =
    controllers.business.routes.BusinessAddressController.callBack(editMode,"").absoluteURL().replace("""\?*.+$""", "")

  private[controllers] def initConfig(editMode: Boolean)(implicit request: Request[AnyContent]): AddressLookupInitRequest =
    AddressLookupInitRequest(
      callbackUrl(editMode),
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
          searchAgainLinkText = Some(Messages("business.address.confirm.change")),
          submitLabel = if (editMode) Some(Messages("base.update")) else None
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

  def init(editMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      addressLookupService.init(initConfig(editMode)).flatMap {
        case Right(url) => Future.successful(Redirect(url))
        case Left(err) => Future.failed(new InternalServerException("BusinessAddressController.init failed unexpectedly, status=" + err.status))
      }
  }

  def callBack(editMode: Boolean, id: String): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      addressLookupService.retrieveAddress(id).flatMap {
        case Right(ReturnedAddress(_, _, address)) =>
          address.country.fold("GB")(_.code) match {
            case Address.UKCountryCode =>
              keystoreService.saveBusinessAddress(address).map {
                _ =>
                  if (editMode)
                    Redirect(controllers.routes.CheckYourAnswersController.show())
                  else
                    Redirect(controllers.business.routes.BusinessStartDateController.show())
              }
            case _ =>
              // TODO handle if it's not an UK address when it's designed
              Future.successful(NotImplemented("Not UK address"))
          }
        case Left(UnexpectedStatusReturned(status)) =>
          Future.failed(new InternalServerException("BusinessAddressController.callBack failed unexpectedly, status=" + status))
        case Left(MalformatAddressReturned) =>
          Future.failed(new InternalServerException("BusinessAddressController.callBack failed unexpectedly, malformed address retrieved"))
      }
  }

  def view(address: Address, backUrl: String, isEditMode: Boolean)(implicit request: Request[_]): Html =
    views.html.business.edit_business_address(
      address,
      controllers.business.routes.BusinessAddressController.submit(editMode = isEditMode),
      backUrl,
      editUrl = if (isEditMode) None else Some(controllers.business.routes.BusinessAddressController.init().url)
    )

  def show(editMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchBusinessAddress() map {
        case Some(address) => Ok(view(address, controllers.business.routes.BusinessPhoneNumberController.show().url, editMode))
        case None => Redirect(controllers.business.routes.BusinessAddressController.init())
      }
  }

  def submit(editMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (editMode)
        Future.successful(Redirect(controllers.business.routes.BusinessAddressController.init(editMode = true)))
      else
        Future.successful(Redirect(controllers.business.routes.BusinessStartDateController.show()))
  }

}

