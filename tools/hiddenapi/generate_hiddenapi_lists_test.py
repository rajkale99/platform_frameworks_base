#!/usr/bin/env python
#
# Copyright (C) 2018 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Unit tests for Hidden API list generation."""
import unittest
from generate_hiddenapi_lists import *

class TestHiddenapiListGeneration(unittest.TestCase):

    def test_filter_apis(self):
        # Initialize flags so that A and B are put on the whitelist and
        # C, D, E are left unassigned. Try filtering for the unassigned ones.
        flags = FlagsDict()
        flags.parse_and_merge_csv(['A,' + FLAG_SDK, 'B,' + FLAG_SDK,
                        'C', 'D', 'E'])
        filter_set = flags.filter_apis(lambda api, flags: not flags)
        self.assertTrue(isinstance(filter_set, set))
        self.assertEqual(filter_set, set([ 'C', 'D', 'E' ]))

    def test_get_valid_subset_of_unassigned_keys(self):
        # Create flags where only A is unassigned.
        flags = FlagsDict()
        flags.parse_and_merge_csv(['A,' + FLAG_SDK, 'B', 'C'])
        flags.assign_flag(FLAG_UNSUPPORTED, set(['C']))
        self.assertEqual(flags.generate_csv(),
            [ 'A,' + OLD_FLAG_SDK, 'B', 'C,' + OLD_FLAG_UNSUPPORTED ])

        # Check three things:
        # (1) B is selected as valid unassigned
        # (2) A is not selected because it is assigned 'whitelist'
        # (3) D is not selected because it is not a valid key
        self.assertEqual(
            flags.get_valid_subset_of_unassigned_apis(set(['A', 'B', 'D'])), set([ 'B' ]))

    def test_parse_and_merge_csv(self):
        flags = FlagsDict()

        # Test empty CSV entry.
        self.assertEqual(flags.generate_csv(), [])

        # Test new additions. CSV generator produces values with old flags
        # to be backwards compatible.
        flags.parse_and_merge_csv([
            'A,' + FLAG_UNSUPPORTED,
            'B,' + FLAG_BLOCKED + ',' + FLAG_MAX_TARGET_O,
            'C,' + FLAG_SDK + ',' + FLAG_SYSTEM_API,
            'D,' + FLAG_UNSUPPORTED + ',' + FLAG_TEST_API,
            'E,' + FLAG_BLOCKED + ',' + FLAG_TEST_API,
        ])
        self.assertEqual(flags.generate_csv(), [
            'A,' + OLD_FLAG_UNSUPPORTED,
            'B,' + OLD_FLAG_BLOCKED + "," + OLD_FLAG_MAX_TARGET_O,
            'C,' + FLAG_SYSTEM_API + ',' + OLD_FLAG_SDK,
            'D,' + OLD_FLAG_UNSUPPORTED + ',' + FLAG_TEST_API,
            'E,' + OLD_FLAG_BLOCKED + ',' + FLAG_TEST_API,
        ])

        # Test unknown flag.
        with self.assertRaises(AssertionError):
            flags.parse_and_merge_csv([ 'Z,foo' ])

    def test_assign_flag(self):
        flags = FlagsDict()
        flags.parse_and_merge_csv(['A,' + FLAG_SDK, 'B'])

        # Test new additions.
        flags.assign_flag(FLAG_UNSUPPORTED, set([ 'A', 'B' ]))
        self.assertEqual(flags.generate_csv(),
            [ 'A,' + OLD_FLAG_UNSUPPORTED + "," + OLD_FLAG_SDK, 'B,' + OLD_FLAG_UNSUPPORTED ])

        # Test invalid API signature.
        with self.assertRaises(AssertionError):
            flags.assign_flag(FLAG_SDK, set([ 'C' ]))

        # Test invalid flag.
        with self.assertRaises(AssertionError):
            flags.assign_flag('foo', set([ 'A' ]))

    def test_extract_package(self):
        signature = 'Lcom/foo/bar/Baz;->method1()Lcom/bar/Baz;'
        expected_package = 'com.foo.bar'
        self.assertEqual(extract_package(signature), expected_package)

        signature = 'Lcom/foo1/bar/MyClass;->method2()V'
        expected_package = 'com.foo1.bar'
        self.assertEqual(extract_package(signature), expected_package)

        signature = 'Lcom/foo_bar/baz/MyClass;->method3()V'
        expected_package = 'com.foo_bar.baz'
        self.assertEqual(extract_package(signature), expected_package)

if __name__ == '__main__':
    unittest.main()
