/*
 * Copyright 2022 HM Revenue & Customs
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

import models.{Activated, PaperlessState, Unset}
import play.api.http.Status.{OK, PRECONDITION_FAILED}
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.HttpResponse
import utilities.HttpResult.{HttpConnectorError, HttpResult, HttpResultParser}

object PaperlessPreferenceHttpParser {
  val optedInKey = "optedIn"
  val redirectUserTo = "redirectUserTo"

  implicit object PaperlessPreferenceHttpReads extends HttpResultParser[PaperlessState] {
    override def read(method: String, url: String, response: HttpResponse): HttpResult[PaperlessState] = {
      response.status match {
        case OK =>
          parseJson(response.json) match {
            case JsSuccess(paperlessResponse, _) => Right(paperlessResponse)
            case error: JsError => Left(HttpConnectorError(response, Some(error)))
          }

        case PRECONDITION_FAILED =>
          (response.json \ redirectUserTo).validate[String] match {
            case JsSuccess(redirectUrl, _) => Right(Unset(redirectUrl))
            case error: JsError => Left(HttpConnectorError(response, Some(error)))
          }

        case _ => Left(HttpConnectorError(response))
      }
    }
  }

  private def parseJson(json: JsValue) = for {
    optedIn <- (json \ optedInKey).validate[Boolean]
    redirectUrl <-
      if (optedIn) JsSuccess("")
      else (json \ redirectUserTo).validate[String]
  } yield if (optedIn) Activated
  else Unset(redirectUrl)
}
