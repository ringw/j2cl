/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.readable.packageprivatemethods.package1;

public abstract class Parent extends SuperParent {
  // This is directly exposed by Child
  @Override
  int foo(int a) {
    return a;
  }

  // This directly exposes SuperParent.bar, not is exposed by Child.
  // There should be a dispatch method for it.
  @Override
  public abstract int bar(int a, int b, int c);
}
