/*
 * Copyright 2018 HM Revenue & Customs
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

package assets

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi


class MessagesSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  lazy val allLanguages = app.injector.instanceOf[MessagesApi].messages
  val exclusionKeys = Set(
    "global.error.badRequest400.title",
    "global.error.badRequest400.heading",
    "global.error.badRequest400.message",
    "global.error.pageNotFound404.title",
    "global.error.pageNotFound404.heading",
    "global.error.pageNotFound404.message",
    "global.error.InternalServerError500.title",
    "global.error.InternalServerError500.heading",
    "global.error.InternalServerError500.message",
    "sign-up-complete.title",
    "sign-up-complete.heading",
    "sign-up-complete.whatHappensNext.heading",
    "sign-up-complete.whatHappensNext.number1",
    "sign-up-complete.whatHappensNext.number2",
    "sign-up-complete.whatHappensNext.number3",
    "sign-up-complete.whatHappensNext.number4",
    "sign-up-complete.whatHappensNext.number5",
    "sign-up-complete.whatHappensNext.para1",
    "sign-up-complete.whatHappensNext.bullet1",
    "sign-up-complete.whatHappensNext.bullet2",
    "sign-up-complete.whatHappensNext.para2"
  )

  "the messages file must have welsh translations" should {
    "check all keys in the default file other than those in the exclusion list has a corresponding translation" in {
      val defaults = allLanguages("default")
      val welsh = allLanguages("cy")

      defaults.keys.foreach(
        key =>
          if (!exclusionKeys.contains(key))
            welsh.keys must contain(key)
      )
    }

  }

}
