package com.amrit.commercehub11

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object Slate {
    val s50 = Color(0xFFF8FAFC)
    val s100 = Color(0xFFF1F5F9)
    val s200 = Color(0xFFE2E8F0)
    val s300 = Color(0xFFCBD5E1)
    val s400 = Color(0xFF94A3B8)
    val s500 = Color(0xFF64748B)
    val s600 = Color(0xFF475569)
    val s700 = Color(0xFF334155)
    val s800 = Color(0xFF1E293B)
    val s900 = Color(0xFF0F172A)
    val s950 = Color(0xFF020617)
}

/** Tailwind classes (light / dark:) ka direct mapping */
data class Pal(
    val dark: Boolean,
    val bg: Color,          // body bg: white / slate-950
    val header: Color,      // white / slate-900
    val line: Color,        // slate-200 / slate-800
    val card: Color,        // slate-50 / slate-900
    val cardLine: Color,    // slate-200 / slate-800
    val text: Color,        // slate-900 / slate-100
    val textStrong: Color,  // slate-900 / white
    val sub: Color,         // slate-500 / slate-400
    val muted: Color,       // slate-400 / slate-500
    val chip: Color,        // slate-100 / slate-800
    val chipLine: Color,    // slate-200 / slate-700
    val accent: Color,      // slate-900 / white
    val onAccent: Color,    // white / slate-900
    val coverBox: Color,    // white / slate-950
    val coverLine: Color,   // slate-100 / slate-800
    val body: Color         // slate-600 / slate-300
)

val LightPal = Pal(
    dark = false,
    bg = Color.White, header = Color.White, line = Slate.s200,
    card = Slate.s50, cardLine = Slate.s200,
    text = Slate.s900, textStrong = Slate.s900,
    sub = Slate.s500, muted = Slate.s400,
    chip = Slate.s100, chipLine = Slate.s200,
    accent = Slate.s900, onAccent = Color.White,
    coverBox = Color.White, coverLine = Slate.s100,
    body = Slate.s600
)

val DarkPal = Pal(
    dark = true,
    bg = Slate.s950, header = Slate.s900, line = Slate.s800,
    card = Slate.s900, cardLine = Slate.s800,
    text = Slate.s100, textStrong = Color.White,
    sub = Slate.s400, muted = Slate.s500,
    chip = Slate.s800, chipLine = Slate.s700,
    accent = Color.White, onAccent = Slate.s900,
    coverBox = Slate.s950, coverLine = Slate.s800,
    body = Slate.s300
)

val LocalPal = staticCompositionLocalOf { LightPal }

val Emerald50 = Color(0xFFECFDF5)
val Emerald300 = Color(0xFF6EE7B7)
val Emerald400 = Color(0xFF34D399)
val Emerald600 = Color(0xFF059669)
val Emerald700 = Color(0xFF047857)
val Emerald800 = Color(0xFF065F46)
val Emerald950 = Color(0xFF022C22)
