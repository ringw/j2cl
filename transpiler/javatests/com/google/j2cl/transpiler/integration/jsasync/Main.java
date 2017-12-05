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
package com.google.j2cl.transpiler.integration.jsasync;

import static com.google.j2cl.transpiler.integration.jsasync.Promise.await;

import jsinterop.annotations.JsAsync;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;

public class Main {

  private static Promise<Integer> five() {
    return Promise.resolve(5);
  }

  private static Promise<Integer> ten() {
    return Promise.resolve(10);
  }

  @JsAsync
  @JsMethod
  private static Promise<Integer> fifteen() {
    int a = await(five());
    assert a == 5;
    JsIntFunction asyncFunction =
        () -> {
          int ten = await(ten());
          return Promise.resolve(ten);
        };
    int b = await(asyncFunction.get());
    assert b == 10;
    return Promise.resolve(a + b);
  }

  @JsAsync
  public static Promise<?> main(@SuppressWarnings("unused") String... args) {
    int result = await(fifteen());
    assert result == 15;
    result += await(InterfaceWithDefaultMethod.staticAsyncMethod());
    assert result == 20;
    result += await(new InterfaceWithDefaultMethod() {}.defaultAsyncMethod());
    assert result == 30;
    result += same(await(ten()) + await(ten()));
    assert result == 50;
    result += await(ClinitTest.X);
    assert result == 60;
    return Promise.resolve(result);
  }

  private static int same(int i) {
    return i;
  }

  @JsFunction
  private interface JsIntFunction {
    @JsAsync
    Promise<Integer> get();
  }

  private interface InterfaceWithDefaultMethod {
    @JsAsync
    default Promise<Integer> defaultAsyncMethod() {
      int result = await(ten());
      assert result == 10;
      return Promise.resolve(result);
    }

    @JsAsync
    static Promise<Integer> staticAsyncMethod() {
      int result = await(five());
      assert result == 5;
      return Promise.resolve(result);
    }
  }
}
