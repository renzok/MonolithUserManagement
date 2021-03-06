package com.monolith.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.monolith.domain.User;
import com.monolith.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

/**
 * Rest-Controller of the user.
 */
@RestController
@Api(value = "UserController", description = "Rest-Controller of the user.")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    PasswordEncoder passwordEncoder;

    /**
     * Get all users route.
     *
     * @return the list with all users
     */
    @ApiOperation(value = "Get a list with all users.", response = Collection.class)
    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Collection<User>> getUsers() {
        Collection<User> users = userService.findAll();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    /**
     * Get the specific user.
     *
     * @param username the name of the user
     * @return the user with all information
     */
    @ApiOperation(value = "Get the user with the given username.", response = User.class)
    @RequestMapping(value = "/user", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<User> getUser(@RequestParam("username") String username) {
        return ResponseEntity
                .ok()
                .header("Accept", "application/json")
                .body(userService.findUserByUsername(username));
    }

    /**
     * Create new user.
     *
     * @param request with user information
     * @return new user entity
     */
    @ApiOperation(value = "Create a user object and persist it.", response = User.class)
    @RequestMapping(value = "/user/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<User> createUser(HttpServletRequest request) {
        User user = new User();
        user.setUsername(request.getHeader("username"));
        user.setPassword(passwordEncoder.encode(request.getHeader("password")));
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    /**
     * Edit user route.
     *
     * @param userAsJson the user information
     *                   it should contain: oldUsername, newUsername, oldPassword and newPassword.
     * @return JSON-Response of the edited user
     */
    @ApiOperation(value = "Update a user in the database.", response = User.class)
    @RequestMapping(value = "/user/edit", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<User> editPerson(@RequestBody String userAsJson) {
        JsonParser parser = new JsonParser();
        JsonObject user = parser.parse(userAsJson).getAsJsonObject();
        String oldUsername = getValue(user, "oldUsername");
        String newUsername = getValue(user, "newUsername");
        String oldPassword = getValue(user, "oldPassword");
        String newPassword = getValue(user, "newPassword");
        User persistedUser = userService.findUserByUsername(oldUsername);

        // XOR if one of the passwords is given then 400
        if (!oldPassword.isEmpty() ^ !newPassword.isEmpty()) {
            return new ResponseEntity<>((User) null, HttpStatus.BAD_REQUEST);
        }

        if (arePasswordsEmpty(oldPassword, newPassword)) {
            persistedUser.setUsername(newUsername);
        } else {
            if (!passwordEncoder.matches(oldPassword, persistedUser.getPassword())) {
                return new ResponseEntity<>((User) null, HttpStatus.BAD_REQUEST);
            } else {
                persistedUser.setPassword(passwordEncoder.encode(newPassword));
                persistedUser.setUsername(newUsername);
            }
        }

        return new ResponseEntity<>(userService.update(persistedUser), HttpStatus.OK);
    }

    private boolean arePasswordsEmpty(String oldPassword, String newPassword) {
        return newPassword.isEmpty() && oldPassword.isEmpty();
    }

    private String getValue(JsonObject jsonObject, String attributeName) {
        if (jsonObject.get(attributeName) == null || jsonObject.get(attributeName).isJsonNull()) {
            return null;
        } else {
            return jsonObject.get(attributeName).getAsString();
        }
    }

}
