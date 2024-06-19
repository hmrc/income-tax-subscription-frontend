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
import models.common.AccountingYearModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.http.InternalServerException
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.tasklist.taxyear.TaxYearCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

@Singleton
class TaxYearCheckYourAnswersController @Inject()(checkYourAnswersView: TaxYearCheckYourAnswers)
                                                 (val auditingService: AuditingService,
                                                  val authService: AuthService,
                                                  val appConfig: AppConfig,
                                                  val sessionDataService: SessionDataService,
                                                  val mandationStatusService: MandationStatusService,
                                                  val subscriptionDetailsService: SubscriptionDetailsService)
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

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      handleUnableToSelectTaxYearAgent {
        withAgentReference { reference =>
          subscriptionDetailsService.fetchSelectedTaxYear(reference, user.getClientNino, user.getClientUtr) map { maybeAccountingYearModel =>
            Ok(checkYourAnswersView(
              postAction = controllers.agent.tasklist.taxyear.routes.TaxYearCheckYourAnswersController.submit(),
              viewModel = maybeAccountingYearModel,
              clientName = request.fetchClientName.getOrElse(
                throw new InternalServerException("[TaxYearCheckYourAnswersController][show] - could not retrieve client name from session")
              ),
              clientNino = formatNino(user.clientNino.getOrElse(
                throw new InternalServerException("[TaxYearCheckYourAnswersController][show] - could not retrieve client nino from session")
              )),
              backUrl = backUrl(isEditMode)
            ))
          }
        }
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.fetchSelectedTaxYear(reference, user.getClientNino, user.getClientUtr) flatMap { maybeAccountingYearModel =>
          val accountingYearModel: AccountingYearModel = maybeAccountingYearModel.getOrElse(
            throw new InternalServerException("[TaxYearCheckYourAnswersController][submit] - Could not retrieve accounting year")
          )
          subscriptionDetailsService.saveSelectedTaxYear(reference, accountingYearModel.copy(confirmed = true)) map {
            case Right(_) => Redirect(controllers.agent.tasklist.routes.TaskListController.show().url)
            case Left(_) => throw new InternalServerException("[TaxYearCheckYourAnswersController][submit] - Could not confirm accounting year")
          }
        }
      }
  }

  def backUrl(isEditMode: Boolean): String = if (isEditMode) {
    controllers.agent.tasklist.routes.TaskListController.show().url
  } else {
    controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show(isEditMode).url
  }
}
