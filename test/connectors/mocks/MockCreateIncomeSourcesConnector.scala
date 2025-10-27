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

import connectors.CreateIncomeSourcesConnector
import connectors.httpparser.CreateIncomeSourcesResponseHttpParser.CreateIncomeSourcesResponse
import models.common.subscription.CreateIncomeSourcesModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

trait MockCreateIncomeSourcesConnector extends MockitoSugar with BeforeAndAfterEach {
  suite: Suite =>

  val mockCreateIncomeSourcesConnector: CreateIncomeSourcesConnector = mock[CreateIncomeSourcesConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCreateIncomeSourcesConnector)
  }

  def mockCreateIncomeSources(mtdbsa: String, createIncomeSourcesModel: CreateIncomeSourcesModel)(result: CreateIncomeSourcesResponse): Unit = {
    when(mockCreateIncomeSourcesConnector.createIncomeSources(
      ArgumentMatchers.eq(mtdbsa),
      ArgumentMatchers.eq(createIncomeSourcesModel)
    )(ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))
  }

  def verifyCreateIncomeSources(mtdbsa: String, createIncomeSourcesModel: CreateIncomeSourcesModel, count: Int = 1): Unit = {
    verify(mockCreateIncomeSourcesConnector, times(count)).createIncomeSources(
      ArgumentMatchers.eq(mtdbsa),
      ArgumentMatchers.eq(createIncomeSourcesModel),
    )(ArgumentMatchers.any())
  }

}
