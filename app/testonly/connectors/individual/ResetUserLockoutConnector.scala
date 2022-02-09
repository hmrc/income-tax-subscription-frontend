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

package testonly.connectors.individual

import config.AppConfig
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ResetUserLockoutConnector @Inject()(val appConfig: AppConfig,
                                          val http: HttpClient
                                         )(implicit ec: ExecutionContext) {

  lazy val resetUrl: String = appConfig.userMatchingUrl + ResetUserLockoutConnector.resetUri

  def resetLockout(implicit hc: HeaderCarrier): Future[HttpResponse] = http.GET[HttpResponse](resetUrl)

}

object ResetUserLockoutConnector {
  def resetUri: String = s"/test-only/reset-agent-lockout"
}

