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

package controllers.individual.claimenrolment

import auth.individual.{ClaimEnrolment => ClaimEnrolmentJourney}
import auth.individual.JourneyState.ResultFunctions
import config.AppConfig
import config.featureswitch.FeatureSwitch.ClaimEnrolmentOrigins
import config.featureswitch.FeatureSwitching
import controllers.SignUpBaseController
import controllers.individual.actions.{BasicIdentifierAction, IdentifierAction}
import models.individual.claimenrolment.ClaimEnrolmentOrigin
import play.api.mvc.*
import services.SessionDataService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.individual.claimenrolment.AddMTDITOverview

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddMTDITOverviewController @Inject()(addmtdit: AddMTDITOverview,
                                           sessionDataService: SessionDataService,
                                           identify: IdentifierAction,
                                           basicIdentify: BasicIdentifierAction)
                                          (implicit val ec: ExecutionContext,
                                           val appConfig: AppConfig,
                                           mcc: MessagesControllerComponents) extends SignUpBaseController with FeatureSwitching {


  def show(origin: Option[String] = None): Action[AnyContent] = basicIdentify.async { implicit request =>
    originFromParameter(origin) map { origin =>
      Ok(addmtdit(
        postAction = routes.AddMTDITOverviewController.submit,
        origin = origin
      )).withJourneyState(ClaimEnrolmentJourney)
    }
  }

  def submit: Action[AnyContent] = identify { _ =>
    Redirect(routes.ClaimEnrolmentResolverController.resolve)
  }

  private def originFromParameter(maybeOrigin: Option[String])
                                 (implicit headerCarrier: HeaderCarrier): Future[ClaimEnrolmentOrigin] = {
    val origin: ClaimEnrolmentOrigin = maybeOrigin.map(_.toLowerCase) match {
      case Some(ClaimEnrolmentOrigin.ClaimEnrolmentBTA.key) => ClaimEnrolmentOrigin.ClaimEnrolmentBTA
      case Some(ClaimEnrolmentOrigin.ClaimEnrolmentPTA.key) => ClaimEnrolmentOrigin.ClaimEnrolmentPTA
      case _ if isEnabled(ClaimEnrolmentOrigins) => ClaimEnrolmentOrigin.ClaimEnrolmentSignUp
      case _ => ClaimEnrolmentOrigin.ClaimEnrolmentBTA
    }
    sessionDataService.saveClaimEnrolmentOrigin(origin) map {
      case Right(_) => origin
      case Left(_) => throw new InternalServerException("[AddMTDITOverviewController] - Failed to save origin to session")
    }
  }
}