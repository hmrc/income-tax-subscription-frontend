package models.audits

import services.AuditModel

object ITSASignUpSubmissionRequestAuditing {

  case class ITSASignUpSubmissionRequestAuditModel(agentReferenceNumber: Option[String], utr: Option[String], nino: Option[String], eligibility: String, failureReason: Option[String]) extends JsonAuditModel {
    override val transactionName: Option[String] = None
    val userType: String = if (agentReferenceNumber.isDefined) "agent" else "individual"
    
    val ninoKeyValue: Option[(String, String)] = nino.map("nino" -> _)
    val utrKeyValue: Option[(String, String)] = utr.map("saUtr" -> _)
    val arnKeyValue: Option[(String, String)] = agentReferenceNumber.map("agentReferenceNumber" -> _)
    val eligibilityStatus: String = eligibility
    val eligibilityFailureReason: Option[(String, String)] = failureReason.map("eligibilityFailureReason" -> _)
    override val detail: Map[String, String] = Map(
      "userType" -> userType,
      "eligibilityStatus" -> eligibilityStatus
    ) ++ arnKeyValue ++ ninoKeyValue ++ utrKeyValue ++ eligibilityFailureReason

    override val auditType: String = mtdITSAEligibilityAuditType
  }

}
