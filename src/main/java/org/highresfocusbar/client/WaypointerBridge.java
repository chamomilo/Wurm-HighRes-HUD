package org.highresfocusbar.client;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Optional reflection bootstrap for Wurm Waypointer's stable public API. */
public final class WaypointerBridge {
    private static final Logger LOG = Logger.getLogger("HighResFocusBar");
    private static final String API = "org.waypoints.api.WaypointerApi";
    private static final String OWNER = "org.highresfocusbar";
    private static volatile int availability;
    private static volatile Method installedCheck;
    private static volatile Method markObject;
    private static volatile Method markObjectRequest;
    private static volatile Method requestBuilder;
    private static volatile Method builderOwnerId;
    private static volatile Method builderMarkerKey;
    private static volatile Method builderSubject;
    private static volatile Method builderMarkerType;
    private static volatile Method builderNavigation;
    private static volatile Method builderBuild;
    private static volatile Constructor<?> objectRefConstructor;
    private static volatile Object autoObjectKind;
    private static volatile Object creatureObjectKind;
    private static volatile Object alertMarkerType;
    private static volatile Object noNavigation;
    private static volatile Method resultIsSuccess;
    private static volatile Method resultMarkerId;
    private static volatile Method removeOwnedMarker;
    private static volatile Method subjectVanished;
    private static final Map<Long, UUID> OWNED_MARKS = new ConcurrentHashMap<>();

    private WaypointerBridge() {
    }

    /** Detect once during startup through WaypointerApi.isInstalled(). */
    public static boolean detectAtStartup() {
        try {
            Class<?> api = loader().loadClass(API);
            Method installed = api.getMethod("isInstalled");
            Method version = api.getMethod("apiVersion");
            installedCheck = installed;
            markObject = api.getMethod("markObject", String.class,
                    String.class, String.class, long.class, String.class,
                    boolean.class);
            subjectVanished = api.getMethod("subjectVanished",
                    String.class, long.class);
            configureToggleApi(api);
            if (!Boolean.TRUE.equals(installed.invoke(null))) {
                // The class exists, but another mod may still be initializing.
                availability = 0;
                return false;
            }
            Object suppliedVersion = version.invoke(null);
            availability = suppliedVersion instanceof Number
                    && ((Number) suppliedVersion).intValue() >= 1 ? 1 : -1;
            return availability > 0;
        } catch (Throwable absent) {
            availability = -1;
            return false;
        }
    }

    public static boolean isAvailable() {
        if (availability > 0) return true;
        if (availability < 0) return false;
        Method installed = installedCheck;
        if (installed == null) return false;
        try {
            if (Boolean.TRUE.equals(installed.invoke(null))) {
                availability = 1;
                return true;
            }
        } catch (Throwable failure) {
            availability = -1;
        }
        return false;
    }

    /** Creates or refreshes this mod's 15-minute object mark. */
    public static boolean mark(long wurmId) {
        return mark(wurmId, false);
    }

    private static boolean mark(long wurmId, boolean creature) {
        if (!isAvailable()) return false;
        try {
            Method call = markObject;
            return call != null && Boolean.TRUE.equals(call.invoke(null,
                    OWNER, "focus:" + wurmId, creature ? "CREATURE" : "AUTO",
                    Long.valueOf(wurmId),
                    "ALERT", Boolean.FALSE));
        } catch (Throwable failure) {
            LOG.log(Level.FINE, "Waypointer mark integration failed open", failure);
        }
        return false;
    }

    /** Repeated presses toggle only the marker owned by this focus HUD. */
    public static boolean toggle(long wurmId) {
        return toggle(wurmId, false);
    }

    public static boolean toggle(long wurmId, boolean creature) {
        if (!isAvailable()) return false;
        UUID existing = OWNED_MARKS.get(Long.valueOf(wurmId));
        if (existing != null) {
            try {
                Method remove = removeOwnedMarker;
                boolean removed = remove != null && Boolean.TRUE.equals(
                        remove.invoke(null, OWNER, existing));
                // A marker may already have expired or vanished in Waypointer.
                // Either way this press completes the local "off" half of the
                // toggle so the next press can create it again.
                OWNED_MARKS.remove(Long.valueOf(wurmId), existing);
                return removed;
            } catch (Throwable failure) {
                LOG.log(Level.FINE,
                        "Waypointer mark removal failed open", failure);
                return false;
            }
        }
        UUID created = createOwnedMark(wurmId, creature);
        if (created != null) {
            OWNED_MARKS.put(Long.valueOf(wurmId), created);
            return true;
        }
        return mark(wurmId, creature);
    }

