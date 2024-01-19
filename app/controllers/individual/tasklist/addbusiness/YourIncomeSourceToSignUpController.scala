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
import models.common.business.SelfEmploymentData
import models.common.{OverseasPropertyModel, PropertyModel}
import play.api.mvc._
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.tasklist.addbusiness.YourIncomeSourceToSignUp
import utilities.UserMatchingSessionUtil.{ClientDetails, UserMatchingSessionRequestUtil}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YourIncomeSourceToSignUpController @Inject()(yourIncomeSourceToSignUp: YourIncomeSourceToSignUp,
                                                   val subscriptionDetailsService: SubscriptionDetailsService,
                                                   val auditingService: AuditingService,
                                                   val authService: AuthService)
                                                  (implicit val ec: ExecutionContext,
                                                   val appConfig: AppConfig,
                                                   mcc: MessagesControllerComponents) extends SignUpController with ReferenceRetrieval {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        subscriptionDetailsService.fetchAllIncomeSources(reference) map { incomeSources =>
          Ok(view(
            selfEmployments = incomeSources.selfEmployments,
            ukProperty = incomeSources.ukProperty,
            foreignProperty = incomeSources.foreignProperty
          ))
        }
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        val continue: Result = Redirect(controllers.individual.tasklist.routes.TaskListController.show())

        subscriptionDetailsService.fetchAllIncomeSources(reference) flatMap { incomeSources =>
          if (incomeSources.isComplete){
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

  private def view(selfEmployments: Seq[SelfEmploymentData],
                   ukProperty: Option[PropertyModel],
                   foreignProperty: Option[OverseasPropertyModel])(implicit request: Request[_]): Html =
    yourIncomeSourceToSignUp(
      postAction = routes.YourIncomeSourceToSignUpController.submit,
      backUrl = backUrl,
      selfEmployments,
      ukProperty,
      foreignProperty
    )

}
