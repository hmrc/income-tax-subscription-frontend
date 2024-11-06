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
import models.{Next, Yes}
import models.common.AccountingPeriodModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import services.agent.ClientDetailsRetrieval
import uk.gov.hmrc.http.InternalServerException
import utilities.AccountingPeriodUtil
import views.html.agent.confirmation.SignUpConfirmation

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(signUpConfirmation: SignUpConfirmation,
                                       referenceRetrieval: ReferenceRetrieval,
                                       clientDetailsRetrieval: ClientDetailsRetrieval,
                                       subscriptionDetailsService: SubscriptionDetailsService,
                                       mandationStatusService: MandationStatusService,
                                       sessionDataService: SessionDataService)
                                      (val auditingService: AuditingService,
                                       val authService: AuthService)
                                      (implicit val ec: ExecutionContext,
                                       val appConfig: AppConfig,
                                       mcc: MessagesControllerComponents) extends PostSubmissionController {

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        reference <- referenceRetrieval.getAgentReference
        clientDetails <- clientDetailsRetrieval.getClientDetails
        taxYearSelection <- subscriptionDetailsService.fetchSelectedTaxYear(reference)
        mandationStatus <- mandationStatusService.getMandationStatus
        softwareStatus <- sessionDataService.fetchSoftwareStatus
      } yield {
        val isNextYear = taxYearSelection.map(_.accountingYear).contains(Next)
        val accountingPeriodModel: AccountingPeriodModel = if (isNextYear) {
          AccountingPeriodUtil.getNextTaxYear
        } else {
          AccountingPeriodUtil.getCurrentTaxYear
        }

        softwareStatus match {
          case Left(error) =>
            throw new InternalServerException(s"[ConfirmationController][show] - failure retrieving software status - $error")

          case Right(usingSoftwareStatus) =>
            Ok(signUpConfirmation(
              mandatedCurrentYear = mandationStatus.currentYearStatus.isMandated,
              mandatedNextYear = mandationStatus.nextYearStatus.isMandated,
              isNextYear,
              Some(clientDetails.name),
              clientDetails.formattedNino,
              accountingPeriodModel,
              usingSoftwareStatus = usingSoftwareStatus.contains(Yes)
            ))

        }
      }
  }

  val submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getAgentReference flatMap { reference =>
        subscriptionDetailsService.deleteAll(reference)
          .map(_ => Redirect(controllers.agent.routes.AddAnotherClientController.addAnother()))
      }
  }
}
