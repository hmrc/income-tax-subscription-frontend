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

import javax.inject.{Inject, Singleton}

import common.Constants
import connectors.RawResponseReads
import play.api.libs.json.{JsValue, Json}
import testonly.TestOnlyAppConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future

@Singleton
class DeEnrolmentConnector @Inject()(appConfig: TestOnlyAppConfig,
                                     http: WSHttp) extends RawResponseReads {

  lazy val resetURI = s"${appConfig.ggStubsURL}/test-only/with-refreshed-enrolments/false"

  lazy val deEnrolURI = s"${appConfig.taxEnrolmentsURL}/tax-enrolments/de-enrol/${Constants.mtdItsaEnrolmentName}"

  def resetUsers()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.POSTEmpty[HttpResponse](resetURI)
  }

  def deEnrol()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.POST[JsValue, HttpResponse](deEnrolURI, Json.parse("""{"keepAgentAllocations": true}"""))
  }

}

// $COVERAGE-ON$