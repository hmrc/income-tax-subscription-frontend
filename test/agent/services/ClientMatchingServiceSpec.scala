/*
 * Copyright 2017 HM Revenue & Customs
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

///*
// * Copyright 2017 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package agent.services
//
//import agent.connectors.models.matching.ClientMatchSuccessResponseModel
//import agent.services.mocks.TestClientMatchingService
//import agent.utils.TestConstants._
//import agent.utils.TestModels.testClientDetails
//import play.api.test.Helpers._
//
//class ClientMatchingServiceSpec extends TestClientMatchingService {
//
//  "ClientMatchingService" should {
//
//    "return the nino if authenticator response with ok" in {
//      mockClientMatchSuccess(testClientDetails)
//      val result = await(TestClientMatchingService.matchClient(testClientDetails))
//      result mustBe Some(ClientMatchSuccessResponseModel(testNino, testUtr))
//    }
//
//    "return None if authenticator response with Unauthorized but with a matching error message" in {
//      mockClientMatchNotFound(testClientDetails)
//      val result = TestClientMatchingService.matchClient(testClientDetails)
//      await(result) mustBe None
//    }
//
//    "throw InternalServerException if authenticator response with Unauthorized but with a server error message" in {
//      mockClientMatchException(testClientDetails)
//      val result = TestClientMatchingService.matchClient(testClientDetails)
//
//      intercept[Exception](await(result)) mustBe testException
//    }
//  }
//
//}
