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

package testonly.controllers

import config.AppConfig
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.AuthService
import testonly.connectors.EnrolmentStoreStubConnector
import testonly.form.UpdateEnrolmentsForm
import testonly.views.html.individual.update_enrolments
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}


class UpdateEnrolmentsController @Inject()(mcc: MessagesControllerComponents,
                                           authService: AuthService,
                                           enrolmentStoreStubConnector: EnrolmentStoreStubConnector)
                                          (implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(mcc){

  import authService._

  def show: Action[AnyContent] = Action.async(implicit req =>
    authorised().retrieve(Retrievals.credentials) {
      case Some(Credentials(credId, _)) =>
        Future.successful(Ok(update_enrolments(
          UpdateEnrolmentsForm.updateEnrolmentsForm.fill(credId),
          testonly.controllers.routes.UpdateEnrolmentsController.submit()
        )))
      case _ => throw new InternalServerException("[UpdateEnrolmentsController][show] could not retrieve credentials from auth")
    }
  )

  def submit: Action[AnyContent] = Action.async(implicit req =>
    UpdateEnrolmentsForm.updateEnrolmentsForm.bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(update_enrolments(
          formWithErrors,
          testonly.controllers.routes.UpdateEnrolmentsController.submit()
        ))),
      credentialId => for {
        _ <- enrolmentStoreStubConnector.updateEnrolments(credentialId)
      } yield Ok("Enrolment updated")
    )

  )
}
