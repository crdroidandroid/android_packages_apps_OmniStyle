/*
 * Copyright (C) 2018 The OmniROM Project
 * Copyright (C) 2026 crDroid Android Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.omnirom.omnistyle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

/** Browse installed header packs (read-only). */
open class BrowseHeaderActivity : ComponentActivity() {

    /** When true, tapping a header applies it as the QS header. */
    protected open val pickerMode: Boolean = false

    @get:StringRes
    protected open val titleRes: Int = R.string.omnistyle_browse_header

    /** Restrict the screen to a single pack; null lists every installed pack. */
    protected open fun fixedPack(): HeaderPack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val pack = fixedPack()
        // Settings passes the selected daylight pack so browsing opens on it.
        val initialPackKey = intent?.getStringExtra(EXTRA_HEADER_PACK)
        setContent {
            OmniStyleTheme {
                HeaderPickerScreen(
                    title = stringResource(titleRes),
                    pickerMode = pickerMode,
                    fixedPack = pack,
                    initialPackKey = initialPackKey,
                    onBack = ::finish,
                )
            }
        }
    }

    companion object {
        /** Pack key: "package" or "package/activity", same format as Settings' pack values. */
        const val EXTRA_HEADER_PACK = "header_pack"
    }
}

/** Entry point used by Settings: tapping a header applies it. */
class PickHeaderActivity : BrowseHeaderActivity() {
    override val pickerMode = true
    override val titleRes = R.string.omnistyle_pick_header
}

/** OmniStyle's own bundled theme headers (assets/theme.xml). */
class BrowseThemeHeaderActivity : BrowseHeaderActivity() {
    override val pickerMode = true
    override val titleRes = R.string.omnistyle_browse_theme_header

    override fun fixedPack() = HeaderPack(
        label = getString(R.string.omnistyle_browse_theme_header),
        packageName = packageName,
        configName = "$packageName.theme",
    )
}
