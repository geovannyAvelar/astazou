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

  // Language
  language: string
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

    language: "Language",
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

    language: "Idioma",
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

    language: "Idioma",
  },
}
