package com.easyride.admin_service;

import com.easyride.admin_service.dto.AdminUserDto;
import com.easyride.admin_service.model.AdminUser;
import com.easyride.admin_service.model.Role;
import com.easyride.admin_service.repository.AdminUserRepository;
import com.easyride.admin_service.service.AdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class AdminServiceImplTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createAdminUser_Success() {
        // 1. 构造DTO
        AdminUserDto dto = AdminUserDto.builder()
                .username("adminA")
                .password("pass123")
                .role("FINANCE")
                .enabled(true)
                .build();

        // 2. Mock
        when(adminUserRepository.existsByUsername("adminA")).thenReturn(false);
        when(adminUserRepository.save(any(AdminUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // 返回保存的对象

        // 3. 调用 service
        AdminUser createdUser = adminService.createAdminUser(dto);

        // 4. 验证
        verify(adminUserRepository).existsByUsername("adminA");
        verify(adminUserRepository).save(any(AdminUser.class));

        assertEquals("adminA", createdUser.getUsername());
        assertEquals("pass123", createdUser.getPassword());
        assertEquals(Role.FINANCE, createdUser.getRole());
        assertTrue(createdUser.isEnabled());
    }

    @Test
    void createAdminUser_FailWhenUsernameExists() {
        // 1. 构造DTO
        AdminUserDto dto = AdminUserDto.builder()
                .username("duplicateUser")
                .build();

        // 2. Mock
        when(adminUserRepository.existsByUsername("duplicateUser")).thenReturn(true);

        // 3. 执行
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.createAdminUser(dto)
        );
        assertEquals("管理员用户名已存在", ex.getMessage());

        // 4. 验证
        verify(adminUserRepository, never()).save(any(AdminUser.class));
    }

    @Test
    void updateAdminUser_Success() {
        // 1. 构造DTO
        AdminUserDto dto = AdminUserDto.builder()
                .id(123L)
                .username("adminB")
                .password("secure")
                .role("SUPER_ADMIN")
                .enabled(false)
                .build();

        // 2. Mock
        AdminUser existing = AdminUser.builder()
                .id(123L)
                .username("oldUser")
                .password("oldPass")
                .role(Role.OPERATOR)
                .enabled(true)
                .build();
        when(adminUserRepository.findById(123L)).thenReturn(Optional.of(existing));
        when(adminUserRepository.save(any(AdminUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 3. 执行
        AdminUser updated = adminService.updateAdminUser(dto);

        // 4. 验证
        assertEquals("adminB", updated.getUsername());
        assertEquals("secure", updated.getPassword());
        assertEquals(Role.SUPER_ADMIN, updated.getRole());
        assertFalse(updated.isEnabled());
    }

    // More tests for disableAdminUser, interveneOrder, etc.
}
