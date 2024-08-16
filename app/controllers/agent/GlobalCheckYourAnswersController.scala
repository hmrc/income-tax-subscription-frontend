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

import auth.agent.{AuthenticatedController, IncomeTaxAgentUser}
import common.Constants.ITSASessionKeys
import config.AppConfig
import config.featureswitch.FeatureSwitch.CheckClientRelationship
import config.featureswitch.FeatureSwitchingImpl
import controllers.utils.ReferenceRetrieval
import models.common.subscription.{CreateIncomeSourcesModel, SubscriptionSuccess}
import play.api.mvc._
import play.twirl.api.Html
import services.GetCompleteDetailsService.CompleteDetails
import services._
import services.agent.{ClientRelationshipService, SubscriptionOrchestrationService}
import testonly.form.agent.NinoForm.nino
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.agent.GlobalCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GlobalCheckYourAnswersController @Inject()(globalCheckYourAnswers: GlobalCheckYourAnswers,
                                                 getCompleteDetailsService: GetCompleteDetailsService,
                                                 referenceRetrieval: ReferenceRetrieval,
                                                 ninoService: NinoService,
                                                 utrService: UTRService,
                                                 subscriptionService: SubscriptionOrchestrationService,
                                                 clientRelationshipService: ClientRelationshipService)
                                                (val auditingService: AuditingService,
                                                 val authService: AuthService,
                                                 val subscriptionDetailsService: SubscriptionDetailsService,
                                                 val appConfig: AppConfig,
                                                 val sessionDataService: SessionDataService)
                                                (implicit val ec: ExecutionContext,
                                                 mcc: MessagesControllerComponents) extends AuthenticatedController {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getAgentReference flatMap { reference =>
        withCompleteDetails(reference) { completeDetails =>
          Future.successful(Ok(view(
            postAction = routes.GlobalCheckYourAnswersController.submit,
            backUrl = controllers.agent.tasklist.routes.TaskListController.show().url,
            completeDetails = completeDetails
          )))
        }
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      referenceRetrieval.getAgentReference flatMap { reference =>
        withCompleteDetails(reference) { completeDetails =>
          if(isEnabled(CheckClientRelationship)) {
            clientRelationshipService.isPreExistingRelationship(user.arn, nino)
            clientRelationshipService.isMTDPreExistingRelationship(user.arn, nino)
          }
          signUp(completeDetails) {
            case Some(id) =>
              Redirect(controllers.agent.routes.ConfirmationController.show)
                .addingToSession(ITSASessionKeys.MTDITID -> id)
            case None =>
              Redirect(controllers.agent.routes.ConfirmationController.show)
                .addingToSession(ITSASessionKeys.MTDITID -> "already-signed-up")

          }
        }
      }
  }

  private def signUp(completeDetails: CompleteDetails)
                    (onSuccessfulSignUp: Option[String] => Result)
                    (implicit request: Request[AnyContent], user: IncomeTaxAgentUser): Future[Result] = {
    val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)

    ninoService.getNino flatMap { nino =>
      utrService.getUTR flatMap { utr =>
        subscriptionService.createSubscriptionFromTaskList(
          arn = user.arn,
          utr = utr,
          createIncomeSourcesModel = CreateIncomeSourcesModel.createIncomeSources(nino, completeDetails)
        )(headerCarrier) map {
          case Right(Some(SubscriptionSuccess(id))) =>
            onSuccessfulSignUp(Some(id))
          case Right(None) =>
            onSuccessfulSignUp(None)
          case Left(failure) =>
            throw new InternalServerException(
              s"[GlobalCheckYourAnswersController][submit] - failure response received from submission: ${failure.toString}"
            )
        }
      }
    }
  }


  private def view(postAction: Call,
                   backUrl: String,
                   completeDetails: CompleteDetails)
                  (implicit request: Request[AnyContent]): Html = {
    globalCheckYourAnswers(
      postAction = postAction,
      backUrl = backUrl,
      completeDetails = completeDetails
    )
  }

  private def withCompleteDetails(reference: String)
                                 (f: CompleteDetails => Future[Result])
                                 (implicit request: Request[AnyContent]): Future[Result] = {
    getCompleteDetailsService.getCompleteSignUpDetails(reference) flatMap {
      case Right(completeDetails) => f(completeDetails)
      case Left(_) => Future.successful(Redirect(controllers.agent.tasklist.routes.TaskListController.show()))
    }
  }

}

