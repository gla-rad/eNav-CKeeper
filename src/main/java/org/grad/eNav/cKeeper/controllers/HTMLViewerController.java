/*
 * Copyright (c) 2021 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.cKeeper.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * The Home Viewer Controller.
 *
 * This is the home controller that allows user to view the main options.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Controller
public class HTMLViewerController {

    /**
     * The Application Name Information.
     */
    @Value("${gla.rad.ckeeper.info.name.name:Certificate Keeper}")
    private String appName;

    /**
     * The Application Version Information.
     */
    @Value("${gla.rad.ckeeper.info.version:0.0.0}")
    private String appVersion;

    /**
     * The Application Operator Name Information.
     */
    @Value("${gla.rad.ckeeper.info.operatorName:Unknown}")
    private String appOperatorName;

    /**
     * The Application Operator Contact Information.
     */
    @Value("${gla.rad.ckeeper.info.operatorContact:Unknown}")
    private String appOperatorContact;

    /**
     * The Application Operator URL Information.
     */
    @Value("${gla.rad.ckeeper.info.operatorUrl:}")
    private String appOperatorUrl;

    /**
     * The Application Copyright Information.
     */
    @Value("${gla.rad.ckeeper.info.copyright:}")
    private String appCopyright;

    /**
     * The home page of the VDES Controller Application.
     *
     * @param model The application UI model
     * @return The index page
     */
    @GetMapping("/index")
    public String index(Model model) {
        model.addAttribute("appName", this.appName);
        model.addAttribute("appOperatorUrl", this.appOperatorUrl);
        model.addAttribute("appCopyright", this.appCopyright);
        // Return the rendered index
        return "index";
    }

    /**
     * The about page of the VDES Controller Application.
     *
     * @param model The application UI model
     * @return The index page
     */
    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("appName", this.appName);
        model.addAttribute("appVersion", this.appVersion);
        model.addAttribute("appOperatorName", this.appOperatorName);
        model.addAttribute("appOperatorContact", this.appOperatorContact);
        model.addAttribute("appOperatorUrl", this.appOperatorUrl);
        model.addAttribute("appCopyright", this.appCopyright);
        return "about";
    }

    /**
     * Logs the user in an authenticated session and redirect to the home page.
     *
     * @param request The logout request
     * @return The home page
     */
    @GetMapping(path = "/login")
    public ModelAndView login(HttpServletRequest request) {
        return new ModelAndView("redirect:" + "/");
    }

    /**
     * Logs the user out of the authenticated session.
     *
     * @param request The logout request
     * @return The home page
     * @throws ServletException Servlet Exception during the logout
     */
    @GetMapping(path = "/logout")
    public ModelAndView logout(HttpServletRequest request) throws ServletException {
        request.logout();
        return new ModelAndView("redirect:" + "/");
    }

}
