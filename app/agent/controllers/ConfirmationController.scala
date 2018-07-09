/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.controllers

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import agent.audit.Logging
import agent.auth.PostSubmissionController
import core.models.DateModel.dateConvert
import agent.services.KeystoreService
import agent.views.html.{confirmation, sign_up_complete}
import core.config.BaseControllerConfig
import core.services.AuthService
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.language.LanguageUtils._

@Singleton
class ConfirmationController @Inject()(val baseConfig: BaseControllerConfig,
                                       val messagesApi: MessagesApi,
                                       val keystoreService: KeystoreService,
                                       val authService: AuthService,
                                       val logging: Logging
                                      ) extends PostSubmissionController {

  val show: Action[AnyContent] = Authenticated { implicit request =>
    implicit user =>
      val submissionDate = dateConvert(LocalDate.now())
      val postAction = agent.controllers.routes.AddAnotherClientController.addAnother()
      val signOutAction = core.controllers.SignOutController.signOut(origin = routes.ConfirmationController.show())

      if (getCurrentLang == Welsh)
        Ok(confirmation(submissionDate, postAction, signOutAction))
      else
        Ok(sign_up_complete(submissionDate, postAction, signOutAction))

  }

}
