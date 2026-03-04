package com.example.mobile.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.mobile.MainActivity
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppHeader
import com.example.mobile.ui.components.AppTextField
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.icons.AppIcons
import com.example.mobile.ui.theme.AccentBlue
import com.example.mobile.ui.theme.AccentGreen
import com.example.mobile.ui.theme.AccentRed
import com.example.mobile.ui.theme.DarkSurface
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.DarkTextSecondary
import kotlinx.coroutines.launch

private enum class AuthMode {
    None,
    Login,
    Register
}

@Composable
fun AccountScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val bottomBarItems = listOf(
        BottomBarItem(AppIcons.Home, AppIcons.HomeFilled) {
            context.startActivity(Intent(context, MainActivity::class.java))
        },
        BottomBarItem(AppIcons.Search, AppIcons.SearchFilled) {
            context.startActivity(Intent(context, com.example.mobile.StatsActivity::class.java))
        },
        BottomBarItem(AppIcons.Profile, AppIcons.ProfileFilled) { },
        BottomBarItem(AppIcons.Cart, AppIcons.CartFilled) {
            context.startActivity(Intent(context, com.example.mobile.ShopActivity::class.java))
        }
    )

    androidx.compose.material3.Scaffold(
        topBar = {
            AppHeader(title = "Compte", onBackClick = onBackClick)
        },
        bottomBar = {
            AppBottomBar(items = bottomBarItems, selectedIndex = 2)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (AccountState.isLoggedIn) {
                LoggedInAccountView()
            } else {
                LoggedOutAccountView()
            }
        }
    }
}

@Composable
private fun LoggedOutAccountView() {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(AuthMode.None) }
    var error by remember { mutableStateOf<String?>(null) }

    var loginUsername by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    var registerUsername by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var registerConfirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mon Compte",
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkTextPrimary
        )
        Text(
            text = "Connecte-toi pour retrouver ton profil et ton activité.",
            fontSize = 14.sp,
            color = DarkTextSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppButton(
                text = "Connexion",
                onClick = {
                    mode = AuthMode.Login
                    error = null
                },
                variant = if (mode == AuthMode.Login) ButtonVariant.Primary else ButtonVariant.Outline,
                size = ButtonSize.Medium,
                modifier = Modifier.weight(1f)
            )
            AppButton(
                text = "Inscription",
                onClick = {
                    mode = AuthMode.Register
                    error = null
                },
                variant = if (mode == AuthMode.Register) ButtonVariant.Primary else ButtonVariant.Outline,
                size = ButtonSize.Medium,
                modifier = Modifier.weight(1f)
            )
        }

        if (mode == AuthMode.Login) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Connexion", color = DarkTextPrimary, fontWeight = FontWeight.SemiBold)

                AppTextField(
                    value = loginUsername,
                    onValueChange = { loginUsername = it },
                    label = "Pseudo",
                    placeholder = "Ton pseudo",
                    transparentContainer = true
                )
                AppTextField(
                    value = loginPassword,
                    onValueChange = { loginPassword = it },
                    label = "Mot de passe",
                    placeholder = "••••••",
                    transparentContainer = true
                )

                if (error != null) {
                    Text(
                        text = error ?: "",
                        fontSize = 12.sp,
                        color = AccentRed
                    )
                }

                AppButton(
                    text = if (AccountState.isLoading) "Connexion..." else "Se connecter",
                    onClick = {
                        error = null
                        scope.launch {
                            AccountState.login(
                                inputUsername = loginUsername,
                                inputPassword = loginPassword
                            ).onFailure { e ->
                                error = e.message ?: "Identifiants invalides."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (mode == AuthMode.Register) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Inscription", color = DarkTextPrimary, fontWeight = FontWeight.SemiBold)

                AppTextField(
                    value = registerUsername,
                    onValueChange = { registerUsername = it },
                    label = "Pseudo",
                    placeholder = "Ton pseudo",
                    transparentContainer = true
                )
                AppTextField(
                    value = registerEmail,
                    onValueChange = { registerEmail = it },
                    label = "Email",
                    placeholder = "nom@exemple.com",
                    transparentContainer = true
                )
                AppTextField(
                    value = registerPassword,
                    onValueChange = { registerPassword = it },
                    label = "Mot de passe",
                    placeholder = "Minimum 6 caractères",
                    transparentContainer = true
                )
                AppTextField(
                    value = registerConfirmPassword,
                    onValueChange = { registerConfirmPassword = it },
                    label = "Confirmer le mot de passe",
                    placeholder = "Répète le mot de passe",
                    transparentContainer = true
                )

                if (error != null) {
                    Text(
                        text = error ?: "",
                        fontSize = 12.sp,
                        color = AccentRed
                    )
                }

                AppButton(
                    text = if (AccountState.isLoading) "Création..." else "Créer un compte",
                    onClick = {
                        error = null
                        scope.launch {
                            AccountState.register(
                                inputUsername = registerUsername,
                                inputEmail = registerEmail,
                                inputPassword = registerPassword,
                                inputConfirmPassword = registerConfirmPassword
                            ).onFailure { e ->
                                error = e.message ?: "Vérifie les champs du formulaire."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LoggedInAccountView() {
    val memberSince = AccountState.memberSince?.toString() ?: "-"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Mon Compte",
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkTextPrimary
        )
        Text(
            text = "Connecté",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentGreen
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountInfoRow(label = "Pseudo", value = AccountState.username)
            AccountInfoRow(label = "Email", value = AccountState.email)
            AccountInfoRow(label = "Membre depuis", value = memberSince)
            AccountInfoRow(label = "Solde pinos", value = BalanceState.balance.intValue.toString())
        }

        AppButton(
            text = "Se déconnecter",
            onClick = { AccountState.logout() },
            variant = ButtonVariant.Outline,
            size = ButtonSize.Medium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AccountInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = DarkTextSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkTextPrimary
        )
    }
}
