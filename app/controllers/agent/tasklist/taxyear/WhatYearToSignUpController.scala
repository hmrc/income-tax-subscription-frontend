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

package controllers.agent.tasklist.taxyear

import auth.agent.AuthenticatedController
import config.AppConfig
import controllers.utils.{ReferenceRetrieval, TaxYearNavigationHelper}
import forms.agent.AccountingYearForm
import models.AccountingYear
import models.common.AccountingYearModel
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services._
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.tasklist.taxyear.WhatYearToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

@Singleton
class WhatYearToSignUpController @Inject()(accountingPeriodService: AccountingPeriodService,
                                           whatYearToSignUp: WhatYearToSignUp)
                                          (val auditingService: AuditingService,
                                           val authService: AuthService,
                                           val appConfig: AppConfig,
                                           val subscriptionDetailsService: SubscriptionDetailsService,
                                           val getEligibilityStatusService: GetEligibilityStatusService,
                                           val mandationStatusService: MandationStatusService,
                                           val sessionDataService: SessionDataService)
                                          (implicit val ec: ExecutionContext,
                                           mcc: MessagesControllerComponents)
  extends AuthenticatedController with ReferenceRetrieval with TaxYearNavigationHelper {

  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  private def formatNino(clientNino: String): String = {
    clientNino match {
      case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
        s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
      case other => other
    }
  }

  def backUrl(isEditMode: Boolean): Option[String] =
    if (isEditMode)
      Some(controllers.agent.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show(editMode = true).url)
    else
      Some(controllers.agent.tasklist.routes.TaskListController.show().url)

  def view(accountingYearForm: Form[AccountingYear], clientName: String, clientNino: String, isEditMode: Boolean)(implicit request: Request[_]): Html = {
    whatYearToSignUp(
      accountingYearForm = accountingYearForm,
      postAction = controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.submit(editMode = isEditMode),
      clientName,
      clientNino,
      backUrl = backUrl(isEditMode),
      endYearOfCurrentTaxPeriod = accountingPeriodService.currentTaxYear,
      isEditMode = isEditMode
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleUnableToSelectTaxYearAgent {
        withAgentReference { reference =>
          subscriptionDetailsService.fetchSelectedTaxYear(reference, user.getClientNino, user.getClientUtr) map { accountingYearModel =>
            Ok(view(accountingYearForm = AccountingYearForm.accountingYearForm.fill(
              accountingYearModel.map(aym => aym.accountingYear)),
              clientName = request.fetchClientName.getOrElse(
                throw new InternalServerException("[AccountingPeriodCheckController][show] - could not retrieve client name from session")
              ),
              clientNino = formatNino(user.clientNino.getOrElse(
                throw new InternalServerException("[AccountingPeriodCheckController][show] - could not retrieve client nino from session")
              )),
              isEditMode = isEditMode))
          }
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      val clientName = request.fetchClientName.get
      val clientNino = user.clientNino.get
      withAgentReference { reference =>
        AccountingYearForm.accountingYearForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(accountingYearForm = formWithErrors, clientName, clientNino, isEditMode = isEditMode))),
          accountingYear => {
            subscriptionDetailsService.saveSelectedTaxYear(reference, AccountingYearModel(accountingYear)) map {
              case Right(_) => Redirect(controllers.agent.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.show())
              case Left(_) => throw new InternalServerException("[WhatYearToSignUpController][submit] - Could not save accounting year")
            }
          }
        )
      }
  }
}
