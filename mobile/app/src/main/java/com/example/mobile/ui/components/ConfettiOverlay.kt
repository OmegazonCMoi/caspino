package com.example.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.core.Party

/**
 * Composant d'overlay pour afficher les confettis au premier plan.
 *
 * À utiliser dans un Box englobant tout l'écran, en dernier enfant,
 * pour que les confettis passent devant le header, le contenu et la bottom bar.
 */
@Composable
fun ConfettiOverlay(
    parties: List<Party>,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (parties.isEmpty()) return

    // ConfettiKit se charge de gérer la durée et le fade out par Party.
    ConfettiKit(
        modifier = modifier,
        parties = parties,
        onParticleSystemEnded = { _, activeSystems ->
            if (activeSystems == 0) {
                onFinished()
            }
        }
    )
}

