/*
 * Copyright (c) 2024 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.cKeeper.exceptions;

/**
 * The MCP Connectivity Exception Class.
 *
 * This is a bespoke exception to signify that there is no connectivity with
 * the MCP platform. In a maritime environment where connectivity is not
 * always a given it is important to be able to handle those events
 * gracefully.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class McpConnectivityException extends Exception {
    public McpConnectivityException(String message) {
        super(message);
    }
}
