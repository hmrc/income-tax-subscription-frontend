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

//$COVERAGE-OFF$Disabling scoverage on this test only controller as it is only required by our acceptance test

package testonly.controllers.agent

import auth.agent.StatelessController
import config.AppConfig

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AuditingService, AuthService}
import uk.gov.hmrc.http.{HttpPatch, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}


/**
  * This controller is used to update the confidence level for users logging in via GG
  * This is necessary as it is currently not possible to set NINO or confidence level in GG Stubs
  * But we need to stub out the enrolment calls which we cannot simulate solely using the auth stubs
  */
@Singleton
class AuthUpdateController @Inject()(val auditingService: AuditingService,
                                     val authService: AuthService,
                                     val appConfig: AppConfig,
                                     http: HttpPatch)
                                    (implicit val ec: ExecutionContext,
                                     mcc: MessagesControllerComponents) extends StatelessController {

  lazy val noAction: Future[String] = Future.successful("no actions taken")
  lazy val updated: Future[Result] = Future.successful(Ok("updated"))

  lazy val updateURL = s"${appConfig.authUrl}/auth/authority"

  val update: Action[AnyContent] = Authenticated.async { implicit user =>
    _ =>
      val confidencePatch = http.PATCH[JsObject, HttpResponse](updateURL, Json.obj("confidenceLevel" -> 200))
      confidencePatch.flatMap(_ => updated)
  }

}

// $COVERAGE-ON$
