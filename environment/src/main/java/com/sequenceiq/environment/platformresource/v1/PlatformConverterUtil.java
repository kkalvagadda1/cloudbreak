package com.sequenceiq.environment.platformresource.v1;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class PlatformConverterUtil {

    private PlatformConverterUtil() {
    }

    public static <T extends StringType> Map<String, String> convertDefaults(Map<Platform, T> vms) {
        Map<String, String> result = Maps.newHashMap();
        for (Entry<Platform, T> entry : vms.entrySet()) {
            result.put(entry.getKey().value(), entry.getValue().value());
        }
        return result;
    }

    public static <P extends StringType, T extends StringType, C extends Collection<T>> Map<String, Collection<String>> convertPlatformMap(Map<P, C> vms) {
        Map<String, Collection<String>> result = Maps.newHashMap();
        for (Entry<P, C> entry : vms.entrySet()) {
            result.put(entry.getKey().value(), convertList(entry.getValue()));
        }
        return result;
    }

    public static <P extends StringType, T extends StringType, C extends StringType> Map<String, Map<String, String>>
        convertDisplayNameMap(Map<P, Map<T, C>> dNs) {
        Map<String, Map<String, String>> result = Maps.newHashMap();
        for (Entry<P, Map<T, C>> entry : dNs.entrySet()) {
            Map<String, String> innerMap = new HashMap<>();
            for (Entry<T, C> tzEntry : entry.getValue().entrySet()) {
                innerMap.put(tzEntry.getKey().value(), tzEntry.getValue().value());
            }
            result.put(entry.getKey().value(), innerMap);
        }
        return result;
    }

    public static <T extends StringType> Collection<String> convertList(Iterable<T> vmlist) {
        Collection<String> result = Lists.newArrayList();
        for (T item : vmlist) {
            result.add(item.value());
        }
        return result;
    }
}
