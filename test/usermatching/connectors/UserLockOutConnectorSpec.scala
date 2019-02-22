/*
 * Copyright 2019 HM Revenue & Customs
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

package usermatching.connectors

import usermatching.connectors.mocks.TestUserLockoutConnector
import core.utils.TestConstants.testNino
import core.utils.UnitTestTrait

class UserLockOutConnectorSpec extends UnitTestTrait with TestUserLockoutConnector{

  "UserLockOutConnector" should {
    "have the correct url" in {
      TestUserLockoutConnector.userLockoutUrl(testNino) must endWith(s"/income-tax-subscription/client-matching/lock/$testNino")
    }
  }

}
