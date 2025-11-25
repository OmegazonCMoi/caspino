package com.example.mobile.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    val Home: ImageVector
        get() {
            if (_home != null) {
                return _home!!
            }
            _home = ImageVector.Builder(
                name = "Home",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                path(
                    fill = null,
                    fillAlpha = 1f,
                    stroke = SolidColor(Color.Black),
                    strokeAlpha = 1f,
                    strokeLineWidth = 2.0f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(3.0f, 10.0f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, true, 0.709f, -1.528f)
                    lineToRelative(7.0f, -6.0f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, true, 2.582f, 0.0f)
                    lineToRelative(7.0f, 6.0f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, true, 0.709f, 1.528f)
                    verticalLineToRelative(9.0f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, true, -2.0f, 2.0f)
                    horizontalLineToRelative(-14.0f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, true, -2.0f, -2.0f)
                    close()
                }
            }.build()
            return _home!!
        }
    private var _home: ImageVector? = null

    val HomeFilled: ImageVector
        get() {
            if (_homeFilled != null) {
                return _homeFilled!!
            }
            _homeFilled = ImageVector.Builder(
                name = "HomeFilled",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(3.0f, 10.0f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, true, 0.709f, -1.528f)
                    lineToRelative(7.0f, -6.0f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, true, 2.582f, 0.0f)
                    lineToRelative(7.0f, 6.0f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, true, 0.709f, 1.528f)
                    verticalLineToRelative(9.0f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, true, -2.0f, 2.0f)
                    horizontalLineToRelative(-14.0f)
                    arcToRelative(2.0f, 2.0f, 0.0f, false, true, -2.0f, -2.0f)
                    close()
                }
            }.build()
            return _homeFilled!!
        }
    private var _homeFilled: ImageVector? = null

    val Search: ImageVector
        get() {
            if (_search != null) {
                return _search!!
            }
            _search = ImageVector.Builder(
                name = "Search",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                path(
                    fill = null,
                    fillAlpha = 1f,
                    stroke = SolidColor(Color.Black),
                    strokeAlpha = 1f,
                    strokeLineWidth = 2.0f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(21.0f, 21.0f)
                    lineToRelative(-4.34f, -4.34f)
                    moveToRelative(0.0f, 0.0f)
                    arcToRelative(8.0f, 8.0f, 0.0f, true, true, -11.32f, -11.32f)
                    arcToRelative(8.0f, 8.0f, 0.0f, true, true, 11.32f, 11.32f)
                    close()
                }
            }.build()
            return _search!!
        }
    private var _search: ImageVector? = null

    val SearchFilled: ImageVector
        get() {
            if (_searchFilled != null) {
                return _searchFilled!!
            }
            _searchFilled = ImageVector.Builder(
                name = "SearchFilled",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(21.0f, 21.0f)
                    lineToRelative(-4.34f, -4.34f)
                    moveToRelative(0.0f, 0.0f)
                    arcToRelative(8.0f, 8.0f, 0.0f, true, true, -11.32f, -11.32f)
                    arcToRelative(8.0f, 8.0f, 0.0f, true, true, 11.32f, 11.32f)
                    close()
                }
            }.build()
            return _searchFilled!!
        }
    private var _searchFilled: ImageVector? = null

    val Profile: ImageVector
        get() {
            if (_profile != null) {
                return _profile!!
            }
            _profile = ImageVector.Builder(
                name = "Profile",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                // Path pour le corps
                path(
                    fill = null,
                    fillAlpha = 1f,
                    stroke = SolidColor(Color.Black),
                    strokeAlpha = 1f,
                    strokeLineWidth = 2.0f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(19.0f, 21.0f)
                    verticalLineToRelative(-2.0f)
                    arcToRelative(4.0f, 4.0f, 0.0f, false, false, -4.0f, -4.0f)
                    horizontalLineToRelative(-6.0f)
                    arcToRelative(4.0f, 4.0f, 0.0f, false, false, -4.0f, 4.0f)
                    verticalLineToRelative(2.0f)
                }
                // Circle pour la tête
                path(
                    fill = null,
                    fillAlpha = 1f,
                    stroke = SolidColor(Color.Black),
                    strokeAlpha = 1f,
                    strokeLineWidth = 2.0f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(12.0f, 11.0f)
                    arcToRelative(4.0f, 4.0f, 0.0f, true, true, 0.0f, -8.0f)
                    arcToRelative(4.0f, 4.0f, 0.0f, true, true, 0.0f, 8.0f)
                    close()
                }
            }.build()
            return _profile!!
        }
    private var _profile: ImageVector? = null

    val ProfileFilled: ImageVector
        get() {
            if (_profileFilled != null) {
                return _profileFilled!!
            }
            _profileFilled = ImageVector.Builder(
                name = "ProfileFilled",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(19.0f, 21.0f)
                    verticalLineToRelative(-2.0f)
                    arcToRelative(4.0f, 4.0f, 0.0f, false, false, -4.0f, -4.0f)
                    horizontalLineToRelative(-6.0f)
                    arcToRelative(4.0f, 4.0f, 0.0f, false, false, -4.0f, 4.0f)
                    verticalLineToRelative(2.0f)
                    moveToRelative(16.0f, 0.0f)
                    horizontalLineToRelative(2.0f)
                    moveToRelative(-2.0f, 0.0f)
                    horizontalLineToRelative(-16.0f)
                    moveToRelative(8.0f, -10.0f)
                    arcToRelative(4.0f, 4.0f, 0.0f, true, true, 0.0f, -8.0f)
                    arcToRelative(4.0f, 4.0f, 0.0f, true, true, 0.0f, 8.0f)
                    close()
                }
            }.build()
            return _profileFilled!!
        }
    private var _profileFilled: ImageVector? = null

    val Settings: ImageVector
        get() {
            if (_settings != null) {
                return _settings!!
            }
            _settings = ImageVector.Builder(
                name = "Settings",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                path(
                    fill = null,
                    fillAlpha = 1f,
                    stroke = SolidColor(Color.Black),
                    strokeAlpha = 1f,
                    strokeLineWidth = 2.0f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(9.671f, 4.136f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, true, 4.659f, 0.0f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, false, 3.319f, 1.915f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, true, 2.33f, 4.033f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, false, 0.0f, 3.831f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, true, -2.33f, 4.033f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, false, -3.319f, 1.915f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, true, -4.659f, 0.0f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, false, -3.32f, -1.915f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, true, -2.33f, -4.033f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, false, 0.0f, -3.831f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, true, 2.33f, -4.033f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, false, 3.32f, -1.915f)
                    close()
                    moveTo(12.0f, 15.0f)
                    arcToRelative(3.0f, 3.0f, 0.0f, true, false, 0.0f, -6.0f)
                    arcToRelative(3.0f, 3.0f, 0.0f, false, false, 0.0f, 6.0f)
                    close()
                }
            }.build()
            return _settings!!
        }
    private var _settings: ImageVector? = null

    val SettingsFilled: ImageVector
        get() {
            if (_settingsFilled != null) {
                return _settingsFilled!!
            }
            _settingsFilled = ImageVector.Builder(
                name = "SettingsFilled",
                defaultWidth = 24.0.dp,
                defaultHeight = 24.0.dp,
                viewportWidth = 24.0f,
                viewportHeight = 24.0f
            ).apply {
                path(
                    fill = SolidColor(Color.White),
                    fillAlpha = 1f,
                    stroke = null,
                    strokeAlpha = 1f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    strokeLineMiter = 1f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(9.671f, 4.136f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, true, 4.659f, 0.0f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, false, 3.319f, 1.915f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, true, 2.33f, 4.033f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, false, 0.0f, 3.831f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, true, -2.33f, 4.033f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, false, -3.319f, 1.915f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, true, -4.659f, 0.0f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, false, -3.32f, -1.915f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, true, -2.33f, -4.033f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, false, 0.0f, -3.831f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, true, 2.33f, -4.033f)
                    arcToRelative(2.34f, 2.34f, 0.0f, false, false, 3.32f, -1.915f)
                    close()
                    moveTo(12.0f, 15.0f)
                    arcToRelative(3.0f, 3.0f, 0.0f, true, false, 0.0f, -6.0f)
                    arcToRelative(3.0f, 3.0f, 0.0f, false, false, 0.0f, 6.0f)
                    close()
                }
            }.build()
            return _settingsFilled!!
        }
    private var _settingsFilled: ImageVector? = null
}

