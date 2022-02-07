/*
 * Copyright 2022 HM Revenue & Customs
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

package views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.twirl.api.{Html, HtmlFormat}
import views.html.templates.GovUkWrapper

class ServiceNameViewSpec extends ViewSpecTrait {


  val layout: GovUkWrapper = app.injector.instanceOf[GovUkWrapper]

  class Setup(serviceName: String, serviceUrl: String) {

    val page: HtmlFormat.Appendable = layout(
      title = "title",
      serviceName = serviceName,
      serviceUrl = serviceUrl,
      optForm = None,
      backLink = Some("backUrl"),
      showSignOutLink = false
    )(Html(""))(FakeRequest(), implicitly, appConfig)

    val document: Document = Jsoup.parse(page.body)
  }

  "layout" must {
    "have a service name" when {
      "passing in an individual service name" in new Setup("Use software to send Income Tax updates",
        appConfig.govukGuidanceITSASignUpIndivLink) {
        val serviceName = "Use software to send Income Tax updates"
        val serviceUrl = appConfig.govukGuidanceITSASignUpIndivLink
        document.getElementsByClass("hmrc-header__service-name").text() mustBe serviceName
        document.getElementsByClass("hmrc-header__service-name--linked").attr("href") mustBe serviceUrl

      }

      "passing in an agent service name" in new Setup("Use software to report your client’s Income Tax",
        appConfig.govukGuidanceITSASignUpAgentLink) {
        val serviceName = "Use software to report your client’s Income Tax"
        val serviceUrl = appConfig.govukGuidanceITSASignUpAgentLink
        document.getElementsByClass("hmrc-header__service-name").text() mustBe serviceName
        document.getElementsByClass("hmrc-header__service-name--linked").attr("href") mustBe serviceUrl
      }
    }
  }


  }
