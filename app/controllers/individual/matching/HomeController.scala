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

package controllers.individual.matching

import auth.individual.JourneyState._
import auth.individual.{SignUp, StatelessController}
import common.Constants
import common.Constants.ITSASessionKeys._
import config.AppConfig
import config.featureswitch.FeatureSwitch.PrePopulate
import controllers.utils.ReferenceRetrieval
import models.audits.EligibilityAuditing.EligibilityAuditModel
import models.audits.SignupStartedAuditing
import models.common.subscription.SubscriptionSuccess
import models.{EligibilityStatus, PrePopData}
import play.api.mvc._
import services._
import services.individual._
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeController @Inject()(citizenDetailsService: CitizenDetailsService,
                               getEligibilityStatusService: GetEligibilityStatusService,
                               subscriptionService: SubscriptionService,
                               throttlingService: ThrottlingService,
                               prePopulationService: PrePopulationService,
                               ninoService: NinoService,
                               sessionDataService: SessionDataService,
                               referenceRetrieval: ReferenceRetrieval)
                              (val auditingService: AuditingService,
                               val authService: AuthService,
                               val appConfig: AppConfig)
                              (implicit val ec: ExecutionContext,
                               mcc: MessagesControllerComponents) extends StatelessController {

  def home: Action[AnyContent] = Action {
    val redirect = routes.HomeController.index
    Redirect(redirect)
  }

  def index: Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        retrieveUTRAndName(user.enrolments) flatMap {
          case (None, _) =>
            for {
              nino <- ninoService.getNino
              _ <- startIndividualSignupAudit(None)
              _ <- auditingService.audit(EligibilityAuditModel(
                agentReferenceNumber = None,
                utr = None,
                nino = Some(nino),
                eligibility = "ineligible",
                failureReason = Some("no-self-assessment")
              ))
            } yield {
              Redirect(routes.NoSAController.show).removingFromSession(JourneyStateKey)
            }
          case (Some(utr), name) =>
            user.getSPSEntityId match {
              case Some(entityId) if request.session.isInState(SignUp) =>
                Future.successful(Redirect(controllers.individual.sps.routes.SPSCallbackController.callback(Some(entityId))))
              case _ => tryToSubscribe(utr, name)
            }
        }
    }

  private def retrieveUTRAndName(enrolments: Enrolments)(implicit hc: HeaderCarrier): Future[(Option[String], Option[String])] = {
    sessionDataService.fetchUTR flatMap {
      case Left(error) =>
        throw new InternalServerException(s"[HomeController][retrieveUTRAndName] - Failure when attempting to fetch utr from session: $error")
      case Right(None) =>
        enrolments.getEnrolment(Constants.utrEnrolmentName).flatMap(_.getIdentifier(Constants.utrEnrolmentIdentifierKey)).map(_.value) match {
          case Some(utr) => Future.successful((Some(utr), None))
          case None => for {
            nino <- ninoService.getNino
            details <- citizenDetailsService.lookupCitizenDetails(nino)
          } yield {
            (details.utr, details.name)
          }
        }
      case Right(Some(utr)) =>
        Future.successful((Some(utr), None))
    }
  }

  private def tryToSubscribe(utr: String, fullNameMaybe: Option[String])(implicit request: Request[AnyContent]) =
    throttlingService.throttled(IndividualStartOfJourneyThrottle) {
      ninoService.getNino flatMap { nino =>
        getSubscription(nino) flatMap {
          case Some(SubscriptionSuccess(_)) => // found an already existing subscription, go to claim it
            Future.successful(Redirect(controllers.individual.claimenrolment.routes.AddMTDITOverviewController.show.url))
          case None => // did not find a subscription, continue into sign up journey
            sessionDataService.saveUTR(utr) flatMap {
              case Right(_) => handleNoSubscriptionFound(nino, utr)
              case Left(_) => throw new InternalServerException("[HomeController][tryToSubscribe] - Failure saving utr to session")
            }
        } map { response =>
          val cookiesToAdd = fullNameMaybe map (fullName => FULLNAME -> fullName)
          response.addingToSession(cookiesToAdd.toSeq: _*)
        }
      }
    }

  private def handleNoSubscriptionFound(nino: String, utr: String)
                                       (implicit hc: HeaderCarrier, request: Request[AnyContent]) = {
    getEligibilityStatusService.getEligibilityStatus flatMap {
      // Check eligibility (this is complete, and gives us the control list response including pre-pop information)
      case EligibilityStatus(false, false) =>
        auditingService.audit(EligibilityAuditModel(
          agentReferenceNumber = None,
          utr = Some(utr),
          nino = Some(nino),
          eligibility = "ineligible",
          failureReason = Some("control-list-ineligible")
        ))
        Future.successful(Redirect(controllers.individual.controllist.routes.NotEligibleForIncomeTaxController.show()))
      case EligibilityStatus(thisYear, _) =>
        referenceRetrieval.getReference(None) flatMap { reference =>
          handlePrepop(reference, None) map { _ =>
            auditingService.audit(EligibilityAuditModel(
              agentReferenceNumber = None,
              utr = Some(utr),
              nino = Some(nino),
              eligibility = if (thisYear) "eligible" else "eligible - next year only",
              failureReason = None
            ))
            goToSignUp(thisYear)
              .withJourneyState(SignUp)
          }
        }
    }

  }

  private def getSubscription(nino: String)(implicit request: Request[AnyContent]): Future[Option[SubscriptionSuccess]] =
    subscriptionService.getSubscription(nino) map {
      case Right(optionalSubscription) => optionalSubscription
      case Left(err) => throw new InternalServerException(s"HomeController.index: unexpected error calling the subscription service:\n$err")
    }

  private def goToSignUp(thisYear: Boolean): Result = {
    val location: Call = if (thisYear)
      controllers.individual.sps.routes.SPSHandoffController.redirectToSPS
    else
      controllers.individual.controllist.routes.CannotSignUpThisYearController.show
    Redirect(location)
  }

  private def handlePrepop(reference: String, prepopMaybe: Option[PrePopData])(implicit hc: HeaderCarrier) =
    prepopMaybe match {
      case Some(prepop) if isEnabled(PrePopulate) => prePopulationService.prePopulate(reference, prepop)
      case _ => Future.successful(())
    }

  private def startIndividualSignupAudit(utr: Option[String])(implicit request: Request[_]) = {
    ninoService.getNino flatMap { nino =>
      val auditModel = SignupStartedAuditing.SignupStartedAuditModel(
        agentReferenceNumber = None,
        utr = utr,
        nino = Some(nino)
      )
      auditingService.audit(auditModel)
    }
  }
}
