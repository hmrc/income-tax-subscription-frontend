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

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, ResolverStyle}
import javax.inject.{Inject, Singleton}

import audit.Logging
import connectors.RawResponseReads
import models.DateModel
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import testonly.TestOnlyAppConfig
import testonly.models.UserToStubModel
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }


case class Value(value: String)

object Value {
  implicit val format = Json.format[Value]
}

case class UserData(nino: Value = Value("AA 11 11 11 A"),
                    sautr: Option[Value] = Some(Value("1234567890")),
                    firstName: Value = Value("Test"),
                    lastName: Value = Value("User"),
                    dob: Value = Value("01011980")) {
  //$COVERAGE-OFF$Disabling scoverage on this method as it is only intended to be used by the test only controller

  def toUserToStubModel: UserToStubModel = UserToStubModel(
    firstName = firstName.value,
    lastName = lastName.value,
    nino = nino.value,
    sautr = sautr.map(_.value),
    dateOfBirth = LocalDate.parse(dob.value, UserData.dobFormat): DateModel
  )

  // $COVERAGE-ON$

}

object UserData {

  //$COVERAGE-OFF$Disabling scoverage on these fields and metho as they are only intended to be used by the test only controller

  private val dobFormat = DateTimeFormatter.ofPattern("ddMMuuuu").withResolverStyle(ResolverStyle.STRICT)

  implicit def convert(clientToStubModel: UserToStubModel): UserData = UserData(
    nino = Value(clientToStubModel.ninoFormatted),
    sautr = clientToStubModel.sautr.map(Value.apply),
    firstName = Value(clientToStubModel.firstName),
    lastName = Value(clientToStubModel.lastName),
    dob = Value(clientToStubModel.dateOfBirth.toLocalDate.format(dobFormat))
  )

  // $COVERAGE-ON$

  implicit val format = Json.format[UserData]

}

/*
 * the N.B. in order to make use of the stub the testId must be sent in the header under "True-Client-IP"
 */
case class Request(
                    data: UserData,
                    testId: String = "ITSA",
                    name: String = "CID",
                    service: String = "find",
                    resultCode: Option[Int] = Some(200),
                    delay: Option[Int] = None,
                    timeToLive: Option[Int] = Some(120000 * 60)
                  )

object Request {
  implicit val format = Json.format[Request]
}

import utils.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class MatchingStubConnector @Inject()(appConfig: TestOnlyAppConfig,
                                      http: WSHttp,
                                      logging: Logging) extends RawResponseReads {

  lazy val dynamicStubUrl = appConfig.matchingStubsURL + "/dynamic-cid"

  /*
  *  N.B. This creates a stubbed user via the MatchingStubs service
  *  In order to make use of this user the request must include a "True-Client-IP" header with the same
  *  testId specified by the request.
  *  Currently this is hardcoded in the Request object as "ITSA-AGENT"
  */
  def newUser(userData: UserData)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.POST[Request, HttpResponse](dynamicStubUrl, Request(userData)).flatMap {
      response =>
        response.status match {
          case CREATED =>
            logging.info("MatchingStubConnector.newUser successful")
            true
          case status =>
            logging.warn(
              s"""MatchingStubConnector.newUser failure:
                 | Request {
                 |   dynamicStubUrl: $dynamicStubUrl
                 |   hc.headers: ${hc.headers.map { case (a, b) => s"a$a: $b" }.mkString("\n")}
                 |   json: ${UserData.format.writes(userData): JsValue}
                 | }
                 | Response: status=$status, body=${response.body}""".stripMargin)
            false
        }
    }
  }

}

// $COVERAGE-ON$