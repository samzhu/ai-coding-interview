'use strict';

/**
 * A simple URL router that matches paths to handler names.
 *
 * Supported pattern syntax:
 *   /static/path       - matches exactly
 *   /users/:id         - :id matches one path segment (no slashes)
 *   /files/*           - * matches any remaining path
 */
class Router {
    constructor() {
        this.routes = [];
    }

    /**
     * Register a route pattern with a handler name.
     *
     * @param {string} pattern - URL pattern (e.g. '/users/:id')
     * @param {string} handler - Handler name to return on match
     */
    register(pattern, handler) {
        const { regex, paramNames } = this.patternToRegex(pattern);
        this.routes.push({ pattern, handler, regex, paramNames });
    }

    /**
     * Convert a URL pattern to a regex and extract parameter names.
     *
     * BUG: Dynamic segments use (.+) instead of ([^/]+).
     * (.+) is greedy and matches slashes, causing /users/:id/profile
     * to incorrectly match the entire "123/profile" as the :id value.
     *
     * Fix: replace (.+) with ([^/]+) to stop matching at the next slash.
     *
     * @param {string} pattern
     * @returns {{ regex: RegExp, paramNames: string[] }}
     */
    patternToRegex(pattern) {
        const paramNames = [];
        let regexStr = pattern
            .replace(/:[^/]+/g, (match) => {
                paramNames.push(match.slice(1));
                return '(.+)';  // BUG: should be ([^/]+)
            })
            .replace(/\*/g, '.*');
        const regex = new RegExp('^' + regexStr + '$');
        return { regex, paramNames };
    }

    /**
     * Match a URL path against registered routes.
     *
     * TODO: Implement this method.
     *
     * Steps:
     * 1. For each registered route, test path against route.regex
     * 2. If matched, extract captured groups as parameter values
     * 3. Apply decodeURIComponent to each parameter value
     * 4. Return { handler: route.handler, params: { name: value, ... } }
     * 5. If no route matches, return null
     *
     * @param {string} path - The URL path to match (e.g. '/users/42')
     * @returns {{ handler: string, params: Object } | null}
     */
    match(path) {
        // TODO: Implement route matching with parameter extraction
        return null;
    }
}

module.exports = { Router };
