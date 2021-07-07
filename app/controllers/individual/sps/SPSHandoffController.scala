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

package controllers.individual.sps

import auth.individual.AuthPredicate.AuthPredicate
import auth.individual.{IncomeTaxSAUser, StatelessController}
import config.AppConfig
import config.featureswitch.FeatureSwitch.SPSEnabled
import config.featureswitch.FeatureSwitching
import play.api.mvc._
import services.{AuditingService, AuthService}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.http.NotFoundException

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SPSHandoffController @Inject()(
                                      val auditingService: AuditingService,
                                      val authService: AuthService,
                                      val crypto: ApplicationCrypto)
                                    (implicit val ec: ExecutionContext,
                                     val appConfig: AppConfig,
                                     mcc: MessagesControllerComponents) extends StatelessController with FeatureSwitching {

  override val statelessDefaultPredicate: AuthPredicate[IncomeTaxSAUser] = preferencesPredicate


  def redirectToSPS: Action[AnyContent] = {
    Authenticated {
      implicit request =>

        implicit user =>
          if (isEnabled(SPSEnabled)) {
            goToSPS(returnUrl = controllers.individual.sps.routes.SPSCallbackController.callback().absoluteURL(),
              returnLinkText = "I have verified",
              regime = "itsa"
            )(request)
          } else {
            throw new NotFoundException("[SPSHandoffController][redirectToSPS] - SPS FS is not enabled")
          }
    }
  }

  private def encryptAndEncodeString(s: String): String = {
    URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(s)).value, "UTF-8")
  }

  def goToSPS(returnUrl: String, returnLinkText: String, regime: String)
             (implicit request: Request[AnyContent]): Result = {

    val encryptedReturnUrl = encryptAndEncodeString(returnUrl)
    val encryptedReturnLinkText = encryptAndEncodeString(returnLinkText)
    val encryptedRegime = encryptAndEncodeString(regime)

    Redirect(s"http://localhost:9024/paperless/choose/capture?returnUrl=$encryptedReturnUrl&returnLinkText=$encryptedReturnLinkText&regime=$encryptedRegime")

  }

}
