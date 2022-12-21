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

package views.individual.matching

import messagelookup.individual.MessageLookup.{AffinityGroup => messages}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.ViewSpecTrait
import views.html.agent.matching.AgentAffinityGroupError

class AgentAffinityGroupErrorViewSpec extends ViewSpecTrait {

  implicit val request: Request[_] = FakeRequest()

  private val agentAffinityGroupErrorView = app.injector.instanceOf[AgentAffinityGroupError]

  lazy val page: HtmlFormat.Appendable = agentAffinityGroupErrorView()(FakeRequest(), implicitly, appConfig)

  "The Affinity Group Error view" should {

    val testPage = TestView(
      name = "Affinity Group Error view",
      title = messages.title,
      heading = messages.heading,
      isAgent = true,
      page = page)

    testPage.mustHavePara(messages.Agent.line1)
    testPage.mustHaveALink(messages.Agent.linkId, messages.Agent.linkText, appConfig.agentSignUpUrl)

  }

}
