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

console.log('=== CP2: Route Matching Tests ===');

const router = new Router();
router.register('/users', 'listUsers');
router.register('/users/:id', 'userById');
router.register('/users/:id/profile', 'userProfile');
router.register('/files/*', 'serveFile');

// Static route match
const r1 = router.match('/users');
check(r1 !== null, "Static route /users should match");
if (r1) eq('listUsers', r1.handler, "Static route handler is 'listUsers'");

// Dynamic segment match
const r2 = router.match('/users/42');
check(r2 !== null, "/users/42 should match /users/:id");
if (r2) {
    eq('userById', r2.handler, "Dynamic route handler is 'userById'");
    eq('42', r2.params.id, "params.id === '42'");
}

// Dynamic segment — specific path
const r3 = router.match('/users/123/profile');
check(r3 !== null, "/users/123/profile should match /users/:id/profile");
if (r3) {
    eq('userProfile', r3.handler, "Handler is 'userProfile'");
    eq('123', r3.params.id, "params.id === '123'");
}

// No match
const r4 = router.match('/notfound');
check(r4 === null, "Unregistered path returns null");

// Wildcard
const r5 = router.match('/files/images/photo.png');
check(r5 !== null, "Wildcard /files/* matches /files/images/photo.png");
if (r5) eq('serveFile', r5.handler, "Wildcard handler is 'serveFile'");

// URL encoded param
const r6 = router.match('/users/John%20Doe');
check(r6 !== null, "URL-encoded path should match");
if (r6) eq('John Doe', r6.params.id,
    "params.id should be decoded: 'John Doe' not 'John%20Doe'");

console.log(`\nResults: ${passed} passed, ${failed} failed`);
process.exit(failed > 0 ? 1 : 0);
