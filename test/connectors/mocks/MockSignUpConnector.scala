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

package connectors.mocks

import connectors.SignUpConnector
import connectors.httpparser.SignUpResponseHttpParser.SignUpResponse
import models.AccountingYear
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

trait MockSignUpConnector extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockSignUpConnector: SignUpConnector = mock[SignUpConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSignUpConnector)
  }

  def mockSignUp(nino: String, utr: String, taxYear: AccountingYear)(result: SignUpResponse): Unit = {
    when(mockSignUpConnector.signUp(
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(utr),
      ArgumentMatchers.eq(taxYear)
    )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def verifySignUp(nino: String, utr: String, taxYear: AccountingYear, count: Int = 1): Unit = {
    verify(mockSignUpConnector, times(count)).signUp(
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(utr),
      ArgumentMatchers.eq(taxYear)
    )(ArgumentMatchers.any())
  }

}
