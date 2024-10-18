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

package controllers.individual.tasklist.addbusiness

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import models.common.IncomeSources
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.addbusiness.YourIncomeSourceToSignUp

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YourIncomeSourceToSignUpController @Inject()(yourIncomeSourceToSignUp: YourIncomeSourceToSignUp,
                                                   subscriptionDetailsService: SubscriptionDetailsService,
                                                   referenceRetrieval: ReferenceRetrieval)
                                                  (val auditingService: AuditingService,
                                                   val appConfig: AppConfig,
                                                   val authService: AuthService)
                                                  (implicit val ec: ExecutionContext,
                                                   mcc: MessagesControllerComponents) extends SignUpController {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      for {
        reference <- referenceRetrieval.getIndividualReference
        incomeSources <- subscriptionDetailsService.fetchAllIncomeSources(reference)
        maybePrePop <- subscriptionDetailsService.fetchPrePopFlag(reference)
      } yield {
        Ok(view(incomeSources, maybePrePop.contains(true)))
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    _ =>
      referenceRetrieval.getIndividualReference flatMap { reference =>
        val continue: Result = Redirect(controllers.individual.tasklist.routes.TaskListController.show())

        subscriptionDetailsService.fetchAllIncomeSources(reference) flatMap { incomeSources =>
          if (incomeSources.isComplete) {
            subscriptionDetailsService.saveIncomeSourcesConfirmation(reference) map {
              case Right(_) => continue
              case Left(_) => throw new InternalServerException("[YourIncomeSourceToSignUpController][submit] - failed to save income sources confirmation")
            }
          } else {
            Future.successful(continue)
          }
        }
      }
  }


  def backUrl: String = controllers.individual.tasklist.routes.TaskListController.show().url


  private def view(incomeSources: IncomeSources, isPrePopulated: Boolean)(implicit request: Request[AnyContent]): Html = {
    yourIncomeSourceToSignUp(
      postAction = routes.YourIncomeSourceToSignUpController.submit,
      backUrl = backUrl,
      incomeSources,
      isPrePopulated
    )
  }

}
