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

/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.individual.business

import auth.individual.SignUpController
import config.AppConfig
import controllers.utils.ReferenceRetrieval
import forms.individual.business.UkPropertyCountForm
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService, SubscriptionDetailsService}
import uk.gov.hmrc.http.InternalServerException
import views.html.individual.incometax.business.UkPropertyCount

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UkPropertyCountController @Inject()(val auditingService: AuditingService,
                                          ukPropertyCount: UkPropertyCount,
                                          val authService: AuthService,
                                          val subscriptionDetailsService: SubscriptionDetailsService)
                                         (implicit val ec: ExecutionContext,
                                          val appConfig: AppConfig,
                                          mcc: MessagesControllerComponents) extends SignUpController with ReferenceRetrieval {

  def view(countForm: Form[Int], isEditMode: Boolean)
          (implicit request: Request[_]): Html = {
    ukPropertyCount(
      countForm = countForm,
      postAction = controllers.individual.business.routes.UkPropertyCountController.submit(editMode = isEditMode),
      isEditMode,
      backUrl = backUrl(isEditMode)
    )
  }

  def show(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        subscriptionDetailsService.fetchUkPropertyCount(reference).map { count =>
          Ok(view(
            countForm = UkPropertyCountForm.form.fill(count),
            isEditMode = isEditMode
          ))
        }
      }
  }

  def submit(isEditMode: Boolean): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      withReference { reference =>
        UkPropertyCountForm.form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(countForm = formWithErrors, isEditMode = isEditMode))),
          count => {
            subscriptionDetailsService.saveUkPropertyCount(reference, count) map {
              case Right(_) =>
                if (isEditMode) {
                  Redirect(controllers.individual.business.routes.PropertyCheckYourAnswersController.show(isEditMode))
                } else {
                  Redirect(controllers.individual.business.routes.PropertyAccountingMethodController.show(isEditMode))
                }
              case Left(_) => throw new InternalServerException("[PropertyCountController][submit] - Could not save number of properties")
            }
          }
        )
      }
  }

  def backUrl(isEditMode: Boolean): String = {
    if (isEditMode) {
      controllers.individual.business.routes.PropertyCheckYourAnswersController.show(editMode = true).url
    } else {
      controllers.individual.business.routes.PropertyStartDateController.show().url
    }
  }
}
