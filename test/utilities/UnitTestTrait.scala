/*
 * Copyright 2021 HM Revenue & Customs
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

package utilities

import config.{AppConfig, MockConfig}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.{Configuration, Environment}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.{ExecutionContext, Future}

trait UnitTestTrait extends PlaySpec with GuiceOneServerPerSuite with Implicits with I18nSupport {

  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockLanguageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]

  implicit def futureWrapperUtil[T](value: T): Future[T] = Future.successful(value)

  implicit def futureWrapperUtil[T](err: Throwable): Future[T] = Future.failed(err)

  implicit def futureOptionWrapperUtil[T](value: T): Future[Option[T]] = Future.successful(value)

  implicit class HtmlFormatUtil(html: Html) {
    def doc: Document = Jsoup.parse(html.body)
  }

  implicit val appConfig: AppConfig = MockConfig

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val env: Environment = app.injector.instanceOf[Environment]

  implicit lazy val mockMessages: Messages = messagesApi.preferred(FakeRequest())

  implicit lazy val mockMessagesControllerComponents: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

}
