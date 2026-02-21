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

    // Language
    language: string

    month: string
    year: string
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

        language: "Language",

        month: "Month",
        year: "Year"
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

        language: "Idioma",

        month: "month",
        year: "year"
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

        language: "Idioma",

        month: "Mês",
        year: "Ano"
    },
}
