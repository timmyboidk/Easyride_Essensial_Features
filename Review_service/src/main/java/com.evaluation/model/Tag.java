package com.evaluation.model;

import jakarta.persistence.*;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签实体类
 */
@Entity
@Table(name = "tags")
@Data
@NoArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 标签名称
    @Column(nullable = false, unique = true)
    private String name;

    // 关联的评价（可选）
    @ManyToMany(mappedBy = "tags")
    private Set<Evaluation> evaluations;
}
