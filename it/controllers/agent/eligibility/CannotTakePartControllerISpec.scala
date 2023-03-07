/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.agent.eligibility

import helpers.agent.ComponentSpecBase
import helpers.agent.servicemocks.AuthStub
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class CannotTakePartControllerISpec extends ComponentSpecBase {

  trait Setup {
    AuthStub.stubAuthSuccess()

    val result: WSResponse = IncomeTaxSubscriptionFrontend.showCannotTakePart
    val doc: Document = Jsoup.parse(result.body)
    val pageContent: Element = doc.mainContent
  }

  object CannotTakePartMessages {
    val title: String = "Your client cannot take part in this pilot yet"
  }

  "GET /client/other-sources-of-income-error" should {

    "return a status of OK" in new Setup {
      result.status mustBe OK
    }

    "return a page" which {

      "has the correct title" in new Setup {
        val serviceNameGovUk = " - Use software to report your client’s Income Tax - GOV.UK"
        doc.title mustBe CannotTakePartMessages.title + serviceNameGovUk
      }

    }
  }
}
