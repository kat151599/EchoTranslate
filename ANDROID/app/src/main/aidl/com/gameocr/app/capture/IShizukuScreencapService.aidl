package com.gameocr.app.capture;

import android.os.ParcelFileDescriptor;

interface IShizukuScreencapService {
    ParcelFileDescriptor capturePng() = 1;
    void destroy() = 16777114;
}
