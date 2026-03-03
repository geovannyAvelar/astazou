<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <style>
    @page {
      size: A4;
      margin: 20mm 15mm 20mm 15mm;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      font-family: Arial, Helvetica, sans-serif;
      font-size: 10pt;
      color: #1a1a1a;
      background-color: #ffffff;
    }

    /* ── Header ── */
    .header {
      width: 100%;
      border-bottom: 2px solid #2563eb;
      padding-bottom: 8px;
      margin-bottom: 16px;
    }

    .header-title {
      font-size: 18pt;
      font-weight: bold;
      color: #2563eb;
    }

    .header-subtitle {
      font-size: 10pt;
      color: #6b7280;
      margin-top: 2px;
    }

    /* ── Info block ── */
    .info-block {
      width: 100%;
      margin-bottom: 16px;
      border: 1px solid #e5e7eb;
      background-color: #f9fafb;
      padding: 8px 12px;
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
      color: #374151;
      width: 120px;
    }

    /* ── Summary cards ── */
    .summary-row {
      width: 100%;
      margin-bottom: 16px;
    }

    .summary-row table {
      width: 100%;
      border-collapse: separate;
      border-spacing: 6px;
    }

    .summary-card {
      border: 1px solid #e5e7eb;
      border-radius: 4px;
      padding: 8px 12px;
      text-align: center;
      width: 33%;
    }

    .summary-card-label {
      font-size: 8.5pt;
      color: #6b7280;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .summary-card-value {
      font-size: 13pt;
      font-weight: bold;
      margin-top: 4px;
    }

    .income-value  { color: #16a34a; }
    .expense-value { color: #dc2626; }
    .balance-value { color: #2563eb; }

    /* ── Transactions table ── */
    .transactions-table {
      width: 100%;
      border-collapse: collapse;
      margin-bottom: 20px;
    }

    .transactions-table thead tr {
      background-color: #2563eb;
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
      background-color: #f3f4f6;
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
      padding: 1px 6px;
      border-radius: 8px;
      font-size: 8pt;
      font-weight: bold;
    }

    .badge-credit          { background-color: #dcfce7; color: #15803d; }
    .badge-debit           { background-color: #fee2e2; color: #b91c1c; }
    .badge-transfer        { background-color: #dbeafe; color: #1d4ed8; }
    .badge-transfer_credit { background-color: #dbeafe; color: #1d4ed8; }
    .badge-transfer_debit  { background-color: #ede9fe; color: #6d28d9; }

    .amount-credit          { color: #16a34a; }
    .amount-debit           { color: #dc2626; }
    .amount-transfer        { color: #1d4ed8; }
    .amount-transfer_credit { color: #1d4ed8; }
    .amount-transfer_debit  { color: #6d28d9; }

    /* ── Empty state ── */
    .empty-state {
      text-align: center;
      padding: 24px;
      color: #9ca3af;
      font-size: 10pt;
    }

    /* ── Footer ── */
    .footer {
      border-top: 1px solid #e5e7eb;
      padding-top: 6px;
      font-size: 8pt;
      color: #9ca3af;
      text-align: center;
    }
  </style>
</head>
<body>

  <!-- Header -->
  <div class="header">
    <div class="header-title">Monthly Transaction Report</div>
    <div class="header-subtitle">
      ${monthName} ${year}
      <#if accountName?has_content> &nbsp;·&nbsp; Account: ${accountName}</#if>
    </div>
  </div>

  <!-- Info block -->
  <div class="info-block">
    <table>
      <tr>
        <td class="info-label">Period:</td>
        <td>${monthName} / ${year}</td>
        <td class="info-label">Account:</td>
        <td>${accountName!"—"}</td>
      </tr>
      <tr>
        <td class="info-label">Generated at:</td>
        <td>${generatedAt}</td>
        <td class="info-label">Total transactions:</td>
        <td>${transactions?size}</td>
      </tr>
    </table>
  </div>

  <!-- Summary cards -->
  <div class="summary-row">
    <table>
      <tr>
        <td class="summary-card">
          <div class="summary-card-label">Income</div>
          <div class="summary-card-value income-value">
            R$ ${income?string["#,##0.00"]}
          </div>
        </td>
        <td class="summary-card">
          <div class="summary-card-label">Expenses</div>
          <div class="summary-card-value expense-value">
            R$ ${expenses?string["#,##0.00"]}
          </div>
        </td>
        <td class="summary-card">
          <div class="summary-card-label">Balance</div>
          <div class="summary-card-value balance-value">
            R$ ${balance?string["#,##0.00"]}
          </div>
        </td>
      </tr>
    </table>
  </div>

  <!-- Transactions table -->
  <#if transactions?has_content>
  <table class="transactions-table">
    <thead>
      <tr>
        <th style="width:90px;">Date</th>
        <th>Description</th>
        <th style="width:80px;" class="text-center">Type</th>
        <th style="width:110px;" class="text-right">Amount (R$)</th>
      </tr>
    </thead>
    <tbody>
      <#list transactions as tx>
      <tr>
        <td>${tx.transactionDate}</td>
        <td>${tx.description!"—"}</td>
        <td class="text-center">
          <span class="badge badge-${tx.type!"debit"}">${tx.type!"—"}</span>
        </td>
        <td class="text-right amount-${tx.type!"debit"}">
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
  <div class="empty-state">No transactions found for this period.</div>
  </#if>

  <!-- Footer -->
  <div class="footer">
    Generated by Astazou &nbsp;·&nbsp; ${generatedAt}
  </div>

</body>
</html>

