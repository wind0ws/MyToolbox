package com.threshold.toolbox.log;

import android.util.SparseArray;

public class TagFinderCache {

    private final SparseArray<String> mTagCache = new SparseArray<>(128);

    /**
     * Find tag from class
     * @param className the full class name to find cache
     * @return the tag
     */
    public String findTag(final String className){
        final int tagKey = className.hashCode();
        String tagFoundFromCache = mTagCache.get(tagKey);
        if (tagFoundFromCache == null) {
            final String searchedTag = TagFinder.searchTagInClass(className);
            mTagCache.put(tagKey, searchedTag);
            tagFoundFromCache = searchedTag;
        }
        return tagFoundFromCache;
    }

}
