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
    eligibilityStatusService.getEligibilityStatus(request.sessionData).flatMap {
      case EligibilityStatus(eligibleCurrentYear, _, _) =>
        if (eligibleCurrentYear) {
          if (isEnabled(WhenDoYouWantToStartPage)) {
            mandationStatusService.getMandationStatus(request.sessionData).map { mandationStatus =>
              val isVoluntaryCurrentYear: Boolean = mandationStatus.currentYearStatus.isVoluntary
              val isVoluntaryNextYear: Boolean = mandationStatus.nextYearStatus.isVoluntary
              val isMandatedCurrentYear: Boolean = mandationStatus.currentYearStatus.isMandated
              val isMandatedNextYear: Boolean = mandationStatus.nextYearStatus.isMandated

              val nextCall =
                if (isVoluntaryCurrentYear && isVoluntaryNextYear) {
                  controllers.individual.tasklist.taxyear.routes.WhenDoYouWantToStartController.show()
                } else if (isEnabled(WhenDoYouWantToStartPage) && isMandatedCurrentYear && isMandatedNextYear) {
                    controllers.individual.tasklist.taxyear.routes.MandatoryBothSignUpController.show()
              } else {
                  controllers.individual.routes.YouCanSignUpController.show
                }
              Redirect(nextCall).withJourneyState(SignUp)
            }
          } else {
            Future.successful(
              Redirect(controllers.individual.routes.YouCanSignUpController.show)
                .withJourneyState(SignUp)
            )
          }
        } else {
          Future.successful(
            Redirect(controllers.individual.controllist.routes.CannotSignUpThisYearController.show)
              .withJourneyState(SignUp)
          )
        }
    }
  }
}
