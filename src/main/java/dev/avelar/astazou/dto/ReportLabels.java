package dev.avelar.astazou.dto;

/**
 * Holds all i18n-aware labels passed to the monthly-transactions-report FreeMarker template.
 */
public class ReportLabels {

    // Header / title
    public final String appName;
    public final String reportTitle;

    // Info block
    public final String labelPeriod;
    public final String labelAccount;
    public final String labelGeneratedAt;
    public final String labelTotalTransactions;

    // Summary cards
    public final String labelIncome;
    public final String labelExpenses;
    public final String labelBalance;

    // Table headers
    public final String colDate;
    public final String colDescription;
    public final String colType;
    public final String colAmount;

    // Transaction types
    public final String typeDebit;
    public final String typeCredit;
    public final String typeTransfer;
    public final String typeTransferCredit;
    public final String typeTransferDebit;

    // Empty state
    public final String emptyState;

    // Footer
    public final String footerTagline;

    // Section heading
    public final String sectionTransactions;

    // QR code validation
    public final String qrValidationTitle;
    public final String qrValidationSubtitle;

    private ReportLabels(Builder b) {
        this.appName = b.appName;
        this.reportTitle = b.reportTitle;
        this.labelPeriod = b.labelPeriod;
        this.labelAccount = b.labelAccount;
        this.labelGeneratedAt = b.labelGeneratedAt;
        this.labelTotalTransactions = b.labelTotalTransactions;
        this.labelIncome = b.labelIncome;
        this.labelExpenses = b.labelExpenses;
        this.labelBalance = b.labelBalance;
        this.colDate = b.colDate;
        this.colDescription = b.colDescription;
        this.colType = b.colType;
        this.colAmount = b.colAmount;
        this.typeDebit = b.typeDebit;
        this.typeCredit = b.typeCredit;
        this.typeTransfer = b.typeTransfer;
        this.typeTransferCredit = b.typeTransferCredit;
        this.typeTransferDebit = b.typeTransferDebit;
        this.emptyState = b.emptyState;
        this.footerTagline = b.footerTagline;
        this.sectionTransactions = b.sectionTransactions;
        this.qrValidationTitle = b.qrValidationTitle;
        this.qrValidationSubtitle = b.qrValidationSubtitle;
    }

    // ── Getters (required by FreeMarker bean wrapper) ────────────────────────

    public String getAppName()                { return appName; }
    public String getReportTitle()            { return reportTitle; }
    public String getLabelPeriod()            { return labelPeriod; }
    public String getLabelAccount()           { return labelAccount; }
    public String getLabelGeneratedAt()       { return labelGeneratedAt; }
    public String getLabelTotalTransactions() { return labelTotalTransactions; }
    public String getLabelIncome()            { return labelIncome; }
    public String getLabelExpenses()          { return labelExpenses; }
    public String getLabelBalance()           { return labelBalance; }
    public String getColDate()                { return colDate; }
    public String getColDescription()         { return colDescription; }
    public String getColType()                { return colType; }
    public String getColAmount()              { return colAmount; }
    public String getTypeDebit()              { return typeDebit; }
    public String getTypeCredit()             { return typeCredit; }
    public String getTypeTransfer()           { return typeTransfer; }
    public String getTypeTransferCredit()     { return typeTransferCredit; }
    public String getTypeTransferDebit()      { return typeTransferDebit; }
    public String getEmptyState()             { return emptyState; }
    public String getFooterTagline()          { return footerTagline; }
    public String getSectionTransactions()    { return sectionTransactions; }
    public String getQrValidationTitle()      { return qrValidationTitle; }
    public String getQrValidationSubtitle()   { return qrValidationSubtitle; }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static ReportLabels forLocale(String lang) {
        if (lang == null) lang = "en";
        return switch (lang.toLowerCase()) {
            case "pt" -> pt();
            case "es" -> es();
            default  -> en();
        };
    }

    private static ReportLabels en() {
        return new Builder()
                .appName("Astazou")
                .reportTitle("Monthly Transaction Report")
                .labelPeriod("Period")
                .labelAccount("Account")
                .labelGeneratedAt("Generated at")
                .labelTotalTransactions("Total transactions")
                .labelIncome("Income")
                .labelExpenses("Expenses")
                .labelBalance("Balance")
                .colDate("Date")
                .colDescription("Description")
                .colType("Type")
                .colAmount("Amount (R$)")
                .typeDebit("Debit")
                .typeCredit("Credit")
                .typeTransfer("Transfer")
                .typeTransferCredit("Transfer (Credit)")
                .typeTransferDebit("Transfer (Debit)")
                .emptyState("No transactions found for this period.")
                .footerTagline("Personal Finance Manager")
                .sectionTransactions("Transactions")
                .qrValidationTitle("Verify this report")
                .qrValidationSubtitle("Scan the QR code or visit the URL below to confirm this report was generated by Astazou.")
                .build();
    }

