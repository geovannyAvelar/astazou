export type Locale = "en" | "pt" | "es"

export interface Translations {
    // Login page - branding panel
    brandTagline: string
    brandDescription: string
    featureBalanceTracking: string
    featureSpendingAnalytics: string
    featureSecurity: string
    trustedBy: string

    // Login form
    loginTitle: string
    loginDescription: string
    usernameLabel: string
    usernamePlaceholder: string
    passwordLabel: string
    passwordPlaceholder: string
    loginSubmit: string
    loginSubmitting: string
    loginErrorEmpty: string
    loginFooterSecurity: string
    hidePassword: string
    showPassword: string

    // Dashboard
    greetingMorning: string
    greetingAfternoon: string
    greetingEvening: string
    dashboardOverview: string
    totalBalance: string
    monthlyIncome: string
    monthlyExpenses: string
    savings: string
    recentTransactions: string
    recentTransactionsDescription: string
    signOut: string
    loading: string

    // Bank Accounts
    bankAccounts: string
    bankAccountsDescription: string
    createAccount: string
    accountName: string
    accountNamePlaceholder: string
    initialBalance: string
    initialBalancePlaceholder: string
    creating: string
    noAccounts: string
    noAccountsDescription: string
    balance: string
    backToDashboard: string
    accountCreated: string
    accountCreateError: string
    accountNameRequired: string
    page: string
    of: string
    previous: string
    next: string

    // Transactions
    transactions: string
    transactionsDescription: string
    uploadPdf: string
    uploadPdfDescription: string
    selectAccount: string
    selectAccountPlaceholder: string
    dragAndDrop: string
    dragAndDropOr: string
    browseFiles: string
    uploading: string
    uploadSuccess: string
    uploadError: string
    noFileSelected: string
    noAccountSelected: string
    noTransactions: string
    noTransactionsDescription: string
    transactionDate: string
    transactionDescription: string
    transactionAmount: string
    backToAccounts: string
    pdfOnly: string
    fileSelected: string
    removeFile: string
    createTransaction: string
    createTransactionDescription: string
    transactionDescriptionPlaceholder: string
    transactionType: string
    debit: string
    credit: string
    bankAccount: string
    cancel: string
    create: string
    actions: string
    deleteTransaction: string
    deleteTransactionDescription: string
    deleting: string
    delete: string
    updateAccount: string

    // Search
    search: string
    searchTransactions: string
    searchPlaceholder: string
    dateRange: string
    from: string
    to: string
    noSearchResults: string
    noSearchResultsDescription: string

    // Transform to Transfer
    transformToTransfer: string
    transformToTransferDescription: string
    selectDestinationAccount: string
    transforming: string
    transform: string
    transformSuccess: string
    transformError: string
    cannotTransformSameAccount: string

    // Edit Account
    editAccount: string
    editAccountDescription: string
    currentBalance: string
    saving: string
    save: string
    accountUpdated: string
    accountUpdateError: string

    // Language
    language: string

    month: string
    year: string

    // Credit Cards
    creditCards: string
    creditCardsDescription: string
    createCreditCard: string
    creditCardName: string
    creditCardNamePlaceholder: string
    creditCardNumber: string
    creditCardNumberPlaceholder: string
    creditCardBrand: string
    creditCardBrandPlaceholder: string
    creditCardCreated: string
    creditCardCreateError: string
    creditCardNameRequired: string
    creditCardNumber4Digits: string
    noCreditCards: string
    noCreditCardsDescription: string
    creditCardTransactions: string
    creditCardTransactionsDescription: string
    statementMonth: string
    selectMonth: string
    creditCardStatement: string

    // OFX Upload for bank accounts
    uploadOfx: string
    uploadOfxDescription: string
    ofxOnly: string
    uploadOfxSuccess: string
    uploadMode: string
    uploadModePdf: string
    uploadModeOfx: string

    // Session
    sessionExpiredTitle: string
    sessionExpiredMessage: string
}

