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

package views

import assets.MessageLookup.{UnplannedOutage => messages}
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class UnplannedOutageSpec extends ViewSpecTrait {

  lazy val page = views.html.unplanned_outage()(
    FakeRequest(),
    applicationMessages,
    appConfig
  )

  "The Unplanned outage view" must {

    val testPage = TestView(
      name = "Unplanned outage View",
      title = messages.title,
      heading = messages.heading,
      page = page,
      showSignOutInBanner = false
    )

    testPage.mustHaveParaSeq(
      messages.line1,
      messages.line2
    )

    testPage.mustHaveBulletSeq(
      messages.bullet1,
      messages.bullet2
    )

    testPage.mustHaveALink("main", messages.link1, appConfig.unplannedOutagePageMainContent)
    testPage.mustHaveALink("related", messages.link2, appConfig.unplannedOutagePageRelatedContent)

  }

}

