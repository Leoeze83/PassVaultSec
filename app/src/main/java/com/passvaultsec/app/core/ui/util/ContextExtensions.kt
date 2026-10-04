package com.passvaultsec.app.core.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * Resuelve recursivamente la Activity subyacente desempaquetando ContextWrappers
 * (como los que inyecta Compose en AlertDialogs o temas).
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Resuelve recursivamente la FragmentActivity subyacente para compatibilidad con BiometricPrompt.
 */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
