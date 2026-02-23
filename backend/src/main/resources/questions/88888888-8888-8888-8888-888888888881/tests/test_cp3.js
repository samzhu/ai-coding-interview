'use strict';

const path = require('path');
const { Router } = require(path.join('/tmp/workspace', 'router.js'));

let passed = 0;
let failed = 0;

function check(condition, name) {
    if (condition) {
        console.log(`  PASS ${name}`);
        passed++;
    } else {
        console.log(`  FAIL ${name}`);
        failed++;
    }
}

function eq(expected, actual, name) {
    if (JSON.stringify(expected) === JSON.stringify(actual)) {
        console.log(`  PASS ${name}`);
        passed++;
    } else {
        console.log(`  FAIL ${name} [expected=${JSON.stringify(expected)}, got=${JSON.stringify(actual)}]`);
        failed++;
    }
}

console.log('=== CP3: Edge Cases Tests ===');

const router = new Router();
router.register('/users/:id', 'userById');
router.register('/posts/:postId/comments/:commentId', 'comment');
router.register('/files/*', 'serveFile');
router.register('/search', 'search');

// URL encoded path segment
console.log('\n-- URL encoding --');
const r1 = router.match('/users/Jane%20Doe');
check(r1 !== null, "URL-encoded path matches");
if (r1) eq('Jane Doe', r1.params.id, "Decoded param: 'Jane Doe'");

const r2 = router.match('/users/caf%C3%A9');
check(r2 !== null, "UTF-8 encoded path matches");
if (r2) eq('café', r2.params.id, "UTF-8 decoded param: 'café'");

// Multiple dynamic params
console.log('\n-- Multiple params --');
const r3 = router.match('/posts/42/comments/7');
check(r3 !== null, "Multiple params route matches");
if (r3) {
    eq('comment', r3.handler, "Handler is 'comment'");
    eq('42', r3.params.postId, "postId === '42'");
    eq('7', r3.params.commentId, "commentId === '7'");
}

// No match returns null
console.log('\n-- No match --');
const r4 = router.match('/does/not/exist/at/all');
check(r4 === null, "Deeply nested unknown path returns null");

// Wildcard matches deep paths
console.log('\n-- Wildcard --');
const r5 = router.match('/files/a/b/c/d.txt');
check(r5 !== null, "Wildcard matches deeply nested file path");
if (r5) eq('serveFile', r5.handler, "Wildcard handler correct");

// Exact match wins over partial
console.log('\n-- Exact match --');
const r6 = router.match('/search');
check(r6 !== null, "Exact static match works");
if (r6) eq('search', r6.handler, "Exact match handler is 'search'");

console.log(`\nResults: ${passed} passed, ${failed} failed`);
process.exit(failed > 0 ? 1 : 0);
