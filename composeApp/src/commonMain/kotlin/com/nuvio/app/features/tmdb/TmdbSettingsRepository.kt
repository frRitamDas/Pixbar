package com.nuvio.app.features.tmdb

import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingEnrichmentCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TmdbSettingsRepository {
    private val _uiState = MutableStateFlow(TmdbSettings())
    val uiState: StateFlow<TmdbSettings> = _uiState.asStateFlow()
    private var hasLoaded = false

    private var enabled = true
    private var apiKey = BACKEND_SENTINEL
    private var language = "en"
    private var useTrailers = true
    private var useArtwork = true
    private var useBasicInfo = true
    private var useDetails = true
    private var useReleaseDates = false
    private var useCredits = true
    private var useProductions = true
    private var useNetworks = true
    private var useEpisodes = true
    private var useSeasonPosters = true
    private var useMoreLikeThis = true
    private var useCollections = true

    private const val BACKEND_SENTINEL = "pixbar-backend-tmdb"

    fun ensureLoaded() { if (!hasLoaded) loadFromDisk() }
    fun onProfileChanged() { loadFromDisk() }
    fun snapshot(): TmdbSettings { ensureLoaded(); return _uiState.value }

    fun setEnabled(value: Boolean) {
        ensureLoaded()
        enabled = value
        publish()
        TmdbSettingsStorage.saveEnabled(value)
    }

    fun setApiKey(value: String) {
        ensureLoaded()
        // The TMDB credential is server-side. Ignore client-supplied credentials.
        apiKey = BACKEND_SENTINEL
        publish()
    }

    fun setLanguage(value: String) {
        ensureLoaded()
        val normalized = normalizeLanguage(value)
        if (language == normalized) return
        language = normalized
        publish()
        TmdbSettingsStorage.saveLanguage(normalized)
    }

    fun setUseTrailers(value: Boolean) = setBoolean(useTrailers, value, { useTrailers = it }, TmdbSettingsStorage::saveUseTrailers)
    fun setUseArtwork(value: Boolean) = setBoolean(useArtwork, value, { useArtwork = it }, TmdbSettingsStorage::saveUseArtwork)
    fun setUseBasicInfo(value: Boolean) = setBoolean(useBasicInfo, value, { useBasicInfo = it }, TmdbSettingsStorage::saveUseBasicInfo)
    fun setUseDetails(value: Boolean) = setBoolean(useDetails, value, { useDetails = it }, TmdbSettingsStorage::saveUseDetails)

    fun setUseReleaseDates(value: Boolean) {
        ensureLoaded()
        if (useReleaseDates == value) return
        useReleaseDates = value
        publish()
        TmdbSettingsStorage.saveUseReleaseDates(value)
        invalidateReleaseDateMetadata()
    }

    fun setUseCredits(value: Boolean) = setBoolean(useCredits, value, { useCredits = it }, TmdbSettingsStorage::saveUseCredits)
    fun setUseProductions(value: Boolean) = setBoolean(useProductions, value, { useProductions = it }, TmdbSettingsStorage::saveUseProductions)
    fun setUseNetworks(value: Boolean) = setBoolean(useNetworks, value, { useNetworks = it }, TmdbSettingsStorage::saveUseNetworks)
    fun setUseEpisodes(value: Boolean) = setBoolean(useEpisodes, value, { useEpisodes = it }, TmdbSettingsStorage::saveUseEpisodes)
    fun setUseSeasonPosters(value: Boolean) = setBoolean(useSeasonPosters, value, { useSeasonPosters = it }, TmdbSettingsStorage::saveUseSeasonPosters)
    fun setUseMoreLikeThis(value: Boolean) = setBoolean(useMoreLikeThis, value, { useMoreLikeThis = it }, TmdbSettingsStorage::saveUseMoreLikeThis)
    fun setUseCollections(value: Boolean) = setBoolean(useCollections, value, { useCollections = it }, TmdbSettingsStorage::saveUseCollections)

    private fun setBoolean(current: Boolean, next: Boolean, update: (Boolean) -> Unit, persist: (Boolean) -> Unit) {
        ensureLoaded()
        if (current == next) return
        update(next)
        publish()
        persist(next)
    }

    private fun loadFromDisk() {
        val wasLoaded = hasLoaded
        val previousUseReleaseDates = useReleaseDates
        hasLoaded = true
        // Never load or persist a TMDB API credential on the client.
        apiKey = BACKEND_SENTINEL
        enabled = true
        val storedLanguage = TmdbSettingsStorage.loadLanguage()
        language = if (storedLanguage == null) "en" else normalizeLanguage(storedLanguage)
        useTrailers = TmdbSettingsStorage.loadUseTrailers() ?: true
        useArtwork = TmdbSettingsStorage.loadUseArtwork() ?: true
        useBasicInfo = TmdbSettingsStorage.loadUseBasicInfo() ?: true
        useDetails = TmdbSettingsStorage.loadUseDetails() ?: true
        useReleaseDates = TmdbSettingsStorage.loadUseReleaseDates() ?: false
        useCredits = TmdbSettingsStorage.loadUseCredits() ?: true
        useProductions = TmdbSettingsStorage.loadUseProductions() ?: true
        useNetworks = TmdbSettingsStorage.loadUseNetworks() ?: true
        useEpisodes = TmdbSettingsStorage.loadUseEpisodes() ?: true
        useSeasonPosters = TmdbSettingsStorage.loadUseSeasonPosters() ?: true
        useMoreLikeThis = TmdbSettingsStorage.loadUseMoreLikeThis() ?: true
        useCollections = TmdbSettingsStorage.loadUseCollections() ?: true
        publish()
        if (wasLoaded && previousUseReleaseDates != useReleaseDates) invalidateReleaseDateMetadata()
    }

    private fun publish() {
        _uiState.value = TmdbSettings(
            enabled = enabled,
            apiKey = apiKey,
            language = language,
            useTrailers = useTrailers,
            useArtwork = useArtwork,
            useBasicInfo = useBasicInfo,
            useDetails = useDetails,
            useReleaseDates = useReleaseDates,
            useCredits = useCredits,
            useProductions = useProductions,
            useNetworks = useNetworks,
            useEpisodes = useEpisodes,
            useSeasonPosters = useSeasonPosters,
            useMoreLikeThis = useMoreLikeThis,
            useCollections = useCollections,
        )
    }

    private fun invalidateReleaseDateMetadata() {
        MetaDetailsRepository.clear()
        ContinueWatchingEnrichmentCache.clearAll(ProfileRepository.activeProfileId)
    }
}

internal fun normalizeLanguage(value: String?): String {
    val trimmed = value?.trim()?.replace('_', '-') ?: return ""
    return trimmed.takeIf { it.isNotBlank() } ?: ""
}
