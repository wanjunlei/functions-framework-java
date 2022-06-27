/*
Copyright 2022 The OpenFunction Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package dev.openfunction.functions;

@FunctionalInterface
public interface OpenFunction {

  /**
   * Called to service an incoming event. This interface is implemented by user code to provide the
   * action for a given function.
   *
   * @param context context
   * @param payload incoming event
   * @return Out
   * @throws Exception Exception
   */
  Out accept(Context context, String payload) throws Exception;
}
