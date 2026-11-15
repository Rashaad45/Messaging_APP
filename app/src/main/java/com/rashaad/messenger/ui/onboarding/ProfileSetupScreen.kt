package com.rashaad.messenger.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rashaad.messenger.R
import com.rashaad.messenger.domain.validation.DisplayNameValidation
import com.rashaad.messenger.domain.validation.UsernameValidation
import com.rashaad.messenger.ui.components.LoadingButton
import com.rashaad.messenger.ui.components.messageRes
import com.rashaad.messenger.ui.theme.MessengerTheme

@Composable
fun ProfileSetupRoute(onSignOut: () -> Unit, viewModel: ProfileSetupViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileSetupScreen(
        uiState = uiState,
        onUsernameChange = viewModel::onUsernameChange,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onSubmit = viewModel::onSubmit,
        onErrorShown = viewModel::onErrorShown,
        onSignOut = onSignOut,
    )
}

@Composable
fun ProfileSetupScreen(
    uiState: ProfileSetupUiState,
    onUsernameChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onErrorShown: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    uiState.error?.let { error ->
        val message = stringResource(error.messageRes())
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.widthIn(max = 480.dp)) {
                Text(
                    text = stringResource(R.string.profile_setup_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.profile_setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.username)) },
                    prefix = { Text("@") },
                    singleLine = true,
                    enabled = !uiState.isSaving,
                    isError = uiState.usernameError != null,
                    supportingText = {
                        Text(stringResource(uiState.usernameError?.messageRes() ?: R.string.username_hint))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text(stringResource(R.string.display_name)) },
                    singleLine = true,
                    enabled = !uiState.isSaving,
                    isError = uiState.displayNameError != null,
                    supportingText = {
                        Text(stringResource(uiState.displayNameError?.messageRes() ?: R.string.display_name_hint))
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onSubmit()
                    }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                LoadingButton(
                    text = stringResource(R.string.continue_button),
                    loading = uiState.isSaving,
                    onClick = onSubmit,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onSignOut,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(stringResource(R.string.sign_out))
                }
            }
        }
    }
}

private fun UsernameValidation.messageRes(): Int = when (this) {
    UsernameValidation.TooShort -> R.string.error_username_too_short
    UsernameValidation.TooLong -> R.string.error_username_too_long
    UsernameValidation.InvalidCharacters -> R.string.error_username_invalid_chars
    is UsernameValidation.Valid -> R.string.username_hint
}

private fun DisplayNameValidation.messageRes(): Int = when (this) {
    DisplayNameValidation.Empty -> R.string.error_display_name_empty
    DisplayNameValidation.TooLong -> R.string.error_display_name_too_long
    is DisplayNameValidation.Valid -> R.string.display_name_hint
}

@Preview(showBackground = true)
@Composable
private fun ProfileSetupScreenPreview() {
    MessengerTheme {
        ProfileSetupScreen(
            uiState = ProfileSetupUiState(username = "ada", displayName = "Ada Lovelace"),
            onUsernameChange = {},
            onDisplayNameChange = {},
            onSubmit = {},
            onErrorShown = {},
            onSignOut = {},
        )
    }
}
