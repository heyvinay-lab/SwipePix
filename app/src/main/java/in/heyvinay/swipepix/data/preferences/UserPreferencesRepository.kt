package `in`.heyvinay.swipepix.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("app_theme")
        val REMEMBER_PROGRESS = booleanPreferencesKey("remember_progress")
        val SHOW_UNDO_OPTION = booleanPreferencesKey("show_undo_option")
        val CONFIRM_BEFORE_APPLYING = booleanPreferencesKey("confirm_before_applying")
    }

    private val safePreferences: Flow<Preferences> = dataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    val appTheme: Flow<AppTheme> = safePreferences.map { preferences ->
        val themeName = preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
        try {
            AppTheme.valueOf(themeName)
        } catch (_: IllegalArgumentException) {
            AppTheme.SYSTEM
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    val rememberProgress: Flow<Boolean> = safePreferences.map { preferences ->
        preferences[PreferencesKeys.REMEMBER_PROGRESS] ?: true
    }

    suspend fun setRememberProgress(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMEMBER_PROGRESS] = enabled
        }
    }

    val showUndoOption: Flow<Boolean> = safePreferences.map { preferences ->
        preferences[PreferencesKeys.SHOW_UNDO_OPTION] ?: true
    }

    suspend fun setShowUndoOption(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_UNDO_OPTION] = enabled
        }
    }

    val confirmBeforeApplying: Flow<Boolean> = safePreferences.map { preferences ->
        preferences[PreferencesKeys.CONFIRM_BEFORE_APPLYING] ?: true
    }

    suspend fun setConfirmBeforeApplying(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONFIRM_BEFORE_APPLYING] = enabled
        }
    }
}
