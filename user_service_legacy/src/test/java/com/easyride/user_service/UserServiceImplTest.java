package com.easyride.user_service;

import com.easyride.user_service.dto.UserRegistrationDto;
import com.easyride.user_service.model.Role;
import com.easyride.user_service.model.User;
import com.easyride.user_service.repository.UserRepository;
import com.easyride.user_service.service.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.security.crypto.password.PasswordEncoder;

public class UserServiceImplTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

	private final UserServiceImpl userService = new UserServiceImpl(userRepository, passwordEncoder);

	@Test
	void registerUser_Success() {
		UserRegistrationDto registrationDto = new UserRegistrationDto();
		registrationDto.setUsername("newuser");
		registrationDto.setPassword("password123");
		registrationDto.setEmail("newuser@example.com");
		registrationDto.setRole("PASSENGER");

		when(userRepository.existsByUsername("newuser")).thenReturn(false);
		when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");

		userService.registerUser(registrationDto);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());

		User savedUser = userCaptor.getValue();
		assertEquals("newuser", savedUser.getUsername());
		assertEquals("encodedPassword", savedUser.getPassword());
		assertEquals("newuser@example.com", savedUser.getEmail());
		assertEquals(Role.PASSENGER, savedUser.getRole());
	}

	@Test
	void registerUser_UsernameExists() {
		UserRegistrationDto registrationDto = new UserRegistrationDto();
		registrationDto.setUsername("existinguser");
		registrationDto.setPassword("password123");
		registrationDto.setEmail("newuser@example.com");
		registrationDto.setRole("PASSENGER");

		when(userRepository.existsByUsername("existinguser")).thenReturn(true);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			userService.registerUser(registrationDto);
		});

		assertEquals("用户名已存在", exception.getMessage());
	}

	@Test
	void registerUser_EmailExists() {
		UserRegistrationDto registrationDto = new UserRegistrationDto();
		registrationDto.setUsername("newuser");
		registrationDto.setPassword("password123");
		registrationDto.setEmail("existing@example.com");
		registrationDto.setRole("PASSENGER");

		when(userRepository.existsByUsername("newuser")).thenReturn(false);
		when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			userService.registerUser(registrationDto);
		});

		assertEquals("邮箱已注册", exception.getMessage());
	}
}
