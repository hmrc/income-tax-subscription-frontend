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

package connectors

import javax.inject.{Inject, Singleton}

import audit.Logging
import config.AppConfig
import connectors.models.Enrolment
import play.api.http.Status._
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import common.Constants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EnrolmentConnector @Inject()(appConfig: AppConfig,
                                   val http: HttpGet,
                                   logging: Logging) {

//  def getIncomeTaxSAEnrolment(uri: String)(implicit hc: HeaderCarrier): Future[Option[Enrolment]] = {
//    val getUrl = s"${appConfig.authUrl}$uri/enrolments"
//    lazy val requestDetails: Map[String, String] = Map("uri" -> uri)
//    logging.debug(s"Request:\n$requestDetails")
//    http.GET[HttpResponse](getUrl).map {
//      response =>
//        response.status match {
//          case OK => response.json.as[Seq[Enrolment]].find(_.key == ggServiceName)
//          case _ =>
//            logging.warn("Get Income Tax enrolment responded with a unexpected error")
//            None
//        }
//    }
//  }
  def getEnrolments(uri: String)(implicit hc: HeaderCarrier): Future[Option[Seq[Enrolment]]] = {
    val getUrl = s"${appConfig.authUrl}$uri/enrolments"
    lazy val requestDetails: Map[String, String] = Map("uri" -> uri)
    logging.debug(s"Request:\n$requestDetails")
    http.GET[HttpResponse](getUrl).map {
      response =>
        response.status match {
          case OK => response.json.as[Seq[Enrolment]] match {
            case Nil => None
            case x => Some(x)
          }
          case _ =>
            logging.warn("Get enrolments responded with a unexpected error")
            None
        }
    }
  }

}