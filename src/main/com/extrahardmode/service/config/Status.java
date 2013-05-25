package com.extrahardmode.service.config;

/**
 * Easier to read than meaningless null return values
 */
public enum Status
{
    /**
     * The config has been adjusted and needs to be saved
     */
    ADJUSTED,
    /**
     * This value should be rewritten back to config, but hasn't been adjusted yet, or needs to be adjusted by another method
     */
    NEEDS_TO_BE_ADJUSTED,
    /**
     * Config hasn't been altered and doesn't need to be saved
     */
    OK,
    /**
     * Requested value not found
     */
    NOT_FOUND,
    /**
     * For use as a default for another Object (e.g. Config)
     */
    DEFAULTS,
    /**
     * This value inherits from something
     */
    INHERITS,
    /**
     * The values to disable this option should be loaded
     */
    DISABLES,
    /**
     * This Object has been fully processed and will be ignored
     */
    PROCESSED
}