    private static ReportLabels pt() {
        return new Builder()
                .appName("Astazou")
                .reportTitle("Relatório Mensal de Transações")
                .labelPeriod("Período")
                .labelAccount("Conta")
                .labelGeneratedAt("Gerado em")
                .labelTotalTransactions("Total de transações")
                .labelIncome("Receita")
                .labelExpenses("Despesas")
                .labelBalance("Saldo")
                .colDate("Data")
                .colDescription("Descrição")
                .colType("Tipo")
                .colAmount("Valor (R$)")
                .typeDebit("Débito")
                .typeCredit("Crédito")
                .typeTransfer("Transferência")
                .typeTransferCredit("Transferência (Crédito)")
                .typeTransferDebit("Transferência (Débito)")
                .emptyState("Nenhuma transação encontrada para este período.")
                .footerTagline("Gerenciador de Finanças Pessoais")
                .sectionTransactions("Transações")
                .qrValidationTitle("Verifique este relatório")
                .qrValidationSubtitle("Escaneie o QR code ou acesse o link abaixo para confirmar que este relatório foi gerado pelo Astazou.")
                .build();
    }

    private static ReportLabels es() {
        return new Builder()
                .appName("Astazou")
                .reportTitle("Informe Mensual de Transacciones")
                .labelPeriod("Período")
                .labelAccount("Cuenta")
                .labelGeneratedAt("Generado el")
                .labelTotalTransactions("Total de transacciones")
                .labelIncome("Ingresos")
                .labelExpenses("Gastos")
                .labelBalance("Saldo")
                .colDate("Fecha")
                .colDescription("Descripción")
                .colType("Tipo")
                .colAmount("Monto (R$)")
                .typeDebit("Débito")
                .typeCredit("Crédito")
                .typeTransfer("Transferencia")
                .typeTransferCredit("Transferencia (Crédito)")
                .typeTransferDebit("Transferencia (Débito)")
                .emptyState("No se encontraron transacciones para este período.")
                .footerTagline("Administrador de Finanzas Personales")
                .sectionTransactions("Transacciones")
                .qrValidationTitle("Verifique este informe")
                .qrValidationSubtitle("Escanee el código QR o visite la URL a continuación para confirmar que este informe fue generado por Astazou.")
                .build();
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static class Builder {
        private String appName, reportTitle, labelPeriod, labelAccount, labelGeneratedAt,
                labelTotalTransactions, labelIncome, labelExpenses, labelBalance,
                colDate, colDescription, colType, colAmount,
                typeDebit, typeCredit, typeTransfer, typeTransferCredit, typeTransferDebit,
                emptyState, footerTagline, sectionTransactions,
                qrValidationTitle, qrValidationSubtitle;

        public Builder appName(String v)               { appName = v; return this; }
        public Builder reportTitle(String v)           { reportTitle = v; return this; }
        public Builder labelPeriod(String v)           { labelPeriod = v; return this; }
        public Builder labelAccount(String v)          { labelAccount = v; return this; }
        public Builder labelGeneratedAt(String v)      { labelGeneratedAt = v; return this; }
        public Builder labelTotalTransactions(String v){ labelTotalTransactions = v; return this; }
        public Builder labelIncome(String v)           { labelIncome = v; return this; }
        public Builder labelExpenses(String v)         { labelExpenses = v; return this; }
        public Builder labelBalance(String v)          { labelBalance = v; return this; }
        public Builder colDate(String v)               { colDate = v; return this; }
        public Builder colDescription(String v)        { colDescription = v; return this; }
        public Builder colType(String v)               { colType = v; return this; }
        public Builder colAmount(String v)             { colAmount = v; return this; }
        public Builder typeDebit(String v)             { typeDebit = v; return this; }
        public Builder typeCredit(String v)            { typeCredit = v; return this; }
        public Builder typeTransfer(String v)          { typeTransfer = v; return this; }
        public Builder typeTransferCredit(String v)    { typeTransferCredit = v; return this; }
        public Builder typeTransferDebit(String v)     { typeTransferDebit = v; return this; }
        public Builder emptyState(String v)            { emptyState = v; return this; }
        public Builder footerTagline(String v)         { footerTagline = v; return this; }
        public Builder sectionTransactions(String v)   { sectionTransactions = v; return this; }
        public Builder qrValidationTitle(String v)     { qrValidationTitle = v; return this; }
        public Builder qrValidationSubtitle(String v)  { qrValidationSubtitle = v; return this; }

        public ReportLabels build() { return new ReportLabels(this); }
    }
}

