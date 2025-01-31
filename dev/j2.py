#!/usr/bin/python3

# Copyright 2021 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Entry point for various tools useful for J2CL development."""

import argparse

import diff
import make_size_report
import replace_all
import test_all
import test_integration


def _add_cmd(subparsers, name, handler, descr):
  parser = subparsers.add_parser(name, help=descr, description=descr)
  if hasattr(handler, "add_arguments"):
    handler.add_arguments(parser)
  parser.set_defaults(func=handler.main)


if __name__ == "__main__":
  base_parser = argparse.ArgumentParser(description="j2 dev script.")
  parsers = base_parser.add_subparsers()

  _add_cmd(parsers, "size", make_size_report, "Generate size report.")
  _add_cmd(parsers, "gen", replace_all, "Regenerate readable examples.")
  _add_cmd(parsers, "diff", diff, "Compare output.")
  _add_cmd(parsers, "test", test_integration, "Run an integration test.")
  _add_cmd(parsers, "testall", test_all, "Run all tests.")

  # TODO(goktug): Add presubmit cmd to run various tasks needed before submit.
  # TODO(goktug): Add benchmarking support.
  # TODO(goktug): Add WASM deobfuscation support.
  # TODO(goktug): Add top level filter for different backends (--platform).

  args = base_parser.parse_args()
  if not hasattr(args, "func"):
    base_parser.error("too few arguments")
  args.func(args)
