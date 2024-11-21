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

import auth.individual.SignUpController
import common.Constants.ITSASessionKeys
import common.Constants.ITSASessionKeys.SPSEntityId
import config.AppConfig
import config.featureswitch.FeatureSwitch.PrePopulate
import controllers.utils.ReferenceRetrieval
import models.common.subscription.CreateIncomeSourcesModel
import play.api.mvc._
import play.twirl.api.Html
import services.GetCompleteDetailsService.CompleteDetails
import services._
import services.individual.SubscriptionOrchestrationService
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.individual.GlobalCheckYourAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GlobalCheckYourAnswersController @Inject()(subscriptionService: SubscriptionOrchestrationService,
                                                 getCompleteDetailsService: GetCompleteDetailsService,
                                                 ninoService: NinoService,
                                                 referenceRetrieval: ReferenceRetrieval,
                                                 globalCheckYourAnswers: GlobalCheckYourAnswers)
                                                (val auditingService: AuditingService,
                                                 val authService: AuthService,
                                                 val appConfig: AppConfig)
                                                (implicit val ec: ExecutionContext,
                                                 mcc: MessagesControllerComponents) extends SignUpController {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        withCompleteDetails(reference) { completeDetails =>
          Future.successful(Ok(view(
            postAction = routes.GlobalCheckYourAnswersController.submit,
            backUrl = backUrl,
            completeDetails = completeDetails
          )))
        }
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        withCompleteDetails(reference) { completeDetails =>
          signUp(completeDetails)(
            onSuccessfulSignUp = Redirect(controllers.individual.routes.ConfirmationController.show)
          )
        }
      }
  }

  def backUrl: String = {
    if (isEnabled(PrePopulate)) tasklist.addbusiness.routes.YourIncomeSourceToSignUpController.show.url
    else tasklist.routes.TaskListController.show().url
  }

  private def signUp(completeDetails: CompleteDetails)
                    (onSuccessfulSignUp: Result)
                    (implicit request: Request[AnyContent]): Future[Result] = {
    val headerCarrier = implicitly[HeaderCarrier].withExtraHeaders(ITSASessionKeys.RequestURI -> request.uri)
    val session = request.session

    ninoService.getNino flatMap { nino =>
      subscriptionService.signUpAndCreateIncomeSourcesFromTaskList(
        createIncomeSourceModel = CreateIncomeSourcesModel.createIncomeSources(nino, completeDetails),
        maybeSpsEntityId = session.get(SPSEntityId)
      )(headerCarrier) map {
        case Right(_) =>
          onSuccessfulSignUp
        case Left(failure) =>
          throw new InternalServerException(
            s"[GlobalCheckYourAnswersController][submit] - failure response received from submission: ${failure.toString}"
          )
      }
    }
  }


  private def view(
                    postAction: Call,
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
      case Left(_) => Future.successful(Redirect(backUrl))
    }
  }

}

