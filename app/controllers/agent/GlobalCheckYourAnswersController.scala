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

import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.JourneyStateKey
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import models.agent.JourneyStep.Confirmation
import models.common.subscription.CreateIncomeSourcesModel
import models.requests.agent.ConfirmedClientRequest
import play.api.mvc.*
import services.*
import services.GetCompleteDetailsService.CompleteDetails
import services.agent.SignUpOrchestrationService
import services.agent.SignUpOrchestrationService.{AlreadySignedUp, HandledUnprocessableSignUp, SignUpOrchestrationResponse}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.agent.GlobalCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GlobalCheckYourAnswersController @Inject()(globalCheckYourAnswers: GlobalCheckYourAnswers,
                                                 identify: IdentifierAction,
                                                 journeyRefiner: ConfirmedClientJourneyRefiner,
                                                 getCompleteDetailsService: GetCompleteDetailsService,
                                                 signUpOrchestrationService: SignUpOrchestrationService,
                                                 mandationStatusService: MandationStatusService,
                                                 throttlingService: ThrottlingService)
                                                (implicit ec: ExecutionContext,
                                                 mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    withCompleteDetails(request.reference) { completeDetails =>
      mandationStatusService.getMandationStatus(request.sessionData) map { mandationStatus =>
        Ok(
          globalCheckYourAnswers(
            postAction = routes.GlobalCheckYourAnswersController.submit,
            backUrl = backUrl,
            completeDetails = completeDetails,
            clientDetails = request.clientDetails,
            softwareStatus = request.sessionData.fetchSoftwareStatus,
            isMandatedNextYear = mandationStatus.nextYearStatus.isMandated
          )
        )
      }
    }
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    withCompleteDetails(request.reference) { completeDetails =>
      throttlingService.throttled(AgentEndOfJourneyThrottle, sessionData = request.sessionData) {
        signUp(completeDetails) map {
          case Right(_) | Left(AlreadySignedUp) =>
            Redirect(routes.ConfirmationController.show)
              .addingToSession(JourneyStateKey -> Confirmation.key)
          case Left(HandledUnprocessableSignUp) =>
            Redirect(controllers.errors.routes.ContactHMRCController.show)
          case Left(failure) =>
            throw new InternalServerException(
              s"[GlobalCheckYourAnswersController] - failure response received from submission: ${failure.toString}"
            )
        }
      }
    }
  }

  def backUrl: String = {
    tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
  }

  private def signUp(completeDetails: CompleteDetails)
                    (implicit request: ConfirmedClientRequest[AnyContent]): Future[SignUpOrchestrationResponse] = {
    val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)

    signUpOrchestrationService.orchestrateSignUp(
      arn = request.arn,
      nino = request.clientDetails.nino,
      utr = request.utr,
      taxYear = completeDetails.taxYear.accountingYear,
      incomeSources = CreateIncomeSourcesModel.createIncomeSources(request.clientDetails.nino, completeDetails)
    )(headerCarrier)
  }

  private def withCompleteDetails(reference: String)
                                 (f: CompleteDetails => Future[Result])
                                 (implicit request: Request[AnyContent]): Future[Result] = {
    getCompleteDetailsService.getCompleteSignUpDetails(reference) flatMap {
      case Right(completeDetails) => f(completeDetails)
      case Left(_) => Future.successful(Redirect(
        tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show
      ))
    }
  }

}

