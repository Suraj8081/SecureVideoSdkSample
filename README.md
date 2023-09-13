# SecureVideoSdkSample
this is sample project of implementing secure video sdk.

# Implementation of SDK
**Step 1:** Implement the **InitializeMyAppPlayer** interface in your activity.


    class VideoPlayer : AppCompatActivity(), InitializeMyAppPlayer {
    }
                                

And overwrite this method.

    override fun initializePlayer(url: String?, responce: String?) {
        @url param provide the live video url like https://xyz.m3u8
        @responce key provide the all details related the video.
    }

**Step 2:** For get video details.

    val licenceUrl = VideoPlayerInit.getInstance(this@VideoPlayer)
    ?.prePareLicense(siteId, accesskey, mediaId, userId, 30,C.WIDEVINE_UUID)
    licenceUrl?.let { it1 -> playDrmVideo(onlinePlayList[0].url, it1) }
    

**this is demo responce of reocrded video**

          {
            "status": true,
            "message": "Video Meta Displayed",
            "data": {
              "hls_url": "",
              "duration": "0",
              "meta": [
                {
                  "url": "https://xxxxxxxxxxxxxxxxxxxxxxxxxx/720p/encrypted.mp4",
                  "height": "0",
                  "width": "0",
                  "filesize": "0",
                  "label": "720p",
                  "type": "mp4",
                  "meta": {
                    "password": "xyz",
                    "download": "1"
                  },
                  {
                    "url": "https://xxxxxxxxxxxxxxxxxxxx/720p/drm/stream.mpd",
                    "height": "0",
                    "width": "0",
                    "filesize": "0",
                    "label": "720p",
                    "type": "mpd",
                    "meta": {
                      
                    }
                  },
                }
              ]
            }
          }

In this responce we get **meta** key which have listing of video label.If you see the video lebel listing then one another **meta** key this key about video downalodeble or not and another information is give password of this video.

