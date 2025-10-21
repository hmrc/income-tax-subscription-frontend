/*
 * Copyright 2025 HM Revenue & Customs
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

package views.eligibility.individual

import messagelookup.individual.MessageLookup._
import models.DateModel
import models.common.AccountingPeriodModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.eligibility.individual.SigningUp

class IndividualSigningUpControllerViewSpec extends ViewSpec {

  implicit val testMessages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  private val signingUp: SigningUp = app.injector.instanceOf[SigningUp]

  private val startDate: DateModel = DateModel(getRandomDate, "4", "2010")

  private def getRandomDate = (Math.random() * 10 + 1).toInt.toString

  private val endDate: DateModel = DateModel(getRandomDate, "4", "2011")

  val testAccountingPeriodModel: AccountingPeriodModel = AccountingPeriodModel(startDate, endDate)

  val document: Document = Jsoup.parse(page.body)
  val mainContent: Element = document.mainContent

  def page: Html = {
    signingUp(
      postAction = testCall,
      backUrl = testBackUrl,
      currentTaxYear = testAccountingPeriodModel,
      nextTaxYear = testAccountingPeriodModel)
  }

    "return a view" which {

      "uses the correct template details" in new TemplateViewTest(view = page,
        title = IndividualSignUpTerms.heading,
        isAgent = false,
        backLink = Some(testBackUrl),
        hasSignOutLink = false,
        error = None)


        "has a main heading" in {
          mainContent.getH1Element.text mustBe IndividualSignUpTerms.heading
        }

        "has a before you sign up section" that {
          import IndividualSignUpTerms.beforeYouSignUp._

          "has a heading" in {
            mainContent.selectHead("h2").text mustBe heading
          }

          "Has first paragraph" in {
            mainContent.selectNth("p", 1).text mustBe paraOne
          }

          "has a second paragraph" in {
            mainContent.selectNth("p", 2).text mustBe paraTwo
          }

        }

        "has a sole trader section" that {
          import IndividualSignUpTerms.soleTrader._

          "has a heading" in {
            mainContent.selectHead("h3").text mustBe heading
          }

          "has a first paragraph" in {
            mainContent.selectNth("p", 3).text mustBe paraOne
          }

          "has a second paragraph" in {
            mainContent.selectNth("p", 4).text mustBe paraTwo
          }
        }

        "has a income from properties section" that {
          import IndividualSignUpTerms.incomeProperty._

          "has a heading" in {
            mainContent.selectNth("h3", 2).text mustBe heading
          }

          "has a paragraph" in {
            mainContent.selectNth("p", 5).text mustBe paraOne
          }
        }

        "has an identity verification section" that {
          import IndividualSignUpTerms.identityVerification._
          "has a heading" in {
            mainContent.selectNth("h2", 2).text mustBe heading
          }
          "has a paragraph" in {
            mainContent.selectNth("p", 6).text mustBe paraOne
          }
          "has a bullet list" which {
            val bulletList: Element = mainContent.selectHead("ul")
            "has a first point" in {
              bulletList.selectNth("li", 1).text mustBe bulletOne

            }
            "has a second point" in {
              bulletList.selectNth("li", 2).text mustBe bulletTwo
            }
          }
        }

        "has a form" which {
          def form: Element = document.getForm

          "has the correct form attributes" in {
            form must have(
              method("POST"),
              action("/test-url")
            )
          }
          "has a continue button" in {
            form.selectHead("button").text mustBe Base.continue
          }
        }

      }
  }
