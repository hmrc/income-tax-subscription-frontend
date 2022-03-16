
package helpers.agent

import models._
import models.common.business._
import models.common._
import models.usermatching.UserDetailsModel
import play.api.libs.json.JsValue
import utilities.{AccountingPeriodUtil, SubscriptionDataKeys}

import java.time.LocalDate

object IntegrationTestModels {
  val testStartDate: DateModel = DateModel.dateConvert(LocalDate.now)
  val testEndDate: DateModel = helpers.IntegrationTestModels.testEndDate
  val testEndDateNext: DateModel = AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1).plusDays(-1)
  val testEndDatePlus1Y: DateModel = AccountingPeriodUtil.getCurrentTaxYearEndDate.plusYears(1)
  val testAccountingYearNext: AccountingYearModel = AccountingYearModel(Next)
  val testAccountingYearCurrent: AccountingYearModel = AccountingYearModel(Current)
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
  val testAccountingMethod: AccountingMethodModel = AccountingMethodModel(Cash)
  val testAccountingMethodProperty: AccountingMethodPropertyModel = AccountingMethodPropertyModel(Cash)
  val testAccountingMethodForeignProperty: OverseasAccountingMethodPropertyModel = OverseasAccountingMethodPropertyModel(Cash)
  val testValidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusYears(1))
  val testInvalidStartDate: DateModel = DateModel.dateConvert(LocalDate.now.minusDays(364))
  val testPropertyStartDate: PropertyStartDateModel = PropertyStartDateModel(testValidStartDate)
  val testOverseasPropertyStartDate: OverseasPropertyStartDateModel = OverseasPropertyStartDateModel(testValidStartDate)
  val testInvalidOverseasPropertyStartDate: OverseasPropertyStartDateModel = OverseasPropertyStartDateModel(testInvalidStartDate)
  val testInvalidPropertyStartDate: PropertyStartDateModel = PropertyStartDateModel(testInvalidStartDate)
  val testBusinesses: Seq[SelfEmploymentData] = Seq(SelfEmploymentData(
    id = "businessId",
    businessStartDate = Some(BusinessStartDate(DateModel("19", "03", "1999"))),
    businessName = Some(testBusinessName),
    businessTradeName = Some(testBusinessTrade),
    businessAddress = Some(testBusinessAddress)
  ))


  val testBusinessTradeName: BusinessTradeNameModel = BusinessTradeNameModel("test trade name")
  val testBusinessStartDate: BusinessStartDate = BusinessStartDate(DateModel("05", "04", "2018"))
  val testBusinessAddressModel: BusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), "TF2 1PF"))
  val testId = "testId"


  val fullSubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceAll),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod)
    )

  val ukPropertySubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceProperty),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = None,
      accountingMethod = None
    )

  val overseasPropertySubscriptionData: Map[String, JsValue] =
    subscriptionData(
      incomeSource = Some(testIncomeSourceProperty),
      selectedTaxYear = Some(testAccountingYearCurrent),
      businessName = None,
      accountingMethod = None
    )

  def subscriptionData(
                        incomeSource: Option[IncomeSourceModel] = None,
                        selectedTaxYear: Option[AccountingYearModel] = None,
                        businessName: Option[BusinessNameModel] = None,
                        accountingMethod: Option[AccountingMethodModel] = None)
  : Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => SubscriptionDataKeys.IncomeSource -> IncomeSourceModel.format.writes(model)) ++
      selectedTaxYear.map(model => SubscriptionDataKeys.SelectedTaxYear -> AccountingYearModel.format.writes(model)) ++
      businessName.map(model => SubscriptionDataKeys.BusinessName -> BusinessNameModel.format.writes(model)) ++
      accountingMethod.map(model => SubscriptionDataKeys.AccountingMethod -> AccountingMethodModel.format.writes(model))
  }

  lazy val testIncomeSourceBusiness: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = false, foreignProperty = false)

  lazy val testIncomeSourceProperty: IncomeSourceModel = IncomeSourceModel(selfEmployment = false, ukProperty = true, foreignProperty = false)

  lazy val testIncomeSourceBoth: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = false)

  lazy val testIncomeSourceAll: IncomeSourceModel = IncomeSourceModel(selfEmployment = true, ukProperty = true, foreignProperty = true)

  // we don't verify date of birth since an incorrect one would not result in a match so it can be any date
  lazy val testClientDetails: UserDetailsModel = helpers.IntegrationTestModels.testUserDetails

  lazy val testSummaryDataSelfEmploymentData =
    Seq(SelfEmploymentData(
      id = testId,
      businessStartDate = Some(testBusinessStartDate),
      businessName = Some(testBusinessName),
      businessTradeName = Some(testBusinessTradeName),
      businessAddress = Some(BusinessAddressModel("auditRef", Address(Seq("line 1", "line 2"), "TF2 1PF")))
    ))
}
