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

package connectors.agent

import config.AppConfig
import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeTaxSessionDataConnector @Inject()(appConfig: AppConfig,
                                              http: HttpClientV2)
                                             (implicit ec: ExecutionContext) extends Logging {

  def setupViewAndChangeSessionData(mtdid: String, nino: String, utr: String)
                                   (implicit hc: HeaderCarrier): Future[Boolean] = {
    import IncomeTaxSessionDataConnector.HttpReads

    http.post(url"${appConfig.getIncomeTaxSessionDataHost}/income-tax-session-data").withBody(Json.obj(
      "mtditid" -> mtdid,
      "nino" -> nino,
      "utr" -> utr
    )).execute map { status =>
      status / 100 == 2
    }
  }
}

object IncomeTaxSessionDataConnector {

  implicit object HttpReads extends HttpReads[Int] {
    override def read(method: String, url: String, response: HttpResponse): Int = {
      response.status
    }
  }

}