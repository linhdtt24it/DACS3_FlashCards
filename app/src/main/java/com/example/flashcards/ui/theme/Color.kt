package com.example.flashcards.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ============================================================
// Brand accent colors (không đổi theo theme)
// ============================================================
val BrandPrimary      = Color(0xFF4A7CF7)   // xanh dương tươi
val BrandPrimaryDark  = Color(0xFF2D5FD6)   // xanh đậm hơn (hover/pressed)
val BrandSuccess      = Color(0xFF22C55E)
val BrandSuccessDim   = Color(0xFF166534)
val BrandWarning      = Color(0xFFF59E0B)
val BrandWarningDim   = Color(0xFF92400E)
val BrandError        = Color(0xFFEF4444)
val BrandErrorDim     = Color(0xFF991B1B)

// ============================================================
// Color scheme data class
// ============================================================
data class FlowColorScheme(
    // Backgrounds
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,

    // Brand
    val primary: Color,
    val primaryContainer: Color,   // light tint behind primary elements
    val onPrimary: Color,

    // Status
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val error: Color,
    val errorContainer: Color,

    // Stroke / Divider
    val stroke: Color,
    val divider: Color,

    // Gradient pair for hero sections
    val gradientStart: Color,
    val gradientEnd: Color,

    val isDark: Boolean
)

// ============================================================
// Light Scheme
// ============================================================
val LightFlowColors = FlowColorScheme(
    background       = Color(0xFFF4F6FB),
    surface          = Color(0xFFFFFFFF),
    surfaceVariant   = Color(0xFFEEF1F8),
    surfaceElevated  = Color(0xFFFFFFFF),
    textPrimary      = Color(0xFF111827),
    textSecondary    = Color(0xFF6B7280),
    textHint         = Color(0xFFA0AEC0),
    primary          = BrandPrimary,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimary        = Color.White,
    success          = BrandSuccess,
    successContainer = Color(0xFFDCFCE7),
    warning          = BrandWarning,
    warningContainer = Color(0xFFFEF3C7),
    error            = BrandError,
    errorContainer   = Color(0xFFFEE2E2),
    stroke           = Color(0xFFE5E7EB),
    divider          = Color(0xFFF3F4F6),
    gradientStart    = Color(0xFF4A7CF7),
    gradientEnd      = Color(0xFF7C3AED),
    isDark           = false
)

// ============================================================
// Dark Scheme — deep navy palette
// ============================================================
val DarkFlowColors = FlowColorScheme(
    background       = Color(0xFF0B0D17),   // rất tối, navy-black
    surface          = Color(0xFF13162B),   // dark navy card
    surfaceVariant   = Color(0xFF1C1F38),   // slightly lighter
    surfaceElevated  = Color(0xFF21253E),   // elevated cards
    textPrimary      = Color(0xFFE8EFFE),
    textSecondary    = Color(0xFF7B8DB3),
    textHint         = Color(0xFF4A5578),
    primary          = Color(0xFF6B9BFF),   // sáng hơn để nổi trên nền tối
    primaryContainer = Color(0xFF1A2952),   // dark tint
    onPrimary        = Color.White,
    success          = Color(0xFF4ADE80),
    successContainer = Color(0xFF052E16),
    warning          = Color(0xFFFBBF24),
    warningContainer = Color(0xFF261600),
    error            = Color(0xFFF87171),
    errorContainer   = Color(0xFF3B0C0C),
    stroke           = Color(0xFF252A45),
    divider          = Color(0xFF1A1E35),
    gradientStart    = Color(0xFF3B5FD4),
    gradientEnd      = Color(0xFF5B21B6),
    isDark           = true
)

// ============================================================
// CompositionLocal
// ============================================================
val LocalFlowColors = staticCompositionLocalOf<FlowColorScheme> { LightFlowColors }

// ============================================================
// Legacy alias shims — giữ để không lỗi build ở code chưa migrated
// ============================================================
// Các file chưa migrate vẫn import com.example.flashcards.ui.theme.*
// và dùng FlowBackground, FlowSurface, ... Những val này giữ nguyên
// giá trị light-mode để không bị lỗi compile-time.
val FlowPrimary       = BrandPrimary
val FlowPrimaryLight  = LightFlowColors.primaryContainer
val FlowBackground    = LightFlowColors.background
val FlowSurface       = LightFlowColors.surface
val FlowTextPrimary   = LightFlowColors.textPrimary
val FlowTextSecondary = LightFlowColors.textSecondary
val FlowSuccess       = LightFlowColors.success
val FlowSuccessLight  = LightFlowColors.successContainer
val FlowWarning       = LightFlowColors.warning
val FlowWarningLight  = LightFlowColors.warningContainer
val FlowGood          = BrandPrimary
val FlowGoodLight     = LightFlowColors.primaryContainer
val FlowCardStroke    = LightFlowColors.stroke