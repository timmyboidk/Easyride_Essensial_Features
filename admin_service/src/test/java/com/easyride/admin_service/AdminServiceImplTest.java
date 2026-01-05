package com.easyride.admin_service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easyride.admin_service.dto.AdminUserDto;
import com.easyride.admin_service.model.AdminUser;
import com.easyride.admin_service.model.Role;
import com.easyride.admin_service.repository.AdminUserMapper;
import com.easyride.admin_service.rocket.AdminRocketProducer;
import com.easyride.admin_service.service.AdminServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

        @Mock
        private AdminUserMapper adminUserMapper;

        @Mock
        private AdminRocketProducer adminRocketProducer;

        @InjectMocks
        private AdminServiceImpl adminService;

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
                when(adminUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AdminUser>>any())).thenReturn(0L);
                when(adminUserMapper.insert(any(AdminUser.class)))
                                .thenAnswer(invocation -> {
                                        AdminUser user = invocation.getArgument(0);
                                        user.setId(1L); // Simulate ID being set by database
                                        return 1;
                                });

                // 3. 调用 service
                AdminUser createdUser = adminService.createAdminUser(dto);

                // 4. 验证
                verify(adminUserMapper).selectCount(ArgumentMatchers.<LambdaQueryWrapper<AdminUser>>any());
                verify(adminUserMapper).insert(any(AdminUser.class));

                assertNotNull(createdUser);
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
                                .role("FINANCE") // Add role to prevent NPE
                                .build();

                // 2. Mock
                when(adminUserMapper.selectCount(ArgumentMatchers.<LambdaQueryWrapper<AdminUser>>any())).thenReturn(1L);

                // 3. 执行
                RuntimeException ex = assertThrows(RuntimeException.class,
                                () -> adminService.createAdminUser(dto));
                assertEquals("管理员用户名已存在", ex.getMessage());

                // 4. 验证
                verify(adminUserMapper, never()).insert(any(AdminUser.class));
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
                when(adminUserMapper.selectById(123L)).thenReturn(existing);
                when(adminUserMapper.updateById(any(AdminUser.class))).thenReturn(1);

                // 3. 执行
                AdminUser updated = adminService.updateAdminUser(dto);

                // 4. 验证
                assertEquals("adminB", updated.getUsername());
                assertEquals("secure", updated.getPassword());
                assertEquals(Role.SUPER_ADMIN, updated.getRole());
                assertFalse(updated.isEnabled());
                verify(adminUserMapper).updateById(existing);
        }
}
