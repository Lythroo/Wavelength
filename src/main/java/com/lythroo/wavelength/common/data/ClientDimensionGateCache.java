package com.lythroo.wavelength.common.data;

public final class ClientDimensionGateCache {

    private static boolean netherOpen = true;
    private static boolean endOpen    = true;

    private static boolean hasSynced  = false;

    private static boolean netherBaseEnabled       = true;
    private static boolean endBaseEnabled          = true;
    private static String  netherLinkedEventId     = "";
    private static String  endLinkedEventId        = "";
    private static String  netherLinkedEventName   = "";
    private static String  endLinkedEventName      = "";

    private ClientDimensionGateCache() {}

    public static void update(boolean netherOpen, boolean endOpen,
                              boolean netherBase, boolean endBase,
                              String netherEventId, String endEventId,
                              String netherEventName, String endEventName) {
        ClientDimensionGateCache.netherOpen           = netherOpen;
        ClientDimensionGateCache.endOpen              = endOpen;
        ClientDimensionGateCache.netherBaseEnabled    = netherBase;
        ClientDimensionGateCache.endBaseEnabled       = endBase;
        ClientDimensionGateCache.netherLinkedEventId  = netherEventId  == null ? "" : netherEventId;
        ClientDimensionGateCache.endLinkedEventId     = endEventId     == null ? "" : endEventId;
        ClientDimensionGateCache.netherLinkedEventName = netherEventName == null ? "" : netherEventName;
        ClientDimensionGateCache.endLinkedEventName    = endEventName   == null ? "" : endEventName;
        ClientDimensionGateCache.hasSynced = true;
    }

    public static boolean hasSynced() { return hasSynced; }

    public static void reset() {
        netherOpen          = true;  endOpen           = true;
        netherBaseEnabled   = true;  endBaseEnabled    = true;
        netherLinkedEventId = "";    endLinkedEventId  = "";
        netherLinkedEventName = "";  endLinkedEventName = "";
        hasSynced = false;
    }

    public static boolean isNetherOpen()    { return netherOpen; }

    public static boolean isEndOpen()       { return endOpen; }

    public static boolean getNetherBaseEnabled()    { return netherBaseEnabled; }
    public static boolean getEndBaseEnabled()       { return endBaseEnabled; }
    public static String  getNetherLinkedEventId()  { return netherLinkedEventId; }
    public static String  getEndLinkedEventId()     { return endLinkedEventId; }
    public static String  getNetherLinkedEventName(){ return netherLinkedEventName; }
    public static String  getEndLinkedEventName()   { return endLinkedEventName; }
}