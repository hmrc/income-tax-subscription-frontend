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

package utils

import auth.MockConfig
import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.i18n.MessagesApi
import play.twirl.api.Html

import scala.concurrent.{ExecutionContextExecutor, Future}
import uk.gov.hmrc.http.HeaderCarrier


trait UnitTestTrait extends PlaySpec with GuiceOneServerPerSuite with Implicits {

  implicit val executionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.Implicits.global

  implicit val hc = HeaderCarrier()

  implicit def futureWrapperUtil[T](value: T): Future[T] = Future.successful(value)

  implicit def futureWrapperUtil[T](err: Throwable): Future[T] = Future.failed(err)

  implicit def futureOptionWrapperUtil[T](value: T): Future[Option[T]] = Future.successful(value)

  implicit class HtmlFormatUtil(html: Html) {
    def doc: Document = Jsoup.parse(html.body)
  }

  implicit val appConfig: AppConfig = MockConfig

  implicit lazy val messagesApi = app.injector.instanceOf[MessagesApi]

}
