/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors.usermatching.mocks

import connectors.agent.UsersGroupsSearchConnector
import connectors.agent.httpparsers.GetUsersForGroupsHttpParser.GetUsersForGroupsResponse
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, _}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockUsersGroupsSearchConnector extends MockitoSugar with BeforeAndAfterEach{
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUsersGroupsSearchConnector)
  }

  val mockUsersGroupsSearchConnector: UsersGroupsSearchConnector = mock[UsersGroupsSearchConnector]

  def mockGetUsersForGroups(groupId: String)(response: Future[GetUsersForGroupsResponse]): Unit = {
    when(mockUsersGroupsSearchConnector.getUsersForGroups(
      ArgumentMatchers.eq(groupId)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response
  }

}