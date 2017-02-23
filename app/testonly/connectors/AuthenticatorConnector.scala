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

//$COVERAGE-OFF$Disabling scoverage on this test only connector as it is only required by our acceptance test

package testonly.connectors

import com.google.inject.{Inject, Singleton}
import connectors.RawResponseReads
import testonly.TestOnlyAppConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

@Singleton
class AuthenticatorConnector @Inject()(appConfig: TestOnlyAppConfig,
                                       http: WSHttp) extends RawResponseReads {

  lazy val refreshURI = s"${appConfig.authenticatorURL}/authenticator/refresh-profile"

  def refreshProfile()(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http.POSTEmpty[HttpResponse](refreshURI)

}

// $COVERAGE-ON$