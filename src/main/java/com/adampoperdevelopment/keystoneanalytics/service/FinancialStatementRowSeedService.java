package com.adampoperdevelopment.keystoneanalytics.service;

import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRow;
import com.adampoperdevelopment.keystoneanalytics.entity.FinancialStatementRowTag;
import com.adampoperdevelopment.keystoneanalytics.entity.StatementCategory;
import com.adampoperdevelopment.keystoneanalytics.repository.FinancialStatementRowRepository;
import com.adampoperdevelopment.keystoneanalytics.repository.FinancialStatementRowTagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FinancialStatementRowSeedService {

    private static final Logger log = LoggerFactory.getLogger(FinancialStatementRowSeedService.class);

    private static final String US_GAAP = "us-gaap";

    private final FinancialStatementRowRepository rowRepository;
    private final FinancialStatementRowTagRepository tagRepository;

    public FinancialStatementRowSeedService(FinancialStatementRowRepository rowRepository,
                                            FinancialStatementRowTagRepository tagRepository) {
        this.rowRepository = rowRepository;
        this.tagRepository = tagRepository;
    }

    @Order(2)
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        log.info("Running financial statement row seed...");

        List<TermDef> terms = buildTermDefs();
        int createdRows = 0;
        int addedTags = 0;

        for (TermDef def : terms) {
            FinancialStatementRow row = rowRepository.findByTitle(def.title());

            if (row == null) {
                row = new FinancialStatementRow();
                row.setTitle(def.title());
                row.setStatementCategory(def.category());
                row.setDescription(def.description());
                row.setIsCalculated(false);
                row.setCreatedAt(System.currentTimeMillis());
                rowRepository.save(row);

                for (int i = 0; i < def.tags().length; i++) {
                    FinancialStatementRowTag tag = new FinancialStatementRowTag();
                    tag.setFinancialStatementRow(row);
                    tag.setTag(def.tags()[i]);
                    tag.setPriority(i + 1);
                    tag.setTaxonomy(US_GAAP);
                    tagRepository.save(tag);
                    addedTags++;
                }

                log.info("Created '{}' with {} tags", def.title(), def.tags().length);
                createdRows++;
            } else {
                Set<String> existing = tagRepository.findByFinancialStatementRowOrderByPriorityAsc(row)
                        .stream()
                        .map(FinancialStatementRowTag::getTag)
                        .collect(Collectors.toSet());

                int newTags = 0;
                for (int i = 0; i < def.tags().length; i++) {
                    if (!existing.contains(def.tags()[i])) {
                        FinancialStatementRowTag tag = new FinancialStatementRowTag();
                        tag.setFinancialStatementRow(row);
                        tag.setTag(def.tags()[i]);
                        tag.setPriority(i + 1);
                        tag.setTaxonomy(US_GAAP);
                        tagRepository.save(tag);
                        addedTags++;
                        newTags++;
                    }
                }

                if (newTags > 0) {
                    log.info("'{}': added {} missing tag(s)", def.title(), newTags);
                }
            }
        }

        log.info("Seed complete: {} new rows, {} new tags.", createdRows, addedTags);
    }

    private List<TermDef> buildTermDefs() {
        List<TermDef> terms = new ArrayList<>();

        // ── Cash Flow ─────────────────────────────────────────────────────────

        terms.add(new TermDef("Depreciation and Amortization", StatementCategory.CASH_FLOW,
                "Non-cash charge reducing the value of assets over time",
                "DepreciationDepletionAndAmortization",
                "DepreciationAndAmortization",
                "Depreciation"));

        terms.add(new TermDef("Stock Based Compensation", StatementCategory.CASH_FLOW,
                "Non-cash expense for equity awards granted to employees",
                "ShareBasedCompensation",
                "AllocatedShareBasedCompensationExpense"));

        terms.add(new TermDef("Common Stock Repurchased", StatementCategory.CASH_FLOW,
                "Cash spent repurchasing the company's own common shares",
                "PaymentsForRepurchaseOfCommonStock",
                "StockRepurchasedDuringPeriodValue"));

        terms.add(new TermDef("Operating Cash Flow", StatementCategory.CASH_FLOW,
                "Net cash generated from core business operations",
                "NetCashProvidedByUsedInOperatingActivities"));

        terms.add(new TermDef("Capital Expenditure", StatementCategory.CASH_FLOW,
                "Cash spent acquiring or maintaining physical assets",
                "PaymentsToAcquirePropertyPlantAndEquipment",
                "PaymentsToAcquireProductiveAssets"));

        // ── Income Statement ──────────────────────────────────────────────────

        terms.add(new TermDef("Revenue", StatementCategory.INCOME_STATEMENT,
                "Total income from goods and services sold",
                "RevenueFromContractWithCustomerExcludingAssessedTax",
                "Revenues",
                "SalesRevenueNet",
                "RevenueFromContractWithCustomerIncludingAssessedTax"));

        terms.add(new TermDef("Cost of Revenue", StatementCategory.INCOME_STATEMENT,
                "Direct costs attributable to production of goods or services sold",
                "CostOfRevenue",
                "CostOfGoodsAndServicesSold",
                "CostOfGoodsSold"));

        terms.add(new TermDef("Gross Profit", StatementCategory.INCOME_STATEMENT,
                "Revenue minus cost of revenue",
                "GrossProfit"));

        terms.add(new TermDef("General and Administrative Expenses", StatementCategory.INCOME_STATEMENT,
                "Overhead costs not directly tied to production",
                "GeneralAndAdministrativeExpense",
                "SellingGeneralAndAdministrativeExpense"));

        terms.add(new TermDef("Other Expenses", StatementCategory.INCOME_STATEMENT,
                "Non-operating or miscellaneous expenses not classified elsewhere",
                "OtherExpenses",
                "OtherNonoperatingExpense",
                "OtherCostAndExpenseOperating"));

        terms.add(new TermDef("Operating Expenses", StatementCategory.INCOME_STATEMENT,
                "Total expenses incurred from normal business operations",
                "OperatingExpenses",
                "CostsAndExpenses"));

        terms.add(new TermDef("Cost and Expenses", StatementCategory.INCOME_STATEMENT,
                "Combined cost of revenue and operating expenses",
                "CostsAndExpenses",
                "OperatingCostsAndExpenses"));

        terms.add(new TermDef("Interest Income", StatementCategory.INCOME_STATEMENT,
                "Income earned from interest-bearing assets",
                "InvestmentIncomeInterest",
                "InterestAndDividendIncomeOperating",
                "InterestIncomeOperating"));

        terms.add(new TermDef("Interest Expense", StatementCategory.INCOME_STATEMENT,
                "Cost of debt obligations and borrowings",
                "InterestExpense",
                "InterestAndDebtExpense",
                "InterestExpenseDebt"));

        terms.add(new TermDef("Operating Income", StatementCategory.INCOME_STATEMENT,
                "Profit from operations before interest and taxes",
                "OperatingIncomeLoss"));

        terms.add(new TermDef("Income Before Tax", StatementCategory.INCOME_STATEMENT,
                "Earnings before income tax expense is deducted",
                "IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest",
                "IncomeLossFromContinuingOperationsBeforeIncomeTaxesMinorityInterestAndIncomeLossFromEquityMethodInvestments"));

        terms.add(new TermDef("Income Tax Expense", StatementCategory.INCOME_STATEMENT,
                "Tax charged on pre-tax income; negative value indicates a benefit",
                "IncomeTaxExpenseBenefit"));

        terms.add(new TermDef("Net Income", StatementCategory.INCOME_STATEMENT,
                "Total earnings after all expenses and taxes",
                "NetIncomeLoss",
                "ProfitLoss",
                "NetIncomeLossAvailableToCommonStockholdersBasic"));

        terms.add(new TermDef("Earnings Per Share", StatementCategory.INCOME_STATEMENT,
                "Net income attributable to each outstanding share (USD per share)",
                "EarningsPerShareDiluted",
                "EarningsPerShareBasic"));

        // ── Balance Sheet ─────────────────────────────────────────────────────

        terms.add(new TermDef("Cash and Cash Equivalents", StatementCategory.BALANCE_SHEET,
                "Liquid assets including currency and short-duration instruments",
                "CashAndCashEquivalentsAtCarryingValue",
                "Cash"));

        terms.add(new TermDef("Short Term Investments", StatementCategory.BALANCE_SHEET,
                "Investments expected to be converted to cash within one year",
                "ShortTermInvestments",
                "MarketableSecuritiesCurrent",
                "AvailableForSaleSecuritiesCurrent"));

        terms.add(new TermDef("Cash and Short Term Investments", StatementCategory.BALANCE_SHEET,
                "Cash, equivalents, and short-term investment holdings combined",
                "CashCashEquivalentsAndShortTermInvestments"));

        terms.add(new TermDef("Accounts Receivable", StatementCategory.BALANCE_SHEET,
                "Amounts owed to the company by customers for goods or services delivered",
                "AccountsReceivableNetCurrent",
                "ReceivablesNetCurrent"));

        terms.add(new TermDef("Other Current Assets", StatementCategory.BALANCE_SHEET,
                "Current assets not classified in other specific categories",
                "OtherAssetsCurrent",
                "PrepaidExpenseAndOtherAssetsCurrent"));

        terms.add(new TermDef("Total Current Assets", StatementCategory.BALANCE_SHEET,
                "All assets expected to be converted to cash within one year",
                "AssetsCurrent"));

        terms.add(new TermDef("Property Plant and Equipment", StatementCategory.BALANCE_SHEET,
                "Tangible long-term assets net of accumulated depreciation",
                "PropertyPlantAndEquipmentNet",
                "PropertyPlantAndEquipmentGross"));

        terms.add(new TermDef("Long Term Investments", StatementCategory.BALANCE_SHEET,
                "Investments not expected to be liquidated within one year",
                "LongTermInvestments",
                "MarketableSecuritiesNoncurrent",
                "AvailableForSaleSecuritiesNoncurrent"));

        terms.add(new TermDef("Total Assets", StatementCategory.BALANCE_SHEET,
                "Sum of all current and non-current assets",
                "Assets"));

        terms.add(new TermDef("Accounts Payable", StatementCategory.BALANCE_SHEET,
                "Amounts owed by the company to suppliers for goods or services received",
                "AccountsPayableCurrent",
                "AccountsPayableAndAccruedLiabilitiesCurrent"));

        terms.add(new TermDef("Short Term Debt", StatementCategory.BALANCE_SHEET,
                "Debt obligations due within one year",
                "DebtCurrent",
                "ShortTermBorrowings",
                "LongTermDebtCurrentMaturities"));

        terms.add(new TermDef("Total Liabilities", StatementCategory.BALANCE_SHEET,
                "Sum of all current and non-current obligations",
                "Liabilities"));

        terms.add(new TermDef("Total Stockholder Equity", StatementCategory.BALANCE_SHEET,
                "Net assets attributable to shareholders",
                "StockholdersEquity",
                "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest"));

        terms.add(new TermDef("Total Investments", StatementCategory.BALANCE_SHEET,
                "Combined short and long-term investment holdings",
                "Investments",
                "AvailableForSaleSecurities"));

        terms.add(new TermDef("Total Debt", StatementCategory.BALANCE_SHEET,
                "All interest-bearing debt obligations, short and long term",
                "DebtLongtermAndShorttermCombinedAmount",
                "LongTermDebt"));

        return terms;
    }

    private record TermDef(String title, StatementCategory category, String description, String... tags) {}
}