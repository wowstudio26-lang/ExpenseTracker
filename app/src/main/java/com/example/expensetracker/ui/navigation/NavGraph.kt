composable("more") {
    MoreScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToSync = { navController.navigate("sync") },
        onNavigateToScan = { navController.navigate("scan") },
        onNavigateToExport = { navController.navigate("export") },
        onNavigateToBudget = { navController.navigate("budget") },
        onNavigateToSettings = { navController.navigate("settings") },
        onNavigateToAbout = { navController.navigate("about") }
    )
}

// Add Transaction with optional prefillAmount
composable(
    route = "add_transaction?prefillAmount={prefillAmount}",
    arguments = listOf(
        navArgument("prefillAmount") {
            type = NavType.StringType
            defaultValue = ""
        }
    )
) { backStackEntry ->
    val prefillAmount = backStackEntry.arguments?.getString("prefillAmount") ?: ""
    AddTransactionScreen(
        onNavigateBack = { navController.popBackStack() },
        prefillAmount = prefillAmount
    )
}

// Navigate from Scan to Add with prefill
// navController.navigate("add_transaction?prefillAmount=25.50")
