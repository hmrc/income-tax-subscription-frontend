
package helpers

import java.time.LocalDate

import helpers.IntegrationTestConstants._
import models._
import models.common._
import models.common.business._
import models.common.subscription.EnrolmentKey
import models.usermatching.UserDetailsModel
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Generator
import utilities.AccountingPeriodUtil
import utilities.individual.Constants
import utilities.individual.Constants.GovernmentGateway._


object IntegrationTestModels {

  import utilities.SubscriptionDataKeys._

  /*
   * this function returns a random nino each time it is called, if you need a constant nino use TestConstants.testNino
   */
  def newNino: String = new Generator().nextNino.nino

  val testStartDate: DateModel = DateModel.dateConvert(LocalDate.now)
  val testOneDayAgo: DateModel = DateModel.dateConvert(LocalDate.now.minusDays(1))
  val testEndDate: DateModel = DateModel.dateConvert(LocalDate.now.plusYears(1).plusDays(-1))
  val testEndDateNext: DateModel = AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1).plusDays(-1)
  val testEndDatePlus1Y: DateModel = AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1)
  val testAccountingYearCurrent: AccountingYearModel = AccountingYearModel(Current)
  val testAccountingYearNext: AccountingYearModel = AccountingYearModel(Next)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName: BusinessNameModel = BusinessNameModel("test business")
  val testBusinessTrade: BusinessTradeNameModel = BusinessTradeNameModel("test trade")
  val testBusinessAddress: BusinessAddressModel = BusinessAddressModel(
    "",
    Address(
      lines = Seq(
        "1 long road",
        "lonely town",
        "quiet county"
      ),
      postcode = "ZZ11ZZ"
    )
  )
  val testAccountingMethod = AccountingMethodModel(Cash)
  val testAccountingMethodProperty = AccountingMethodPropertyModel(Cash)
  val testAccountingMethodForeignProperty = OverseasAccountingMethodPropertyModel(Cash)
  val testValidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
  val testInvalidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusDays(364))
  val testPropertyStartDate = PropertyStartDateModel(testValidStartDate)
  val testPropertyStartDateModel = PropertyStartDateModel(DateModel("05","04","2017"))
  val testOverseasPropertyStartDate = OverseasPropertyStartDateModel(testValidStartDate)
  val testOverseasPropertyStartDateModel = OverseasPropertyStartDateModel(DateModel("05","04","2017"))
  val testInvalidPropertyStartDate = PropertyStartDateModel(testInvalidStartDate)
  val testBusinesses: Seq[SelfEmploymentData] = Seq(SelfEmploymentData(
    id = "12345",
    businessStartDate = Some(BusinessStartDate(DateModel("05", "04", "2017"))),
    businessName = Some(testBusinessName),
    businessTradeName = Some(testBusinessTrade),
    businessAddress = Some(testBusinessAddress)
  ))
  val testInvalidOverseasPropertyStartDate = OverseasPropertyStartDateModel(testInvalidStartDate)

  val testBusinessTradeName = BusinessTradeNameModel("test trade name")
  val testBusinessStartDate = BusinessStartDate(DateModel("05", "04", "2018"))
  val testBusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), "TF2 1PF"))
  val testId = "testId"

  lazy val fullSubscriptionDataBothPost: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(IncomeSourceModel(true, true, true)),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod),
      propertyStartDate = Some(testPropertyStartDate),
      propertyAccountingMethod = Some(testAccountingMethodProperty)
    )

  lazy val fullSubscriptionDataAllPost: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(IncomeSourceModel(true, true, true)),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod),
      propertyStartDate = Some(testPropertyStartDate),
      propertyAccountingMethod = Some(testAccountingMethodProperty),
      overseasPropertyStartDate = Some(testOverseasPropertyStartDate),
      overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)
    )

  lazy val fullSubscriptionDataPropertyPost: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(IncomeSourceModel(false, true, false)),
      propertyAccountingMethod = Some(testAccountingMethodProperty),
      overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty)

    )

  val selfEmploymentSubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceBusiness),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod),
      propertyStartDate = None,
      propertyAccountingMethod = None,
      overseasPropertyAccountingMethod = None,
      overseasPropertyStartDate = None
    )

  val ukPropertySubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceProperty),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = None,
      accountingMethod = None,
      propertyStartDate = Some(testPropertyStartDateModel),
      propertyAccountingMethod = Some(testAccountingMethodProperty),
      overseasPropertyAccountingMethod = None,
      overseasPropertyStartDate = None
    )

  val overseasPropertySubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceOverseas),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = None,
      accountingMethod = None,
      propertyStartDate = None,
      propertyAccountingMethod = None,
      overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty),
      overseasPropertyStartDate = Some(testOverseasPropertyStartDateModel)
    )

  val AllSubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceAll),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod),
      propertyStartDate = Some(testPropertyStartDateModel),
      propertyAccountingMethod = Some(testAccountingMethodProperty),
      overseasPropertyAccountingMethod = Some(testAccountingMethodForeignProperty),
      overseasPropertyStartDate = Some(testOverseasPropertyStartDateModel)
    )


  def subscriptionData(incomeSource: Option[IncomeSourceModel] = None,
                       selectedTaxYear: Option[AccountingYearModel] = None,
                       businessName: Option[BusinessNameModel] = None,
                       accountingMethod: Option[AccountingMethodModel] = None,
                       propertyStartDate: Option[PropertyStartDateModel] = None,
                       propertyAccountingMethod: Option[AccountingMethodPropertyModel] = None,
                       overseasPropertyAccountingMethod: Option[OverseasAccountingMethodPropertyModel] = None,
                       overseasPropertyStartDate: Option[OverseasPropertyStartDateModel] = None
                      ): Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => IncomeSource -> IncomeSourceModel.format.writes(model)) ++
      selectedTaxYear.map(model => SelectedTaxYear -> AccountingYearModel.format.writes(model)) ++
      businessName.map(model => BusinessName -> BusinessNameModel.format.writes(model)) ++
      accountingMethod.map(model => AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      propertyStartDate.map(model => PropertyStartDate -> PropertyStartDateModel.format.writes(model)) ++
      propertyAccountingMethod.map(model => PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model)) ++
      overseasPropertyAccountingMethod.map(model => OverseasPropertyAccountingMethod -> OverseasAccountingMethodPropertyModel.format.writes(model)) ++
      overseasPropertyStartDate.map(model => OverseasPropertyStartDate -> OverseasPropertyStartDateModel.format.writes(model))
  }

  lazy val testIncomeSourceBusiness: IncomeSourceModel = IncomeSourceModel(true, false, false)

  lazy val testIncomeSourceProperty: IncomeSourceModel = IncomeSourceModel(false, true, false)

  lazy val testIncomeSourceBoth: IncomeSourceModel = IncomeSourceModel(true, true, false)

  lazy val testIncomeSourceOverseas: IncomeSourceModel = IncomeSourceModel(false, false, true)

  lazy val testIncomeSourceAll: IncomeSourceModel = IncomeSourceModel(true, true, true)

  lazy val testIncomeSourceIndivProperty: IncomeSourceModel = IncomeSourceModel(false, true, false)

  lazy val testUserDetails = UserDetailsModel(testFirstName, testLastName, testNino, testOneDayAgo)

  lazy val testMTDITEnrolmentKey: EnrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> testMtdId)
  lazy val testIRSAEnrolmentKey: EnrolmentKey = EnrolmentKey(Constants.utrEnrolmentName, Constants.utrEnrolmentIdentifierKey -> testUtr)

  lazy val testSummaryDataSelfEmploymentData =
    Seq(SelfEmploymentData
    (
      id = testId,
      businessStartDate = Some(testBusinessStartDate),
      businessName = Some(testBusinessName),
      businessTradeName = Some(testBusinessTradeName),
      businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), "TF2 1PF")))
    )
    )
}
