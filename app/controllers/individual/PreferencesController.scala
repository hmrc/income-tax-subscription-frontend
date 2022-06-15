/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.individual.AuthPredicate.AuthPredicate
import auth.individual.{IncomeTaxSAUser, StatelessController}
import common.Constants.ITSASessionKeys
import config.AppConfig
import config.featureswitch.FeatureSwitch.SaveAndRetrieve
import controllers.utils.ReferenceRetrieval
import models.{Activated, Unset}
import play.api.mvc._
import play.twirl.api.Html
import services._
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.ContinueRegistration

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PreferencesController @Inject()(val continueRegistration: ContinueRegistration,
                                      val auditingService: AuditingService,
                                      val authService: AuthService,
                                      val subscriptionDetailsService: SubscriptionDetailsService,
                                      preferencesService: PreferencesService,
                                      paperlessPreferenceTokenService: PaperlessPreferenceTokenService)
                                     (implicit val ec: ExecutionContext,
                                      val appConfig: AppConfig,
                                      mcc: MessagesControllerComponents) extends StatelessController with ReferenceRetrieval {

  override val statelessDefaultPredicate: AuthPredicate[IncomeTaxSAUser] = preferencesPredicate

  def view()(implicit request: Request[AnyContent]): Html = {
    continueRegistration(
      postAction = controllers.individual.routes.PreferencesController.submit
    )
  }

  def checkPreferences: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        for {
          token <- paperlessPreferenceTokenService.storeNino(user.nino.get, reference)
          res <- preferencesService.checkPaperless(token)
        } yield res match {
          case Right(Activated) if isEnabled(SaveAndRetrieve) => Redirect(controllers.individual.business.routes.TaskListController.show())
          case Right(Activated) => Redirect(controllers.individual.business.routes.WhatYearToSignUpController.show())
          case Right(Unset(url)) => Redirect(url)
          case _ => throw new InternalServerException("Could not get paperless preferences")
        }
      }
  }

  def callback: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        paperlessPreferenceTokenService.storeNino(user.nino.get, reference) flatMap {
          token =>
            preferencesService.checkPaperless(token).map {
              case Right(Activated) if isEnabled(SaveAndRetrieve) => Redirect(controllers.individual.business.routes.TaskListController.show())
              case Right(Activated) => Redirect(controllers.individual.business.routes.WhatYearToSignUpController.show())
              case Right(Unset(url)) => Redirect(controllers.individual.routes.PreferencesController.show)
                .addingToSession(ITSASessionKeys.PreferencesRedirectUrl -> url)
              case _ => throw new InternalServerException("Could not get paperless preferences")
            }
        }
      }
  }

  def show: Action[AnyContent] = Authenticated { implicit request =>
    _ => Ok(view())
  }

  def submit: Action[AnyContent] = Authenticated { implicit request =>
    _ => gotoPreferences
  }

  def gotoPreferences(implicit request: Request[AnyContent]): Result =
    request.session.get(ITSASessionKeys.PreferencesRedirectUrl) match {
      case Some(redirectUrl) => Redirect(redirectUrl)
        .removingFromSession(ITSASessionKeys.PreferencesRedirectUrl)
      case None => throw new InternalServerException("No preferences redirect URL provided")
    }

}
