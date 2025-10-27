/*
 * Copyright 2023 HM Revenue & Customs
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
import views.html.templates.{AgentMainTemplate, PrincipalMainTemplate}

class ServiceNameViewSpec extends ViewSpecTrait {
  private val individualLayout: PrincipalMainTemplate = app.injector.instanceOf[PrincipalMainTemplate]
  private val agentLayout: AgentMainTemplate = app.injector.instanceOf[AgentMainTemplate]

  private def page(isAgent: Boolean): HtmlFormat.Appendable = if (isAgent) {
    agentLayout(
      title = "title"
    )(Html(""))(FakeRequest(), implicitly)
  } else {
    individualLayout(
      title = "title"
    )(Html(""))(FakeRequest(), implicitly)
  }

  private def document(isAgent: Boolean): Document = Jsoup.parse(page(isAgent).body)

  "layout" must {
    "have a service name" when {
      "passing in an individual service name" in {
        val serviceName = "Sign up for Making Tax Digital for Income Tax"
        val serviceUrl = appConfig.govukGuidanceITSASignUpIndivLink
        document(isAgent = false).getElementsByClass("govuk-header__service-name").text() mustBe serviceName
        document(isAgent = false).getElementsByClass("govuk-header__service-name").attr("href") mustBe serviceUrl

      }

      "passing in an agent service name" in {
        val serviceName = "Sign up your clients for Making Tax Digital for Income Tax"
        val serviceUrl = appConfig.govukGuidanceITSASignUpAgentLink
        document(isAgent = true).getElementsByClass("govuk-header__service-name").text() mustBe serviceName
        document(isAgent = true).getElementsByClass("govuk-header__service-name").attr("href") mustBe serviceUrl
      }
    }
  }


}
