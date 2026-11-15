package com.rashaad.messenger.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rashaad.messenger.R
import com.rashaad.messenger.domain.validation.CredentialsValidator
import com.rashaad.messenger.ui.components.LoadingButton
import com.rashaad.messenger.ui.components.PasswordField
import com.rashaad.messenger.ui.components.messageRes
import com.rashaad.messenger.ui.theme.MessengerTheme
import kotlinx.coroutines.launch

/** Stateful entry point: wires the ViewModel and Google's Credential Manager to the stateless screen. */
@Composable
fun SignInRoute(viewModel: SignInViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    SignInScreen(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
        onToggleMode = viewModel::onToggleMode,
        onGoogleClick = {
            scope.launch {
                when (val result = GoogleSignIn.requestIdToken(context)) {
                    is GoogleSignIn.Result.Token -> viewModel.onGoogleIdToken(result.idToken)
                    GoogleSignIn.Result.Unavailable -> viewModel.onGoogleSignInUnavailable()
                    GoogleSignIn.Result.Cancelled -> Unit // user closed the picker: not an error
                }
            }
        },
        onErrorShown = viewModel::onErrorShown,
    )
}

@Composable
fun SignInScreen(
    uiState: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    onGoogleClick: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val isSignUp = uiState.mode == AuthMode.SIGN_UP

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
                .imePadding() // keep the form above the keyboard
                .verticalScroll(rememberScrollState()) // and scrollable at large font sizes
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(modifier = Modifier.widthIn(max = 480.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.ic_chat_bubble),
                    contentDescription = null, // decorative
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(if (isSignUp) R.string.sign_up_title else R.string.sign_in_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    label = { Text(stringResource(R.string.email)) },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    isError = uiState.emailError != null,
                    supportingText = uiState.emailError?.let { { Text(stringResource(R.string.error_email_invalid)) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                PasswordField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    errorText = when (uiState.passwordError) {
                        PasswordFieldError.EMPTY -> stringResource(R.string.error_password_empty)
                        PasswordFieldError.TOO_SHORT ->
                            pluralStringResource(
                                R.plurals.error_password_too_short,
                                CredentialsValidator.MIN_PASSWORD_LENGTH,
                                CredentialsValidator.MIN_PASSWORD_LENGTH,
                            )
                        null -> null
                    },
                    supportingText = if (isSignUp) {
                        pluralStringResource(
                            R.plurals.password_hint_sign_up,
                            CredentialsValidator.MIN_PASSWORD_LENGTH,
                            CredentialsValidator.MIN_PASSWORD_LENGTH,
                        )
                    } else {
                        null
                    },
                    onImeDone = {
                        focusManager.clearFocus()
                        onSubmit()
                    },
                    enabled = !uiState.isLoading,
                )
                Spacer(Modifier.height(16.dp))
                LoadingButton(
                    text = stringResource(if (isSignUp) R.string.sign_up else R.string.sign_in),
                    loading = uiState.isLoading,
                    onClick = onSubmit,
                )
                TextButton(
                    onClick = onToggleMode,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(if (isSignUp) R.string.switch_to_sign_in else R.string.switch_to_sign_up))
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.or),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    HorizontalDivider(Modifier.weight(1f))
                }

                OutlinedButton(
                    onClick = onGoogleClick,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.continue_with_google))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SignInScreenPreview() {
    MessengerTheme {
        SignInScreen(
            uiState = SignInUiState(email = "ada@example.com", passwordError = PasswordFieldError.EMPTY),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {},
            onToggleMode = {},
            onGoogleClick = {},
            onErrorShown = {},
        )
    }
}
