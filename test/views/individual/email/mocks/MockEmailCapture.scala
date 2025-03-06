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

package views.individual.email.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import views.html.individual.email.EmailCapture

trait MockEmailCapture extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockEmailCapture: EmailCapture = mock[EmailCapture]

  def mockView(form: Form[_], postAction: Call, backUrl: String): Unit = {
    when(mockEmailCapture(
      ArgumentMatchers.eq(form),
      ArgumentMatchers.eq(postAction),
      ArgumentMatchers.eq(backUrl)
    )(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(HtmlFormat.empty)
  }

}
