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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import services.AuthService

import scala.concurrent.Future
import uk.gov.hmrc.http.HttpPatch


/**
  * This controller is used to update the confidence level for users logging in via GG
  * This is necessary as it is currently not possible to set NINO or confidence level in GG Stubs
  * But we need to stub out the enrolment calls which we cannot simulate solely using the auth stubs
  */
@Singleton
class AuthUpdateController @Inject()(val baseConfig: BaseControllerConfig,
                                     val messagesApi: MessagesApi,
                                     val http: HttpPatch,
                                     val authService: AuthService
                                    ) extends SignUpController with I18nSupport {

  lazy val noAction = Future.successful("no actions taken")
  lazy val updated = Future.successful(Ok("updated"))

  lazy val updateURL = s"${baseConfig.applicationConfig.authUrl}/auth/authority"

  val update = Authenticated.async { implicit request =>
    implicit user =>
      val confidencePatch = http.PATCH(updateURL, Json.obj("confidenceLevel" -> 200))
      confidencePatch.flatMap(_ => updated)
  }

}

// $COVERAGE-ON$
