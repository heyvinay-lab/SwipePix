package `in`.heyvinay.swipepix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import `in`.heyvinay.swipepix.data.preferences.AppTheme
import `in`.heyvinay.swipepix.data.preferences.UserPreferencesRepository
import `in`.heyvinay.swipepix.ui.navigation.SwipePixNavGraph
import `in`.heyvinay.swipepix.ui.theme.SwipePixTheme
import javax.inject.Inject

/**
 * Single Activity for SwipePix.
 * All navigation is handled by Navigation Compose within this Activity.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by preferencesRepository.appTheme.collectAsStateWithLifecycle(
                initialValue = AppTheme.SYSTEM,
            )
            SwipePixTheme(appTheme = appTheme) {
                val navController = rememberNavController()
                SwipePixNavGraph(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

