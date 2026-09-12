package com.sandeshmusic.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandeshmusic.app.data.auth.AuthUser
import com.sandeshmusic.app.ui.theme.CoralPrimary
import com.sandeshmusic.app.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onDeveloperSupportClick: () -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val email by authViewModel.email.collectAsState()
    val password by authViewModel.password.collectAsState()
    val isSignUpMode by authViewModel.isSignUpMode.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val successMessage by authViewModel.successMessage.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("auth_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentUser != null) {
                // User Profile & Email Verification View
                SignedInContent(
                    user = currentUser!!,
                    errorMessage = errorMessage,
                    successMessage = successMessage,
                    isLoading = isLoading,
                    onSendVerification = { authViewModel.sendVerificationEmail() },
                    onReloadUser = { authViewModel.reloadUser() },
                    onDeveloperSupportClick = onDeveloperSupportClick,
                    onSignOut = {
                        authViewModel.signOut()
                    }
                )
            } else {
                // Email Sign In / Sign Up Form
                SignedOutContent(
                    email = email,
                    password = password,
                    isSignUpMode = isSignUpMode,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    successMessage = successMessage,
                    passwordVisible = passwordVisible,
                    onEmailChange = { authViewModel.onEmailChange(it) },
                    onPasswordChange = { authViewModel.onPasswordChange(it) },
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                    onTabSelected = { isSignUp -> authViewModel.setSignUpMode(isSignUp) },
                    onDeveloperSupportClick = onDeveloperSupportClick,
                    onSubmit = {
                        focusManager.clearFocus()
                        authViewModel.authenticate()
                    },
                    onForgotPassword = {
                        focusManager.clearFocus()
                        authViewModel.resetPassword()
                    }
                )
            }
        }
    }
}

@Composable
private fun SignedInContent(
    user: AuthUser,
    errorMessage: String?,
    successMessage: String?,
    isLoading: Boolean,
    onSendVerification: () -> Unit,
    onReloadUser: () -> Unit,
    onDeveloperSupportClick: () -> Unit,
    onSignOut: () -> Unit
) {
    // If not verified, auto-check server immediately when opening profile sheet
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!user.isEmailVerified) {
            onReloadUser()
        }
    }

    val initialLetter = user.email.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    Spacer(modifier = Modifier.height(8.dp))

    Surface(
        shape = CircleShape,
        color = CoralPrimary,
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initialLetter,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "Account",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Text(
        text = user.email,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Messages
    AnimatedVisibility(visible = errorMessage != null) {
        errorMessage?.let { error ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    AnimatedVisibility(visible = successMessage != null) {
        successMessage?.let { msg ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CoralPrimary.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CoralPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralPrimary
                    )
                }
            }
        }
    }

    // Email Verification Status Card
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (user.isEmailVerified) {
            Color(0xFF2E7D32).copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("verification_status_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (user.isEmailVerified) Icons.Default.CheckCircle else Icons.Default.Email,
                    contentDescription = "Verification status",
                    tint = if (user.isEmailVerified) Color(0xFF2E7D32) else CoralPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (user.isEmailVerified) "Email Verified" else "Email Not Verified",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (user.isEmailVerified) {
                            "Your email address is verified."
                        } else {
                            "A verification email was sent. Verify your email to secure your account."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!user.isEmailVerified) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSendVerification,
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("resend_verification_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Resend Email",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    FilledTonalButton(
                        onClick = onReloadUser,
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("check_verification_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Refresh Status",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Developer Support & Updates Card
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onDeveloperSupportClick)
            .testTag("auth_sheet_developer_support_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = CoralPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = CoralPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Developer Support",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "GitHub, Instagram, Facebook & Updates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open Developer Support",
                tint = CoralPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    OutlinedButton(
        onClick = onSignOut,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("sign_out_button"),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(imageVector = Icons.Default.Logout, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Sign Out",
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SignedOutContent(
    email: String,
    password: String,
    isSignUpMode: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String?,
    passwordVisible: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onTabSelected: (Boolean) -> Unit,
    onDeveloperSupportClick: () -> Unit = {},
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit
) {
    // Header
    Surface(
        shape = CircleShape,
        color = CoralPrimary.copy(alpha = 0.15f),
        modifier = Modifier.size(60.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = CoralPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = if (isSignUpMode) "Create an Account" else "Welcome to Sandesh Music",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Sign in to save your favorite songs and automatically sync across all your devices.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp),
        lineHeight = 20.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Tab switcher
    TabRow(
        selectedTabIndex = if (isSignUpMode) 1 else 0,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[if (isSignUpMode) 1 else 0]),
                color = CoralPrimary
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Tab(
            selected = !isSignUpMode,
            onClick = { onTabSelected(false) },
            text = {
                Text(
                    text = "Sign In",
                    fontWeight = if (!isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (!isSignUpMode) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        Tab(
            selected = isSignUpMode,
            onClick = { onTabSelected(true) },
            text = {
                Text(
                    text = "Register",
                    fontWeight = if (isSignUpMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSignUpMode) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Error message display
    AnimatedVisibility(visible = errorMessage != null) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }

    // Success message display
    AnimatedVisibility(visible = successMessage != null) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = successMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Email input
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email address") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = CoralPrimary
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CoralPrimary,
            focusedLabelColor = CoralPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_email_input")
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Password input
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(if (isSignUpMode) "Password (at least 6 chars)" else "Password") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CoralPrimary
            )
        },
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisibility) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { onSubmit() }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CoralPrimary,
            focusedLabelColor = CoralPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_password_input")
    )

    if (!isSignUpMode) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onForgotPassword,
                modifier = Modifier.testTag("forgot_password_btn")
            ) {
                Text(
                    text = "Forgot password?",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoralPrimary
                )
            }
        }
    } else {
        Spacer(modifier = Modifier.height(16.dp))
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Submit button
    Button(
        onClick = onSubmit,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("auth_submit_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = if (isSignUpMode) "Create Account" else "Sign In",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    OutlinedButton(
        onClick = onDeveloperSupportClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("auth_developer_support_button")
    ) {
        Icon(
            imageVector = Icons.Default.SupportAgent,
            contentDescription = null,
            tint = CoralPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Developer Support & Updates",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
