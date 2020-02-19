
package helpers.agent.servicemocks

import java.time.OffsetDateTime

import helpers.agent.IntegrationTestConstants.testARN
import models.usermatching.LockedOut
import play.api.http.Status

object AgentLockoutStub extends WireMockMethods {

  def lockoutURI(arn: String): String = s"/income-tax-subscription/client-matching/lock/$arn"

  val testLock: LockedOut = LockedOut(testARN, OffsetDateTime.now())

  def stubLockAgent(arn: String): Unit =
    when(method = POST, uri = lockoutURI(arn))
      .thenReturn(Status.CREATED)

  def stubAgentIsLocked(arn: String): Unit =
    when(method = GET, uri = lockoutURI(arn))
      .thenReturn(Status.OK, testLock)

  def stubAgentIsNotLocked(arn: String): Unit =
    when(method = GET, uri = lockoutURI(arn))
      .thenReturn(Status.NOT_FOUND)

}
