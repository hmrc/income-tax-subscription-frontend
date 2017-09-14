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

package connectors.preferences

import javax.inject.Inject

import config.AppConfig
import connectors.httpparsers.PaperlessPreferenceTokenResultHttpParser._
import connectors.models.preferences.PaperlessPreferenceTokenResult._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class PaperlessPreferenceTokenConnector @Inject()(httpPost: HttpPost,
                                                  appConfig: AppConfig) {

  def storeNino(token: String, nino: String)(implicit hc: HeaderCarrier): Future[PaperlessPreferenceTokenResult] = {
    httpPost.POST[JsObject, PaperlessPreferenceTokenResult](
      url = appConfig.storeNinoUrl(token),
      body = Json.obj("nino" -> nino)
    )
  }

}

