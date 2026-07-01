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
import controllers.SignUpBaseController
import controllers.agent.actions.{ConfirmedClientJourneyRefiner, IdentifierAction}
import models.SubmissionStatus.{handledError, inProgress, otherError, success}
import models.audits.ITSASignUpSubmissionRequestAuditing.ITSASignUpSubmissionRequestAuditModel
import models.common.subscription.CreateIncomeSourcesModel
import models.requests.agent.ConfirmedClientRequest
import play.api.mvc.*
import services.GetCompleteDetailsService.CompleteDetails
import services.agent.SignUpOrchestrationService
import services.agent.SignUpOrchestrationService.{AlreadySignedUp, HandledUnprocessableSignUp, SignUpOrchestrationResponse}
import services.*
import uk.gov.hmrc.http.HeaderCarrier
import utilities.{AccountingPeriodUtil, CurrentDateProvider}
import views.html.agent.GlobalCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class GlobalCheckYourAnswersController @Inject()(globalCheckYourAnswers: GlobalCheckYourAnswers,
                                                 identify: IdentifierAction,
                                                 journeyRefiner: ConfirmedClientJourneyRefiner,
                                                 sessionDataService: SessionDataService,
                                                 getCompleteDetailsService: GetCompleteDetailsService,
                                                 signUpOrchestrationService: SignUpOrchestrationService,
                                                 subscriptionDetailsService: SubscriptionDetailsService,
                                                 eligibilityStatusService: GetEligibilityStatusService,
                                                 currentDateProvider: CurrentDateProvider,
                                                 utrService: UTRService,
                                                 ninoService: NinoService,
                                                 mandationStatusService: MandationStatusService)
                                                (val auditingService: AuditingService)
                                                (implicit ec: ExecutionContext,
                                                 mcc: MessagesControllerComponents) extends SignUpBaseController {

  def show: Action[AnyContent] = (identify andThen journeyRefiner).async { implicit request =>
    withCompleteDetails(request.reference) { completeDetails =>
      mandationStatusService.getMandationStatus(request.sessionData) map { mandationStatus =>
        Ok(
          globalCheckYourAnswers(
            postAction = routes.GlobalCheckYourAnswersController.submit,
            completeDetails = completeDetails,
            clientDetails = request.clientDetails,
            softwareStatus = request.sessionData.fetchSoftwareStatus,
            isMandatedNextYear = mandationStatus.nextYearStatus.isMandated
          )
        )
      }
    }
  }

  def submit: Action[AnyContent] =
    (identify andThen journeyRefiner).async { implicit request =>
      withCompleteDetails(request.reference) { completeDetails =>
        itsaSignUpSubmissionRequestAuditEvent(completeDetails).flatMap { _ =>
          sessionDataService.saveSubmissionStatus(inProgress).map { _ =>
            backgroundSignUp(completeDetails)
            Redirect(routes.LoadingSpinnerController.show)
          }
        }
      }
    }

  private def backgroundSignUp(completeDetails: CompleteDetails)(implicit request: ConfirmedClientRequest[AnyContent]): Unit = {
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

  private def itsaSignUpSubmissionRequestAuditEvent(completeDetails: CompleteDetails)
                                                   (implicit request: ConfirmedClientRequest[AnyContent], hc: HeaderCarrier): Future[Unit] = {
    val arn = request.arn

    for {
      eligibility <- eligibilityStatusService.getEligibilityStatus(request.sessionData)
      mandationStatus <- mandationStatusService.getMandationStatus(request.sessionData)

      auditModel =
        ITSASignUpSubmissionRequestAuditModel(
          agentReferenceNumber = Some(arn),
          utr = request.utr,
          nino = request.clientDetails.nino,
          eligibility = eligibility,
          itsaStatus = mandationStatus,
          completeDetails = completeDetails
        )

      _ <- auditingService.audit(auditModel)

    } yield ()
  }
}

