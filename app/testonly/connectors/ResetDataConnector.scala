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

package testonly.connectors

import play.api.http.Status.OK
import testonly.TestOnlyAppConfig
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ResetDataConnector @Inject()(appConfig: TestOnlyAppConfig,
                                   http: HttpClient)
                                  (implicit ec: ExecutionContext) {

  def resetDataUrl(utr: String): String = s"${appConfig.protectedMicroServiceTestOnlyUrl}/remove-data/$utr"

  def reset(utr: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    http.DELETE[HttpResponse](resetDataUrl(utr)) map { response =>
      response.status match {
        case OK => true
        case _ => false
      }
    }
}
