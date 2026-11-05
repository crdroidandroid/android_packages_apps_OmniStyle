/*
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

import android.content.res.Resources
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private val CardShape = RoundedCornerShape(20.dp)
private val ThumbHeight = 112.dp
private val HeroHeight = 148.dp

@Composable
fun OmniStyleTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context)
                 else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = scheme, content = content)
}

/**
 * @param pickerMode when true, tapping a header applies it (PickHeaderActivity);
 *                   when false the list is browse-only (BrowseHeaderActivity).
 * @param fixedPack  show only this pack and hide the pack selector.
 * @param initialPackKey pack to open on (e.g. the daylight pack chosen in Settings);
 *                   falls back to the pack holding the applied header, then the first one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderPickerScreen(
    title: String,
    pickerMode: Boolean,
    fixedPack: HeaderPack?,
    initialPackKey: String? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val appliedMessage = stringResource(R.string.omnistyle_custom_header_image_notice)

    var packs by remember { mutableStateOf<List<HeaderPack>?>(null) }
    var selectedPackKey by rememberSaveable { mutableStateOf<String?>(null) }
    var loadedPack by remember { mutableStateOf<LoadedPack?>(null) }
    var loading by remember { mutableStateOf(true) }
    var applied by remember { mutableStateOf<AppliedHeader?>(null) }

    LaunchedEffect(Unit) {
        val (found, current) = withContext(Dispatchers.IO) {
            (fixedPack?.let { listOf(it) } ?: HeaderRepository.queryPacks(context)) to
                HeaderRepository.appliedHeader(context)
        }
        applied = current
        packs = found
        if (selectedPackKey == null || found.none { it.key == selectedPackKey }) {
            selectedPackKey = (found.firstOrNull { it.key == initialPackKey }
                ?: found.firstOrNull { it.packageName == current?.packageName }
                ?: found.firstOrNull())?.key
        }
        if (found.isEmpty()) loading = false
    }

    LaunchedEffect(selectedPackKey, packs) {
        val pack = packs?.firstOrNull { it.key == selectedPackKey } ?: return@LaunchedEffect
        loading = true
        loadedPack = withContext(Dispatchers.IO) { HeaderRepository.loadPack(context, pack) }
        loading = false
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.omnistyle_navigate_up),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { inner ->
        val layoutDir = LocalLayoutDirection.current
        val pack = loadedPack
        val selectedEntryKey = applied?.key

        LazyVerticalGrid(
            // One column on phones in portrait, two or more on landscape / large screens.
            columns = GridCells.Adaptive(minSize = 340.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = inner.calculateStartPadding(layoutDir) + 16.dp,
                end = inner.calculateEndPadding(layoutDir) + 16.dp,
                top = inner.calculateTopPadding() + 8.dp,
                // Scaffold's inner padding already includes the navigation bar.
                bottom = inner.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pickerMode) {
                applied?.let { current ->
                    fullWidth("current") {
                        CurrentHeaderCard(
                            applied = current,
                            label = pack?.takeIf { it.pack.packageName == current.packageName }
                                ?.headers?.firstOrNull { it.image == current.image }?.displayName
                                ?: current.image,
                        )
                    }
                }
            }

            val allPacks = packs
            if (fixedPack == null && allPacks != null && allPacks.size > 1) {
                fullWidth("selector") {
                    PackSelector(
                        packs = allPacks,
                        selectedKey = selectedPackKey,
                        onSelect = { selectedPackKey = it.key },
                    )
                }
            }

            when {
                allPacks != null && allPacks.isEmpty() -> fullWidth("empty") {
                    EmptyState(
                        title = stringResource(R.string.omnistyle_header_empty_title),
                        summary = stringResource(R.string.omnistyle_header_empty_summary),
                    )
                }

                loading || pack == null -> fullWidth("loading") {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }

                else -> {
                    fullWidth("info") { PackInfo(pack) }
                    if (pack.headers.isEmpty()) {
                        fullWidth("pack_empty") {
                            EmptyState(
                                title = stringResource(R.string.omnistyle_header_pack_empty),
                                summary = null,
                            )
                        }
                    } else {
                        val res = pack.resources
                        items(pack.headers, key = { "${pack.pack.key}/${it.type}/${it.image}/${it.hour}/${it.day}/${it.month}" }) { entry ->
                            val entryKey = "${pack.pack.packageName}/${entry.image}"
                            HeaderCard(
                                resources = res,
                                packageName = pack.pack.packageName,
                                entry = entry,
                                selected = pickerMode && entryKey == selectedEntryKey,
                                onClick = if (pickerMode) {
                                    {
                                        HeaderRepository.applyHeader(
                                            context, pack.pack.packageName, entry.image
                                        )
                                        applied = AppliedHeader(pack.pack.packageName, entry.image)
                                        scope.launch {
                                            snackbar.currentSnackbarData?.dismiss()
                                            snackbar.showSnackbar(appliedMessage)
                                        }
                                    }
                                } else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun rememberThumbnail(
    resources: Resources?,
    packageName: String,
    image: String,
): ImageBitmap? {
    val density = LocalDensity.current
    val widthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    val thumb by produceState<ImageBitmap?>(null, resources, packageName, image, widthPx) {
        value = if (resources == null) null else withContext(Dispatchers.IO) {
            HeaderRepository.loadThumbnail(resources, packageName, image, widthPx)?.asImageBitmap()
        }
    }
    return thumb
}

@Composable
private fun HeaderImage(bitmap: ImageBitmap?, height: Dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Crossfade(targetState = bitmap, label = "thumb") { bmp ->
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Spacer(Modifier.fillMaxSize())
            }
        }
        // Scrim so the white label stays readable on light images.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.6f),
                    )
                )
        )
    }
}

@Composable
private fun CurrentHeaderCard(applied: AppliedHeader, label: String) {
    val context = LocalContext.current
    val res by produceState<Resources?>(null, applied.packageName) {
        value = withContext(Dispatchers.IO) {
            HeaderRepository.resourcesFor(context, applied.packageName)
        }
    }
    val bitmap = rememberThumbnail(res, applied.packageName, applied.image)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.omnistyle_header_current),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 10.dp),
            )
            Box(Modifier.clip(CardShape)) {
                HeaderImage(bitmap, HeroHeight)
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun PackSelector(
    packs: List<HeaderPack>,
    selectedKey: String?,
    onSelect: (HeaderPack) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(packs, key = { it.key }) { pack ->
            val selected = pack.key == selectedKey
            FilterChip(
                selected = selected,
                onClick = { onSelect(pack) },
                label = { Text(pack.label) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else null,
            )
        }
    }
}

@Composable
private fun PackInfo(pack: LoadedPack) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Text(
            text = pack.pack.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val count = pluralStringResource(R.plurals.omnistyle_header_count, pack.headers.size, pack.headers.size)
        val subtitle = pack.creator?.let {
            stringResource(R.string.omnistyle_header_pack_summary, count, it)
        } ?: count
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeaderCard(
    resources: Resources?,
    packageName: String,
    entry: HeaderEntry,
    selected: Boolean,
    onClick: (() -> Unit)?,
) {
    val bitmap = rememberThumbnail(resources, packageName, entry.image)
    val border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
    val body: @Composable () -> Unit = {
        Box {
            HeaderImage(bitmap, ThumbHeight)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                AnimatedVisibility(visible = selected, enter = fadeIn(), exit = fadeOut()) {
                    Row {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(3.dp).size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            scheduleLabel(entry)?.let { label ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    if (onClick != null) {
        Card(onClick = onClick, shape = CardShape, border = border, colors = colors,
             modifier = Modifier.fillMaxWidth()) { body() }
    } else {
        Card(shape = CardShape, border = border, colors = colors,
             modifier = Modifier.fillMaxWidth()) { body() }
    }
}

/** "06:00" for hourly headers, "25 Dec" for date headers, null for list headers. */
@Composable
private fun scheduleLabel(entry: HeaderEntry): String? {
    val context = LocalContext.current
    return remember(entry) {
        when (entry.type) {
            1 -> Calendar.getInstance().run {
                set(Calendar.HOUR_OF_DAY, entry.hour.coerceIn(0, 23))
                set(Calendar.MINUTE, 0)
                DateFormat.getTimeFormat(context).format(time)
            }
            0 -> Calendar.getInstance().run {
                // Header packs store a 1-based month; SystemUI's DaylightHeaderProvider
                // compares it against Calendar.MONTH + 1.
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.MONTH, (entry.month - 1).coerceIn(0, 11))
                set(Calendar.DAY_OF_MONTH, entry.day.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
                DateFormat.format(DateFormat.getBestDateTimePattern(
                    context.resources.configuration.locales[0], "dMMM"), this).toString()
            }
            else -> null
        }
    }
}

@Composable
private fun EmptyState(title: String, summary: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (summary != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
