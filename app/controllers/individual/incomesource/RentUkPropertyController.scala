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

package controllers.individual.incomesource

import auth.individual.SignUpController
import config.AppConfig
import forms.individual.incomesource.RentUkPropertyForm._
import javax.inject.{Inject, Singleton}
import models.individual.incomesource.RentUkPropertyModel
import models.{No, Yes}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.{AuthService, KeystoreService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RentUkPropertyController @Inject()(val authService: AuthService, keystoreService: KeystoreService)(implicit val ec: ExecutionContext,
                                                         appConfig: AppConfig, mcc: MessagesControllerComponents) extends SignUpController {

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      keystoreService.fetchRentUkProperty() map { rentUkProperty =>
        Ok(view(rentUkPropertyForm = rentUkPropertyForm.fill(rentUkProperty), isEditMode = isEditMode))
      }
  }

  def view(rentUkPropertyForm: Form[RentUkPropertyModel], isEditMode: Boolean)(implicit request: Request[_]): Html = {
    views.html.individual.incometax.incomesource.rent_uk_property(
      rentUkPropertyForm = rentUkPropertyForm,
      postAction = controllers.individual.incomesource.routes.RentUkPropertyController.submit(editMode = isEditMode),
      isEditMode = isEditMode,
      backUrl = backUrl
    )
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      rentUkPropertyForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(view(
            rentUkPropertyForm = formWithErrors,
            isEditMode = isEditMode
          ))),
        data => {
          lazy val linearJourney: Future[Result] =
            keystoreService.saveRentUkProperty(data) map { _ =>
              (data.rentUkProperty, data.onlySourceOfSelfEmployedIncome) match {
                case (No, _) =>
                  Redirect(controllers.individual.incomesource.routes.AreYouSelfEmployedController.show())
                case (Yes, Some(No)) =>
                  Redirect(controllers.individual.incomesource.routes.AreYouSelfEmployedController.show())
                case _ =>
                  Redirect(controllers.individual.business.routes.PropertyAccountingMethodController.show())
              }
            }
          if (!isEditMode) {
            linearJourney
          } else {
            keystoreService.fetchRentUkProperty() flatMap {
              case Some(`data`) => Future.successful(Redirect(controllers.individual.subscription.routes.CheckYourAnswersController.submit()))
              case _ => linearJourney
            }
          }
        }
      )
  }

  lazy val backUrl: String =
    controllers.individual.subscription.routes.CheckYourAnswersController.show().url
}
