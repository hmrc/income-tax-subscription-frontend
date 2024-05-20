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

package views.agent

import models.common.business._
import models.common.{IncomeSources, OverseasPropertyModel, PropertyModel}
import models.{Cash, DateModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.twirl.api.Html
import utilities.UserMatchingSessionUtil.ClientDetails
import utilities.ViewSpec
import views.html.agent.tasklist.addbusiness.YourIncomeSourceToSignUp

//scalastyle:off
class YourIncomeSourceToSignUpViewSpec extends ViewSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  object AgentIncomeSource {
    val headingNoIncomeAdded = "Add your client’s income sources"
    val headingIncompleteIncomeAdded = "Manage your client’s income sources"
    val leadNoIncomeAdded = "You only need to add the income sources that apply to your client."
    val leadIncompleteIncomeAdded = "You can manage income sources that apply to your client. This includes finishing adding an income source you have already started, adding a new one or removing an existing one."
    val paragraph1: String = "If your client is self-employed, you must add all of their sole trader businesses if they have more than one. " +
      "If they have income from property you must add it, but this is limited to one UK property business."
    val paragraph1Overseas: String = "Your client can have up to 50 sole trader businesses. " +
      "However, they can have only one UK property business and one overseas property."
    val paragraph2 = "Renting out a property includes using a letting agency."
    val soleTrader = "Sole trader"
    val soleTraderLinkText = "Add sole trader income source"
    val anotherSoleTraderLinkText = "Add another sole trader income source"
    val soleTraderLink: String = appConfig.incomeTaxSelfEmploymentsFrontendClientInitialiseUrl
    val ukProperty = "UK property"
    val ukPropertyLabel = "UK property income source"
    val ukPropertyLinkText = "Add UK property income source"
    val ukPropertyLink: String = controllers.agent.tasklist.ukproperty.routes.PropertyStartDateController.show().url
    val foreignPropertyHeading = "Foreign property"
    val foreignPropertyLabel = "Foreign property income source"
    val foreignPropertyLinkText = "Add foreign property income source"
    val foreignPropertyLink: String = controllers.agent.tasklist.overseasproperty.routes.OverseasPropertyStartDateController.show().url

    val finalNoteOne = "You must add all your client’s income sources to continue to sign up. You can do this now or come back later."
    val finalNoteTwo = "Your client’s income source information can be changed at anytime."

    val continue = "Continue"
    val saveAndComeBackLater = "Save and come back later"

    val change: String = "Change"
    val remove: String = "Remove"

    def selfEmploymentChange(name: String) = s"Change $name"

    def selfEmploymentRemove(name: String) = s"Remove $name"

    val ukPropertyChange = "Change UK property income source"
    val ukPropertyRemove = "Remove UK property income source"

    val foreignPropertyChange = "Change foreign property income source"
    val foreignPropertyRemove = "Remove foreign property income source"
  }

  private val incomeSourceView = app.injector.instanceOf[YourIncomeSourceToSignUp]

  private val clientDetails = ClientDetails("FirstName LastName", "ZZ111111Z")

  val completeSelfEmployments: Seq[SelfEmploymentData] = Seq(
    SelfEmploymentData(
      id = "idOne",
      businessStartDate = Some(BusinessStartDate(DateModel("1", "1", "1980"))),
      businessName = Some(BusinessNameModel("business name")),
      businessTradeName = Some(BusinessTradeNameModel("business trade")),
      businessAddress = Some(BusinessAddressModel(Address(Seq("1 Long Road"), Some("ZZ1 1ZZ")))),
      confirmed = true
    )
  )
  val completeUKProperty: Option[PropertyModel] = Some(PropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("1", "1", "1981")),
    confirmed = true
  ))
  val completeForeignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel(
    accountingMethod = Some(Cash),
    startDate = Some(DateModel("1", "1", "1982")),
    confirmed = true
  ))

  val incompleteSelfEmployments: Seq[SelfEmploymentData] = Seq(
    SelfEmploymentData("idOne", None, Some(BusinessNameModel("business name")), Some(BusinessTradeNameModel("business trade"))),
    SelfEmploymentData("idTwo", None, Some(BusinessNameModel("business name"))),
    SelfEmploymentData("idThree", None, None, businessTradeName = Some(BusinessTradeNameModel("business trade"))),
    SelfEmploymentData("idFour")
  )
  val incompleteUKProperty: Option[PropertyModel] = Some(PropertyModel())
  val incompleteForeignProperty: Option[OverseasPropertyModel] = Some(OverseasPropertyModel())

  def view(incomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None)): Html = {
    incomeSourceView(
      postAction = testCall,
      backUrl = testBackUrl,
      clientDetails = clientDetails,
      incomeSources = incomeSources
    )
  }

  class ViewTest(incomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None)) {

    val document: Document = Jsoup.parse(view(incomeSources).body)

  }

  "YourIncomeSourceToSignUp" should {
    "display the template correctly" when {
      "the are no income sources added" in new TemplateViewTest(
        view = view(),
        title = AgentIncomeSource.headingNoIncomeAdded,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "the are incomplete income sources added" in new TemplateViewTest(
        view = view(IncomeSources(incompleteSelfEmployments, incompleteUKProperty, incompleteForeignProperty)),
        title = AgentIncomeSource.headingIncompleteIncomeAdded,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
      "the are complete income sources added" in new TemplateViewTest(
        view = view(IncomeSources(completeSelfEmployments, completeUKProperty, completeForeignProperty)),
        title = AgentIncomeSource.headingIncompleteIncomeAdded,
        isAgent = true,
        backLink = Some(testBackUrl),
        hasSignOutLink = true
      )
    }
  }

  "YourIncomeSourceToSignUp" when {
    "there are no income sources added" should {
      def noIncomeSources: IncomeSources = IncomeSources(Seq.empty[SelfEmploymentData], None, None)

      "have a heading for the page" in new ViewTest(noIncomeSources) {
        document.mainContent.getH1Element.text mustBe AgentIncomeSource.headingNoIncomeAdded
      }
      "have a caption with the clien's details" in new ViewTest(noIncomeSources) {
        document.mainContent.selectHead("span.govuk-caption-l").text mustBe s"${clientDetails.name} | ${clientDetails.formattedNino}"
      }
      "have a lead paragraph" in new ViewTest(noIncomeSources) {
        document.mainContent.selectHead("p.govuk-body-l").text mustBe AgentIncomeSource.leadNoIncomeAdded
      }
      "have a sole trader section" which {
        "has a heading" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe AgentIncomeSource.soleTrader
        }
        "has an add business link" in new ViewTest(noIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
          link.text mustBe AgentIncomeSource.soleTraderLinkText
          link.attr("href") mustBe AgentIncomeSource.soleTraderLink
        }
      }
      "have a uk property section" which {
        "has a heading" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("h2", 2).text mustBe AgentIncomeSource.ukProperty
        }

        "has an add business link" in new ViewTest(noIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-uk-property").selectHead("a")
          link.text mustBe AgentIncomeSource.ukPropertyLinkText
          link.attr("href") mustBe AgentIncomeSource.ukPropertyLink
        }
      }
      "have a foreign property section" which {
        "has a heading" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("h2", 3).text mustBe AgentIncomeSource.foreignPropertyHeading
        }
        "has an add business link" in new ViewTest(noIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-foreign-property").selectHead("a")
          link.text mustBe AgentIncomeSource.foreignPropertyLinkText
          link.attr("href") mustBe AgentIncomeSource.foreignPropertyLink
        }
      }
      "have a final note" which {
        "has a first first paragraph" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("p.govuk-body", 4).text mustBe AgentIncomeSource.finalNoteOne
        }
        "has a second paragraph" in new ViewTest(noIncomeSources) {
          document.mainContent.selectNth("p.govuk-body", 5).text mustBe AgentIncomeSource.finalNoteTwo
        }
      }
      "have a form" which {
        def form(document: Document): Element = document.mainContent.getForm

        "has the correct attributes" in new ViewTest(noIncomeSources) {
          form(document).attr("method") mustBe testCall.method
          form(document).attr("action") mustBe testCall.url
        }
        "has a continue button" in new ViewTest(noIncomeSources) {
          form(document).getGovukSubmitButton.text mustBe AgentIncomeSource.continue
        }
        "has no save and come back later button" in new ViewTest(noIncomeSources) {
          form(document).selectOptionally(".govuk-button--secondary") mustBe None
        }
      }
    }
    "there are incomplete income sources added" should {
      def incompleteIncomeSources: IncomeSources = IncomeSources(incompleteSelfEmployments, incompleteUKProperty, incompleteForeignProperty)

      "have a heading for the page" in new ViewTest(incompleteIncomeSources) {
        document.mainContent.getH1Element.text mustBe AgentIncomeSource.headingIncompleteIncomeAdded
      }
      "have a caption with the clien's details" in new ViewTest(incompleteIncomeSources) {
        document.mainContent.selectHead("span.govuk-caption-l").text mustBe s"${clientDetails.name} | ${clientDetails.formattedNino}"
      }
      "have a lead paragraph" in new ViewTest(incompleteIncomeSources) {
        document.mainContent.selectHead("p.govuk-body-l").text mustBe AgentIncomeSource.leadIncompleteIncomeAdded
      }
      "have a sole trader section" which {
        "has a heading" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe AgentIncomeSource.soleTrader
        }
        "has a summary of incomplete self employed businesses" which {
          def selfEmploymentSummary(document: Document): Element = document.mainContent.selectNth("dl", 1)

          "has a first business" which {
            def businessSummary(document: Document): Element = selfEmploymentSummary(document)
              .selectNth("div.govuk-summary-list__row", 1)

            "has a label" in new ViewTest(incompleteIncomeSources) {
              businessSummary(document).selectHead("dt").text mustBe "business trade - business name"
            }
            "has a set of actions" which {
              def actions(document: Document): Element = businessSummary(document).selectHead("dd").selectHead("ul")

              "has a change action" in new ViewTest(incompleteIncomeSources) {
                val link: Element = actions(document).selectNth("li", 1).selectHead("a")
                link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.change
                link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.selfEmploymentChange("business trade - business name")
              }
              "has a remove action" in new ViewTest(incompleteIncomeSources) {
                val link: Element = actions(document).selectNth("li", 2).selectHead("a")
                link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.remove
                link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.selfEmploymentRemove("business trade - business name")
              }
            }
          }
          "has a second business" which {
            def businessSummary(document: Document): Element = selfEmploymentSummary(document)
              .selectNth("div.govuk-summary-list__row", 2)

            "has a label" in new ViewTest(incompleteIncomeSources) {
              businessSummary(document).selectHead("dt").text mustBe "business name"
            }
            "has a set of actions" which {
              def actions(document: Document): Element = businessSummary(document).selectHead("dd").selectHead("ul")

              "has a change action" in new ViewTest(incompleteIncomeSources) {
                val link: Element = actions(document).selectNth("li", 1).selectHead("a")
                link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.change
                link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.selfEmploymentChange("business name")
              }
              "has a remove action" in new ViewTest(incompleteIncomeSources) {
                val link: Element = actions(document).selectNth("li", 2).selectHead("a")
                link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.remove
                link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.selfEmploymentRemove("business name")
              }
            }
          }
          "has a third business" which {
            def businessSummary(document: Document): Element = selfEmploymentSummary(document)
              .selectNth("div.govuk-summary-list__row", 3)

            "has a label" in new ViewTest(incompleteIncomeSources) {
              businessSummary(document).selectHead("dt").text mustBe "business trade"
            }
            "has a set of actions" which {
              def actions(document: Document): Element = businessSummary(document).selectHead("dd").selectHead("ul")

              "has a change action" in new ViewTest(incompleteIncomeSources) {
                val link: Element = actions(document).selectNth("li", 1).selectHead("a")
                link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.change
                link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.selfEmploymentChange("business trade")
              }
              "has a remove action" in new ViewTest(incompleteIncomeSources) {
                val link: Element = actions(document).selectNth("li", 2).selectHead("a")
                link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.remove
                link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.selfEmploymentRemove("business trade")
              }
            }
          }
          "has a forth business" which {
            def businessSummary(document: Document): Element = selfEmploymentSummary(document)
              .selectNth("div.govuk-summary-list__row", 4)

            "has a label" in new ViewTest(incompleteIncomeSources) {
              businessSummary(document).selectHead("dt").text mustBe "Business 4"
            }
            "has a set of actions" which {
              def actions(document: Document): Element = businessSummary(document).selectHead("dd").selectHead("ul")

              "has a change action" in new ViewTest(incompleteIncomeSources) {
                val link: Element = actions(document).selectNth("li", 1).selectHead("a")
                link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.change
                link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.selfEmploymentChange("Business 4")
              }
              "has a remove action" in new ViewTest(incompleteIncomeSources) {
                val link: Element = actions(document).selectNth("li", 2).selectHead("a")
                link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.remove
                link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.selfEmploymentRemove("Business 4")
              }
            }
          }
        }
        "has an add business link" in new ViewTest(incompleteIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
          link.text mustBe AgentIncomeSource.anotherSoleTraderLinkText
          link.attr("href") mustBe AgentIncomeSource.soleTraderLink
        }
      }
      "have a uk property section" which {
        "has a heading" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.selectNth("h2", 2).text mustBe AgentIncomeSource.ukProperty
        }
        "has a summary of the incomplete uk property" which {
          def ukPropertySummary(document: Document): Element = document.mainContent.selectNth("dl", 2).selectHead("div.govuk-summary-list__row")

          "has a label" in new ViewTest(incompleteIncomeSources) {
            ukPropertySummary(document).selectHead("dt").text mustBe AgentIncomeSource.ukPropertyLabel
          }
          "has a set of actions" which {
            def actions(document: Document): Element = ukPropertySummary(document).selectHead("dd").selectHead("ul")

            "has a change action" in new ViewTest(incompleteIncomeSources) {
              val link: Element = actions(document).selectNth("li", 1)
              link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.change
              link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.ukPropertyChange
            }
            "has a remove action" in new ViewTest(incompleteIncomeSources) {
              val link: Element = actions(document).selectNth("li", 2)
              link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.remove
              link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.ukPropertyRemove
            }
          }
        }
      }
      "have a foreign property section" which {
        "has a heading" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.selectNth("h2", 3).text mustBe AgentIncomeSource.foreignPropertyHeading
        }
        "has a summary of the incomplete foreign property" which {
          def foreignPropertySummary(document: Document): Element = document.mainContent.selectNth("dl", 3).selectHead("div.govuk-summary-list__row")

          "has a label" in new ViewTest(incompleteIncomeSources) {
            foreignPropertySummary(document).selectHead("dt").text mustBe AgentIncomeSource.foreignPropertyLabel
          }
          "has a set of actions" which {
            def actions(document: Document): Element = foreignPropertySummary(document).selectHead("dd").selectHead("ul")

            "has a change action" in new ViewTest(incompleteIncomeSources) {
              val link: Element = actions(document).selectNth("li", 1)
              link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.change
              link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.foreignPropertyChange
            }
            "has a remove action" in new ViewTest(incompleteIncomeSources) {
              val link: Element = actions(document).selectNth("li", 2)
              link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.remove
              link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.foreignPropertyRemove
            }
          }
        }
      }
      "have a final note" which {
        "has a first first paragraph" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.selectNth("p.govuk-body", 2).text mustBe AgentIncomeSource.finalNoteOne
        }
        "has a second paragraph" in new ViewTest(incompleteIncomeSources) {
          document.mainContent.selectNth("p.govuk-body", 3).text mustBe AgentIncomeSource.finalNoteTwo
        }
      }
      "have a form" which {
        def form(document: Document): Element = document.mainContent.getForm

        "has the correct attributes" in new ViewTest(incompleteIncomeSources) {
          form(document).attr("method") mustBe testCall.method
          form(document).attr("action") mustBe testCall.url
        }
        "has a continue button" in new ViewTest(incompleteIncomeSources) {
          form(document).getGovukSubmitButton.text mustBe AgentIncomeSource.continue
        }
        "has a save and come back later button" in new ViewTest(incompleteIncomeSources) {
          val button: Element = form(document).selectHead(".govuk-button--secondary")
          button.text mustBe AgentIncomeSource.saveAndComeBackLater
          button.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(Some("income-sources")).url
        }
      }
    }
    "there are fully complete income sources added" should {
      def completeIncomeSources: IncomeSources = IncomeSources(completeSelfEmployments, completeUKProperty, completeForeignProperty)

      "have a heading for the page" in new ViewTest(completeIncomeSources) {
        document.mainContent.getH1Element.text mustBe AgentIncomeSource.headingIncompleteIncomeAdded
      }
      "have a caption with the clien's details" in new ViewTest(completeIncomeSources) {
        document.mainContent.selectHead("span.govuk-caption-l").text mustBe s"${clientDetails.name} | ${clientDetails.formattedNino}"
      }
      "have a lead paragraph" in new ViewTest(completeIncomeSources) {
        document.mainContent.selectHead("p.govuk-body-l").text mustBe AgentIncomeSource.leadIncompleteIncomeAdded
      }
      "have a sole trader section" which {
        "has a heading" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("h2", 1).text mustBe AgentIncomeSource.soleTrader
        }
        "has a summary of incomplete self employed businesses" which {
          def selfEmploymentSummary(document: Document): Element = document.mainContent.selectNth("dl", 1).selectHead("div.govuk-summary-list__row")

          "has a label" in new ViewTest(completeIncomeSources) {
            selfEmploymentSummary(document).selectHead("dt").text mustBe "business trade - business name"
          }
          "has a set of actions" which {
            def actions(document: Document): Element = selfEmploymentSummary(document).selectHead("dd").selectHead("ul")

            "has a change action" in new ViewTest(completeIncomeSources) {
              val link: Element = actions(document).selectNth("li", 1).selectHead("a")
              link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.change
              link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.selfEmploymentChange("business trade - business name")
            }
            "has a remove action" in new ViewTest(completeIncomeSources) {
              val link: Element = actions(document).selectNth("li", 2).selectHead("a")
              link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.remove
              link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.selfEmploymentRemove("business trade - business name")
            }
          }
        }
        "has an add business link" in new ViewTest(completeIncomeSources) {
          val link: Element = document.mainContent.getElementById("add-self-employment").selectHead("a")
          link.text mustBe AgentIncomeSource.anotherSoleTraderLinkText
          link.attr("href") mustBe AgentIncomeSource.soleTraderLink
        }
      }
      "have a uk property section" which {
        "has a heading" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("h2", 2).text mustBe AgentIncomeSource.ukProperty
        }
        "has a summary of the incomplete uk property" which {
          def ukPropertySummary(document: Document): Element = document.mainContent.selectNth("dl", 2).selectHead("div.govuk-summary-list__row")

          "has a label" in new ViewTest(completeIncomeSources) {
            ukPropertySummary(document).selectHead("dt").text mustBe AgentIncomeSource.ukPropertyLabel
          }
          "has a set of actions" which {
            def actions(document: Document): Element = ukPropertySummary(document).selectHead("dd").selectHead("ul")

            "has a change action" in new ViewTest(completeIncomeSources) {
              val link: Element = actions(document).selectNth("li", 1)
              link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.change
              link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.ukPropertyChange
            }
            "has a remove action" in new ViewTest(completeIncomeSources) {
              val link: Element = actions(document).selectNth("li", 2)
              link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.remove
              link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.ukPropertyRemove
            }
          }
        }
      }
      "have a foreign property section" which {
        "has a heading" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("h2", 3).text mustBe AgentIncomeSource.foreignPropertyHeading
        }
        "has a summary of the incomplete foreign property" which {
          def foreignPropertySummary(document: Document): Element = document.mainContent.selectNth("dl", 3).selectHead("div.govuk-summary-list__row")

          "has a label" in new ViewTest(completeIncomeSources) {
            foreignPropertySummary(document).selectHead("dt").text mustBe AgentIncomeSource.foreignPropertyLabel
          }
          "has a set of actions" which {
            def actions(document: Document): Element = foreignPropertySummary(document).selectHead("dd").selectHead("ul")

            "has a change action" in new ViewTest(completeIncomeSources) {
              val link: Element = actions(document).selectNth("li", 1)
              link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.change
              link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.foreignPropertyChange
            }
            "has a remove action" in new ViewTest(completeIncomeSources) {
              val link: Element = actions(document).selectNth("li", 2)
              link.selectHead("span[aria-hidden=true]").text mustBe AgentIncomeSource.remove
              link.selectHead("span.govuk-visually-hidden").text mustBe AgentIncomeSource.foreignPropertyRemove
            }
          }
        }
      }
      "have a final note" which {
        "has a first first paragraph" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("p.govuk-body", 2).text mustBe AgentIncomeSource.finalNoteOne
        }
        "has a second paragraph" in new ViewTest(completeIncomeSources) {
          document.mainContent.selectNth("p.govuk-body", 3).text mustBe AgentIncomeSource.finalNoteTwo
        }
      }
      "have a form" which {
        def form(document: Document): Element = document.mainContent.getForm

        "has the correct attributes" in new ViewTest(completeIncomeSources) {
          form(document).attr("method") mustBe testCall.method
          form(document).attr("action") mustBe testCall.url
        }
        "has a continue button" in new ViewTest(completeIncomeSources) {
          form(document).getGovukSubmitButton.text mustBe AgentIncomeSource.continue
        }
        "has a save and come back later button" in new ViewTest(completeIncomeSources) {
          val button: Element = form(document).selectHead(".govuk-button--secondary")
          button.text mustBe AgentIncomeSource.saveAndComeBackLater
          button.attr("href") mustBe controllers.agent.tasklist.routes.ProgressSavedController.show(Some("income-sources")).url
        }
      }
    }
  }
}
