package com.mankeu.presentation.guide

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedContent
import com.mankeu.presentation.LocalMotion
import com.mankeu.presentation.components.sharedAxisX
import com.mankeu.presentation.components.crossFade
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mankeu.R

/** Opens an article in Pusat Bantuan from anywhere (empty states, tour cards). Provided by `MankeuApp`. */
val LocalOpenHelp = staticCompositionLocalOf<(HelpArticle) -> Unit> { {} }

/**
 * The first visit to a page offers its tour instead of starting it: a small card the owner can
 * take or leave, so the screen stays usable for someone who does not want a walkthrough.
 */
@Composable
fun TourOfferCard(stepCount: Int, onStart: () -> Unit, onLater: () -> Unit, onNever: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.tour_offer_text, stepCount),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onNever) { Text(stringResource(R.string.tour_offer_never)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onLater) { Text(stringResource(R.string.tour_offer_later)) }
                Button(onClick = onStart) { Text(stringResource(R.string.tour_offer_start), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/** Pusat Bantuan: a searchable, grouped list, or one article when [article] is set. */
@Composable
fun HelpCenter(
    article: HelpArticle?,
    onOpen: (HelpArticle?) -> Unit,
    onShow: (HelpArticle) -> Unit,
) {
    val motion = LocalMotion.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val travelPx = with(density) { 24.dp.roundToPx() }
    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    AnimatedContent(
        targetState = article,
        transitionSpec = {
            if (initialState == null && targetState != null) {
                // Forward: Opening article
                sharedAxisX(motion, forward = true, slideDistancePx = travelPx)
            } else if (initialState != null && targetState == null) {
                // Backward: Returning to list
                sharedAxisX(motion, forward = false, slideDistancePx = travelPx)
            } else {
                // Switching between related articles: crossFade
                crossFade(motion)
            }
        },
        label = "helpCenterNavigation",
    ) { currentArticle ->
        if (currentArticle != null) {
            HelpArticleView(
                article = currentArticle,
                onBack = { onOpen(null) },
                onShow = onShow,
                onOpen = onOpen,
            )
        } else {
            val texts = HelpArticle.entries.associateWith { searchText(it) }
            val words = query.trim().lowercase().split(' ').filter { it.isNotBlank() }
            val matches = HelpArticle.entries.filter { a -> words.all { texts.getValue(a).contains(it) } }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.help_search_hint)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                }
                if (matches.isEmpty()) {
                    item { Text(stringResource(R.string.help_search_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                HelpGroup.entries.forEach { group ->
                    val inGroup = matches.filter { it.group == group }
                    if (inGroup.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            ) {
                                Icon(
                                    helpGroupIcon(group),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    stringResource(group.titleRes),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        items(inGroup, key = { it.name }) { ArticleRow(it) { onOpen(it) } }
                    }
                }
            }
        }
    }
}

fun helpGroupIcon(group: HelpGroup): androidx.compose.ui.graphics.vector.ImageVector = when (group) {
    HelpGroup.START -> Icons.Default.Storefront
    HelpGroup.RECORD -> Icons.AutoMirrored.Filled.ReceiptLong
    HelpGroup.PLAN -> Icons.Default.Calculate
    HelpGroup.STOCK -> Icons.Default.Inventory2
    HelpGroup.REPORT -> Icons.Default.Assessment
    HelpGroup.DATA -> Icons.Default.FolderOpen
    HelpGroup.FAQ -> Icons.AutoMirrored.Filled.HelpOutline
}

@Composable
private fun searchText(article: HelpArticle): String = listOf(
    stringResource(article.titleRes),
    stringResource(article.bodyRes),
    stringResource(article.stepsRes),
).joinToString(" ").lowercase()

@Composable
private fun ArticleRow(article: HelpArticle, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(helpGroupIcon(article.group), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(article.titleRes), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HelpArticleView(article: HelpArticle, onBack: () -> Unit, onShow: (HelpArticle) -> Unit, onOpen: (HelpArticle?) -> Unit) {
    val steps = stringResource(article.stepsRes).split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    val image = helpImage(article.show.imageName)
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.help_all_topics))
            }
        }
        item {
            Text(stringResource(article.titleRes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        item { Text(stringResource(article.bodyRes), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                steps.forEachIndexed { index, step ->
                    Row {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(26.dp)) {
                            Text(
                                "${index + 1}",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(top = 3.dp).fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(step, modifier = Modifier.weight(1f).padding(top = 2.dp))
                    }
                }
            }
        }
        image?.let { image ->
            item {
                // Screenshots are light-theme only (owner decision): the outline keeps their edge
                // visible on a dark background.
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Image(
                        painterResource(image),
                        contentDescription = stringResource(R.string.help_screenshot_desc, stringResource(article.titleRes)),
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                    )
                }
            }
        }
        item {
            Button(onClick = { onShow(article) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.TouchApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.help_show_in_app), fontWeight = FontWeight.Bold)
            }
        }
        val related = HelpArticle.entries.filter { it != article && it.show.target == article.show.target }
        if (related.isNotEmpty()) {
            item {
                Text(stringResource(R.string.help_related), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            }
            items(related, key = { it.name }) { ArticleRow(it) { onOpen(it) } }
        }
    }
}

/** The bundled screenshot for [name], or null while it has not been generated yet. */
@SuppressLint("DiscouragedApi")
@Composable
private fun helpImage(name: String): Int? {
    val context = LocalContext.current
    return remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName).takeIf { it != 0 } }
}

/**
 * What the ? button opens: replay this page's tour, the articles about this page, or Pusat
 * Bantuan. The moment someone needs an explanation is the moment they are looking at the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSheet(
    tour: Tour?,
    onDismiss: () -> Unit,
    onPlayTour: () -> Unit,
    onOpenArticle: (HelpArticle) -> Unit,
    onOpenCenter: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.help_sheet_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (tour != null) {
                SheetAction(Icons.Default.PlayCircle, stringResource(R.string.help_play_tour, tour.steps.size), onPlayTour)
            }
            val articles = articlesFor(tour).take(5)
            if (articles.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text(stringResource(R.string.help_for_this_page), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                articles.forEach { article -> SheetAction(helpGroupIcon(article.group), stringResource(article.titleRes)) { onOpenArticle(article) } }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Button(onClick = onOpenCenter, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.help_open_center), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SheetAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, fontWeight = FontWeight.Medium)
    }
}
