package com.resonance.player.core.ui.components

import com.resonance.player.R
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.resonance.player.core.ui.theme.ResonanceTheme

/** Top app bar (Material 3): optional back, title, search / overflow / custom actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResonanceTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    searchIcon: ImageVector? = null,
    onSearch: (() -> Unit)? = null,
    searchDescription: String? = null,
    overflowIcon: ImageVector? = null,
    onOverflow: (() -> Unit)? = null,
    overflowDescription: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            }
        },
        actions = {
            if (searchIcon != null && onSearch != null) {
                IconButton(onClick = onSearch) { Icon(searchIcon, contentDescription = searchDescription) }
            }
            if (overflowIcon != null && onOverflow != null) {
                IconButton(onClick = onOverflow) { Icon(overflowIcon, contentDescription = overflowDescription) }
            }
            trailing?.invoke()
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    )
}

/** Section header: title-md + optional copper action (View all / Sort). */
@Composable
fun ResonanceSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ResonanceTheme.spacing.lg)
    ) {
        Text(
            title,
            style = typography.titleMd,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = typography.labelMd,
                color = colors.accent,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
        trailing?.invoke()
    }
}

/** 48dp filled search field (container-high, 8dp radius, clear action). */
@Composable
fun ResonanceSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    searchIcon: ImageVector? = null,
    clearIcon: ImageVector? = null,
    clearDescription: String? = null,
    onSearch: (() -> Unit)? = null
) {
    val colors = ResonanceTheme.colors
    val typography = ResonanceTheme.typography
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = typography.bodyMd) },
        singleLine = true,
        shape = ResonanceTheme.radii.control,
        textStyle = typography.bodyLg.copy(color = colors.textPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = colors.surfaceHigh,
            focusedContainerColor = colors.surfaceHigh,
            unfocusedBorderColor = colors.outlineSubtle,
            focusedBorderColor = colors.accent,
            unfocusedLabelColor = colors.textMuted,
            focusedLabelColor = colors.textSecondary,
            cursorColor = colors.accent
        ),
        leadingIcon = if (searchIcon != null) {
            { Icon(searchIcon, contentDescription = null, tint = colors.textSecondary) }
        } else {
            null
        },
        trailingIcon = if (value.isNotEmpty() && clearIcon != null) {
            {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(ResonanceTheme.spacing.touchMin)
                ) {
                    Icon(clearIcon, contentDescription = clearDescription, tint = colors.textSecondary)
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    )
}

/**
 * Bottom navigation (Material 3 navigation bar): the selected destination
 * gets the standard indicator pill, animated by the platform component.
 */
@Composable
fun ResonanceNavDock(
    destinations: List<NavDockDestination>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = ResonanceTheme.colors.surfaceLow,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = destination.route == selectedRoute,
                onClick = { onSelect(destination.route) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label, maxLines = 1) }
            )
        }
    }
}

data class NavDockDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)
