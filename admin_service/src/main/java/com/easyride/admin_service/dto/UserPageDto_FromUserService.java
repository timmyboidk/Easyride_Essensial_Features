package com.easyride.admin_service.dto;

import lombok.Data;
import java.util.List;

// This DTO represents a "page" of user data, including the list of users
// for the current page and pagination details.
@Data
public class UserPageDto_FromUserService {

    private List<UserSummaryDto> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    // We need a summary DTO for the list to avoid sending full details for every user
    @Data
    public static class UserSummaryDto {
        private Long id;
        private String username;
        private String email;
        private String role;
        private boolean enabled;
    }
}