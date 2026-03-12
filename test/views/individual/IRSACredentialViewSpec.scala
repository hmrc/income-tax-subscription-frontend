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
import models.individual.ObfuscatedIdentifier
import models.individual.ObfuscatedIdentifier.{ObfuscatedUserId, UserEmail}

class IRSACredentialViewSpec extends ViewSpec {

  private val irsaCredential: IRSACredential = app.injector.instanceOf[IRSACredential]

  private val testFormError: FormError = FormError(IRSACredentialForm.fieldName, "irsa-cred.error")

  val testCurrentGGCredential: ObfuscatedUserId = ObfuscatedUserId("******1234")
  val testSAGGCredential: ObfuscatedUserId = ObfuscatedUserId("******5678")
  val testCurrentOLCredential: UserEmail = UserEmail("test123@example.com")
  val testSAOLCredential: UserEmail = UserEmail("t*****3@example.com")

  "IRSA Credential View" must {
    import IRSACredentialMessages.*

    "GG signed in and SA on GG" should {
      def mainContent: Element = document(
        currentCredential = testCurrentGGCredential,
        saCredential = testSAGGCredential
      ).mainContent

      "have a heading" in {
        mainContent.getH1Element.text mustBe heading
      }

      "have the correct first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe s"$ggParagraph1 ${testCurrentGGCredential.id.grouped(2).mkString(" ")}"
      }

      "have the correct second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe s"$ggParagraph2 ${testSAGGCredential.id.replace("*", "").grouped(2).mkString(" ")}"
      }

      "have the correct third paragraph" in {
        mainContent.selectNth("p", 3).text mustBe paragraph3
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
            legend = heading2,
            isHeading = false,
            isLegendHidden = false,
            hint = None,
            errorMessage = None,
            yesHint = Some(Text(s"${IRSACredentialMessages.ggHintText} ${testSAGGCredential.id.replace("*", "").grouped(2).mkString(" ")}")),
            inline = false
          )
        }

        "has a continue button" in {
          form.select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
        }
      }
    }

    "OL signed in and SA on GG" should {
      def mainContent: Element = document(
        currentCredential = testCurrentOLCredential,
        saCredential = testSAGGCredential
      ).mainContent

      "have a heading" in {
        mainContent.getH1Element.text mustBe heading
      }

      "have the correct first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe s"$olParagraph1 ${testCurrentOLCredential.email}"
      }

      "have the correct second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe s"$ggParagraph2 ${testSAGGCredential.id.replace("*", "").grouped(2).mkString(" ")}"
      }

      "have the correct third paragraph" in {
        mainContent.selectNth("p", 3).text mustBe paragraph3
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
            legend = heading2,
            isHeading = false,
            isLegendHidden = false,
            hint = None,
            errorMessage = None,
            yesHint = Some(Text(s"${IRSACredentialMessages.ggHintText} ${testSAGGCredential.id.replace("*", "").grouped(2).mkString(" ")}")),
            inline = false
          )
        }

        "has a continue button" in {
          form.select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
        }
      }
    }

    "GG signed in and SA on OL" should {
      def mainContent: Element = document(
        currentCredential = testCurrentGGCredential,
        saCredential = testSAOLCredential
      ).mainContent

      "have a heading" in {
        mainContent.getH1Element.text mustBe heading
      }

      "have the correct first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe s"$ggParagraph1 ${testCurrentGGCredential.id.grouped(2).mkString(" ")}"
      }

      "have the correct second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe s"$olParagraph2 ${testSAOLCredential.obfuscatedEmail}"
      }

      "have the correct third paragraph" in {
        mainContent.selectNth("p", 3).text mustBe paragraph3
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
            legend = heading2,
            isHeading = false,
            isLegendHidden = false,
            hint = None,
            errorMessage = None,
            yesHint = Some(Text(s"${IRSACredentialMessages.olHintText} ${testSAOLCredential.obfuscatedEmail}")),
            inline = false
          )
        }

        "has a continue button" in {
          form.select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
        }
      }
    }

    "OL signed in and SA on OL" should {
      def mainContent: Element = document(
        currentCredential = testCurrentOLCredential,
        saCredential = testSAOLCredential
      ).mainContent

      "have a heading" in {
        mainContent.getH1Element.text mustBe heading
      }

      "have the correct first paragraph" in {
        mainContent.selectNth("p", 1).text mustBe s"${olParagraph1} ${testCurrentOLCredential.email}"
      }

      "have the correct second paragraph" in {
        mainContent.selectNth("p", 2).text mustBe s"${olParagraph2} ${testSAOLCredential.obfuscatedEmail}"
      }

      "have the correct third paragraph" in {
        mainContent.selectNth("p", 3).text mustBe paragraph3
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
            legend = heading2,
            isHeading = false,
            isLegendHidden = false,
            hint = None,
            errorMessage = None,
            yesHint = Some(Text(s"${IRSACredentialMessages.olHintText} ${testSAOLCredential.obfuscatedEmail}")),
            inline = false
          )
        }

        "has a continue button" in {
          form.select("button[id=continue-button]").text mustBe MessageLookup.Base.continue
        }
      }
    }
  }

  private def page(
                    hasError: Boolean = false,
                    currentCredential: ObfuscatedIdentifier,
                    saCredential: ObfuscatedIdentifier
                  ): Html = {
    irsaCredential(
      if (hasError) IRSACredentialForm.irsaCredentialForm.withError(testFormError)
      else IRSACredentialForm.irsaCredentialForm,
      testCall,
      currentCredential,
      saCredential
    )
  }

  private def document(
                        hasError: Boolean = false,
                        currentCredential: ObfuscatedIdentifier,
                        saCredential: ObfuscatedIdentifier
                      ): Document = Jsoup.parse(page(hasError, currentCredential, saCredential).body)

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
  }
}
