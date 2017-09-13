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

package testonly.connectors

import javax.inject.{Inject, Singleton}

import connectors.RawResponseReads
import play.api.http.Status._
import testonly.TestOnlyAppConfig
import utils.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpDelete, HttpGet, HttpResponse }

@Singleton
class ClearPreferencesConnector @Inject()(appConfig: TestOnlyAppConfig,
                                          httpGet: HttpGet,
                                          http: HttpDelete) extends RawResponseReads {

  val getEntityId: String => String = (nino: String) => appConfig.entityResolverURL + s"/entity-resolver/paye/$nino"

  val clearPreferencesURL: String => String = (entityId: String) => appConfig.preferencesURL + s"/preferences-admin/$entityId"

  def clear(nino: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpGet.GET(getEntityId(nino)).flatMap { response =>
      response.status match {
        case OK =>
          val entityId = (response.json \ "_id").as[String]
          http.DELETE(clearPreferencesURL(entityId))
        case failure => response
      }
    }

}
