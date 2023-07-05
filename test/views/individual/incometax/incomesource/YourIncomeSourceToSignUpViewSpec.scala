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

package views.individual.incometax.incomesource

import config.featureswitch.FeatureSwitch.ForeignProperty
import forms.individual.incomesource.BusinessIncomeSourceForm.incomeSourceKey
import models.IncomeSourcesStatus
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.FormError
import play.api.mvc.Call
import play.twirl.api.Html
import utilities.ViewSpec
import views.ViewSpecTrait
import views.html.individual.incometax.incomesource.YourIncomeSourceToSignUp


class YourIncomeSourceToSignUpViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ForeignProperty)
  }

  object IndividualIncomeSource {
    val title = "Your income sources"
    val heading: String = title
    val paragraph1: String = "You only need to fill in the sections that apply to you."
    val business = "Sole trader (self-employed)"
    val businessHint = "You’re self-employed if you run your own business as an individual and work for yourself. This is also known as being a ‘sole trader’. If you work through a limited company, you’re not a sole trader."
    val ukProperty = "UK property business"
    val ukPropertyHint = "A UK property business is when you get income from land or buildings in the UK. For instance, letting houses, flat or holiday homes either on a long or short term basis."
    val ukPropertyHint2 = "You can include multiple properties or addresses in one UK property business."
    val ukPropertyLink = "/report-quarterly/income-and-expenses/sign-up/business/property-commencement-date"
    val foreignProperty = "Foreign property business"
    val foreignPropertyHint = "A foreign property business is when you get income from land or buildings in another country. For instance, letting houses, flat or holiday homes either on a long or short term basis."
    val foreignPropertyHint2 = "You can include multiple properties or addresses in one foreign property business."
    val foreignPropertyLink = "/report-quarterly/income-and-expenses/sign-up/business/overseas-property-start-date"
  }

  val backUrl: String = ViewSpecTrait.testBackUrl

  val action: Call = ViewSpecTrait.testCall

  val incomeSource: YourIncomeSourceToSignUp = app.injector.instanceOf[YourIncomeSourceToSignUp]

  val testFormError: FormError = FormError(incomeSourceKey, "error.business-income-source.all-sources")

  def view(hasError: Boolean = false): Html = {
    incomeSource(
      backUrl = testBackUrl
    )
  }

  class ViewTest(
                  incomeSourcesStatus: IncomeSourcesStatus = IncomeSourcesStatus(
                    selfEmploymentAvailable = true,
                    ukPropertyAvailable = true,
                    overseasPropertyAvailable = true
                  ),
                  hasError: Boolean = false,
                  overseasEnabled: Boolean = false
                ) {

    if (overseasEnabled) enable(ForeignProperty)

    val document: Document = Jsoup.parse(view(
      hasError = hasError
    ).body)

  }

  "IncomeSource view" should {
    "there is no error" in new TemplateViewTest(
      view = view(
      ),
      title = IndividualIncomeSource.title,
      isAgent = false,
      backLink = Some(testBackUrl),
    )
  }


  "have the heading for the page" in new ViewTest {
    document.selectHead("h1").text mustBe IndividualIncomeSource.heading
  }

  "has information for the user that advises they only need to fill in sections that apply to them" in new ViewTest {
    document.mainContent.getNthParagraph(1).text mustBe IndividualIncomeSource.paragraph1
  }

  "has a section on Sole trader" which {
    "has the sole trader paragraph heading for the section" in new ViewTest {
      document.mainContent.getElementById("heading-self-employed").text mustBe IndividualIncomeSource.business
    }

    "has sole trader hint text" in new ViewTest(overseasEnabled = true) {
      document.mainContent.selectNth("p", 2).text() mustBe IndividualIncomeSource.businessHint
    }

    "has a link to self-employed page" in new ViewTest {
      document.mainContent.getLinkNth().attr("href") mustBe appConfig.incomeTaxSelfEmploymentsFrontendInitialiseUrl
    }
  }


  "has a section on Uk property" which {
    "have the UK property paragraph heading for the section" in new ViewTest {
      document.mainContent.getElementById("heading-uk-property").text mustBe IndividualIncomeSource.ukProperty
    }

    "has Uk property  hint text" in new ViewTest(overseasEnabled = true) {
      document.mainContent.selectNth("p", 3).text() mustBe IndividualIncomeSource.ukPropertyHint
    }
    "has Uk property  hint2  text" in new ViewTest(overseasEnabled = true) {
      document.mainContent.selectNth("p", 4).text() mustBe IndividualIncomeSource.ukPropertyHint2
    }

    "has a link to property commencement page" in new ViewTest {
      document.mainContent.getLinkNth(1).attr("href") mustBe IndividualIncomeSource.ukPropertyLink
    }
  }


  "has a section on Foreign property" which {
    "have the UK property paragraph heading for the section" in new ViewTest(overseasEnabled = true) {
      document.mainContent.getElementById("heading-foreign-property").text mustBe IndividualIncomeSource.foreignProperty
    }

    "has foreign property  hint text" in new ViewTest(overseasEnabled = true) {
      document.mainContent.selectNth("p", 5).text() mustBe IndividualIncomeSource.foreignPropertyHint
    }

    "has foreign property  hint2 text" in new ViewTest(overseasEnabled = true) {
      document.mainContent.selectNth("p", 6).text() mustBe IndividualIncomeSource.foreignPropertyHint2
    }

    "has a link to foreign property start date page" in new ViewTest(overseasEnabled = true) {
      document.mainContent.getLinkNth(2).attr("href") mustBe IndividualIncomeSource.foreignPropertyLink
    }
  }

}

