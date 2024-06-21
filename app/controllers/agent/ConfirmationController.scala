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

package controllers.agent

import auth.agent.PostSubmissionController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models.Next
import models.common.AccountingPeriodModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.http.InternalServerException
import utilities.AccountingPeriodUtil
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.confirmation.SignUpConfirmation

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(signUpConfirmation: SignUpConfirmation,
                                       mandationStatusService: MandationStatusService)
                                      (val auditingService: AuditingService,
                                       val authService: AuthService,
                                       val sessionDataService: SessionDataService,
                                       val subscriptionDetailsService: SubscriptionDetailsService)
                                      (implicit val ec: ExecutionContext,
                                       val appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends PostSubmissionController with ReferenceRetrieval {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      val clientName = request.fetchClientName
        .getOrElse(throw new InternalServerException("[ConfirmationController][show] - could not retrieve client name from session"))

      withAgentReference { reference =>
        for {
          taxYearSelection <- subscriptionDetailsService.fetchSelectedTaxYear(reference, user.getClientNino, user.getClientUtr)
          mandationStatus <- mandationStatusService.getMandationStatus(user.getClientNino, user.getClientUtr)
        } yield {
          val isNextYear = taxYearSelection.map(_.accountingYear).contains(Next)
          val accountingPeriodModel: AccountingPeriodModel = if (isNextYear) {
            AccountingPeriodUtil.getNextTaxYear
          } else {
            AccountingPeriodUtil.getCurrentTaxYear
          }

          Ok(signUpConfirmation(
            mandatedCurrentYear = mandationStatus.currentYearStatus.isMandated,
            mandatedNextYear = mandationStatus.nextYearStatus.isMandated,
            isNextYear,
            Some(clientName),
            user.getClientNino,
            accountingPeriodModel
          ))
        }
      }
  }

  val submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.deleteAll(reference)
          .map(_ => Redirect(controllers.agent.routes.AddAnotherClientController.addAnother()))
      }
  }

}
