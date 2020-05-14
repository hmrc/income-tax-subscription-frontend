
package helpers.agent

import models.{Cash, Current, DateModel, Next, Yes}
import models.common.{AccountingMethodModel, AccountingMethodPropertyModel, AccountingYearModel, BusinessNameModel}
import models.individual.business.{AccountingPeriodModel, MatchTaxYearModel}
import models.individual.subscription.{Both, Business, IncomeSourceType, Property}
import models.usermatching.UserDetailsModel
import play.api.libs.json.JsValue
import utilities.CacheConstants

object IntegrationTestModels {

  val testStartDate: DateModel = helpers.IntegrationTestModels.testStartDate
  val testEndDate: DateModel = helpers.IntegrationTestModels.testEndDate
  val testMatchTaxYearYes: MatchTaxYearModel = MatchTaxYearModel(Yes)
  val testAccountingYearNext: AccountingYearModel = AccountingYearModel(Next)
  val testAccountingYearCurrent: AccountingYearModel = AccountingYearModel(Current)
  val testAccountingPeriod: AccountingPeriodModel =
    testAccountingPeriod(testStartDate, testEndDate)

  def testAccountingPeriod(startDate: DateModel = testStartDate,
                           endDate: DateModel = testEndDate): AccountingPeriodModel =
    AccountingPeriodModel(startDate, endDate)

  val testBusinessName = BusinessNameModel("test business")
  val testAccountingMethod = AccountingMethodModel(Cash)
  val testPropertyAccountingMethod = AccountingMethodPropertyModel(Cash)

  val fullKeystoreData: Map[String, JsValue] =
    keystoreData(
      incomeSource = Some(testIncomeSourceBoth),
      matchTaxYear = Some(testMatchTaxYearYes),
      selectedTaxYear = Some(testAccountingYearCurrent),
      accountingPeriodDate = Some(testAccountingPeriod),
      businessName = Some(testBusinessName),
      accountingMethod = Some(testAccountingMethod),
      accountingMethodProperty = Some(testPropertyAccountingMethod)
    )

  def keystoreData(
                    incomeSource: Option[IncomeSourceType] = None,
                    matchTaxYear: Option[MatchTaxYearModel] = None,
                    selectedTaxYear: Option[AccountingYearModel] = None,
                    accountingPeriodDate: Option[AccountingPeriodModel] = None,
                    businessName: Option[BusinessNameModel] = None,
                    accountingMethod: Option[AccountingMethodModel] = None,
                    accountingMethodProperty: Option[AccountingMethodPropertyModel] = None): Map[String, JsValue] = {
    Map.empty[String, JsValue] ++
      incomeSource.map(model => CacheConstants.IncomeSource -> IncomeSourceType.format.writes(model)) ++
      accountingPeriodDate.map(model => CacheConstants.AccountingPeriodDate -> AccountingPeriodModel.format.writes(model)) ++
      businessName.map(model => CacheConstants.BusinessName -> BusinessNameModel.format.writes(model)) ++
      accountingMethod.map(model => CacheConstants.AccountingMethod -> AccountingMethodModel.format.writes(model)) ++
      accountingMethodProperty.map(model => CacheConstants.PropertyAccountingMethod -> AccountingMethodPropertyModel.format.writes(model)) ++
      matchTaxYear.map(model => CacheConstants.MatchTaxYear -> MatchTaxYearModel.format.writes(model)) ++
      selectedTaxYear.map(model => CacheConstants.SelectedTaxYear -> AccountingYearModel.format.writes(model))
  }

  lazy val testIncomeSourceBusiness: Business.type = Business

  lazy val testIncomeSourceProperty: Property.type = Property

  lazy val testIncomeSourceBoth: Both.type = Both

  // we don't verify date of birth since an incorrect one would not result in a match so it can be any date
  lazy val testClientDetails: UserDetailsModel = helpers.IntegrationTestModels.testUserDetails

}
