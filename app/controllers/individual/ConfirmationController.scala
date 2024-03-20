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

import auth.individual.{IncomeTaxSAUser, PostSubmissionController}
import common.Constants.ITSASessionKeys
import config.AppConfig
import config.featureswitch.FeatureSwitch.ConfirmationPage
import connectors.individual.PreferencesFrontendConnector
import controllers.utils.ReferenceRetrieval
import models.Next
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService, SessionDataService, SubscriptionDetailsService}
import views.html.individual.confirmation.{SignUpComplete, SignUpConfirmation}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(signUpComplete: SignUpComplete,
                                       signUpConfirmation: SignUpConfirmation,
                                       preferencesFrontendConnector: PreferencesFrontendConnector)
                                      (val auditingService: AuditingService,
                                       val authService: AuthService,
                                       val subscriptionDetailsService: SubscriptionDetailsService,
                                       val sessionDataService: SessionDataService)
                                      (implicit val ec: ExecutionContext,
                                       val appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends PostSubmissionController with ReferenceRetrieval {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withIndividualReference { reference =>
        if (isEnabled(ConfirmationPage)) {
          for {
            preference <- preferencesFrontendConnector.getOptedInStatus
            selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
          } yield {
            val taxYearSelectionIsNext = selectedTaxYear.map(_.accountingYear).contains(Next)
            val mandatedCurrentYear: Boolean = request.session.get(ITSASessionKeys.MANDATED_CURRENT_YEAR).contains("true")

            Ok(signUpConfirmation(
              mandatedCurrentYear = mandatedCurrentYear,
              taxYearSelectionIsNext = taxYearSelectionIsNext,
              individualUserNameMaybe = IncomeTaxSAUser.fullName,
              individualUserNino = user.nino.getOrElse(
                throw new Exception("[ConfirmationController][show]-could not retrieve individual nino from session")
              ),
              preference = preference
            ))
          }
        } else {
          for {
            selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
          } yield {
            val taxYearSelectionIsNext = selectedTaxYear.map(_.accountingYear).contains(Next)
            Ok(signUpComplete(
              taxYearSelectionIsNext = taxYearSelectionIsNext,
              postAction = routes.ConfirmationController.submit
            ))
          }
        }

      }
  }

  def submit: Action[AnyContent] = Authenticated {
    _ => _ => Redirect(controllers.routes.SignOutController.signOut)
  }

}
