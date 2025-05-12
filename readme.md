[1] 유저가 갤러리에서 사진을 선택한다
└ feature.gallery.GalleryPicker.kt

[2] 사진을 편집 화면으로 넘긴다
└ feature.editor.EditorScreen.kt

[3] ViewModel이 처리 상태를 관리한다
└ feature.editor.EditorStateViewModel.kt

[4] Bokeh 효과 적용을 위해 배경을 분리하고 블러 처리한다
├─ MLKit → SelfieSegmentor.kt
└─ 배경 블러 → BokehProcessor.kt → blur.kt

[5] 최종 이미지를 보여준다 / 저장한다

