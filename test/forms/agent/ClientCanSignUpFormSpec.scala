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

package forms.agent

import forms.submapping.YesNoMapping
import models.{No, Yes, YesNo}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.data.{Form, FormError}

class ClientCanSignUpFormSpec extends PlaySpec with GuiceOneAppPerTest {

  "ClientCanSignUpForm.bind" should {
    "return Yes" when {
      s"${YesNoMapping.option_yes} was provided" in {
        val boundForm: Form[YesNo] = ClientCanSignUpForm.clientCanSignUpForm.bind(
          Map(ClientCanSignUpForm.fieldName -> YesNoMapping.option_yes)
        )

        boundForm.errors mustBe Seq.empty[FormError]
        boundForm.value mustBe Some(Yes)
      }
    }
  }
  "return No" when {
    s"${YesNoMapping.option_no} was provided" in {
      val boundForm: Form[YesNo] = ClientCanSignUpForm.clientCanSignUpForm.bind(
        Map(ClientCanSignUpForm.fieldName -> YesNoMapping.option_no)
      )

      boundForm.errors mustBe Seq.empty[FormError]
      boundForm.value mustBe Some(No)
    }
  }
  "return an error" when {
    "no option was provided" in {
      val boundForm: Form[YesNo] = ClientCanSignUpForm.clientCanSignUpForm.bind(Map.empty[String, String])

      boundForm.value mustBe None
      boundForm.errors mustBe Seq(FormError(ClientCanSignUpForm.fieldName, "error.agent.client-can-sign-up.invalid"))
    }
  }

}
