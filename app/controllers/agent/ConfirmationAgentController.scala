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
import common.Constants.ITSASessionKeys
import config.AppConfig
import config.featureswitch.FeatureSwitch.ConfirmationPage
import controllers.utils.ReferenceRetrieval
import models.Next
import models.common.AccountingPeriodModel
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import utilities.AccountingPeriodUtil
import utilities.UserMatchingSessionUtil.UserMatchingSessionRequestUtil
import views.html.agent.{SignUpComplete, SignUpConfirmation}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

@Singleton
class ConfirmationAgentController @Inject()(val auditingService: AuditingService,
                                            val authService: AuthService,
                                            signUpComplete: SignUpComplete,
                                            signUpConfirmation: SignUpConfirmation,
                                            val subscriptionDetailsService: SubscriptionDetailsService)
                                           (implicit val ec: ExecutionContext,
                                            val appConfig: AppConfig,
                                            mcc: MessagesControllerComponents) extends PostSubmissionController with ReferenceRetrieval {


  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r

  private def formatNino(clientNino: String): String = {
    clientNino match {
      case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
        s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
      case other => other
    }

  }

  val show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>

      val postAction = controllers.agent.routes.AddAnotherClientController.addAnother()
      val signOutAction = controllers.SignOutController.signOut
      val clientName = request.fetchClientName.getOrElse(throw new Exception("[ConfirmationController][show]-could not retrieve client name from session"))
      val clientNino = user.clientNino.getOrElse(throw new Exception("[ConfirmationController][show]-could not retrieve client nino from session"))

      val formattedClientNino = formatNino(clientNino)

      withAgentReference { reference =>
        subscriptionDetailsService.fetchSelectedTaxYear(reference) map { taxYearSelection =>
          if (isEnabled(ConfirmationPage)) {
            val isNextYear = taxYearSelection.map(_.accountingYear).contains(Next)
            val accountingPeriodModel: AccountingPeriodModel = if (isNextYear) AccountingPeriodUtil.getNextTaxYear else AccountingPeriodUtil.getCurrentTaxYear
            val mandatedCurrentYear: Boolean = request.session.get(ITSASessionKeys.MANDATED_CURRENT_YEAR).contains("true")
            val mandatedNextYear: Boolean = request.session.get(ITSASessionKeys.MANDATED_NEXT_YEAR).contains("true")
            Ok(signUpConfirmation(
              mandatedCurrentYear = mandatedCurrentYear,
              mandatedNextYear = mandatedNextYear,
              isNextYear,
              Some(clientName),
              clientNino,
              accountingPeriodModel))
          } else {
            Ok(signUpComplete(taxYearSelection.map(_.accountingYear), clientName, formattedClientNino, postAction, signOutAction))
          }
        }
      }
  }

  val submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withAgentReference { reference =>
        subscriptionDetailsService.deleteAll(reference).map(_ => Redirect(controllers.agent.routes.AddAnotherClientController.addAnother()))
      }
  }

}
