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

package views.individual.incometax.eligibility

import assets.MessageLookup.{Base => common, NotEligibleForIncomeTax => messages}
import views.ViewSpecTrait

class NotEligibleForIncomeTaxSpec extends ViewSpecTrait {

  val request = ViewSpecTrait.viewTestRequest

  lazy val page = views.html.individual.incometax.eligibility.not_eligible_for_income_tax()(request, implicitly, appConfig)


  "The Income Tax Not Eligible view" should {

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

