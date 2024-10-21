/*
 * Copyright 2024 HM Revenue & Customs
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

import auth.agent.AuthenticatedController
import config.AppConfig
import forms.agent.UsingSoftwareForm
import models.YesNo
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import services.agent.ClientDetailsRetrieval
import services.{AuditingService, AuthService, GetEligibilityStatusService, SessionDataService}
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.UsingSoftware
import config.featureswitch.FeatureSwitch.PrePopulate
import config.featureswitch.FeatureSwitching

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex


@Singleton
class UsingSoftwareController @Inject()(clientDetailsRetrieval: ClientDetailsRetrieval,
                                        usingSoftware: UsingSoftware,
                                        sessionDataService: SessionDataService,
                                        eligibilityStatusService: GetEligibilityStatusService)
                                       (val auditingService: AuditingService,
                                        val authService: AuthService,
                                        val appConfig: AppConfig)
                                       (implicit val ec: ExecutionContext,
                                        mcc: MessagesControllerComponents)
  extends AuthenticatedController with FeatureSwitching {

  private val ninoRegex: Regex = """^([a-zA-Z]{2})\s*(\d{2})\s*(\d{2})\s*(\d{2})\s*([a-zA-Z])$""".r
  private val form: Form[YesNo] = UsingSoftwareForm.usingSoftwareForm

  private def formatNino(clientNino: String): String = {
    clientNino match {
      case ninoRegex(startLetters, firstDigits, secondDigits, thirdDigits, finalLetter) =>
        s"$startLetters $firstDigits $secondDigits $thirdDigits $finalLetter"
      case other => other
    }
  }

  def backUrl(eligibleNextYearOnly: Boolean): Option[String] = {
    if (eligibleNextYearOnly)
      Some(controllers.agent.eligibility.routes.CannotSignUpThisYearController.show.url)
    else
      Some(controllers.agent.eligibility.routes.ClientCanSignUpController.show().url)
  }


  def view(usingSoftwareForm: Form[YesNo],
           clientName: String,
           clientNino: String,
           eligibleNextYearOnly: Boolean)
          (implicit request: Request[_]): Html = {
    usingSoftware(
      usingSoftwareForm = usingSoftwareForm,
      postAction = controllers.agent.routes.UsingSoftwareController.submit(),
      clientName,
      clientNino,
      backUrl = backUrl(eligibleNextYearOnly)
    )
  }

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      for {
        clientDetails <- clientDetailsRetrieval.getClientDetails
        usingSoftwareStatus <- sessionDataService.fetchSoftwareStatus
        eligibilityStatus <- eligibilityStatusService.getEligibilityStatus
      } yield {
        usingSoftwareStatus match {
          case Left(_) => throw new InternalServerException("[UsingSoftwareController][show] - Could not fetch software status")

          case Right(maybeYesNo) =>
            Ok(view(
              usingSoftwareForm = form.fill(maybeYesNo),
              clientName = clientDetails.name,
              clientNino = formatNino(clientDetails.nino),
              eligibleNextYearOnly = eligibilityStatus.eligibleNextYearOnly
            ))
        }

      }
  }

  def submit(): Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      form.bindFromRequest().fold(
        formWithErrors =>
          clientDetailsRetrieval.getClientDetails flatMap { clientDetails =>
            eligibilityStatusService.getEligibilityStatus map { eligibility =>
              BadRequest(
                view(
                  usingSoftwareForm = formWithErrors,
                  clientDetails.name,
                  clientDetails.nino,
                  eligibleNextYearOnly = eligibility.eligibleNextYearOnly
                )
              )
            }
          }, yesNo =>
          sessionDataService.saveSoftwareStatus(yesNo) flatMap {
            case Left(_) => Future.failed(new InternalServerException("[UsingSoftwareController][submit] - Could not save using software answer"))
            case Right(_) =>
              if (isEnabled(PrePopulate)) {
                Future.successful(Redirect(controllers.agent.tasklist.taxyear.routes.WhatYearToSignUpController.show()))
              } else {
                Future.successful(Redirect(controllers.agent.routes.WhatYouNeedToDoController.show()))
              }
          }
      )
  }
}
