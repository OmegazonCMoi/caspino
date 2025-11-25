package com.example.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.ui.components.AppBottomBar
import com.example.mobile.ui.components.AppButton
import com.example.mobile.ui.components.AppCard
import com.example.mobile.ui.components.AppTextField
import com.example.mobile.ui.components.BottomBarItem
import com.example.mobile.ui.components.ButtonSize
import com.example.mobile.ui.components.ButtonVariant
import com.example.mobile.ui.components.CardVariant
import com.example.mobile.ui.icons.AppIcons
import com.example.mobile.ui.theme.DarkTextPrimary
import com.example.mobile.ui.theme.MobileTheme

@Composable
fun TestScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var textFieldValue by remember { mutableStateOf("") }
    
    val bottomBarItems = listOf(
        BottomBarItem(
            icon = AppIcons.Home,
            selectedIcon = AppIcons.HomeFilled,
            onClick = { selectedTab = 0 }
        ),
        BottomBarItem(
            icon = AppIcons.Search,
            selectedIcon = AppIcons.SearchFilled,
            onClick = { selectedTab = 1 }
        ),
        BottomBarItem(
            icon = AppIcons.Profile,
            selectedIcon = AppIcons.ProfileFilled,
            onClick = { selectedTab = 2 }
        ),
        BottomBarItem(
            icon = AppIcons.Settings,
            selectedIcon = AppIcons.SettingsFilled,
            onClick = { selectedTab = 3 }
        )
    )
    
    Scaffold(
        bottomBar = {
            AppBottomBar(
                items = bottomBarItems,
                selectedIndex = selectedTab
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
//                .padding(top = 48.dp, horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            
            // Titre
            Text(
                text = "Composants UI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Style Apple / shadcn - Dark Mode",
                fontSize = 16.sp,
                color = com.example.mobile.ui.theme.DarkTextSecondary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Section Boutons
            Text(
                text = "Boutons",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkTextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            AppButton(
                text = "Primary Button",
                onClick = { },
                variant = ButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppButton(
                text = "Secondary Button",
                onClick = { },
                variant = ButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppButton(
                text = "Outline Button",
                onClick = { },
                variant = ButtonVariant.Outline,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppButton(
                text = "Ghost Button",
                onClick = { },
                variant = ButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppButton(
                text = "Destructive Button",
                onClick = { },
                variant = ButtonVariant.Destructive,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppButton(
                text = "Button with Icon",
                onClick = { },
                variant = ButtonVariant.Primary,
                icon = {
                    Icon(
                        imageVector = AppIcons.Home,
                        contentDescription = null,
                        modifier = Modifier.height(20.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppButton(
                text = "Small Button",
                onClick = { },
                size = ButtonSize.Small,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppButton(
                text = "Large Button",
                onClick = { },
                size = ButtonSize.Large,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Section Input
            Text(
                text = "Champs de texte",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkTextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            AppTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                label = "Nom",
                placeholder = "Entrez votre nom",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AppTextField(
                value = "",
                onValueChange = { },
                label = "Email",
                placeholder = "exemple@email.com",
                isError = true,
                errorMessage = "Email invalide",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Section Cartes
            Text(
                text = "Cartes",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkTextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.Default
            ) {
                Text(
                    text = "Carte par défaut",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ceci est une carte avec le style par défaut. Elle a un fond sombre et des coins arrondis.",
                    fontSize = 14.sp,
                    color = com.example.mobile.ui.theme.DarkTextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.Elevated,
                onClick = { }
            ) {
                Text(
                    text = "Carte élevée (cliquable)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cette carte a un fond légèrement plus clair et est cliquable.",
                    fontSize = 14.sp,
                    color = com.example.mobile.ui.theme.DarkTextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                variant = CardVariant.Outlined
            ) {
                Text(
                    text = "Carte avec bordure",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cette carte a une bordure visible pour plus de définition.",
                    fontSize = 14.sp,
                    color = com.example.mobile.ui.theme.DarkTextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

