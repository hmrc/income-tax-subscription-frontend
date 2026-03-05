/*
 * Copyright 2024 HM Revenue & Customs
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

package views.individual

import forms.individual.IRSACredentialForm
import messagelookup.individual.MessageLookup
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.FormError
import play.twirl.api.Html
import utilities.ViewSpec
import views.html.individual.IRSACredential
import uk.gov.hmrc.govukfrontend.views.Aliases.Text

class IRSACredentialViewSpec extends ViewSpec {

  private val irsaCredential: IRSACredential = app.injector.instanceOf[IRSACredential]

  private val testFormError: FormError = FormError(IRSACredentialForm.fieldName, "irsa-cred.error")

  "IRSA Credential View" must {
    import IRSACredentialMessages.*

    "GG user ID and obfuscated GG user ID" should {
      def mainContent: Element = document(
        obfuscatedUserId = Some(IRSACredentialMessages.testObfuscatedUserId),
        obfuscatedEmail = None,
        userID = Some(IRSACredentialMessages.testUserId),
        email = None
      ).mainContent

      "have a heading" in {
        mainContent.getH1Element.text mustBe heading
      }

      "have the correct first paragraph" in {
        mainContent.mainContent.selectNth("p", 1).text mustBe IRSACredentialMessages.ggParagraph1 + testUserId
      }

      "have the correct second paragraph" in {
        mainContent.mainContent.selectNth("p", 2).text mustBe IRSACredentialMessages.ggParagraph2 + testObfuscatedUserId
      }

      "have the correct third paragraph" in {
        mainContent.mainContent.selectNth("p", 3).text mustBe IRSACredentialMessages.paragraph3
      }

      "have a form" which {
        def form: Element = mainContent.selectHead("form")

        "has correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has the correct radio inputs" in {
          form.mustHaveYesNoRadioInputs(selector = "fieldset")(
            name = radioName,
            legend = IRSACredentialMessages.heading2,
            isHeading = false,
            isLegendHidden = false,
            hint = None,
            errorMessage = None,
            yesHintId = None,
            yesHint = Some(Text(IRSACredentialMessages.ggHintText + IRSACredentialMessages.testObfuscatedUserId)),
            noHintId = None,
            noHint = None,
            inline = false,
            yesText = None,
            noText = None
          )
        }

        "has a continue button" in {
          form.select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
        }
      }
    }

    "One Login email and obfuscated GG user ID" should {
      def mainContent: Element = document(
        obfuscatedUserId = Some(IRSACredentialMessages.testObfuscatedUserId),
        obfuscatedEmail = None,
        userID = None,
        email = Some(IRSACredentialMessages.testEmail)
      ).mainContent

      "have a heading" in {
        mainContent.getH1Element.text mustBe heading
      }

      "have the correct first paragraph" in {
        mainContent.mainContent.selectNth("p", 1).text mustBe IRSACredentialMessages.olParagraph1 + testEmail
      }

      "have the correct second paragraph" in {
        mainContent.mainContent.selectNth("p", 2).text mustBe IRSACredentialMessages.ggParagraph2 + testObfuscatedUserId
      }

      "have the correct third paragraph" in {
        mainContent.mainContent.selectNth("p", 3).text mustBe IRSACredentialMessages.paragraph3
      }

      "have a form" which {
        def form: Element = mainContent.selectHead("form")

        "has correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has the correct radio inputs" in {
          form.mustHaveYesNoRadioInputs(selector = "fieldset")(
            name = radioName,
            legend = IRSACredentialMessages.heading2,
            isHeading = false,
            isLegendHidden = false,
            hint = None,
            errorMessage = None,
            yesHintId = None,
            yesHint = Some(Text(IRSACredentialMessages.ggHintText + IRSACredentialMessages.testObfuscatedUserId)),
            noHintId = None,
            noHint = None,
            inline = false,
            yesText = None,
            noText = None
          )
        }

        "has a continue button" in {
          form.select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
        }
      }
    }

    "GG user ID and obfuscated One Login email" should {
      def mainContent: Element = document(
        obfuscatedUserId = None,
        obfuscatedEmail = Some(IRSACredentialMessages.testObfuscatedEmail),
        userID = Some(IRSACredentialMessages.testUserId),
        email = None
      ).mainContent

      "have a heading" in {
        mainContent.getH1Element.text mustBe heading
      }

      "have the correct first paragraph" in {
        mainContent.mainContent.selectNth("p", 1).text mustBe IRSACredentialMessages.ggParagraph1 + testUserId
      }

      "have the correct second paragraph" in {
        mainContent.mainContent.selectNth("p", 2).text mustBe IRSACredentialMessages.olParagraph2 + testObfuscatedEmail
      }

      "have the correct third paragraph" in {
        mainContent.mainContent.selectNth("p", 3).text mustBe IRSACredentialMessages.paragraph3
      }

      "have a form" which {
        def form: Element = mainContent.selectHead("form")

        "has correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has the correct radio inputs" in {
          form.mustHaveYesNoRadioInputs(selector = "fieldset")(
            name = radioName,
            legend = IRSACredentialMessages.heading2,
            isHeading = false,
            isLegendHidden = false,
            hint = None,
            errorMessage = None,
            yesHintId = None,
            yesHint = Some(Text(IRSACredentialMessages.olHintText + IRSACredentialMessages.testObfuscatedEmail)),
            noHintId = None,
            noHint = None,
            inline = false,
            yesText = None,
            noText = None
          )
        }

        "has a continue button" in {
          form.select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
        }
      }
    }

    "One Login email and obfuscated One Login email" should {
      def mainContent: Element = document(
        obfuscatedUserId = None,
        obfuscatedEmail = Some(IRSACredentialMessages.testObfuscatedEmail),
        userID = None,
        email = Some(IRSACredentialMessages.testEmail)
      ).mainContent

      "have a heading" in {
        mainContent.getH1Element.text mustBe heading
      }

      "have the correct first paragraph" in {
        mainContent.mainContent.selectNth("p", 1).text mustBe IRSACredentialMessages.olParagraph1 + testEmail
      }

      "have the correct second paragraph" in {
        mainContent.mainContent.selectNth("p", 2).text mustBe IRSACredentialMessages.olParagraph2 + testObfuscatedEmail
      }

      "have the correct third paragraph" in {
        mainContent.mainContent.selectNth("p", 3).text mustBe IRSACredentialMessages.paragraph3
      }

      "have a form" which {
        def form: Element = mainContent.selectHead("form")

        "has correct attributes" in {
          form.attr("method") mustBe testCall.method
          form.attr("action") mustBe testCall.url
        }

        "has the correct radio inputs" in {
          form.mustHaveYesNoRadioInputs(selector = "fieldset")(
            name = radioName,
            legend = IRSACredentialMessages.heading2,
            isHeading = false,
            isLegendHidden = false,
            hint = None,
            errorMessage = None,
            yesHintId = None,
            yesHint = Some(Text(IRSACredentialMessages.olHintText + IRSACredentialMessages.testObfuscatedEmail)),
            noHintId = None,
            noHint = None,
            inline = false,
            yesText = None,
            noText = None
          )
        }

        "has a continue button" in {
          form.select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
        }
      }
    }
  }

  private def page(hasError: Boolean = false, obfuscatedUserId: Option[String], obfuscatedEmail: Option[String], userID: Option[String], email: Option[String]): Html = {
    irsaCredential(
      if (hasError) IRSACredentialForm.irsaCredentialForm.withError(testFormError)
      else IRSACredentialForm.irsaCredentialForm,
        testCall,
        obfuscatedUserId,
        obfuscatedEmail,
        userID,
        email
    )
  }

  private def document(
                        hasError: Boolean = false,
                        obfuscatedUserId: Option[String] = None,
                        obfuscatedEmail: Option[String] = None,
                        userID: Option[String] = None,
                        email: Option[String] = None
                      ): Document = {
    Jsoup.parse(page(
      hasError,
      obfuscatedUserId,
      obfuscatedEmail,
      userID,
      email
    ).body)
  }

  private object IRSACredentialMessages {
    val title: String = "You’re not using your Self Assessment sign in details"
    val heading: String = "You’re not using your Self Assessment sign in details"

    val ggParagraph1: String = "You’re signed in with Government Gateway user ID"
    val ggParagraph2: String = "The Government Gateway user ID details you use for Self Assessment ends in"
    val ggHintText: String = "You’ll have to sign in again using Government Gateway user ID ending in"

    val olParagraph1: String = "You’re signed in with GOV.UK One Login details"
    val olParagraph2: String = "The GOV.UK One Login details you use for Self Assessment are:"
    val olHintText: String = "You’ll have to sign in again using GOV.UK One Login details:"

    val paragraph3: String = "We recommend you use the same sign in details that you use for your Self Assessment to sign up to Making Tax Digital for Income Tax."
    val heading2: String = "Do you want to use the same sign in details to access Making Tax Digital for Income Tax and Self Assessment?"
    val error: String = "Select to use the same user ID and password or to keep them separate"
    val radioName: String = "yes-no"

    val testUserId: String = " 1234567890"
    val testEmail: String = " test@example.com"
    val testObfuscatedUserId: String = " 12 34"
    val testObfuscatedEmail: String = " te**@example.com"
  }
}
