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

package views.eligibility.agent

import messagelookup.agent.MessageLookup._
import models.DateModel
import models.common.AccountingPeriodModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.eligibility.agent.SigningUp

class AgentSigningUpControllerViewSpec extends ViewSpec {

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
        title = Heading.heading,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = false,
        error = None)

      "has main content" which {
        "has a heading section" which {
          "has a heading" in {
            mainContent.getH1Element.text mustBe Heading.heading
          }
        }

        "has a before sign up section" which {
          "has a heading" in {
            mainContent.selectHead("h2").text mustBe BeforeSignUp.heading
          }

          "has a first paragraph" in {
            mainContent.selectNth("p", 1).text mustBe BeforeSignUp.paraOne
          }

          "has a second paragraph" in {
            mainContent.selectNth("p", 2).text mustBe BeforeSignUp.paraTwo
          }

          "has a third paragraph" in {
            mainContent.selectNth("p", 3).text mustBe BeforeSignUp.paraThree
          }
        }

        "has a accounting period section" which {
          "has a heading" in {
            mainContent.selectNth("h2", 2).text mustBe AccountingPeriod.heading
          }

          "has a first paragraph" in {
            mainContent.selectNth("p", 4).text mustBe AccountingPeriod.paraOne
          }

          def bulletList: Element = mainContent.selectNth("ul", 1)

          "has a bullet point one" in {
            bulletList.selectHead("li").text mustBe AccountingPeriod.bulletOne
          }

          "has a bullet point two" in {
            bulletList.selectNth("li", 2).text mustBe AccountingPeriod.bulletTwo
          }

          "has a second paragraph" in {
            mainContent.selectNth("p",5).text mustBe AccountingPeriod.paraTwo
          }
        }

        "has a check sign up section" which {
          "has a heading" in {
            mainContent.selectNth("h2", 3).text mustBe CheckSignUp.heading
          }

          "has a first paragraph" in {
            mainContent.selectNth("p", 6).text mustBe CheckSignUp.paraOne
          }

          def bulletList: Element = mainContent.selectNth("ul", 2)

          "has a bullet point one" in {
            bulletList.selectHead("li").text mustBe CheckSignUp.bulletOne
          }

          "has a bullet point two" in {
            bulletList.selectNth("li", 2).text mustBe CheckSignUp.bulletTwo
          }

          "has a bullet point three" in {
            bulletList.selectNth("li", 3).text mustBe CheckSignUp.bulletThree
          }

          "has a second paragraph" in {
            mainContent.selectNth("p", 7).text mustBe CheckSignUp.paraTwo
          }
        }

        "has a continue button" in {
          document.select("button").last().text mustBe Base.continue
        }

        "has the correct form" in {
          document.getForm must have(
            method("POST"),
            action("/test-url")
          )
        }
      }
    }
  }
