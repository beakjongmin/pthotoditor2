package com.ruto.pthotoditor2.core.image.segmentation.process.facelandmark

object FaceMeshIndices {
    // Eyes
    val LEFT_EYE = listOf(
        33, 246, 161, 160, 159, 158, 157, 173,
        133, 155, 154, 153, 145, 144, 163, 7
    )

    val RIGHT_EYE = listOf(
        263, 466, 388, 387, 386, 385, 384, 398,
        362, 382, 381, 380, 374, 373, 390, 249
    )

    // Eyebrows
    val LEFT_EYEBROW = listOf(70, 63, 105, 66, 107, 55, 65, 52, 53, 46)
    val RIGHT_EYEBROW = listOf(336, 296, 334, 293, 300, 276, 283, 282, 295, 285)

    // Lips (Outer and Inner)
    val OUTER_LIPS = listOf(
        61, 146, 91, 181, 84, 17, 314, 405, 321,
        375, 291, 308, 324, 318, 402, 317, 14,
        87, 178, 88, 95
    )

    val INNER_LIPS = listOf(
        78, 95, 88, 178, 87, 14, 317, 402, 318,
        324, 308, 291
    )

    // Face Oval
    val FACE_OVAL = listOf(
        10, 338, 297, 332, 284, 251, 389, 356, 454,
        323, 361, 288, 397, 365, 379, 378, 400,
        377, 152, 148, 176, 149, 150, 136, 172,
        58, 132, 93, 234, 127, 162, 21, 54, 103,
        67, 109
    )

    // Nose bridge
    val NOSE_BRIDGE = listOf(6, 197, 195, 5, 4, 1, 19, 94)

    // Nose bottom and nostrils
    val NOSE_BOTTOM = listOf(168, 417, 146, 91, 181, 84, 17, 314, 405)

    // Left Iris (if using 478 landmark model)
    val LEFT_IRIS = listOf(468, 469, 470, 471, 472)
    val RIGHT_IRIS = listOf(473, 474, 475, 476, 477)
}
