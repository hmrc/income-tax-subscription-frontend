/*
 * Copyright 2020 HM Revenue & Customs
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

package agent.views.eligibility

import assets.MessageLookup.{Base => common, AgentNotEligibleForIncomeTax => messages}
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._

class AgentNotEligibleForIncomeTaxSpec extends ViewSpecTrait {

  val request = ViewSpecTrait.viewTestRequest

  lazy val page = agent.views.html.eligibility.not_eligible_for_income_tax()(request, applicationMessages, appConfig)


  "The Income Tax Not Eligible view for Agents" should {

    val testPage = TestView(
      name = "Cannot Use Service View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHavePara(messages.para1)

    testPage.mustHaveSignOutButton(common.signOut, request.path)

  }

}

