package com.nuvio.app.features.settings

import androidx.compose.foundation.lazy.LazyListScope

internal fun LazyListScope.contentDiscoveryContent(
    isTablet: Boolean,
    showPluginsEntry: Boolean,
    onAddonsClick: () -> Unit,
    onPluginsClick: () -> Unit,
) {
    // Pixbar uses its built-in TMDB metadata/backend pipeline. Addon and plugin
    // installation is intentionally no longer exposed in the settings UI.
}
