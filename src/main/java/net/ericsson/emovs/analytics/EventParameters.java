package net.ericsson.emovs.analytics;

/**
 * Created by Joao Coelho on 2017-10-02.
 */

public class EventParameters {
    public static class PlayerReady {
        public final static String TECHNOLOGY = "Technology";
        public final static String PLAYER_VERSION = "PlayerVersion";
        public final static String TECH_VERSION = "TechVersion";
        public final static String DEVICE_APP_INFO = "DeviceAppInfo";
        public final static String PLAY_MODE = "PlayMode";
    }
    public static class Created {
        public final static String AUTOPLAY = "AutoPlay";
        public final static String TECHNOLOGY = "Technology";
        public final static String PLAYER = "Player";
        public final static String VERSION = "Version";
        public final static String TECH_VERSION = "TechVersion";
        public final static String DEVICE_APP_INFO = "DeviceAppInfo";
        public final static String PLAY_MODE = "PlayMode";
    }
    public static class BitrateChanged {
        public final static String BITRATE = "Bitrate";
    }
    public static class Error {
        public final static String CODE = "Code";
        public final static String MESSAGE = "Message";
        public final static String INFO = "Info";
        public final static String DETAILS = "Details";
    }
    public static class HandshakeStarted {
        public final static String ASSET_ID = "AssetId";
        public final static String PROGRAM_ID = "ProgramId";
    }
    public static class Started {
        public final static String BITRATE = "Bitrate";
        public final static String VIDEO_LENGTH = "VideoLength";
        public final static String MEDIA_LOCATOR = "MediaLocator";
        public final static String ATTRIBUTES = "Attributes";
        public final static String REFERENCE_TIME = "ReferenceTime";
        public final static String PLAY_MODE = "PlayMode";
    }

    public static class Drm {
        public static final String MESSAGE = "Message";
        public static final String CODE = "Code";
        public static final String INFO = "Info";

        public static final String DRM_REQUEST_TYPE = "DrmExoRequestType";
        public static final String DRM_DATA_LENGTH = "DrmExoDataLength";
    }
}