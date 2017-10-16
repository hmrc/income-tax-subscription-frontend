/*
 * Copyright 2017 HM Revenue & Customs
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
package testonly.controllers

import javax.inject.{Inject, Singleton}

import auth.SignUpController
import config.BaseControllerConfig
import incometax.subscription.services.KnownFactsService
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import services.AuthService
import testonly.forms.KnownFactsForm._
import testonly.models.KnownFactsModel

import scala.concurrent.Future

@Singleton
class KnownFactsController @Inject()(val baseConfig: BaseControllerConfig,
                                     val messagesApi: MessagesApi,
                                     val authService: AuthService,
                                     knownFactsService: KnownFactsService
                                    ) extends SignUpController {

  def view(form: Form[KnownFactsModel])(implicit request: Request[_]): Html =
    testonly.views.html.add_known_facts(
      knownFactsForm = form,
      postAction = testonly.controllers.routes.KnownFactsController.submit
    )

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Ok(view(knownFactsForm.form)))
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      knownFactsForm.form.bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(view(form = formWithErrors))),
        knownFacts => {
          import forms.prevalidation.trimAllFunc
          val nino = trimAllFunc(knownFacts.nino).toUpperCase()
          val mtdid = trimAllFunc(knownFacts.mtditid).toUpperCase()

          knownFactsService.addKnownFacts(mtdid, nino).map {
            case Right(_) => Ok(s"known facts added: nino=$nino mtdid=$mtdid")
          }.recoverWith {
            case e =>
              Future.successful(Ok("add known facts failed: " + e))
          }
        }
      )

  }

}

// $COVERAGE-ON$
