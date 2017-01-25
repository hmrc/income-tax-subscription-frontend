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

package connectors

import javax.inject.{Inject, Singleton}

import enums.IdentityVerificationResult.IdentityVerificationResult
import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}

import scala.concurrent.Future

@Singleton
class IdentityVerificationConnector @Inject()(val http: HttpGet) extends ServicesConfig {
  val serviceUrl: String = baseUrl("identity-verification")

  private def url(journeyId: String) = s"$serviceUrl/mdtp/journey/journeyId/$journeyId"

  private[connectors] case class IdentityVerificationResponse(result: IdentityVerificationResult)

  private implicit val formats = Json.format[IdentityVerificationResponse]

  def identityVerificationResponse(journeyId: String)(implicit hc: HeaderCarrier): Future[IdentityVerificationResult] = {
    val ivFuture = http.GET[HttpResponse](url(journeyId)).flatMap { httpResponse =>
      httpResponse.json.validate[IdentityVerificationResponse].fold(
        errs => Future.failed(new JsonValidationException(s"Unable to deserialise: ${formatJsonErrors(errs)}")),
        valid => Future.successful(valid.result)
      )
    }

    ivFuture onFailure {
      case e: Exception =>
        Logger.warn(s"[connectors][IdentityVerificationConnector] - IV Exception. ${e.getMessage}")
    }

    ivFuture
  }

  private def formatJsonErrors(errors: Seq[(JsPath, Seq[ValidationError])]): String = {
    errors.map(p => p._1 + " - " + p._2.map(_.message).mkString(",")).mkString(" | ")
  }
}

private[connectors] class JsonValidationException(message: String) extends Exception(message)
