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

package views.agent.matching.mocks

import models.usermatching.UserDetailsModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import views.html.agent.matching.ClientDetails

trait MockClientDetails extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockClientDetails: ClientDetails = mock[ClientDetails]

  def mockView(form: Form[UserDetailsModel], postAction: Call, isEditMode: Boolean): Unit = {
    when(mockClientDetails(
      ArgumentMatchers.eq(form),
      ArgumentMatchers.eq(postAction),
      ArgumentMatchers.eq(isEditMode)
    )(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(HtmlFormat.empty)
  }
}