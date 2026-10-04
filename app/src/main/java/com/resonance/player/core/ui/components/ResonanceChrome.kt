package com.resonance.player.core.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
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

/** Section title in Material 3 style (sentence case), with an optional text action. */
@Composable
fun ResonanceSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)
            .heightIn(min = 40.dp)
    ) {
        Text(
            title,
            style = ResonanceTheme.typography.headlineMd,
            color = ResonanceTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
        trailing?.invoke()
    }
}

/**
 * Search field as a Material 3 search pill: optional [leading] (e.g. back),
 * a clear button once there is text, and [autoFocus] to open the keyboard
 * straight away.
 */
@Composable
fun ResonanceSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    searchIcon: ImageVector? = null,
    clearIcon: ImageVector? = Icons.Rounded.Close,
    clearDescription: String? = null,
    onSearch: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    autoFocus: Boolean = false
) {
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { focus.requestFocus() }
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, maxLines = 1) },
        singleLine = true,
        shape = CircleShape,
        leadingIcon = leading ?: searchIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        trailingIcon = if (value.isNotEmpty() && clearIcon != null) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(clearIcon, contentDescription = clearDescription ?: stringResource(R.string.cd_clear))
                }
            }
        } else {
            null
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = ResonanceTheme.colors.surfaceHigh,
            unfocusedContainerColor = ResonanceTheme.colors.surfaceHigh,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            cursorColor = ResonanceTheme.colors.accent
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .focusRequester(focus)
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
