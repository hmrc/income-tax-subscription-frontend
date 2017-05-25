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

package controllers

import helpers.ComponentSpecBase
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.Messages

class HomeControllerISpec extends ComponentSpecBase {
  "GET /report-quarterly/income-and-expenses/sign-up" when {
    "feature-switch.show-guidance is true" should {
      "return the guidance page" in {
        val res = IncomeTaxSubscriptionFrontend.startPage

        res.status shouldBe Status.OK
        val document = Jsoup.parse(res.body)

        document.title shouldBe Messages("frontpage.title")
      }
    }
  }
}