export const translations: Record<Locale, Translations> = {
    en: {
        brandTagline: "Take control of your financial future",
        brandDescription:
            "Track expenses, manage budgets, and grow your savings with powerful insights at your fingertips.",
        featureBalanceTracking: "Real-time balance tracking",
        featureSpendingAnalytics: "Spending analytics and insights",
        featureSecurity: "Bank-grade security",
        trustedBy: "Trusted by thousands of users worldwide",

        loginTitle: "Welcome back",
        loginDescription: "Sign in to your account to continue",
        usernameLabel: "Username",
        usernamePlaceholder: "Enter your username",
        passwordLabel: "Password",
        passwordPlaceholder: "Enter your password",
        loginSubmit: "Sign in",
        loginSubmitting: "Signing in...",
        loginErrorEmpty: "Please enter both username and password.",
        loginFooterSecurity:
            "Your data is encrypted and secured with industry-standard protocols.",
        hidePassword: "Hide password",
        showPassword: "Show password",

        greetingMorning: "Good morning",
        greetingAfternoon: "Good afternoon",
        greetingEvening: "Good evening",
        dashboardOverview: "Here's an overview of your finances.",
        totalBalance: "Total Balance",
        monthlyIncome: "Monthly Income",
        monthlyExpenses: "Monthly Expenses",
        savings: "Savings",
        recentTransactions: "Recent Transactions",
        recentTransactionsDescription: "Your latest financial activity",
        signOut: "Sign out",
        loading: "Loading...",

        bankAccounts: "Bank Accounts",
        bankAccountsDescription: "Manage your bank accounts and balances.",
        createAccount: "Create Account",
        accountName: "Account Name",
        accountNamePlaceholder: "e.g. Checking, Savings...",
        initialBalance: "Initial Balance",
        initialBalancePlaceholder: "0.00",
        creating: "Creating...",
        noAccounts: "No accounts yet",
        noAccountsDescription: "Create your first bank account to get started.",
        balance: "Balance",
        backToDashboard: "Back to Dashboard",
        accountCreated: "Account created successfully!",
        accountCreateError: "Failed to create account. Please try again.",
        accountNameRequired: "Please enter an account name.",
        page: "Page",
        of: "of",
        previous: "Previous",
        next: "Next",

        transactions: "Transactions",
        transactionsDescription: "View and import transactions for your accounts.",
        uploadPdf: "Import Statement",
        uploadPdfDescription: "Upload an Itau bank statement PDF to import transactions automatically.",
        selectAccount: "Account",
        selectAccountPlaceholder: "Select an account...",
        dragAndDrop: "Drag and drop your PDF here",
        dragAndDropOr: "or",
        browseFiles: "Browse files",
        uploading: "Uploading...",
        uploadSuccess: "Statement uploaded successfully! Transactions are being processed.",
        uploadError: "Failed to upload statement. Please try again.",
        noFileSelected: "Please select a PDF file.",
        noAccountSelected: "Please select an account.",
        noTransactions: "No transactions yet",
        noTransactionsDescription: "Upload a bank statement to import transactions.",
        transactionDate: "Date",
        transactionDescription: "Description",
        transactionAmount: "Amount",
        backToAccounts: "Back to Accounts",
        pdfOnly: "PDF files only",
        fileSelected: "File selected",
        removeFile: "Remove file",
        createTransaction: "Create Transaction",
        createTransactionDescription: "Add a new transaction manually",
        transactionDescriptionPlaceholder: "Enter description...",
        transactionType: "Type",
        debit: "Debit",
        credit: "Credit",
        bankAccount: "Bank Account",
        cancel: "Cancel",
        create: "Create",
        actions: "Actions",
        deleteTransaction: "Delete transaction",
        deleteTransactionDescription: "This action cannot be undone.",
        deleting: "Deleting...",
        delete: "Delete",
        updateAccount: "Update account balance",

        // Search
        search: "Search",
        searchTransactions: "Search Transactions",
        searchPlaceholder: "Search by description...",
        dateRange: "Date Range",
        from: "From",
        to: "To",
        noSearchResults: "No transactions found",
        noSearchResultsDescription: "Try adjusting your search terms or date range",

        // Transform to Transfer
        transformToTransfer: "Transform to Transfer",
        transformToTransferDescription: "Convert this debit transaction into a transfer to another account",
        selectDestinationAccount: "Destination Account",
        transforming: "Transforming...",
        transform: "Transform",
        transformSuccess: "Transaction transformed to transfer successfully!",
        transformError: "Failed to transform transaction. Please try again.",
        cannotTransformSameAccount: "Source and destination accounts must be different.",

        editAccount: "Edit Account",
        editAccountDescription: "Update account name and balance",
        currentBalance: "Current Balance",
        saving: "Saving...",
        save: "Save",
        accountUpdated: "Account updated successfully!",
        accountUpdateError: "Failed to update account. Please try again.",

        language: "Language",

        month: "Month",
        year: "Year",

        creditCards: "Credit Cards",
        creditCardsDescription: "Manage your credit cards and view statements.",
        createCreditCard: "Create Credit Card",
        creditCardName: "Card Name",
        creditCardNamePlaceholder: "e.g. Personal, Business...",
        creditCardNumber: "Last 4 Digits",
        creditCardNumberPlaceholder: "Enter last 4 digits",
        creditCardBrand: "Card Brand",
        creditCardBrandPlaceholder: "e.g. Visa, Mastercard...",
        creditCardCreated: "Credit card created successfully!",
        creditCardCreateError: "Failed to create credit card. Please try again.",
        creditCardNameRequired: "Please enter a card name.",
        creditCardNumber4Digits: "Please enter 4 digits.",
        noCreditCards: "No credit cards yet",
        noCreditCardsDescription: "Create your first credit card to get started.",
        creditCardTransactions: "Card Transactions",
        creditCardTransactionsDescription: "View your credit card statement and transactions.",
        statementMonth: "Statement Month",
        selectMonth: "Select month and year...",
        creditCardStatement: "Statement",

        uploadOfx: "Import OFX",
        uploadOfxDescription: "Upload an OFX bank statement file to import transactions automatically.",
        ofxOnly: "OFX files only (.ofx, .txt)",
        uploadOfxSuccess: "OFX file uploaded successfully! Transactions are being processed.",
        uploadMode: "Import type",
        uploadModePdf: "Itaú PDF",
        uploadModeOfx: "OFX File",

        sessionExpiredTitle: "Session ended",
        sessionExpiredMessage: "Your session has expired due to inactivity. Please sign in again.",
    },
    pt: {
        brandTagline: "Assuma o controle do seu futuro financeiro",
        brandDescription:
            "Acompanhe despesas, gerencie orçamentos e aumente suas economias com insights poderosos na palma da mão.",
        featureBalanceTracking: "Acompanhamento de saldo em tempo real",
        featureSpendingAnalytics: "Análise de gastos e insights",
        featureSecurity: "Segurança de nível bancário",
        trustedBy: "Confiado por milhares de usuários no mundo todo",

        loginTitle: "Bem-vindo de volta",
        loginDescription: "Entre na sua conta para continuar",
        usernameLabel: "Usuário",
        usernamePlaceholder: "Digite seu usuário",
        passwordLabel: "Senha",
        passwordPlaceholder: "Digite sua senha",
        loginSubmit: "Entrar",
        loginSubmitting: "Entrando...",
        loginErrorEmpty: "Por favor, insira o usuário e a senha.",
        loginFooterSecurity:
            "Seus dados são criptografados e protegidos com protocolos padrão da indústria.",
        hidePassword: "Ocultar senha",
        showPassword: "Mostrar senha",

        greetingMorning: "Bom dia",
        greetingAfternoon: "Boa tarde",
        greetingEvening: "Boa noite",
        dashboardOverview: "Aqui está um resumo das suas finanças.",
        totalBalance: "Saldo Total",
        monthlyIncome: "Renda Mensal",
        monthlyExpenses: "Despesas Mensais",
        savings: "Economias",
        recentTransactions: "Transações Recentes",
        recentTransactionsDescription: "Sua atividade financeira mais recente",
        signOut: "Sair",
        loading: "Carregando...",

        bankAccounts: "Contas Bancárias",
        bankAccountsDescription: "Gerencie suas contas bancárias e saldos.",
        createAccount: "Criar Conta",
        accountName: "Nome da Conta",
        accountNamePlaceholder: "ex. Corrente, Poupança...",
        initialBalance: "Saldo Inicial",
        initialBalancePlaceholder: "0,00",
        creating: "Criando...",
        noAccounts: "Nenhuma conta ainda",
        noAccountsDescription: "Crie sua primeira conta bancária para começar.",
        balance: "Saldo",
        backToDashboard: "Voltar ao Painel",
        accountCreated: "Conta criada com sucesso!",
        accountCreateError: "Falha ao criar conta. Tente novamente.",
        accountNameRequired: "Por favor, insira o nome da conta.",
        page: "Página",
        of: "de",
        previous: "Anterior",
        next: "Próxima",

        transactions: "Transações",
        transactionsDescription: "Veja e importe transações das suas contas.",
        uploadPdf: "Importar Extrato",
        uploadPdfDescription: "Envie um extrato PDF do Itau para importar transações automaticamente.",
        selectAccount: "Conta",
        selectAccountPlaceholder: "Selecione uma conta...",
        dragAndDrop: "Arraste e solte seu PDF aqui",
        dragAndDropOr: "ou",
        browseFiles: "Procurar arquivos",
        uploading: "Enviando...",
        uploadSuccess: "Extrato enviado com sucesso! As transações estão sendo processadas.",
        uploadError: "Falha ao enviar extrato. Tente novamente.",
        noFileSelected: "Por favor, selecione um arquivo PDF.",
        noAccountSelected: "Por favor, selecione uma conta.",
        noTransactions: "Nenhuma transação ainda",
        noTransactionsDescription: "Envie um extrato bancário para importar transações.",
        transactionDate: "Data",
        transactionDescription: "Descrição",
        transactionAmount: "Valor",
        backToAccounts: "Voltar às Contas",
        pdfOnly: "Apenas arquivos PDF",
        fileSelected: "Arquivo selecionado",
        removeFile: "Remover arquivo",
        createTransaction: "Criar Transação",
        createTransactionDescription: "Adicionar uma nova transação manualmente",
        transactionDescriptionPlaceholder: "Digite a descrição...",
        transactionType: "Tipo",
        debit: "Débito",
        credit: "Crédito",
        bankAccount: "Conta Bancária",
        cancel: "Cancelar",
        create: "Criar",
        actions: "Ações",
        deleteTransaction: "Excluir transação",
        deleteTransactionDescription: "Esta ação não pode ser desfeita.",
        deleting: "Excluindo...",
        delete: "Excluir",
        updateAccount: "Atualizar saldo da conta",

        // Search
        search: "Pesquisar",
        searchTransactions: "Pesquisar Transacoes",
        searchPlaceholder: "Pesquisar por descricao...",
        dateRange: "Intervalo de Datas",
        from: "De",
        to: "Ate",
        noSearchResults: "Nenhuma transacao encontrada",
        noSearchResultsDescription: "Tente ajustar seus termos de pesquisa ou intervalo de datas",

        // Transform to Transfer
        transformToTransfer: "Transformar em Transferencia",
        transformToTransferDescription: "Converter esta transação de débito em transferência para outra conta",
        selectDestinationAccount: "Conta de Destino",
        transforming: "Transformando...",
        transform: "Transformar",
        transformSuccess: "Transação transformada em transferência com sucesso!",
        transformError: "Falha ao transformar transação. Tente novamente.",
        cannotTransformSameAccount: "As contas de origem e destino devem ser diferentes.",

        editAccount: "Editar Conta",
        editAccountDescription: "Atualizar nome e saldo da conta",
        currentBalance: "Saldo Atual",
        saving: "Salvando...",
        save: "Salvar",
        accountUpdated: "Conta atualizada com sucesso!",
        accountUpdateError: "Falha ao atualizar conta. Tente novamente.",

        language: "Idioma",

        month: "Mês",
        year: "Ano",

        creditCards: "Cartões de Crédito",
        creditCardsDescription: "Gerencie seus cartões de crédito e visualize extratos.",
        createCreditCard: "Criar Cartão de Crédito",
        creditCardName: "Nome do Cartão",
        creditCardNamePlaceholder: "ex. Pessoal, Comercial...",
        creditCardNumber: "Últimos 4 Dígitos",
        creditCardNumberPlaceholder: "Digite os últimos 4 dígitos",
        creditCardBrand: "Bandeira do Cartão",
        creditCardBrandPlaceholder: "ex. Visa, Mastercard...",
        creditCardCreated: "Cartão de crédito criado com sucesso!",
        creditCardCreateError: "Falha ao criar cartão de crédito. Tente novamente.",
        creditCardNameRequired: "Por favor, insira o nome do cartão.",
        creditCardNumber4Digits: "Por favor, insira 4 dígitos.",
        noCreditCards: "Nenhum cartão de crédito ainda",
        noCreditCardsDescription: "Crie seu primeiro cartão de crédito para começar.",
        creditCardTransactions: "Transações do Cartão",
        creditCardTransactionsDescription: "Visualize o extrato e transações do seu cartão de crédito.",
        statementMonth: "Mês do Extrato",
        selectMonth: "Selecione mês e ano...",
        creditCardStatement: "Extrato",

        uploadOfx: "Importar OFX",
        uploadOfxDescription: "Envie um arquivo OFX de extrato bancário para importar transações automaticamente.",
        ofxOnly: "Apenas arquivos OFX (.ofx, .txt)",
        uploadOfxSuccess: "Arquivo OFX enviado com sucesso! As transações estão sendo processadas.",
        uploadMode: "Tipo de importação",
        uploadModePdf: "PDF Itaú",
        uploadModeOfx: "Arquivo OFX",

        sessionExpiredTitle: "Sessão encerrada",
        sessionExpiredMessage: "Sua sessão foi encerrada por inatividade. Por favor, faça login novamente.",
    },
    es: {
        brandTagline: "Toma el control de tu futuro financiero",
        brandDescription:
            "Rastrea gastos, administra presupuestos y haz crecer tus ahorros con información poderosa al alcance de tu mano.",
        featureBalanceTracking: "Seguimiento de saldo en tiempo real",
        featureSpendingAnalytics: "Análisis de gastos e información",
        featureSecurity: "Seguridad de nivel bancario",
        trustedBy: "Confiado por miles de usuarios en todo el mundo",

        loginTitle: "Bienvenido de vuelta",
        loginDescription: "Inicia sesión en tu cuenta para continuar",
        usernameLabel: "Usuario",
        usernamePlaceholder: "Ingresa tu usuario",
        passwordLabel: "Contraseña",
        passwordPlaceholder: "Ingresa tu contraseña",
        loginSubmit: "Iniciar sesión",
        loginSubmitting: "Iniciando sesión...",
        loginErrorEmpty: "Por favor, ingresa el usuario y la contraseña.",
        loginFooterSecurity:
            "Tus datos están cifrados y protegidos con protocolos estándar de la industria.",
        hidePassword: "Ocultar contraseña",
        showPassword: "Mostrar contraseña",

        greetingMorning: "Buenos días",
        greetingAfternoon: "Buenas tardes",
        greetingEvening: "Buenas noches",
        dashboardOverview: "Aquí tienes un resumen de tus finanzas.",
        totalBalance: "Saldo Total",
        monthlyIncome: "Ingreso Mensual",
        monthlyExpenses: "Gastos Mensuales",
        savings: "Ahorros",
        recentTransactions: "Transacciones Recientes",
        recentTransactionsDescription: "Tu actividad financiera más reciente",
        signOut: "Cerrar sesión",
        loading: "Cargando...",

        bankAccounts: "Cuentas Bancarias",
        bankAccountsDescription: "Administra tus cuentas bancarias y saldos.",
        createAccount: "Crear Cuenta",
        accountName: "Nombre de la Cuenta",
        accountNamePlaceholder: "ej. Corriente, Ahorros...",
        initialBalance: "Saldo Inicial",
        initialBalancePlaceholder: "0.00",
        creating: "Creando...",
        noAccounts: "Sin cuentas aún",
        noAccountsDescription: "Crea tu primera cuenta bancaria para comenzar.",
        balance: "Saldo",
        backToDashboard: "Volver al Panel",
        accountCreated: "Cuenta creada exitosamente!",
        accountCreateError: "Error al crear la cuenta. Inténtalo de nuevo.",
        accountNameRequired: "Por favor, ingresa el nombre de la cuenta.",
        page: "Página",
        of: "de",
        previous: "Anterior",
        next: "Siguiente",

        transactions: "Transacciones",
        transactionsDescription: "Ver e importar transacciones de tus cuentas.",
        uploadPdf: "Importar Extracto",
        uploadPdfDescription: "Sube un extracto PDF de Itau para importar transacciones automáticamente.",
        selectAccount: "Cuenta",
        selectAccountPlaceholder: "Selecciona una cuenta...",
        dragAndDrop: "Arrastra y suelta tu PDF aquí",
        dragAndDropOr: "o",
        browseFiles: "Buscar archivos",
        uploading: "Subiendo...",
        uploadSuccess: "Extracto subido exitosamente! Las transacciones se están procesando.",
        uploadError: "Error al subir extracto. Inténtalo de nuevo.",
        noFileSelected: "Por favor, selecciona un archivo PDF.",
        noAccountSelected: "Por favor, selecciona una cuenta.",
        noTransactions: "Sin transacciones aún",
        noTransactionsDescription: "Sube un extracto bancario para importar transacciones.",
        transactionDate: "Fecha",
        transactionDescription: "Descripción",
        transactionAmount: "Monto",
        backToAccounts: "Volver a Cuentas",
        pdfOnly: "Solo archivos PDF",
        fileSelected: "Archivo seleccionado",
        removeFile: "Eliminar archivo",
        createTransaction: "Crear Transacción",
        createTransactionDescription: "Agregar una nueva transacción manualmente",
        transactionDescriptionPlaceholder: "Ingresa descripción...",
        transactionType: "Tipo",
        debit: "Débito",
        credit: "Crédito",
        bankAccount: "Cuenta Bancaria",
        cancel: "Cancelar",
        create: "Crear",
        actions: "Acciones",
        deleteTransaction: "Eliminar transacción",
        deleteTransactionDescription: "Esta acción no se puede deshacer.",
        deleting: "Eliminando...",
        delete: "Eliminar",
        updateAccount: "Actualizar saldo de la cuenta",

        // Search
        search: "Buscar",
        searchTransactions: "Buscar Transacciones",
        searchPlaceholder: "Buscar por descripcion...",
        dateRange: "Rango de Fechas",
        from: "Desde",
        to: "Hasta",
        noSearchResults: "No se encontraron transacciones",
        noSearchResultsDescription: "Intenta ajustar tus terminos de busqueda o rango de fechas",

        // Transform to Transfer
        transformToTransfer: "Transformar en Transferencia",
        transformToTransferDescription: "Convertir esta transacción de débito en transferencia a otra cuenta",
        selectDestinationAccount: "Cuenta de Destino",
        transforming: "Transformando...",
        transform: "Transformar",
        transformSuccess: "Transacción transformada en transferencia exitosamente!",
        transformError: "Error al transformar transacción. Inténtalo de nuevo.",
        cannotTransformSameAccount: "Las cuentas de origen y destino deben ser diferentes.",

        editAccount: "Editar Cuenta",
        editAccountDescription: "Atualizar nome e saldo da conta",
        currentBalance: "Saldo Atual",
        saving: "Guardando...",
        save: "Guardar",
        accountUpdated: "Cuenta actualizada exitosamente!",
        accountUpdateError: "Error al actualizar cuenta. Inténtalo de nuevo.",

        language: "Idioma",

        month: "Mes",
        year: "Año",

        creditCards: "Tarjetas de Crédito",
        creditCardsDescription: "Administra tus tarjetas de crédito y visualiza extractos.",
        createCreditCard: "Crear Tarjeta de Crédito",
        creditCardName: "Nombre de la Tarjeta",
        creditCardNamePlaceholder: "ej. Personal, Comercial...",
        creditCardNumber: "Últimos 4 Dígitos",
        creditCardNumberPlaceholder: "Ingresa los últimos 4 dígitos",
        creditCardBrand: "Marca de la Tarjeta",
        creditCardBrandPlaceholder: "ej. Visa, Mastercard...",
        creditCardCreated: "Tarjeta de crédito creada exitosamente!",
        creditCardCreateError: "Error al crear tarjeta de crédito. Inténtalo de nuevo.",
        creditCardNameRequired: "Por favor, ingresa el nombre de la tarjeta.",
        creditCardNumber4Digits: "Por favor, ingresa 4 dígitos.",
        noCreditCards: "Sin tarjetas de crédito aún",
        noCreditCardsDescription: "Crea tu primera tarjeta de crédito para comenzar.",
        creditCardTransactions: "Transacciones de la Tarjeta",
        creditCardTransactionsDescription: "Visualiza el extracto y transacciones de tu tarjeta de crédito.",
        statementMonth: "Mes del Extracto",
        selectMonth: "Selecciona mes y año...",
        creditCardStatement: "Extracto",

        uploadOfx: "Importar OFX",
        uploadOfxDescription: "Sube un archivo OFX de extracto bancario para importar transacciones automáticamente.",
        ofxOnly: "Solo archivos OFX (.ofx, .txt)",
        uploadOfxSuccess: "Archivo OFX subido exitosamente! Las transacciones se están procesando.",
        uploadMode: "Tipo de importación",
        uploadModePdf: "PDF Itaú",
        uploadModeOfx: "Archivo OFX",

        sessionExpiredTitle: "Sesión finalizada",
        sessionExpiredMessage: "Tu sesión ha expirado por inactividad. Por favor, inicia sesión nuevamente.",
    },
}
