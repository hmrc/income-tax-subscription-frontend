/*
 * Copyright 2021 HM Revenue & Customs
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

//$COVERAGE-OFF$Disabling scoverage on this test only controller as it is only required by our acceptance test

package testonly.controllers.individual

import auth.individual.SignUpController
import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import services.{AuditingService, AuthService}
import services.individual.KnownFactsService
import testonly.form.individual.KnownFactsForm._
import testonly.models.KnownFactsModel
import testonly.views.html.individual.AddKnownFacts

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class KnownFactsController @Inject()(val auditingService: AuditingService,
                                     addKnownFacts: AddKnownFacts,
                                     val authService: AuthService,
                                     knownFactsService: KnownFactsService)
                                    (implicit val ec: ExecutionContext,
                                     val appConfig: AppConfig,
                                     mcc: MessagesControllerComponents) extends SignUpController {

  def view(form: Form[KnownFactsModel])(implicit request: Request[_]): Html =
    addKnownFacts(
      knownFactsForm = form,
      postAction = testonly.controllers.individual.routes.KnownFactsController.submit
    )

  def show: Action[AnyContent] = Action { implicit request =>
    Ok(view(knownFactsForm.form))
  }

  def submit: Action[AnyContent] = Action.async { implicit request =>
    knownFactsForm.form.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(view(form = formWithErrors))),
      knownFacts => {
        import forms.prevalidation.trimAllFunc
        val nino = trimAllFunc(knownFacts.nino).toUpperCase()
        val mtdid = trimAllFunc(knownFacts.mtditid).toUpperCase()

        knownFactsService.addKnownFacts(mtdid, nino).map {
          case Right(_) => Ok(s"known facts added: nino=$nino mtdid=$mtdid")
          case Left(_) => Ok("add known facts failed")
        }
      }
    )

  }

}

// $COVERAGE-ON$
