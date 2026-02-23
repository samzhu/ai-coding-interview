'use strict';

/**
 * Utility helpers for the router. Do not modify this file.
 */

/**
 * Normalize a URL path by removing trailing slashes (except root).
 * @param {string} path
 * @returns {string}
 */
function normalizePath(path) {
    if (path.length > 1 && path.endsWith('/')) {
        return path.slice(0, -1);
    }
    return path;
}

module.exports = { normalizePath };
