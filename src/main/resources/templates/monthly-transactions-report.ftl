<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <style>
    @page {
      size: A4;
      margin: 18mm 15mm 18mm 15mm;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      font-family: Arial, Helvetica, sans-serif;
      font-size: 10pt;
      color: #111827;
      background-color: #ffffff;
    }

    /* ── Brand colours ── */
    /* Primary green : #1FA971  (oklch 0.62 0.155 162) */
    /* Dark green    : #167a52  */
    /* Light green   : #e6f7f0  */

    /* ── Header ── */
    .header {
      width: 100%;
      background-color: #1FA971;
      border-radius: 6px;
      padding: 14px 16px;
      margin-bottom: 14px;
    }

    .header-inner {
      width: 100%;
    }

    .header-inner table {
      width: 100%;
      border-collapse: collapse;
    }

    .header-logo-cell {
      width: 52px;
      vertical-align: middle;
    }

    .header-logo-cell svg {
      display: block;
    }

    .header-text-cell {
      vertical-align: middle;
      padding-left: 12px;
    }

    .header-app-name {
      font-size: 9pt;
      font-weight: bold;
      color: rgba(255,255,255,0.75);
      letter-spacing: 0.12em;
      text-transform: uppercase;
    }

    .header-title {
      font-size: 17pt;
      font-weight: bold;
      color: #ffffff;
      line-height: 1.15;
      margin-top: 2px;
    }

    .header-subtitle {
      font-size: 9.5pt;
      color: rgba(255,255,255,0.80);
      margin-top: 3px;
    }

    /* ── Info block ── */
    .info-block {
      width: 100%;
      margin-bottom: 14px;
      border: 1px solid #d1fae5;
      border-left: 4px solid #1FA971;
      background-color: #f0fdf8;
      padding: 8px 12px;
      border-radius: 0 4px 4px 0;
    }

    .info-block table {
      width: 100%;
      border-collapse: collapse;
    }

    .info-block td {
      padding: 2px 8px 2px 0;
      font-size: 9.5pt;
    }

    .info-label {
      font-weight: bold;
      color: #167a52;
      width: 130px;
    }

    /* ── Summary cards ── */
    .summary-row {
      width: 100%;
      margin-bottom: 14px;
    }

    .summary-row table {
      width: 100%;
      border-collapse: separate;
      border-spacing: 6px;
    }

    .summary-card {
      border: 1px solid #e5e7eb;
      border-radius: 6px;
      padding: 10px 14px;
      text-align: center;
      width: 33%;
    }

    .summary-card-income  { border-top: 3px solid #1FA971; }
    .summary-card-expense { border-top: 3px solid #ef4444; }
    .summary-card-balance { border-top: 3px solid #167a52; }

    .summary-card-label {
      font-size: 8pt;
      color: #6b7280;
      text-transform: uppercase;
      letter-spacing: 0.07em;
    }

    .summary-card-value {
      font-size: 13pt;
      font-weight: bold;
      margin-top: 5px;
    }

    .income-value  { color: #1FA971; }
    .expense-value { color: #ef4444; }
    .balance-value { color: #167a52; }

    /* ── Section title ── */
    .section-title {
      font-size: 10pt;
      font-weight: bold;
      color: #167a52;
      text-transform: uppercase;
      letter-spacing: 0.07em;
      margin-bottom: 6px;
      padding-bottom: 4px;
      border-bottom: 2px solid #1FA971;
    }

    /* ── Transactions table ── */
    .transactions-table {
      width: 100%;
      border-collapse: collapse;
      margin-bottom: 20px;
    }

    .transactions-table thead tr {
      background-color: #1FA971;
      color: #ffffff;
    }

    .transactions-table thead th {
      padding: 6px 8px;
      text-align: left;
      font-size: 9pt;
      font-weight: bold;
    }

    .transactions-table tbody tr {
      border-bottom: 1px solid #e5e7eb;
    }

    .transactions-table tbody tr:nth-child(even) {
      background-color: #f0fdf8;
    }

    .transactions-table tbody td {
      padding: 5px 8px;
      font-size: 9pt;
      vertical-align: middle;
    }

    .text-right  { text-align: right; }
    .text-center { text-align: center; }

    .badge {
      display: inline-block;
      padding: 1px 7px;
      border-radius: 8px;
      font-size: 7.5pt;
      font-weight: bold;
    }

    .badge-credit          { background-color: #d1fae5; color: #065f46; }
    .badge-debit           { background-color: #fee2e2; color: #991b1b; }
    .badge-transfer        { background-color: #dbeafe; color: #1d4ed8; }
    .badge-transfer_credit { background-color: #dbeafe; color: #1d4ed8; }
    .badge-transfer_debit  { background-color: #ede9fe; color: #6d28d9; }

    .amount-credit          { color: #1FA971; }
    .amount-debit           { color: #ef4444; }
    .amount-transfer        { color: #1d4ed8; }
    .amount-transfer_credit { color: #1d4ed8; }
    .amount-transfer_debit  { color: #6d28d9; }

    /* ── Empty state ── */
    .empty-state {
      text-align: center;
      padding: 28px;
      color: #9ca3af;
      font-size: 10pt;
      border: 1px dashed #d1fae5;
      border-radius: 6px;
    }

    /* ── Footer ── */
    .footer {
      border-top: 1px solid #d1fae5;
      padding-top: 6px;
      font-size: 8pt;
      color: #6b7280;
      text-align: center;
    }

    .footer-brand {
      color: #1FA971;
      font-weight: bold;
    }
  </style>
</head>
<body>

  <!-- Header with logo -->
  <div class="header">
    <div class="header-inner">
      <table>
        <tr>
          <td class="header-logo-cell">
            <img src="${logoDataUri}" alt="Astazou logo" width="46" height="46"
                 style="display:block; border-radius:8px;"/>
          </td>
          <td class="header-text-cell">
            <div class="header-app-name">${labels.appName}</div>
            <div class="header-title">${labels.reportTitle}</div>
            <div class="header-subtitle">
              ${monthName} ${year}<#if accountName?has_content> &nbsp;·&nbsp; ${accountName}</#if>
            </div>
          </td>
        </tr>
      </table>
    </div>
  </div>

  <!-- Info block -->
  <div class="info-block">
    <table>
      <tr>
        <td class="info-label">${labels.labelPeriod}:</td>
        <td>${monthName} / ${year}</td>
        <td class="info-label">${labels.labelAccount}:</td>
        <td>${accountName!"—"}</td>
      </tr>
      <tr>
        <td class="info-label">${labels.labelGeneratedAt}:</td>
        <td>${generatedAt}</td>
        <td class="info-label">${labels.labelTotalTransactions}:</td>
        <td>${transactions?size}</td>
      </tr>
    </table>
  </div>

  <!-- Summary cards -->
  <div class="summary-row">
    <table>
      <tr>
        <td class="summary-card summary-card-income">
          <div class="summary-card-label">${labels.labelIncome}</div>
          <div class="summary-card-value income-value">
            R$ ${income?string["#,##0.00"]}
          </div>
        </td>
        <td class="summary-card summary-card-expense">
          <div class="summary-card-label">${labels.labelExpenses}</div>
          <div class="summary-card-value expense-value">
            R$ ${expenses?string["#,##0.00"]}
          </div>
        </td>
        <td class="summary-card summary-card-balance">
          <div class="summary-card-label">${labels.labelBalance}</div>
          <div class="summary-card-value balance-value">
            R$ ${balance?string["#,##0.00"]}
          </div>
        </td>
      </tr>
    </table>
  </div>

  <!-- Transactions table -->
  <div class="section-title">${labels.sectionTransactions}</div>

  <#if transactions?has_content>
  <table class="transactions-table">
    <thead>
      <tr>
        <th style="width:90px;">${labels.colDate}</th>
        <th>${labels.colDescription}</th>
        <th style="width:85px;" class="text-center">${labels.colType}</th>
        <th style="width:110px;" class="text-right">${labels.colAmount}</th>
      </tr>
    </thead>
    <tbody>
      <#list transactions as tx>
      <#assign txTypeKey = tx.type!"debit">
      <#assign txTypeLabel>
        <#switch txTypeKey>
          <#case "credit">${labels.typeCredit}<#break>
          <#case "transfer">${labels.typeTransfer}<#break>
          <#case "transfer_credit">${labels.typeTransferCredit}<#break>
          <#case "transfer_debit">${labels.typeTransferDebit}<#break>
          <#default>${labels.typeDebit}
        </#switch>
      </#assign>
      <tr>
        <td>${tx.transactionDate}</td>
        <td>${tx.description!"—"}</td>
        <td class="text-center">
          <span class="badge badge-${txTypeKey}">${txTypeLabel?trim}</span>
        </td>
        <td class="text-right amount-${txTypeKey}">
          <#if tx.amount??>
            ${tx.amount?string["#,##0.00"]}
          <#else>
            —
          </#if>
        </td>
      </tr>
      </#list>
    </tbody>
  </table>
  <#else>
  <div class="empty-state">${labels.emptyState}</div>
  </#if>

  <!-- Footer -->
  <div class="footer">
    <span class="footer-brand">${labels.appName}</span> &nbsp;·&nbsp; ${labels.footerTagline} &nbsp;·&nbsp; ${generatedAt}
  </div>

</body>
</html>

