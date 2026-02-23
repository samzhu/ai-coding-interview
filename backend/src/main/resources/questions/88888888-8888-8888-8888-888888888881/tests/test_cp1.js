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

console.log('=== CP1: Regex Bug Fix Tests ===');

// Test that patternToRegex correctly uses non-greedy segment matching
const router = new Router();
router.register('/users/:id/profile', 'userProfile');
router.register('/users/:id', 'userById');

// After fix: /users/123/profile should match 'userProfile' (not 'userById' with id='123/profile')
// The greedy (.+) bug would cause /users/:id to match /users/123/profile
// with id = '123/profile', preventing /users/:id/profile from working correctly

// Test 1: Static path should not match dynamic pattern greedily
const r1 = router.routes.find(r => r.pattern === '/users/:id');
const greedyTest = r1.regex.test('/users/123/profile');
check(!greedyTest,
    "BUG FIX: /users/:id regex must NOT match /users/123/profile (greedy (.+) bug)");

// Test 2: :id pattern should match simple segment
const r2 = router.routes.find(r => r.pattern === '/users/:id');
const simpleTest = r2.regex.test('/users/42');
check(simpleTest, "/users/:id regex matches /users/42");

// Test 3: /users/:id/profile regex should match correctly
const r3 = router.routes.find(r => r.pattern === '/users/:id/profile');
check(r3.regex.test('/users/123/profile'),
    "/users/:id/profile regex matches /users/123/profile");

// Test 4: paramNames extraction
check(r1.paramNames.includes('id'), "paramNames includes 'id'");

console.log(`\nResults: ${passed} passed, ${failed} failed`);
process.exit(failed > 0 ? 1 : 0);
