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

import connectors.RawResponseReads
import javax.inject.{Inject, Singleton}
import testonly.TestOnlyAppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClearPreferencesConnector @Inject()(appConfig: TestOnlyAppConfig,
                                          http: HttpClient)
                                         (implicit ec: ExecutionContext) extends RawResponseReads {

  val clearPreferencesURL: String => String = (nino: String) => appConfig.entityResolverURL + s"/entity-resolver-admin/paye/$nino"

  def clear(nino: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = http.DELETE(clearPreferencesURL(nino))

}
