package com.bombel.AccessControl.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;


    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(CreateUserRequest request) {
        if( userRepository.existsByIdentifier(request.getIdentifier())) {
            throw new IllegalArgumentException("Użytkownik z tym identifikatorem istnieje");
        }
        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setIdentifier(request.getIdentifier());
        newUser.setUserRole(request.getRole());

        return userRepository.save(newUser);
    }
}
