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

package controllers.individual.claimenrolment.sps

import auth.individual.BaseClaimEnrolmentController
import config.AppConfig
import play.api.mvc._
import services.{AuditingService, AuthService}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SPSHandoffForClaimEnrolController @Inject()(
                                                   val auditingService: AuditingService,
                                                   val authService: AuthService,
                                                   val crypto: ApplicationCrypto)
                                                 (implicit val ec: ExecutionContext,
                                                  val appConfig: AppConfig,
                                                  mcc: MessagesControllerComponents) extends BaseClaimEnrolmentController {


  def redirectToSPS: Action[AnyContent] = {
    Authenticated {
      _ =>
        _ =>
          goToSPS(returnUrl = appConfig.baseUrl + controllers.individual.claimenrolment.sps.routes.SPSCallbackForClaimEnrolController.callback,
            returnLinkText = "I have verified",
            regime = "itsa"
          )
    }
  }

  private def encryptAndEncodeString(s: String): String = {
    URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(s)).value, "UTF-8")
  }

  def goToSPS(returnUrl: String, returnLinkText: String, regime: String): Result = {

    val encryptedReturnUrl = encryptAndEncodeString(returnUrl)
    val encryptedReturnLinkText = encryptAndEncodeString(returnLinkText)
    val encryptedRegime = encryptAndEncodeString(regime)

    Redirect(s"${appConfig.preferencesFrontendRedirect}/paperless/choose/capture?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnLinkText&regime=$encryptedRegime")

  }

}
