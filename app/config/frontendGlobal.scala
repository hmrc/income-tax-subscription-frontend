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

package config

import com.typesafe.config.Config
import config.filters.WhitelistFilter
import net.ceedubs.ficus.Ficus._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Results._
import play.api.mvc.{EssentialFilter, Request, RequestHeader, Result}
import play.api.{Application, Configuration, Logger, Play}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{AuthorisationException, BearerTokenExpired, InsufficientEnrolments}
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.filters.{MicroserviceFilterSupport, RecoveryFilter}
import uk.gov.hmrc.play.frontend.bootstrap.{DefaultFrontendGlobal, ShowErrorPage}
import uk.gov.hmrc.play.http.NotFoundException
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter


object FrontendGlobal
  extends DefaultFrontendGlobal with ShowErrorPage {

  override lazy val auditConnector = new FrontendAuditConnector(Play.current)
  override val loggingFilter = LoggingFilter
  override val frontendAuditFilter = AuditFilter

  // this override removes the RecoveryFilter as the filter auto handles all status Not Found.
  // when upgrading bootstrap please ensure this is up to date without RecoveryFilter.
  override protected lazy val defaultFrontendFilters: Seq[EssentialFilter] = {
    val coreFilters = super.defaultFrontendFilters.filterNot(f => f.equals(RecoveryFilter))
    // this adds the whitelisting filter if it's enabled
    val ipWhitelistKey = "feature-switch.enable-ip-whitelisting"
    Play.current.configuration.getString(ipWhitelistKey).getOrElse(throw new Exception(s"Missing configuration key: $ipWhitelistKey")).toBoolean match {
      case true => coreFilters.:+(new WhitelistFilter(Play.current))
      case _ => coreFilters
    }
  }

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html =
    views.html.templates.error_template(pageTitle, heading, message)(implicitly, implicitly, new FrontendAppConfig(Play.current))

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")

  override def resolveError(rh: RequestHeader, ex: Throwable): Result = {
    ex match {
      case _: InsufficientEnrolments =>
        Logger.debug("[AuthenticationPredicate][async] No HMRC-MTD-IT Enrolment and/or No NINO.")
        super.resolveError(rh, ex)
      case _: BearerTokenExpired =>
        Logger.debug("[AuthenticationPredicate][async] Bearer Token Timed Out.")
        Redirect(controllers.routes.SessionTimeoutController.timeout())
      case _: AuthorisationException =>
        Logger.debug("[AuthenticationPredicate][async] Unauthorised request. Redirect to Sign In.")
        Redirect(controllers.routes.SignInController.signIn())
      case _: NotFoundException =>
        NotFound(notFoundTemplate(Request(rh, "")))
      case _ =>
        super.resolveError(rh, ex)
    }
  }
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object LoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object AuditFilter extends FrontendAuditFilter with RunMode with AppName with MicroserviceFilterSupport {
  def app = Play.current

  override lazy val maskedFormFields = Seq("password")

  override lazy val applicationPort = None

  override lazy val auditConnector = new FrontendAuditConnector(app)

  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}
