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

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec

class CacheExpiryDateProviderTest extends UnitSpec with GuiceOneAppPerSuite {

  "format date" should {
    "return the date in the correct format" in {
      implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
      val provider = app.injector.instanceOf[CacheExpiryDateProvider]
      val aDate = new DateTime(1980, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC)
      provider.format(aDate) should be("Tuesday, 1 January 1980")
    }

    "also handle Welsh language translation" in {
      implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest().withCookies(Cookie("PLAY_LANG", "cy")))
      val provider = app.injector.instanceOf[CacheExpiryDateProvider]
      val aDate = new DateTime(1980, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC)
      provider.format(aDate) should be("Dydd Mawrth, 1 Ionawr 1980")
    }
  }

  "expiryDateOf" should {
    "return the date 30 days in the future in the correct format" in {
      implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
      val provider = app.injector.instanceOf[CacheExpiryDateProvider]
      val aDate = new DateTime(2021, 10, 8, 0, 0, 0, 0, DateTimeZone.UTC)
      provider.expiryDateOf(aDate) should be("Sunday, 7 November 2021")
    }
  }

}
