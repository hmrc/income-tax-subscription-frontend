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

package views.agent.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.HtmlFormat
import utilities.AccountingPeriodUtil
import views.html.agent.confirmation.SignUpConfirmation

trait MockSignUpConfirmation extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset()
  }

  val mockSignUpConfirmation: SignUpConfirmation = mock[SignUpConfirmation]

  def mockView(mandatedCurrentYear: Boolean, mandatedNextYear: Boolean, taxYearSelectionIsNext: Boolean, name: String, nino: String, usingSoftware: Boolean): Unit = {
    when(mockSignUpConfirmation(
      ArgumentMatchers.eq(mandatedCurrentYear),
      ArgumentMatchers.eq(mandatedNextYear),
      ArgumentMatchers.eq(taxYearSelectionIsNext),
      ArgumentMatchers.eq(Some(name)),
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(if (taxYearSelectionIsNext) AccountingPeriodUtil.getNextTaxYear else AccountingPeriodUtil.getCurrentTaxYear),
      ArgumentMatchers.eq(usingSoftware)
    )(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(HtmlFormat.empty)
  }

}
