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

package controllers.individual.matching

import auth.individual.JourneyState.ResultFunctions
import auth.individual.SignUp
import config.AppConfig
import config.featureswitch.*
import config.featureswitch.FeatureSwitch.WhenDoYouWantToStartPage
import connectors.UsersGroupsSearchConnector
import connectors.agent.EnrolmentStoreProxyConnector
import controllers.individual.CheckIRSAEnrolmentBaseController
import controllers.individual.actions.IdentifierAction
import models.EligibilityStatus
import models.requests.individual.IdentifierRequest
import models.status.MandationStatus.{Mandated, Voluntary}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{GetEligibilityStatusService, MandationStatusService, UTRService}
import views.html.individual.IRSACredential

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckIRSAEnrolmentController @Inject()(identify: IdentifierAction,
                                             utrService: UTRService,
                                             usersGroupsSearchConnector: UsersGroupsSearchConnector,
                                             enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
                                             eligibilityStatusService: GetEligibilityStatusService,
                                             mandationStatusService: MandationStatusService,
                                             irsaCredential: IRSACredential)
                                            (val appConfig: AppConfig)
                                            (implicit mcc: MessagesControllerComponents,
                                             ec: ExecutionContext)
  extends CheckIRSAEnrolmentBaseController(
    utrService,
    usersGroupsSearchConnector,
    enrolmentStoreProxyConnector,
    irsaCredential,
    appConfig
  ) with FeatureSwitching {

  private lazy val postAction = routes.CheckIRSAEnrolmentController.submit

  def show: Action[AnyContent] = identify.async { implicit request =>
    super.show(postAction)
  }

  def submit: Action[AnyContent] = identify.async { implicit request =>
    super.submit(postAction)
  }

  override protected def redirectToNext(implicit request: IdentifierRequest[_]): Future[Result] = {

    val next = if (isEnabled(WhenDoYouWantToStartPage)) {
      eligibilityStatusService.getEligibilityStatus(request.sessionData) flatMap { eligibilityStatus =>
        mandationStatusService.getMandationStatus(request.sessionData) map { mandationStatus =>
          (eligibilityStatus.eligibleCurrentYear, mandationStatus.currentYearStatus, mandationStatus.nextYearStatus) match {
            case (true, Voluntary, Voluntary) =>
              controllers.individual.tasklist.taxyear.routes.WhenDoYouWantToStartController.show()
            case (true, Voluntary, Mandated) =>
              controllers.individual.tasklist.taxyear.routes.NextYearMandatorySignUpController.show()
            case (true, Mandated, _) =>
              controllers.individual.tasklist.taxyear.routes.MandatoryBothSignUpController.show
            case (false, _, Voluntary) =>
              controllers.individual.tasklist.taxyear.routes.NonEligibleVoluntaryController.show
            case (false, _, Mandated) =>
              controllers.individual.tasklist.taxyear.routes.NonEligibleMandatedController.show
          }
        }
      }
    } else {
      eligibilityStatusService.getEligibilityStatus(request.sessionData) map {
        case EligibilityStatus(true, _, _) =>
          controllers.individual.routes.YouCanSignUpController.show
        case _ =>
          controllers.individual.controllist.routes.CannotSignUpThisYearController.show
      }
    }

    next.map(Redirect(_).withJourneyState(SignUp))
  }
}
