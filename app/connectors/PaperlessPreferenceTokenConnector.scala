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

package connectors

import core.config.AppConfig
import PaperlessPreferenceTokenResultHttpParser._
import digitalcontact.models.PaperlessPreferenceTokenResult.PaperlessPreferenceTokenResult
import javax.inject.Inject
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class PaperlessPreferenceTokenConnector @Inject()(http: HttpClient,
                                                  appConfig: AppConfig) {

  def storeNino(token: String, nino: String)(implicit hc: HeaderCarrier): Future[PaperlessPreferenceTokenResult] = {
    http.POST[JsObject, PaperlessPreferenceTokenResult](
      url = appConfig.storeNinoUrl(token),
      body = Json.obj("nino" -> nino)
    )
  }

}

