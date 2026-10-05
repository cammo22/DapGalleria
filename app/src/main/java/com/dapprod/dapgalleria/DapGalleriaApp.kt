package com.dapprod.dapgalleria

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache

class DapGalleriaApp : Application(), ImageLoaderFactory {
    // Un solo ImageLoader per tutta l'app, con il decoder che estrae i fotogrammi dai video.
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
        .build()
}
