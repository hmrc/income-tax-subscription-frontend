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

package agent.views

import agent.assets.MessageLookup
import agent.assets.MessageLookup.{Base => common, FrontPage => messages}
import core.views.ViewSpecTrait
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class AgentFrontPageViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall

  lazy val page = agent.views.html.agent_frontpage(
    getAction = action
  )(FakeRequest(), applicationMessages, appConfig)

  lazy val document = Jsoup.parse(page.body)

  "The Agent 'Front/Start Page view" should {

    val testPage = TestView(
      name = "Front/Start Page View",
      title = messages.title,
      heading = messages.heading,
      page = page,
      showSignOutInBanner = false
    )

    testPage.mustHaveParaSeq(
      messages.para_1,
      messages.para_2,
      messages.para_3,
      messages.para_4,
      messages.para_5,
      messages.para_6
    )

    testPage.mustHaveBulletSeq(
      messages.bullet_1,
      messages.bullet_2,
      messages.bullet_3,
      messages.bullet_4,
      messages.bullet_5,
      messages.bullet_6,
      messages.bullet_7,
      messages.bullet_8,
      messages.bullet_9,
      messages.bullet_10

    )

    testPage.mustHaveH2(messages.subHeading_1)

    testPage.mustHaveH2(messages.subHeading_2)

    val form = testPage.getForm("Agent 'Front/Start Page view")(actionCall = action)

    form.mustHaveSubmitButton(common.startNow)

  }

  "has a 'To use this service' section" which {

    s"has an Agent services account link '${MessageLookup.FrontPage.linkText_1}'" in {
      val link1 = document.select("#beforeYouStart a").get(0)
      link1.text() mustBe MessageLookup.FrontPage.linkText_1
      link1.attr("href") mustBe appConfig.agentAccountUrl
    }

    s"has an Agent authorisation link '${MessageLookup.FrontPage.linkText_2}'" in {
      val link2 = document.select("#beforeYouStart a").get(1)
      link2.text() mustBe MessageLookup.FrontPage.linkText_2
      link2.attr("href") mustBe appConfig.agentAuthUrl
    }

  }
}
