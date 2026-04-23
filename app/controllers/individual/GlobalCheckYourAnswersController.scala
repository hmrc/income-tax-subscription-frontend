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

package controllers.individual

import common.Constants.ITSASessionKeys
import controllers.SignUpBaseController
import controllers.individual.actions.{IdentifierAction, SignUpJourneyRefiner}
import models.SubmissionStatus.{handledError, inProgress, otherError, success}
import models.common.subscription.CreateIncomeSourcesModel
import models.requests.individual.SignUpRequest
import play.api.mvc.*
import services.*
import services.GetCompleteDetailsService.CompleteDetails
import services.individual.SignUpOrchestrationService
import services.individual.SignUpOrchestrationService.{AlreadySignedUp, HandledUnprocessableSignUp, SignUpOrchestrationResponse}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.individual.GlobalCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class GlobalCheckYourAnswersController @Inject()(identify: IdentifierAction,
                                                 journeyRefiner: SignUpJourneyRefiner,
                                                 sessionDataService: SessionDataService,
                                                 signUpOrchestrationService: SignUpOrchestrationService,
                                                 getCompleteDetailsService: GetCompleteDetailsService,
                                                 subscriptionDetailsService: SubscriptionDetailsService,
                                                 utrService: UTRService,
                                                 globalCheckYourAnswers: GlobalCheckYourAnswers,
                                                 mandationStatusService: MandationStatusService)
                                                (implicit ec: ExecutionContext,
                                                 mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    withCompleteDetails(request.reference) { completeDetails =>
      for {
        maybeAccountingPeriod <- subscriptionDetailsService.fetchAccountingPeriod(request.reference)
        mandationStatus <- mandationStatusService.getMandationStatus(request.sessionData)
      } yield {
        Ok(globalCheckYourAnswers(
          postAction = routes.GlobalCheckYourAnswersController.submit,
          completeDetails = completeDetails,
          maybeAccountingPeriod = maybeAccountingPeriod,
          softwareStatus = request.sessionData.fetchSoftwareStatus,
          isMandatedNextYear = mandationStatus.nextYearStatus.isMandated
        ))
      }
    }
  }

  def submit: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    withCompleteDetails(request.reference) { completeDetails =>
      sessionDataService.saveSubmissionStatus(inProgress) map { _ =>
        backgroundSignUp(completeDetails)
        Redirect(routes.LoadingSpinnerController.show)
      }
    }
  }

  private def backgroundSignUp(completeDetails: CompleteDetails)(implicit request: SignUpRequest[AnyContent]): Unit = {
    signUp(completeDetails).onComplete {
      case Success(Right(_)) | Success(Left(AlreadySignedUp)) =>
        sessionDataService.saveSubmissionStatus(success)
      case Success(Left(HandledUnprocessableSignUp)) =>
        sessionDataService.saveSubmissionStatus(handledError)
      case Success(Left(failure)) =>
        sessionDataService.saveSubmissionStatus(otherError)
      case _ =>
        sessionDataService.saveSubmissionStatus(otherError)
    }
  }

  private def signUp(completeDetails: CompleteDetails)
                    (implicit request: SignUpRequest[AnyContent]): Future[SignUpOrchestrationResponse] = {
    val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)

    utrService.getUTR(request.sessionData) flatMap { utr =>
      signUpOrchestrationService.orchestrateSignUp(
        nino = request.nino,
        utr = utr,
        taxYear = completeDetails.taxYear.accountingYear,
        incomeSources = CreateIncomeSourcesModel.createIncomeSources(request.nino, completeDetails),
        maybeEntityId = request.session.get(ITSASessionKeys.SPSEntityId)
      )(headerCarrier)
    }
  }

  private def withCompleteDetails(reference: String)
                                 (f: CompleteDetails => Future[Result])
                                 (implicit request: Request[AnyContent]): Future[Result] = {
    getCompleteDetailsService.getCompleteSignUpDetails(reference) flatMap {
      case Right(completeDetails) => f(completeDetails)
      case Left(_) => Future.successful(Redirect(
        tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
      ))
    }
  }

}

