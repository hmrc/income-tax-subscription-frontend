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

package views.agent.email.mocks

import forms.agent.email.CaptureConsentForm.captureConsentForm
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import views.html.agent.email.CaptureConsent
trait MockCaptureConsent extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockCaptureConsent: CaptureConsent = mock[CaptureConsent]

  def mockView(form: Form[_], postAction: Call, clientName: String, clientNino: String, backUrl: String): Unit = {
    when(mockCaptureConsent(
      ArgumentMatchers.eq(captureConsentForm),
      ArgumentMatchers.eq(postAction),
      ArgumentMatchers.eq(clientName),
      ArgumentMatchers.eq(clientNino),
      ArgumentMatchers.eq(backUrl)
    )(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(HtmlFormat.empty)
  }
}
