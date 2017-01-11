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

package services


import config.SessionCache
import models.BusinessNameModel
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

trait KeystoreService {

  protected val session: SessionCache

  protected def fetch[T](location: String)(implicit hc: HeaderCarrier, reads: Reads[T]): Future[Option[T]] = session.fetchAndGetEntry(location)

  protected def save[T](location: String, obj: T)(implicit hc: HeaderCarrier, reads: Writes[T]): Future[CacheMap] = session.cache(location, obj)

  def fetchAll()(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = session.fetch()

  def deleteAll()(implicit hc: HeaderCarrier): Future[HttpResponse] = session.remove()

  import CacheConstants._

  def fetchBusinessName()(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): Future[Option[BusinessNameModel]] =
    fetch[BusinessNameModel](BusinessName)

  def saveBusinessName(businessNameModel: BusinessNameModel)(implicit hc: HeaderCarrier, reads: Reads[BusinessNameModel]): Future[CacheMap] =
    save[BusinessNameModel](BusinessName, businessNameModel)

}

object KeystoreService extends KeystoreService {
  val session = SessionCache
}

object CacheConstants {
  val BusinessName = "businessNameModel"
}