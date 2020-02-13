package helpers.agent.servicemocks

import connectors.agent.AgentServicesConnector
import play.api.http.Status

object AgentServicesStub extends WireMockMethods {

  def stubClientRelationship(arn: String, nino: String, exists: Boolean): Unit = {

    when(method = GET, uri = AgentServicesConnector.agentClientURI(arn, nino))
      .thenReturn(status = if (exists) Status.OK else Status.NOT_FOUND)
  }
}
