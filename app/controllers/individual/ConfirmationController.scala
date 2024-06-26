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
import config.AppConfig
import connectors.individual.PreferencesFrontendConnector
import controllers.utils.ReferenceRetrieval
import models.Next
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import views.html.individual.confirmation.SignUpConfirmation

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(signUpConfirmation: SignUpConfirmation,
                                       mandationStatusService: MandationStatusService,
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
        for {
          preference <- preferencesFrontendConnector.getOptedInStatus
          selectedTaxYear <- subscriptionDetailsService.fetchSelectedTaxYear(reference, user.getNino, user.getUtr)
          mandationStatus <- mandationStatusService.getMandationStatus(user.getNino, user.getUtr)
        } yield {
          val taxYearSelectionIsNext = selectedTaxYear.map(_.accountingYear).contains(Next)

          Ok(signUpConfirmation(
            mandatedCurrentYear = mandationStatus.currentYearStatus.isMandated,
            taxYearSelectionIsNext = taxYearSelectionIsNext,
            individualUserNameMaybe = IncomeTaxSAUser.fullName,
            individualUserNino = user.nino.getOrElse(
              throw new Exception("[ConfirmationController][show]-could not retrieve individual nino from session")
            ),
            preference = preference
          ))
        }
      }
  }

  def submit: Action[AnyContent] = Authenticated {
    _ => _ => Redirect(controllers.routes.SignOutController.signOut)
  }

}
