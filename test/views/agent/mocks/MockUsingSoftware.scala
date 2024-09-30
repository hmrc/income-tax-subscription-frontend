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

package views.agent.mocks

import models.YesNo
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import views.html.agent.UsingSoftware

trait MockUsingSoftware extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockUsingSoftware: UsingSoftware = mock[UsingSoftware]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUsingSoftware)
  }

  def mockView(usingSoftwareForm: Form[YesNo], postAction: Call, clientName: String, clientNino: String, backUrl: String): Unit = {
    when(mockUsingSoftware(
      ArgumentMatchers.eq(usingSoftwareForm),
      ArgumentMatchers.eq(postAction),
      ArgumentMatchers.eq(clientName),
      ArgumentMatchers.eq(clientNino),
      ArgumentMatchers.eq(backUrl)
    )(any(), any()))
      .thenReturn(HtmlFormat.empty)
  }

}
