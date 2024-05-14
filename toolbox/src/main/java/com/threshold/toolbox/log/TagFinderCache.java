package com.threshold.toolbox.log;

import android.util.SparseArray;

public class TagFinderCache {

    private final SparseArray<String> mTagCache = new SparseArray<>(128);

    /**
     * Find tag from class annotation,
     * and if fail fallback to find from class field("LOG_TAG" or "TAG" or "tag").
     * @param className the full class name to find cache
     * @return the tag
     */
    public String findTag(final String className){
        final int tagKey = className.hashCode();
        String tagFoundFromCache = mTagCache.get(tagKey);
        if (null == tagFoundFromCache) {
            final String searchedTag = TagFinder.searchTagInClass(className);
            mTagCache.put(tagKey, searchedTag);
            tagFoundFromCache = searchedTag;
        }
        return tagFoundFromCache;
    }

}
