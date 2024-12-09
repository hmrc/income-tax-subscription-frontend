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

import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmationJourneyRefiner, IdentifierAction}
import models.common.AccountingPeriodModel
import models.{Next, Yes}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.http.InternalServerException
import utilities.AccountingPeriodUtil
import views.html.agent.confirmation.SignUpConfirmation

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ConfirmationController @Inject()(view: SignUpConfirmation,
                                       identify: IdentifierAction,
                                       journeyRefiner: ConfirmationJourneyRefiner,
                                       subscriptionDetailsService: SubscriptionDetailsService,
                                       mandationStatusService: MandationStatusService,
                                       sessionDataService: SessionDataService)
                                      (implicit ec: ExecutionContext, cc: MessagesControllerComponents) extends SignUpBaseController {

  val show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    for {
      taxYearSelection <- subscriptionDetailsService.fetchSelectedTaxYear(request.reference)
      mandationStatus <- mandationStatusService.getMandationStatus
      softwareStatus <- sessionDataService.fetchSoftwareStatus
    } yield {
      val isNextYear: Boolean = taxYearSelection.map(_.accountingYear).contains(Next)
      val accountingPeriodModel: AccountingPeriodModel = if (isNextYear) {
        AccountingPeriodUtil.getNextTaxYear
      } else {
        AccountingPeriodUtil.getCurrentTaxYear
      }

      softwareStatus match {
        case Left(error) =>
          throw new InternalServerException(s"[ConfirmationController][show] - failure retrieving software status - $error")
        case Right(usingSoftwareStatus) =>
          Ok(view(
            mandatedCurrentYear = mandationStatus.currentYearStatus.isMandated,
            mandatedNextYear = mandationStatus.nextYearStatus.isMandated,
            taxYearSelectionIsNext = isNextYear,
            userNameMaybe = Some(request.clientDetails.name),
            individualUserNino = request.clientDetails.formattedNino,
            accountingPeriodModel = accountingPeriodModel,
            usingSoftwareStatus = usingSoftwareStatus.contains(Yes)
          ))
      }
    }
  }

  val submit: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    subscriptionDetailsService.deleteAll(request.reference) map { _ =>
      Redirect(controllers.agent.routes.AddAnotherClientController.addAnother())
    }
  }

}
