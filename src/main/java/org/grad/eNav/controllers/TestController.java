package org.grad.eNav.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class TestController {

    @RequestMapping(value = "/test", method = GET)
    @ResponseBody
    public ResponseEntity<String> getTestString() {

        return new ResponseEntity<>("Success", HttpStatus.OK);
    }

}