    public static boolean isMarked(long wurmId) {
        return OWNED_MARKS.containsKey(Long.valueOf(wurmId));
    }

    /** Tells Waypointer that a killed creature no longer owns any marker. */
    public static void creatureVanished(long creatureId) {
        OWNED_MARKS.remove(Long.valueOf(creatureId));
        if (!isAvailable()) return;
        try {
            Method call = subjectVanished;
            if (call != null) call.invoke(null, "CREATURE", Long.valueOf(creatureId));
        } catch (Throwable failure) {
            LOG.log(Level.FINE,
                    "Waypointer creature lifecycle integration failed open", failure);
        }
    }

    private static ClassLoader loader() {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        return context == null ? WaypointerBridge.class.getClassLoader() : context;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void configureToggleApi(Class<?> api) {
        try {
            ClassLoader loader = loader();
            Class<?> request = loader.loadClass(
                    "org.waypoints.api.ObjectMarkRequest");
            Class<?> ref = loader.loadClass("org.waypoints.api.WurmObjectRef");
            Class<? extends Enum> kind = (Class<? extends Enum>) loader.loadClass(
                    "org.waypoints.api.WurmObjectKind");
            Class<? extends Enum> marker = (Class<? extends Enum>) loader.loadClass(
                    "org.waypoints.api.ObjectMarkerType");
            Class<? extends Enum> navigation = (Class<? extends Enum>) loader.loadClass(
                    "org.waypoints.api.NavigationRequest");
            Object builder = request.getMethod("builder").invoke(null);
            Class<?> builderType = builder.getClass();
            requestBuilder = request.getMethod("builder");
            builderOwnerId = builderType.getMethod("ownerId", String.class);
            builderMarkerKey = builderType.getMethod("markerKey", String.class);
            builderSubject = builderType.getMethod("subject", ref);
            builderMarkerType = builderType.getMethod("markerType", marker);
            builderNavigation = builderType.getMethod("navigation", navigation);
            builderBuild = builderType.getMethod("build");
            objectRefConstructor = ref.getConstructor(kind, long.class);
            autoObjectKind = Enum.valueOf(kind, "AUTO");
            creatureObjectKind = Enum.valueOf(kind, "CREATURE");
            alertMarkerType = Enum.valueOf(marker, "ALERT");
            noNavigation = Enum.valueOf(navigation, "NONE");
            markObjectRequest = api.getMethod("markObject", request);
            Class<?> result = loader.loadClass("org.waypoints.api.MarkResult");
            resultIsSuccess = result.getMethod("isSuccess");
            resultMarkerId = result.getMethod("getMarkerId");
            removeOwnedMarker = api.getMethod("removeOwnedMarker",
                    String.class, UUID.class);
        } catch (Throwable unsupported) {
            markObjectRequest = null;
            removeOwnedMarker = null;
        }
    }

    private static UUID createOwnedMark(long wurmId, boolean creature) {
        try {
            if (markObjectRequest == null || requestBuilder == null) return null;
            Object builder = requestBuilder.invoke(null);
            builderOwnerId.invoke(builder, OWNER);
            builderMarkerKey.invoke(builder, "focus:" + wurmId);
            Object subject = objectRefConstructor.newInstance(
                    creature ? creatureObjectKind : autoObjectKind,
                    Long.valueOf(wurmId));
            builderSubject.invoke(builder, subject);
            builderMarkerType.invoke(builder, alertMarkerType);
            builderNavigation.invoke(builder, noNavigation);
            Object request = builderBuild.invoke(builder);
            Object result = markObjectRequest.invoke(null, request);
            if (!Boolean.TRUE.equals(resultIsSuccess.invoke(result))) return null;
            Object id = resultMarkerId.invoke(result);
            return id instanceof UUID ? (UUID) id : null;
        } catch (Throwable failure) {
            LOG.log(Level.FINE,
                    "Waypointer rich mark integration failed open", failure);
            return null;
        }
    }
}
