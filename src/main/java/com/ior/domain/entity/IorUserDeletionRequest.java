package com.ior.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ior_user_deletion_requests")
public class IorUserDeletionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String requestReason;

    /** PENDING-待处理, CANCELLED-已取消, COMPLETED-已完成 */
    private String status;

    private LocalDateTime requestedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime completedAt;
}
