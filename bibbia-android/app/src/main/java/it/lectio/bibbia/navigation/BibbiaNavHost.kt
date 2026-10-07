package it.lectio.bibbia.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import it.lectio.bibbia.domain.model.Testament
import it.lectio.bibbia.ui.about.AboutScreen
import it.lectio.bibbia.ui.bookmarks.BookmarksScreen
import it.lectio.bibbia.ui.books.BooksScreen
import it.lectio.bibbia.ui.home.HomeScreen
import it.lectio.bibbia.ui.reader.ReaderScreen
import it.lectio.bibbia.ui.search.SearchScreen
import it.lectio.bibbia.ui.settings.SettingsScreen
import it.lectio.bibbia.ui.translations.TranslationsScreen

/** Apre la lettura sostituendo ciò che sta sopra la home: "indietro" dalla lettura torna sempre alla home. */
private fun NavHostController.openReader(route: ReaderRoute) {
    navigate(route) {
        popUpTo<HomeRoute>()
    }
}

@Composable
fun BibbiaNavHost(navController: NavHostController = rememberNavController()) {
    val back: () -> Unit = { navController.popBackStack() }
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        // Transizioni sobrie: una semplice dissolvenza.
        enterTransition = { fadeIn(tween(180)) },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(180)) },
        popExitTransition = { fadeOut(tween(120)) },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onRead = navController::openReader,
                onBooks = { navController.navigate(BooksRoute(it.name)) },
                onBookmarks = { navController.navigate(BookmarksRoute) },
                onSearch = { navController.navigate(SearchRoute) },
                onTranslations = { navController.navigate(TranslationsRoute) },
                onSettings = { navController.navigate(SettingsRoute) },
                onAbout = { navController.navigate(AboutRoute) },
            )
        }
        composable<ReaderRoute> { entry ->
            ReaderScreen(
                route = entry.toRoute(),
                onBack = back,
                onOpenSearch = { navController.navigate(SearchRoute) },
            )
        }
        composable<BooksRoute> { entry ->
            val route = entry.toRoute<BooksRoute>()
            BooksScreen(
                testament = runCatching { Testament.valueOf(route.testament) }.getOrDefault(Testament.OLD),
                onBack = back,
                onChoose = { translationId, ref ->
                    navController.openReader(ReaderRoute(translationId, ref.bookId, ref.chapter))
                },
            )
        }
        composable<BookmarksRoute> {
            BookmarksScreen(onBack = back, onOpen = navController::openReader)
        }
        composable<SearchRoute> {
            SearchScreen(onBack = back, onOpen = navController::openReader)
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = back)
        }
        composable<TranslationsRoute> {
            TranslationsScreen(onBack = back)
        }
        composable<AboutRoute> {
            AboutScreen(onBack = back)
        }
    }
}
