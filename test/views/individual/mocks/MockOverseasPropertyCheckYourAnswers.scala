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

package views.individual.mocks

import models.common.OverseasPropertyModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Call
import play.twirl.api.HtmlFormat
import views.html.individual.tasklist.overseasproperty.OverseasPropertyCheckYourAnswers

trait MockOverseasPropertyCheckYourAnswers extends PlaySpec with MockitoSugar with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockView)
  }

  val mockView: OverseasPropertyCheckYourAnswers = mock[OverseasPropertyCheckYourAnswers]

  def mockOverseasPropertyCheckYourAnswersView(viewModel: OverseasPropertyModel, postAction: Call, backUrl: String, isGlobalEdit: Boolean): Unit = {
    when(mockView(
      viewModel = ArgumentMatchers.eq(viewModel),
      postAction = ArgumentMatchers.eq(postAction),
      backUrl = ArgumentMatchers.eq(backUrl),
      isGlobalEdit = ArgumentMatchers.eq(isGlobalEdit)
    )(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn HtmlFormat.empty
  }
}
