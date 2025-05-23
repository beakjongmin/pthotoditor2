package com.ruto.pthotoditor2.core.image.commonutil

import android.graphics.Bitmap

object HardwareBitmapConvert {

    fun Bitmap.ensureSoftwareConfig(): Bitmap {

        return if (this.config == Bitmap.Config.HARDWARE || !this.isMutable) {
            this.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            this
        }
    }
}